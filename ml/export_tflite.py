"""
Export a trained DRModel: PyTorch -> ONNX (always) -> LiteRT/TFLite fp16 + int8 (when TensorFlow is available).

    python ml/export_tflite.py --run ml/runs/<timestamp>

Why two stages: PyTorch cannot write .tflite directly. We export ONNX, check it against PyTorch with
onnxruntime, then convert ONNX -> TensorFlow SavedModel (onnx2tf) -> .tflite. The ONNX step works
everywhere; the TensorFlow step needs a machine where TensorFlow imports (Colab, Linux CI, most
Macs). On a machine where it does not, this script stops after ONNX, writes export_status.json
with status "onnx_only" and prints the Colab command to finish the job. Nothing silently fails.

Layout note: onnx2tf converts the NCHW PyTorch input [1,3,S,S] into TFLite's preferred NHWC
[1,S,S,3]. The app reads `input_layout` from export_status.json / model_card.json.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
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


def convert_tflite(onnx_path: Path, run_dir: Path, calibration=None) -> dict:
    """ONNX -> SavedModel -> fp16 and int8 .tflite. Returns a status dict; never raises for a
    missing TensorFlow (that is an expected environment, not a bug)."""
    try:
        import tensorflow as tf  # noqa: F401
        import onnx2tf  # noqa: F401
    except Exception as e:
        return {"status": "onnx_only",
                "reason": f"{type(e).__name__}: {str(e)[:200]}",
                "how_to_finish": "On Colab: pip install -r ml/requirements.txt && "
                                 f"python ml/export_tflite.py --run {run_dir.as_posix()}"}
    import tensorflow as tf
    import onnx2tf
    saved = run_dir / "saved_model"
    onnx2tf.convert(input_onnx_file_path=str(onnx_path), output_folder_path=str(saved),
                    copy_onnx_input_output_names_to_tflite=True, non_verbose=True)
    status = {"status": "ok", "input_layout": "NHWC"}

    # fp16 (accuracy reference)
    conv = tf.lite.TFLiteConverter.from_saved_model(str(saved))
    conv.optimizations = [tf.lite.Optimize.DEFAULT]
    conv.target_spec.supported_types = [tf.float16]
    fp16_path = run_dir / "model_fp16.tflite"
    fp16_path.write_bytes(conv.convert())
    status["tflite_fp16"] = _file_info(fp16_path)

    # int8 (the shipping model, spec §2.1)
    conv = tf.lite.TFLiteConverter.from_saved_model(str(saved))
    conv.optimizations = [tf.lite.Optimize.DEFAULT]
    if calibration is not None:
        def rep():
            for batch in calibration():
                for img in batch:
                    yield [np.transpose(img[None], (0, 2, 3, 1)).astype(np.float32)]
        conv.representative_dataset = rep
        conv.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        conv.inference_input_type = tf.float32   # keep float I/O so the app's preprocessing is unchanged
        conv.inference_output_type = tf.float32
        status["int8_calibration"] = "representative dataset from validation images"
    else:
        status["int8_calibration"] = "none (dynamic-range quantization only; smoke run)"
    int8_path = run_dir / "model_int8.tflite"
    int8_path.write_bytes(conv.convert())
    status["tflite_int8"] = _file_info(int8_path)
    return status


def _file_info(p: Path) -> dict:
    return {"path": p.name, "bytes": p.stat().st_size, "mb": round(p.stat().st_size / 1e6, 2), "sha256": sha256(p)}


def export_run(model: torch.nn.Module, run_dir: Path, size: int, calibration=None) -> dict:
    run_dir = Path(run_dir)
    onnx_path = run_dir / "model.onnx"
    diff = export_onnx(model, size, onnx_path)
    status = {"onnx": _file_info(onnx_path), "onnx_vs_torch_max_abs_diff": diff,
              "input_shape_onnx": [1, 3, size, size], "outputs": OUTPUT_NAMES}
    status.update(convert_tflite(onnx_path, run_dir, calibration))
    if status["status"] == "onnx_only":
        print("TensorFlow is not usable here; wrote ONNX only. " + status["how_to_finish"])
    else:
        print(f"wrote {status['tflite_int8']['path']} ({status['tflite_int8']['mb']} MB) "
              f"and {status['tflite_fp16']['path']} ({status['tflite_fp16']['mb']} MB)")
    write_json(status, run_dir / "export_status.json")
    return status


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--run", required=True, help="run folder containing best.pt and config.json")
    args = ap.parse_args(argv)
    run_dir = Path(args.run)
    config = json.loads((run_dir / "config.json").read_text())
    ckpt = torch.load(run_dir / "best.pt", map_location="cpu", weights_only=False)
    model = DRModel(config["arch"], pretrained=False)
    model.load_state_dict(ckpt["state_dict"])
    export_run(model, run_dir, config["img_size"])
    import make_model_card
    make_model_card.build(run_dir, model)


if __name__ == "__main__":
    main()
