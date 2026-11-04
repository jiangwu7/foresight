"""Unit tests for ml/preprocess.py.

These use small synthetic ARRAYS (a gray disc on black, gradients, constants) to check the
arithmetic. They are not images of retinas and are never written to disk - see §13.1.
The real-image equivalence test (Python vs Kotlin on fixture images) lives in
test_fixture_equivalence.py and skips when fixtures/ has not been built.
"""
import sys
from pathlib import Path

import numpy as np
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import preprocess as pp  # noqa: E402


def disc_image(h=300, w=400, cx=230, cy=150, r=100, value=180):
    img = np.zeros((h, w, 3), dtype=np.uint8)
    yy, xx = np.mgrid[0:h, 0:w]
    mask = (xx - cx) ** 2 + (yy - cy) ** 2 <= r * r
    img[mask] = (value, value // 2, value // 3)
    return img


def test_bbox_finds_offset_disc():
    img = disc_image()
    x0, y0, x1, y1 = pp.find_fundus_bbox(img)
    assert (x0, y0) == (130, 50)
    assert (x1, y1) == (331, 251)


def test_bbox_dark_frame_returns_full_image():
    img = np.zeros((20, 30, 3), dtype=np.uint8)
    assert pp.find_fundus_bbox(img) == (0, 0, 30, 20)


def test_crop_pad_square_is_square_and_centered():
    img = np.zeros((10, 30, 3), dtype=np.uint8)
    img[2:8, 5:25] = 200  # 6 tall, 20 wide
    sq = pp.crop_pad_square(img, pp.find_fundus_bbox(img))
    assert sq.shape == (20, 20, 3)
    assert sq[7:13, :].min() == 200          # content rows centered: (20-6)//2 = 7
    assert sq[:7].max() == 0 and sq[13:].max() == 0


def test_block_mean_exact():
    x = np.arange(16, dtype=np.uint8).reshape(4, 4)
    y = pp.block_mean_downsample(x, 2)
    assert y.shape == (2, 2)
    assert y[0, 0] == pytest.approx((0 + 1 + 4 + 5) / 4)
    assert y[1, 1] == pytest.approx((10 + 11 + 14 + 15) / 4)


def test_bilinear_constant_image_stays_constant():
    x = np.full((37, 37, 3), 77, dtype=np.uint8)
    y = pp.bilinear_resize(x, 16)
    assert y.shape == (16, 16, 3)
    assert np.allclose(y, 77.0)


def test_bilinear_identity_when_same_size():
    x = np.random.RandomState(0).randint(0, 255, (16, 16, 3)).astype(np.uint8)
    assert np.array_equal(pp.bilinear_resize(x, 16), x.astype(np.float32))


def test_bilinear_linear_gradient_is_preserved():
    # A horizontal ramp stays a ramp under bilinear interpolation (up to edge clipping).
    ramp = np.tile(np.linspace(0, 255, 64, dtype=np.float32)[None, :, None], (64, 1, 3))
    y = pp.bilinear_resize(ramp, 32)
    diffs = np.diff(y[10, :, 0])
    assert np.all(diffs > 0)
    assert np.allclose(diffs[1:-1], diffs[1], atol=1e-3)


def test_resize_portable_shape_and_range():
    img = disc_image(h=1200, w=1600, cx=800, cy=600, r=550)
    sq = pp.crop_pad_square(img, pp.find_fundus_bbox(img))
    small = pp.resize_portable(sq, 512)
    assert small.shape == (512, 512, 3)
    assert 0.0 <= small.min() and small.max() <= 255.0
    assert small[256, 256, 0] == pytest.approx(180.0, abs=1.0)  # center of the disc


def test_normalize_matches_formula():
    img = np.full((4, 4, 3), 255, dtype=np.uint8)
    t = pp.normalize(img, pp.IMAGENET_MEAN, pp.IMAGENET_STD)
    assert t.shape == (3, 4, 4)
    assert t[0, 0, 0] == pytest.approx((1.0 - 0.485) / 0.229, rel=1e-5)
    assert t[2, 3, 3] == pytest.approx((1.0 - 0.406) / 0.225, rel=1e-5)


def test_gaussian_blur_preserves_constant_and_mean():
    x = np.full((20, 20, 3), 100.0, dtype=np.float32)
    assert np.allclose(pp.gaussian_blur(x, 2.0), 100.0, atol=1e-3)
    assert pp.gaussian_kernel_1d(1.5).sum() == pytest.approx(1.0, abs=1e-6)


def test_ben_graham_flat_image_goes_to_128():
    x = np.full((20, 20, 3), 90.0, dtype=np.float32)
    assert np.allclose(pp.ben_graham(x, 2.0), 128.0, atol=1e-3)


def test_full_pipeline_is_deterministic_and_correct_shape():
    img = disc_image(h=700, w=900, cx=450, cy=350, r=300)
    cfg = pp.PreprocessConfig(size=256)
    a = pp.preprocess(img, cfg)
    b = pp.preprocess(img, cfg)
    assert a.shape == (3, 256, 256)
    assert a.dtype == np.float32
    assert np.array_equal(a, b)


def test_config_round_trips_through_json():
    cfg = pp.PreprocessConfig(size=224, mean=(0.1, 0.2, 0.3), std=(0.4, 0.5, 0.6), ben_graham_sigma=None)
    assert pp.PreprocessConfig.from_dict(cfg.to_dict()) == cfg


def test_rejects_grayscale_input():
    with pytest.raises(ValueError):
        pp.preprocess(np.zeros((10, 10), dtype=np.uint8), pp.PreprocessConfig(size=8))
