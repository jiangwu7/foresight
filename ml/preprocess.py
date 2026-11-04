"""
Foresight image preprocessing (spec §4.2).

This file is the ONE place where fundus preprocessing is defined. The Android app ports these
exact steps to Kotlin (app/.../ml/Preprocess.kt) and `fixtures/expected/` holds the tensors this
file produces for the fixture images, so the two implementations can be checked against each
other within 1e-3.

Steps (in order):
  1. find_fundus_bbox   - find the round fundus on its black background, return its bounding box
  2. crop_pad_square    - crop to that box and pad with black to a square
  3. resize_portable    - integer block-mean downsample, then bilinear (half-pixel centers) to NxN
  4. ben_graham         - OPTIONAL local-contrast step (off by default; must match training if on)
  5. normalize          - (pixel/255 - mean) / std per channel, output float32 CHW

Every step is written with plain arithmetic that is easy to port. We deliberately do NOT use
cv2.resize or PIL.resize here because their internals differ from Android's Bitmap scaling and
would break the Python <-> Kotlin equivalence test.

Run on one image:
    python ml/preprocess.py path/to/image.png --size 512 --out out.npy
"""
from __future__ import annotations

import argparse
import json
from dataclasses import dataclass, asdict
from pathlib import Path

import numpy as np

# ImageNet statistics; used until a dataset-specific mean/std is computed by train_dr.py and
# written to model_card.json. The app MUST read mean/std from the model card, never hard-code them.
IMAGENET_MEAN = (0.485, 0.456, 0.406)
IMAGENET_STD = (0.229, 0.224, 0.225)


@dataclass(frozen=True)
class PreprocessConfig:
    """Everything the app needs to reproduce preprocessing. Stored verbatim in model_card.json."""
    size: int = 512
    mean: tuple[float, float, float] = IMAGENET_MEAN
    std: tuple[float, float, float] = IMAGENET_STD
    bbox_threshold: int = 10        # gray level above which a pixel counts as "fundus, not background"
    ben_graham_sigma: float | None = None   # None = step 4 disabled

    def to_dict(self) -> dict:
        d = asdict(self)
        d["mean"] = list(self.mean)
        d["std"] = list(self.std)
        return d

    @staticmethod
    def from_dict(d: dict) -> "PreprocessConfig":
        return PreprocessConfig(
            size=int(d["size"]),
            mean=tuple(float(x) for x in d["mean"]),
            std=tuple(float(x) for x in d["std"]),
            bbox_threshold=int(d.get("bbox_threshold", 10)),
            ben_graham_sigma=(None if d.get("ben_graham_sigma") is None else float(d["ben_graham_sigma"])),
        )


# ----------------------------------------------------------------------------------------------
# Step 1: find the fundus circle
# ----------------------------------------------------------------------------------------------
def find_fundus_bbox(img: np.ndarray, threshold: int = 10) -> tuple[int, int, int, int]:
    """Return (x0, y0, x1, y1) of the fundus; x1/y1 are exclusive.

    A fundus photo is a bright disc on a black background. We take the max over channels, mark
    pixels brighter than `threshold`, and return the bounding box of those pixels. If nothing is
    bright enough (a fully dark frame) we return the whole image so later steps still work.
    """
    if img.ndim == 3:
        bright = img.max(axis=2)
    else:
        bright = img
    mask = bright > threshold
    rows = np.where(mask.any(axis=1))[0]
    cols = np.where(mask.any(axis=0))[0]
    if rows.size == 0 or cols.size == 0:
        return 0, 0, img.shape[1], img.shape[0]
    return int(cols[0]), int(rows[0]), int(cols[-1]) + 1, int(rows[-1]) + 1


# ----------------------------------------------------------------------------------------------
# Step 2: crop and pad to a square
# ----------------------------------------------------------------------------------------------
def crop_pad_square(img: np.ndarray, bbox: tuple[int, int, int, int]) -> np.ndarray:
    """Crop to bbox, then pad the shorter side with black so the result is square (centered)."""
    x0, y0, x1, y1 = bbox
    crop = img[y0:y1, x0:x1]
    h, w = crop.shape[:2]
    side = max(h, w)
    out = np.zeros((side, side) + crop.shape[2:], dtype=crop.dtype)
    oy = (side - h) // 2
    ox = (side - w) // 2
    out[oy:oy + h, ox:ox + w] = crop
    return out


# ----------------------------------------------------------------------------------------------
# Step 3: portable resize
# ----------------------------------------------------------------------------------------------
def block_mean_downsample(img: np.ndarray, k: int) -> np.ndarray:
    """Average every k x k block (exact integer-factor downsample). Trims edge pixels that do not
    fill a whole block. Output dtype is float32."""
    if k <= 1:
        return img.astype(np.float32)
    h, w = img.shape[:2]
    h2, w2 = (h // k) * k, (w // k) * k
    x = img[:h2, :w2].astype(np.float32)
    if x.ndim == 3:
        x = x.reshape(h2 // k, k, w2 // k, k, x.shape[2]).mean(axis=(1, 3))
    else:
        x = x.reshape(h2 // k, k, w2 // k, k).mean(axis=(1, 3))
    return x


def bilinear_resize(img: np.ndarray, size: int) -> np.ndarray:
    """Bilinear resize to size x size with half-pixel centers (same convention as
    `align_corners=False`). Pure numpy; float32 output."""
    src = img.astype(np.float32)
    h, w = src.shape[:2]
    if h == size and w == size:
        return src

    def coords(n_src: int, n_dst: int):
        # destination pixel center -> source coordinate
        pos = (np.arange(n_dst, dtype=np.float32) + 0.5) * (n_src / n_dst) - 0.5
        pos = np.clip(pos, 0.0, n_src - 1.0)
        i0 = np.floor(pos).astype(np.int64)
        i1 = np.minimum(i0 + 1, n_src - 1)
        frac = (pos - i0).astype(np.float32)
        return i0, i1, frac

    y0, y1, fy = coords(h, size)
    x0, x1, fx = coords(w, size)
    if src.ndim == 3:
        fy = fy[:, None, None]
        fx = fx[None, :, None]
    else:
        fy = fy[:, None]
        fx = fx[None, :]
    top = src[y0][:, x0] * (1 - fx) + src[y0][:, x1] * fx
    bottom = src[y1][:, x0] * (1 - fx) + src[y1][:, x1] * fx
    return top * (1 - fy) + bottom * fy


def resize_portable(img: np.ndarray, size: int) -> np.ndarray:
    """Downsample a square image to size x size: block-mean by the largest integer factor that
    keeps the image >= size, then bilinear to exactly size. Returns float32 in 0..255 range."""
    side = img.shape[0]
    k = max(1, side // size)
    x = block_mean_downsample(img, k)
    return bilinear_resize(x, size)


# ----------------------------------------------------------------------------------------------
# Step 4 (optional): Ben Graham local contrast enhancement
# ----------------------------------------------------------------------------------------------
def gaussian_kernel_1d(sigma: float) -> np.ndarray:
    radius = int(np.ceil(3.0 * sigma))
    x = np.arange(-radius, radius + 1, dtype=np.float32)
    k = np.exp(-(x * x) / (2.0 * sigma * sigma))
    return (k / k.sum()).astype(np.float32)


def gaussian_blur(img: np.ndarray, sigma: float) -> np.ndarray:
    """Separable Gaussian blur with edge replication. Float32 in/out."""
    k = gaussian_kernel_1d(sigma)
    r = (len(k) - 1) // 2
    x = img.astype(np.float32)
    pad_width = ((r, r), (0, 0)) + (((0, 0),) if x.ndim == 3 else ())
    xp = np.pad(x, pad_width, mode="edge")
    out = np.zeros_like(x)
    for i, wgt in enumerate(k):
        out += wgt * xp[i:i + x.shape[0]]
    pad_width = ((0, 0), (r, r)) + (((0, 0),) if x.ndim == 3 else ())
    xp = np.pad(out, pad_width, mode="edge")
    out2 = np.zeros_like(x)
    for i, wgt in enumerate(k):
        out2 += wgt * xp[:, i:i + x.shape[1]]
    return out2


def ben_graham(img: np.ndarray, sigma: float) -> np.ndarray:
    """4*img - 4*blur(img) + 128, clipped to 0..255 (the classic Kaggle DR trick)."""
    blur = gaussian_blur(img, sigma)
    out = 4.0 * img.astype(np.float32) - 4.0 * blur + 128.0
    return np.clip(out, 0.0, 255.0)


# ----------------------------------------------------------------------------------------------
# Step 5: normalize to a CHW float32 tensor
# ----------------------------------------------------------------------------------------------
def normalize(img: np.ndarray, mean, std) -> np.ndarray:
    x = img.astype(np.float32) / 255.0
    mean = np.asarray(mean, dtype=np.float32)[None, None, :]
    std = np.asarray(std, dtype=np.float32)[None, None, :]
    x = (x - mean) / std
    return np.ascontiguousarray(x.transpose(2, 0, 1))  # HWC -> CHW


# ----------------------------------------------------------------------------------------------
# The whole pipeline
# ----------------------------------------------------------------------------------------------
def crop_and_resize(img_rgb: np.ndarray, cfg: PreprocessConfig) -> np.ndarray:
    """Steps 1-3 (+4 if enabled). Returns float32 HWC in 0..255. Used by training augmentation,
    which wants a uint8-like image before normalization."""
    bbox = find_fundus_bbox(img_rgb, cfg.bbox_threshold)
    sq = crop_pad_square(img_rgb, bbox)
    small = resize_portable(sq, cfg.size)
    if cfg.ben_graham_sigma is not None:
        small = ben_graham(small, cfg.ben_graham_sigma)
    return small


def preprocess(img_rgb: np.ndarray, cfg: PreprocessConfig) -> np.ndarray:
    """Full pipeline: uint8 RGB HWC image -> float32 CHW tensor of shape (3, size, size)."""
    if img_rgb.ndim != 3 or img_rgb.shape[2] != 3:
        raise ValueError("expected an RGB image with shape (H, W, 3)")
    small = crop_and_resize(img_rgb, cfg)
    return normalize(small, cfg.mean, cfg.std)


def load_rgb(path: str | Path) -> np.ndarray:
    """Read an image file as uint8 RGB (HWC)."""
    import cv2
    bgr = cv2.imread(str(path), cv2.IMREAD_COLOR)
    if bgr is None:
        raise FileNotFoundError(path)
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("image")
    ap.add_argument("--size", type=int, default=512)
    ap.add_argument("--ben-graham-sigma", type=float, default=None)
    ap.add_argument("--out", default=None, help=".npy to write the (3,size,size) float32 tensor to")
    args = ap.parse_args()
    cfg = PreprocessConfig(size=args.size, ben_graham_sigma=args.ben_graham_sigma)
    t = preprocess(load_rgb(args.image), cfg)
    print(json.dumps({"shape": list(t.shape), "min": float(t.min()), "max": float(t.max()),
                      "mean": float(t.mean()), "config": cfg.to_dict()}, indent=2))
    if args.out:
        np.save(args.out, t)


if __name__ == "__main__":
    main()
