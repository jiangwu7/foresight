"""Proves a .tflite file actually works before it is shipped (spec §4.3, §8).

Why this exists: on 2026-11-04 the first int8 export (onnx2tf dynamic-range quantization) was shipped
unverified. The desktop LiteRT runtime refuses to load it (per-channel quantization written on the
wrong axis), and the Android runtime loads it silently and returns the *same* output for every image.
Every participant would have been flagged red. Nothing in the export chain had checked that the file
(a) loads, (b) depends on its input, (c) agrees with the ONNX model it came from.

Usage (also called from export_tflite.py after every conversion):
    python ml/verify_tflite.py --tflite ml/runs/<run>/model_int8.tflite --onnx ml/runs/<run>/model.onnx \
        --card ml/runs/<run>/model_card.json

Inputs are the fixture images (fixtures/expected/index.json) preprocessed with the card's config when
they exist on this machine, otherwise random tensors (which still catch "loads" and "input-dependent").
Runtime: ai-edge-litert if installed (works on Windows), else tf.lite.Interpreter (Colab / Linux).
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "ml"))
import preprocess as pp  # noqa: E402

# Tolerances. fp16 should track the ONNX model closely; int8 (per-channel, calibrated) may move logits
# by a few tenths but must keep the decision: same argmax on nearly every input and referable_prob
# within 0.15. These are checked on real fixture images, so they double as a plain-language safety net.
TOL = {
    "fp16": {"referable": 0.05, "argmax_agreement": 0.95},
    "int8": {"referable": 0.15, "argmax_agreement": 0.80},
}


def _interpreter(path: Path):
    try:
        from ai_edge_litert.interpreter import Interpreter
    except Exception:
        import tensorflow as tf
        Interpreter = tf.lite.Interpreter
    it = Interpreter(model_path=str(path), num_threads=4)
    it.allocate_tensors()
    return it


def softmax(z: np.ndarray) -> np.ndarray:
    e = np.exp(z - z.max())
    return e / e.sum()


def run_tflite(path: Path, inputs_nchw: list[np.ndarray]) -> list[tuple[np.ndarray, float]]:
    it = _interpreter(path)
    inp = it.get_input_details()[0]
    outs = it.get_output_details()
    logits_i = next(o["index"] for o in outs if list(o["shape"]) == [1, 5])
    reg_i = next(o["index"] for o in outs if list(o["shape"]) == [1, 1])
    res = []
    for t in inputs_nchw:
        x = np.ascontiguousarray(np.transpose(t, (1, 2, 0))[None].astype(np.float32))  # NHWC
        it.set_tensor(inp["index"], x)
        it.invoke()
        res.append((it.get_tensor(logits_i)[0].copy(), float(it.get_tensor(reg_i)[0][0])))
    return res


def run_onnx(path: Path, inputs_nchw: list[np.ndarray]) -> list[tuple[np.ndarray, float]]:
    import onnxruntime as ort
    sess = ort.InferenceSession(str(path), providers=["CPUExecutionProvider"])
    name = sess.get_inputs()[0].name
    res = []
    for t in inputs_nchw:
        out = sess.run(None, {name: t[None].astype(np.float32)})
        logits = next(r for r in out if r.shape == (1, 5))[0]
        reg = float(next(r for r in out if r.shape == (1, 1))[0][0])
        res.append((logits, reg))
    return res


def fixture_inputs(card: dict, max_images: int = 12) -> tuple[list[np.ndarray], list[str]]:
    index = ROOT / "fixtures" / "expected" / "index.json"
    if not index.exists():
        return [], []
    cfg = pp.PreprocessConfig.from_dict(card["preprocess"])
    tensors, names = [], []
    for item in json.loads(index.read_text())["items"]:
        img = ROOT / item["image"]
        if not img.exists():
            continue
        tensors.append(pp.preprocess(pp.load_rgb(img), cfg).astype(np.float32))
        names.append(img.name)
        if len(tensors) >= max_images:
            break
    return tensors, names


def random_inputs(size: int, n: int = 4, seed: int = 0) -> tuple[list[np.ndarray], list[str]]:
    rng = np.random.default_rng(seed)
    return [rng.standard_normal((3, size, size)).astype(np.float32) for _ in range(n)], [f"random{i}" for i in range(n)]


def verify(tflite: Path, onnx: Path | None, card: dict, kind: str) -> dict:
    """Returns a status dict with an overall "ok". Never raises."""
    tol = TOL.get(kind, TOL["int8"])
    status: dict = {"file": tflite.name, "kind": kind, "ok": False}
    try:
        inputs, names = fixture_inputs(card)
        status["inputs"] = "fixtures" if inputs else "random"
        if not inputs:
            inputs, names = random_inputs(int(card["preprocess"]["size"]))
        status["n_inputs"] = len(inputs)

        tl = run_tflite(tflite, inputs)
        status["loads"] = True
        logits = np.stack([l for l, _ in tl])
        # Input dependence: the spread of logits across different inputs must be clearly non-zero.
        status["input_dependent"] = bool(np.ptp(logits, axis=0).max() > 1e-3)

        if onnx is not None and onnx.exists():
            ox = run_onnx(onnx, inputs)
            o_logits = np.stack([l for l, _ in ox])
            ref_t = np.array([softmax(l)[2:].sum() for l in logits])
            ref_o = np.array([softmax(l)[2:].sum() for l in o_logits])
            status["max_abs_logit_diff"] = float(np.abs(logits - o_logits).max())
            status["max_abs_referable_diff"] = float(np.abs(ref_t - ref_o).max())
            status["argmax_agreement"] = float((logits.argmax(1) == o_logits.argmax(1)).mean())
            status["per_input"] = [
                {"name": n, "tflite_grade": int(a.argmax()), "onnx_grade": int(b.argmax()),
                 "tflite_referable": round(float(softmax(a)[2:].sum()), 4), "onnx_referable": round(float(softmax(b)[2:].sum()), 4)}
                for n, a, b in zip(names, logits, o_logits)
            ]
            agrees = (status["max_abs_referable_diff"] <= tol["referable"]
                      and status["argmax_agreement"] >= tol["argmax_agreement"])
        else:
            status["onnx"] = "not available: only load + input-dependence checked"
            agrees = True
        status["ok"] = bool(status["loads"] and status["input_dependent"] and agrees)
    except Exception as e:  # a model that cannot even be prepared is the main thing we are catching
        status["loads"] = status.get("loads", False)
        status["error"] = f"{type(e).__name__}: {str(e)[:300]}"
    return status


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--tflite", required=True)
    ap.add_argument("--onnx", default=None)
    ap.add_argument("--card", required=True, help="model_card.json (for the preprocess config)")
    ap.add_argument("--kind", default=None, help="fp16 or int8 (default: guessed from the file name)")
    a = ap.parse_args(argv)
    tflite = Path(a.tflite)
    kind = a.kind or ("fp16" if "fp16" in tflite.name else "int8")
    card = json.loads(Path(a.card).read_text())
    status = verify(tflite, Path(a.onnx) if a.onnx else None, card, kind)
    print(json.dumps(status, indent=2))
    return 0 if status["ok"] else 1


if __name__ == "__main__":
    sys.exit(main())
