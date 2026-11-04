"""
Export a trained DRModel: PyTorch -> ONNX (always) -> LiteRT/TFLite fp16 + int8 (when TensorFlow is available).

    python ml/export_tflite.py --run ml/runs/<timestamp>            # re-export + rebuild the model card
    python ml/export_tflite.py --run ml/runs/<timestamp> --calib 100 # int8 calibrated on 100 validation images

Why two stages: PyTorch cannot write .tflite directly. We export ONNX, check it against PyTorch with
onnxruntime, then convert ONNX -> .tflite with onnx2tf. The ONNX step works everywhere; the TensorFlow step
needs a machine where TensorFlow imports (Colab, Linux CI, most Macs). On a machine where it does not,
this script stops after ONNX, writes export_status.json with status "onnx_only" and prints the Colab
command to finish the job. A failure inside the converter is recorded as status "tflite_failed" with the
error; it never aborts a finished training run.

onnx2tf changed its behaviour across versions: older releases write a TensorFlow SavedModel that we then
quantize with the TFLite converter; newer releases write .tflite files directly ("flatbuffer_direct") and
do their own int8 quantization. Both paths are handled below.

Layout note: onnx2tf converts the NCHW PyTorch input [1,3,S,S] into TFLite's preferred NHWC [1,S,S,3].
The app reads `input_layout` from export_status.json / model_card.json.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
import traceback
from pathlib import Path

import numpy as np
import torch

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import DRModel, write_json  # noqa: E402

OUTPUT_NAMES = ["logits", "reg", "features"]


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def export_onnx(model: torch.nn.Module, size: int, path: Path) -> float:
    """Write ONNX and return the max abs difference between PyTorch and onnxruntime outputs."""
    model = model.eval().cpu()
    dummy = torch.randn(1, 3, size, size)
    try:
        torch.onnx.export(model, (dummy,), str(path), input_names=["image"], output_names=OUTPUT_NAMES,
                          opset_version=18, dynamo=True, external_data=False)
    except Exception as e:  # fall back to the legacy exporter if the dynamo one is unavailable
        print(f"dynamo ONNX export failed ({type(e).__name__}: {str(e)[:120]}); trying legacy exporter")
        torch.onnx.export(model, (dummy,), str(path), input_names=["image"], output_names=OUTPUT_NAMES,
                          opset_version=17, dynamo=False)
    import onnxruntime as ort
    sess = ort.InferenceSession(str(path), providers=["CPUExecutionProvider"])
    with torch.no_grad():
        ref = [t.numpy() for t in model(dummy)]
    out = sess.run(None, {"image": dummy.numpy()})
    return max(float(np.max(np.abs(a - b))) for a, b in zip(ref, out))


def _calibration_npy(calibration, run_dir: Path, max_images: int = 200) -> Path | None:
    """Stack calibration batches [B,3,S,S] into one NHWC .npy for onnx2tf's quantizer."""
    if calibration is None:
        return None
    xs = []
    for batch in calibration():
        xs.append(np.asarray(batch, dtype=np.float32))
        if sum(len(x) for x in xs) >= max_images:
            break
    if not xs:
        return None
    data = np.concatenate(xs)[:max_images]
    data = np.transpose(data, (0, 2, 3, 1)).astype(np.float32)   # NCHW -> NHWC
    path = run_dir / "calibration_nhwc.npy"
    np.save(path, data)
    return path


def _find(folder: Path, suffix: str) -> Path | None:
    hits = sorted(folder.glob(f"*{suffix}"))
    return hits[0] if hits else None


def convert_tflite(onnx_path: Path, run_dir: Path, calibration=None) -> dict:
    """ONNX -> fp16 and int8 .tflite via onnx2tf. Returns a status dict; never raises."""
    try:
        import tensorflow as tf  # noqa: F401
        import onnx2tf  # noqa: F401
    except Exception as e:
        return {"status": "onnx_only",
                "reason": f"{type(e).__name__}: {str(e)[:200]}",
                "how_to_finish": "On Colab: pip install -r ml/requirements-export.txt && "
                                 f"python ml/export_tflite.py --run {run_dir.as_posix()}"}
    import tensorflow as tf
    import onnx2tf

    out_dir = run_dir / "tflite_build"
    shutil.rmtree(out_dir, ignore_errors=True)
    _ensure_onnx2tf_sample_data()
    calib_npy = _calibration_npy(calibration, run_dir)
    status = {"status": "ok", "input_layout": "NHWC", "onnx2tf_version": getattr(onnx2tf, "__version__", "unknown")}

    # --- Step 1: run onnx2tf. Ask for int8 directly when we have calibration data; fall back to the
    #     minimal call if this onnx2tf version does not know the quantization arguments.
    kwargs = dict(input_onnx_file_path=str(onnx_path), output_folder_path=str(out_dir),
                  copy_onnx_input_output_names_to_tflite=True, non_verbose=True)
    try:
        onnx2tf_major = int(str(status["onnx2tf_version"]).split(".")[0])
    except ValueError:
        onnx2tf_major = 2
    # onnx2tf 1.x writes a SavedModel (+ float32/float16 .tflite); we quantize that SavedModel ourselves
    # below with the official TFLite converter and our calibration images. Its built-in quantizer is
    # not used: it downloads a pickled sample set that current NumPy refuses to load. onnx2tf 2.x has
    # no SavedModel, so there we must ask it for the quantized files directly.
    if onnx2tf_major >= 2:
        kwargs["output_dynamic_range_quantized_tflite"] = True
    if calib_npy is not None and onnx2tf_major >= 2:
        # Our calibration tensors are already (pixel/255 - mean) / std, i.e. exactly what the model
        # sees, so onnx2tf's own normalization is the identity: mean 0, std 1.
        kwargs.update(output_integer_quantized_tflite=True, quant_type="per-channel",
                      custom_input_op_name_np_data_path=[["image", str(calib_npy),
                                                          np.zeros((1, 1, 1, 3), np.float32),
                                                          np.ones((1, 1, 1, 3), np.float32)]])
    try:
        onnx2tf.convert(**kwargs)
    except TypeError as e:          # unknown keyword on this version: retry the plain conversion
        print(f"onnx2tf rejected an argument ({e}); retrying without quantization options")
        kwargs = {k: v for k, v in kwargs.items() if k in ("input_onnx_file_path", "output_folder_path",
                                                             "copy_onnx_input_output_names_to_tflite", "non_verbose")}
        onnx2tf.convert(**kwargs)
    except Exception as e:          # quantization itself failed: keep the float models, report honestly
        if calib_npy is None:
            raise
        print(f"onnx2tf int8 quantization failed ({type(e).__name__}: {str(e)[:160]}); keeping float16 + dynamic-range")
        status["int8_error"] = f"{type(e).__name__}: {str(e)[:300]}"
        kwargs.pop("output_integer_quantized_tflite", None); kwargs.pop("quant_type", None)
        kwargs.pop("custom_input_op_name_np_data_path", None)
        shutil.rmtree(out_dir, ignore_errors=True)
        onnx2tf.convert(**kwargs)

    # --- Step 2: collect what it produced.
    fp16_src = _find(out_dir, "_float16.tflite")
    int8_src = _find(out_dir, "_integer_quant.tflite")            # int8 weights+activations, float32 I/O
    saved = out_dir if (out_dir / "saved_model.pb").exists() else None

    if fp16_src is not None:
        shutil.copyfile(fp16_src, run_dir / "model_fp16.tflite")
    elif saved is not None:
        conv = tf.lite.TFLiteConverter.from_saved_model(str(saved))
        conv.optimizations = [tf.lite.Optimize.DEFAULT]
        conv.target_spec.supported_types = [tf.float16]
        (run_dir / "model_fp16.tflite").write_bytes(conv.convert())
    else:
        raise RuntimeError(f"onnx2tf produced neither a float16 .tflite nor a SavedModel in {out_dir}")
    status["tflite_fp16"] = _file_info(run_dir / "model_fp16.tflite")

    if int8_src is not None:
        shutil.copyfile(int8_src, run_dir / "model_int8.tflite")
        status["int8_calibration"] = f"onnx2tf per-channel int8, {np.load(calib_npy).shape[0]} validation images"
    elif saved is not None:
        conv = tf.lite.TFLiteConverter.from_saved_model(str(saved))
        conv.optimizations = [tf.lite.Optimize.DEFAULT]
        if calib_npy is not None:
            data = np.load(calib_npy)
            conv.representative_dataset = lambda: ([x[None]] for x in data)
            conv.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
            conv.inference_input_type = tf.float32
            conv.inference_output_type = tf.float32
            status["int8_calibration"] = f"TFLite converter int8, {data.shape[0]} validation images"
        else:
            status["int8_calibration"] = "none (dynamic-range quantization only)"
        (run_dir / "model_int8.tflite").write_bytes(conv.convert())
    else:
        # No calibration data (smoke run) and no SavedModel: ship the dynamic-range file if present.
        dyn = _find(out_dir, "_dynamic_range_quant.tflite")
        if dyn is None:
            status["int8_calibration"] = "not produced (no calibration data on this onnx2tf version)"
            status["tflite_int8"] = None
            return status
        shutil.copyfile(dyn, run_dir / "model_int8.tflite")
        status["int8_calibration"] = "dynamic-range quantization only (no calibration data)"
    status["tflite_int8"] = _file_info(run_dir / "model_int8.tflite")
    return status


def verify_exports(run_dir: Path, onnx_path: Path, status: dict) -> dict:
    """Run ml/verify_tflite.py on every .tflite we produced. A file that fails is renamed *.BROKEN.tflite
    and dropped from the status so it can never be copied into the app by mistake (2026-11-04 lesson)."""
    import verify_tflite as vt
    card_path = run_dir / "model_card.json"
    card = json.loads(card_path.read_text()) if card_path.exists() else {"preprocess": {"size": 224}}
    checks = {}
    for key, kind in (("tflite_fp16", "fp16"), ("tflite_int8", "int8")):
        info = status.get(key)
        if not info:
            continue
        path = run_dir / info["path"]
        res = vt.verify(path, onnx_path, card, kind)
        checks[kind] = res
        if res["ok"]:
            print(f"verified {path.name}: inputs={res.get('inputs')} argmax_agreement={res.get('argmax_agreement')} "
                  f"max_abs_referable_diff={res.get('max_abs_referable_diff')}")
        else:
            broken = path.with_name(path.stem + ".BROKEN.tflite")
            path.replace(broken)
            status[key] = None
            print(f"REJECTED {path.name} -> {broken.name}: {res.get('error') or res}")
    return checks


def _ensure_onnx2tf_sample_data() -> None:
    """onnx2tf 1.x downloads a 20x128x128x3 float32 sample batch (for its own ONNX-vs-TF sanity check)
    into the current directory; the download now returns an HTML page that NumPy refuses to load.
    It only needs *some* image-like batch, so write a deterministic one ourselves (git-ignored)."""
    f = Path.cwd() / "calibration_image_sample_data_20x128x128x3_float32.npy"
    ok = False
    if f.exists():
        try:
            ok = np.load(f).shape == (20, 128, 128, 3)
        except Exception:
            ok = False
    if not ok:
        np.save(f, np.random.default_rng(0).random((20, 128, 128, 3), dtype=np.float32))


def _file_info(p: Path) -> dict:
    return {"path": p.name, "bytes": p.stat().st_size, "mb": round(p.stat().st_size / 1e6, 2), "sha256": sha256(p)}


def export_run(model: torch.nn.Module, run_dir: Path, size: int, calibration=None) -> dict:
    run_dir = Path(run_dir)
    onnx_path = run_dir / "model.onnx"
    diff = export_onnx(model, size, onnx_path)
    status = {"onnx": _file_info(onnx_path), "onnx_vs_torch_max_abs_diff": diff,
              "input_shape_onnx": [1, 3, size, size], "outputs": OUTPUT_NAMES}
    try:
        status.update(convert_tflite(onnx_path, run_dir, calibration))
    except Exception as e:
        status.update({"status": "tflite_failed", "reason": f"{type(e).__name__}: {str(e)[:300]}",
                       "traceback": traceback.format_exc()[-2000:],
                       "how_to_finish": f"fix the converter, then: python ml/export_tflite.py --run {run_dir.as_posix()}"})
    if status["status"] == "ok":
        status["checks"] = verify_exports(run_dir, onnx_path, status)
    if status["status"] == "onnx_only":
        print("TensorFlow is not usable here; wrote ONNX only. " + status["how_to_finish"])
    elif status["status"] == "tflite_failed":
        print("TFLite conversion FAILED (ONNX is fine): " + status["reason"])
    else:
        f16, i8 = status.get("tflite_fp16"), status.get("tflite_int8")
        if not f16 and not i8:
            status["status"] = "tflite_rejected"
            status["how_to_finish"] = ("every .tflite failed ml/verify_tflite.py (see 'checks'); try another onnx2tf/TensorFlow "
                                       "version, e.g. onnx2tf<2 with tensorflow<2.20, then rerun export_tflite.py")
            print("TFLite conversion produced only BROKEN files (ONNX is fine): " + status["how_to_finish"])
        else:
            print("wrote " + " and ".join(f"{x['path']} ({x['mb']} MB)" for x in (f16, i8) if x)
                  + ("" if f16 and i8 else "  (the other file was rejected: see export_status.json 'checks')"))
    write_json(status, run_dir / "export_status.json")
    return status


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--run", required=True, help="run folder containing best.pt and config.json")
    ap.add_argument("--calib", type=int, default=100, help="validation images for int8 calibration (0 = none)")
    ap.add_argument("--data-dir", default=None, help="dataset folder (defaults to ml/data/aptos)")
    args = ap.parse_args(argv)
    run_dir = Path(args.run)
    config = json.loads((run_dir / "config.json").read_text())
    ckpt = torch.load(run_dir / "best.pt", map_location="cpu", weights_only=False)
    model = DRModel(config["arch"], pretrained=False)
    model.load_state_dict(ckpt["state_dict"])

    calibration = None
    if args.calib > 0 and not config.get("smoke"):
        from eval import load_split, ROOT
        from train_dr import _calibration_batches
        data_dir = Path(args.data_dir) if args.data_dir else ROOT / "ml" / "data" / "aptos"
        _, _, ds = load_split(run_dir, data_dir)
        calibration = _calibration_batches(ds, args.calib, 16)
    export_run(model, run_dir, config["img_size"], calibration=calibration)
    import make_model_card
    make_model_card.build(run_dir, model)


if __name__ == "__main__":
    main()
