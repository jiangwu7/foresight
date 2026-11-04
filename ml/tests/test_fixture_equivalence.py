"""Python side of the Python <-> Kotlin preprocessing equivalence test (spec §4.2).

`ml/make_fixtures.py` writes, for each of the first 20 fixture images, the preprocessed tensor to
`fixtures/expected/<name>.bin` (raw float32, little-endian, CHW) plus `fixtures/expected/index.json`.
This test re-runs preprocess.py on the fixture image and checks the result matches the stored
tensor within 1e-3. The Kotlin unit test (app/.../PreprocessEquivalenceTest.kt) loads the same
.bin files and checks the Kotlin port the same way. If both pass, the two implementations agree.

Skips when fixtures have not been built on this machine (they come from APTOS, see
ml/data/download.md, and are never committed).
"""
import json
import sys
from pathlib import Path

import numpy as np
import pytest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "ml"))
import preprocess as pp  # noqa: E402

INDEX = ROOT / "fixtures" / "expected" / "index.json"


def fixtures_built() -> bool:
    """index.json is tracked in git, but the images and tensors are built locally (APTOS terms)."""
    if not INDEX.exists():
        return False
    items = json.loads(INDEX.read_text())["items"]
    return bool(items) and all((ROOT / it["image"]).exists() and (ROOT / it["tensor"]).exists() for it in items)


@pytest.mark.skipif(not fixtures_built(), reason="fixtures not built on this machine; run ml/make_fixtures.py")
def test_fixture_tensors_match_within_tolerance():
    index = json.loads(INDEX.read_text())
    cfg = pp.PreprocessConfig.from_dict(index["preprocess"])
    assert len(index["items"]) >= 20
    for item in index["items"]:
        img = pp.load_rgb(ROOT / item["image"])
        got = pp.preprocess(img, cfg)
        want = np.fromfile(ROOT / item["tensor"], dtype="<f4").reshape(3, cfg.size, cfg.size)
        assert np.max(np.abs(got - want)) < 1e-3, item["image"]
