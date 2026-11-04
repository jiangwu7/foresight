"""The model that ships inside the APK must load, react to its input, and order the fixtures sensibly.

Background: the first shipped int8 file returned the same output for every image (see
ml/verify_tflite.py and DECISIONS.md 2026-11-04). This test runs the exact asset from
app/app/src/main/assets/models on the real fixture images with the model card's preprocessing and fails
if (a) the file cannot be prepared by the LiteRT runtime, (b) the outputs do not depend on the input, or
(c) grade-0 fixtures are not, on average, less "referable" than grade >= 2 fixtures.

Skips when fixtures are not built on this machine or no TFLite runtime is installed (CI without
ai-edge-litert still runs the other tests).
"""
import json
import sys
from pathlib import Path

import numpy as np
import pytest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "ml"))
import preprocess as pp  # noqa: E402
import verify_tflite as vt  # noqa: E402

ASSETS = ROOT / "app" / "app" / "src" / "main" / "assets" / "models"
MODEL = ASSETS / "dr_m2.tflite"
CARD = ASSETS / "dr_m2_model_card.json"
GRADABLE = ROOT / "fixtures" / "gradable"


def runtime_available() -> bool:
    try:
        import ai_edge_litert  # noqa: F401
        return True
    except Exception:
        try:
            import tensorflow  # noqa: F401
            return True
        except Exception:
            return False


def fixtures() -> list[Path]:
    return sorted(GRADABLE.glob("grade*_*.png")) if GRADABLE.exists() else []


pytestmark = [
    pytest.mark.skipif(not MODEL.exists(), reason="no shipped model in app assets"),
    pytest.mark.skipif(not runtime_available(), reason="no TFLite runtime (pip install ai-edge-litert)"),
    pytest.mark.skipif(len(fixtures()) < 10, reason="fixtures not built; run ml/make_fixtures.py"),
]


def test_shipped_model_loads_and_separates_grades():
    card = json.loads(CARD.read_text())
    cfg = pp.PreprocessConfig.from_dict(card["preprocess"])
    files = fixtures()
    tensors = [pp.preprocess(pp.load_rgb(f), cfg).astype(np.float32) for f in files]
    outputs = vt.run_tflite(MODEL, tensors)                    # raises if the file cannot be prepared
    logits = np.stack([l for l, _ in outputs])
    assert np.ptp(logits, axis=0).max() > 1e-3, "model output does not depend on the input image"

    referable = np.array([vt.softmax(l)[2:].sum() for l in logits])
    grades = np.array([int(f.name[len("grade")]) for f in files])
    low, high = referable[grades == 0], referable[grades >= 2]
    assert len(low) and len(high)
    assert low.mean() < high.mean(), f"grade-0 fixtures ({low.mean():.2f}) not below grade>=2 ({high.mean():.2f})"
    # The shipped model should also get the clear cases right most of the time.
    assert (low < 0.5).mean() >= 0.6, f"only {(low < 0.5).mean():.0%} of grade-0 fixtures below threshold"
    assert (high >= 0.5).mean() >= 0.6, f"only {(high >= 0.5).mean():.0%} of grade>=2 fixtures above threshold"
