"""
Build the test fixtures (spec §8) from the REAL APTOS 2019 images on this machine.

    python ml/make_fixtures.py --data-dir ml/data/aptos

Writes (all git-ignored except the two manifests):
    fixtures/gradable/grade<g>_<id>.png        5 real images per DR grade (25), longest side 1024 px
    fixtures/ungradable/<kind>_<id>.png        degraded copies: dark, blur, offcenter, reflection (16)
    fixtures/expected/<name>.bin + index.json  preprocess.py output tensors for the first 20 gradable
                                               images, for the Python <-> Kotlin equivalence test
    fixtures/fixtures_manifest.json            which APTOS ids were used (tracked in git)

Selection is deterministic: sort train.csv by id_code and take the first 5 of each grade, so every
machine builds the same fixtures. The degradations are applied to real photos; nothing here is
generated, and no retina image is ever committed (APTOS terms, see DECISIONS.md).
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import cv2
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import preprocess as pp  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "fixtures"
PER_GRADE = 5
MAX_SIDE = 1024
EXPECTED_N = 20
DEGRADATIONS = ("dark", "blur", "offcenter", "reflection")


def shrink(img: np.ndarray, max_side: int = MAX_SIDE) -> np.ndarray:
    h, w = img.shape[:2]
    s = max_side / max(h, w)
    if s >= 1:
        return img
    return cv2.resize(img, (int(round(w * s)), int(round(h * s))), interpolation=cv2.INTER_AREA)


def degrade(img: np.ndarray, kind: str, rng: np.random.RandomState) -> np.ndarray:
    """Make a real photo ungradable in one of the four ways the app must recognize (spec §3.2)."""
    h, w = img.shape[:2]
    if kind == "dark":
        return np.clip(img.astype(np.float32) * 0.12, 0, 255).astype(np.uint8)
    if kind == "blur":
        return cv2.GaussianBlur(img, (0, 0), sigmaX=0.012 * max(h, w))
    if kind == "offcenter":
        dx = int(0.4 * w)
        out = np.zeros_like(img)
        out[:, dx:] = img[:, :w - dx]
        return out
    if kind == "reflection":
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
        cx, cy = w * (0.5 + rng.uniform(-0.1, 0.1)), h * (0.5 + rng.uniform(-0.1, 0.1))
        sigma = 0.12 * max(h, w)
        blob = np.exp(-((xx - cx) ** 2 + (yy - cy) ** 2) / (2 * sigma ** 2))
        haze = 60.0 * np.exp(-((xx - cx) ** 2 + (yy - cy) ** 2) / (2 * (3 * sigma) ** 2))
        out = img.astype(np.float32) + (230.0 * blob + haze)[:, :, None]
        return np.clip(out, 0, 255).astype(np.uint8)
    raise ValueError(kind)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--data-dir", default=str(ROOT / "ml" / "data" / "aptos"))
    ap.add_argument("--size", type=int, default=512, help="preprocess size for the expected tensors")
    args = ap.parse_args(argv)
    data_dir = Path(args.data_dir)
    import pandas as pd
    df = pd.read_csv(data_dir / "train.csv").sort_values("id_code")

    (FIXTURES / "gradable").mkdir(parents=True, exist_ok=True)
    (FIXTURES / "ungradable").mkdir(parents=True, exist_ok=True)
    (FIXTURES / "expected").mkdir(parents=True, exist_ok=True)
    rng = np.random.RandomState(0)
    manifest = {"source": "APTOS 2019 Blindness Detection (Kaggle), train split",
                "license_note": "Kaggle competition rules: non-commercial research/education; files are built "
                                "locally and never committed.", "max_side": MAX_SIDE, "gradable": [], "ungradable": []}

    gradable_paths = []
    for grade in range(5):
        ids = df[df["diagnosis"] == grade]["id_code"].head(PER_GRADE).tolist()
        for id_code in ids:
            src = data_dir / "train_images" / f"{id_code}.png"
            img = shrink(pp.load_rgb(src))
            name = f"grade{grade}_{id_code}.png"
            cv2.imwrite(str(FIXTURES / "gradable" / name), cv2.cvtColor(img, cv2.COLOR_RGB2BGR))
            manifest["gradable"].append({"file": f"fixtures/gradable/{name}", "id_code": id_code, "diagnosis": grade})
            gradable_paths.append((name, img))

    # 4 degradations x 4 source images (one from each of grades 0..3) = 16 ungradable fixtures
    for k, kind in enumerate(DEGRADATIONS):
        for grade in range(4):
            name, img = gradable_paths[grade * PER_GRADE + k]
            out = degrade(img, kind, rng)
            oname = f"{kind}_{name.split('_', 1)[1]}"
            cv2.imwrite(str(FIXTURES / "ungradable" / oname), cv2.cvtColor(out, cv2.COLOR_RGB2BGR))
            manifest["ungradable"].append({"file": f"fixtures/ungradable/{oname}", "kind": kind, "from": name})

    # expected preprocess tensors for the Kotlin equivalence test
    cfg = pp.PreprocessConfig(size=args.size)
    index = {"preprocess": cfg.to_dict(), "dtype": "float32 little-endian, CHW", "items": []}
    for name, img in gradable_paths[:EXPECTED_N]:
        t = pp.preprocess(img, cfg)
        bin_name = name.replace(".png", f"_{args.size}.bin")
        t.astype("<f4").tofile(FIXTURES / "expected" / bin_name)
        index["items"].append({"image": f"fixtures/gradable/{name}", "tensor": f"fixtures/expected/{bin_name}"})
    (FIXTURES / "expected" / "index.json").write_text(json.dumps(index, indent=2))
    (FIXTURES / "fixtures_manifest.json").write_text(json.dumps(manifest, indent=2))
    print(f"wrote {len(manifest['gradable'])} gradable + {len(manifest['ungradable'])} ungradable fixtures, "
          f"{len(index['items'])} expected tensors")


if __name__ == "__main__":
    main()
