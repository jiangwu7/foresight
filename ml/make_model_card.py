"""
Write model_card.json for a run (spec §4.4). The app shows this card in Settings -> About models,
and the Kotlin preprocessing reads `preprocess` from it, so its shape is part of the app contract.

    python ml/make_model_card.py --run ml/runs/<timestamp>
"""
from __future__ import annotations

import argparse
import json
import sys
from datetime import date
from pathlib import Path

import torch

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import DRModel, GRADE_NAMES, REFERABLE_MIN_GRADE, write_json  # noqa: E402

# Spec §1.3 — fixed wording. Do not edit without clinician review.
DISCLAIMER = ("Foresight is a screening tool operated by students. It is not a medical diagnosis. "
              "Please see a healthcare provider.")

INTENDED_USE = (
    "Screening-grade estimate of diabetic retinopathy severity from a single fundus photograph taken "
    "with the Foresight smartphone adapter, used only to decide whether to recommend a follow-up eye exam. "
    "Output is one of three referral outcomes (green / yellow / red) plus a per-eye grade in plain language. "
    "Not for diagnosis, treatment decisions, or use without a human referral path."
)

LIMITATIONS_BASE = [
    "Trained on public datasets photographed with clinical fundus cameras; phone-adapter images differ in "
    "field of view, illumination and sharpness. Phase 2 adds adapter-style augmentation and a resolution "
    "ablation to quantify this gap.",
    "Public datasets under-represent Latino and Vietnamese populations and rarely record age; bias metrics "
    "are reported only where labels exist.",
    "Ungradable images must be caught by the quality model (M1) before this model runs; this model has no "
    "'ungradable' output and will produce a grade for any input.",
    "Diabetic macular edema is not modeled separately; 'referable' is derived from grade >= moderate only.",
]


def build(run_dir: str | Path, model: torch.nn.Module | None = None) -> Path:
    run_dir = Path(run_dir)
    config = json.loads((run_dir / "config.json").read_text())
    metrics = json.loads((run_dir / "metrics.json").read_text())
    export = json.loads((run_dir / "export_status.json").read_text()) if (run_dir / "export_status.json").exists() else {}
    if model is None:
        ckpt = torch.load(run_dir / "best.pt", map_location="cpu", weights_only=False)
        model = DRModel(config["arch"], pretrained=False)
        model.load_state_dict(ckpt["state_dict"])
    model = model.eval().cpu()

    limitations = list(LIMITATIONS_BASE)
    if config.get("small"):
        limitations.insert(0, "CLASSROOM RUN (--small): 224 px, 3 epochs, APTOS only. Weak by design, for "
                              "learning the pipeline. Not for use with people.")
    if config.get("smoke"):
        limitations.insert(0, "SMOKE RUN: trained on random tensors. Contains no knowledge of retinas at all. "
                              "Exists only to test the code path.")

    files = {k: export[k] for k in ("onnx", "tflite_fp16", "tflite_int8") if k in export}
    card = {
        "name": "foresight-dr-m2",
        "version": f"{config.get('started', 'unknown')}+{config.get('git_sha', 'unknown')}",
        "created": date.today().isoformat(),
        "task": "5-class diabetic retinopathy grading with referable-DR probability",
        "architecture": config["arch"],
        "framework": {"train": f"pytorch {config.get('torch', '')}", "deploy": "LiteRT (TensorFlow Lite)"},
        "input": {
            "description": "one RGB fundus photograph, preprocessed exactly as `preprocess` below",
            "shape_onnx": export.get("input_shape_onnx", [1, 3, config["img_size"], config["img_size"]]),
            "layout_tflite": export.get("input_layout", "NHWC (expected; verify in export_status.json)"),
            "dtype": "float32",
        },
        "preprocess": config["preprocess"],
        "outputs": {
            "logits": {"shape": [1, 5], "classes": GRADE_NAMES,
                       "note": "apply softmax; referable_prob = sum of classes index >= %d" % REFERABLE_MIN_GRADE},
            "reg": {"shape": [1, 1], "note": "auxiliary regression estimate of the grade (0..4)"},
            "features": {"shape": "[1, C, h, w] (ONNX) / [1, h, w, C] (TFLite)", "channels": model.num_features,
                         "note": "last conv feature map; heat map for class k = sum_c head.class_weights[k][c] * features[c]"},
        },
        "head": model.head_weights(),
        "training_data": config.get("sources", {}),
        "training_config": {k: config[k] for k in ("img_size", "epochs", "batch_size", "lr", "seed", "reg_weight", "pretrained") if k in config},
        "metrics_heldout": {
            "split": "stratified 20% of APTOS 2019 train (seed %s)" % config.get("seed") if not config.get("smoke") else "random tensors",
            **{k: metrics[k] for k in ("n", "accuracy", "kappa_quadratic", "kappa_quadratic_regression_head",
                                       "referable_sensitivity", "referable_specificity", "referable_auc",
                                       "referable_threshold") if k in metrics},
            "confusion_matrix": metrics.get("confusion_matrix"),
        },
        "bias_report": {
            "by_source": {src: "see metrics_heldout" for src in config.get("sources", {})},
            "by_age_group": "not available: APTOS has no age labels",
            "known_gaps": "limited images from Latino and Vietnamese populations in public data (spec §6)",
        },
        "targets_from_spec": {"referable_sensitivity": 0.85, "referable_specificity": 0.80,
                              "int8_size_mb_max": 30, "device_latency_s_max": 1.5},
        "intended_use": INTENDED_USE,
        "limitations": limitations,
        "disclaimer": DISCLAIMER,
        "files": files,
        "export_status": export.get("status", "not exported"),
    }
    out = run_dir / "model_card.json"
    write_json(card, out)
    print(f"wrote {out}")
    return out


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--run", required=True)
    args = ap.parse_args(argv)
    build(args.run)


if __name__ == "__main__":
    main()
