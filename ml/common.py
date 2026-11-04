"""
Shared pieces for the Foresight ML scripts: labels, dataset, model, metrics, plots.

Kept in one file on purpose so a student can read it top to bottom. Nothing here is clever:
  - read_aptos_labels()    reads train.csv and finds each image file
  - stratified_split()     fixed-seed 80/20 split so every run has the same held-out set
  - FundusDataset          loads an image, runs preprocess.py, (optionally) augments, normalizes
  - DRModel                timm backbone + one linear head with 5 class logits and 1 regression value
  - compute_metrics()      kappa, accuracy, referable-DR sensitivity/specificity/AUC, confusion matrix
  - save_confusion_matrix() / save_roc_curve()   the PNGs each run writes
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pandas as pd
import torch
import torch.nn as nn

from preprocess import PreprocessConfig, crop_and_resize, load_rgb, normalize

GRADE_NAMES = ["none", "mild", "moderate", "severe", "proliferative"]
NUM_GRADES = 5
REFERABLE_MIN_GRADE = 2          # moderate or worse = "referable DR" (spec §3.4, §12)


# ----------------------------------------------------------------------------------------------
# Labels and splits
# ----------------------------------------------------------------------------------------------
def read_aptos_labels(data_dir: str | Path) -> pd.DataFrame:
    """APTOS layout: <data_dir>/train.csv (id_code, diagnosis) + <data_dir>/train_images/<id>.png"""
    data_dir = Path(data_dir)
    csv = data_dir / "train.csv"
    if not csv.exists():
        raise FileNotFoundError(f"{csv} not found. Follow ml/data/download.md first.")
    df = pd.read_csv(csv)
    df["path"] = [str(data_dir / "train_images" / f"{i}.png") for i in df["id_code"]]
    df["source"] = "aptos2019"
    missing = [p for p in df["path"] if not Path(p).exists()]
    if missing:
        raise FileNotFoundError(f"{len(missing)} images listed in train.csv are missing, e.g. {missing[0]}")
    return df[["id_code", "diagnosis", "path", "source"]]


def stratified_split(df: pd.DataFrame, val_frac: float = 0.2, seed: int = 42):
    """Deterministic per-grade split. Returns (train_df, val_df)."""
    rng = np.random.RandomState(seed)
    val_idx = []
    for grade, group in df.groupby("diagnosis"):
        idx = group.index.to_numpy().copy()   # pandas 3 returns a read-only view; shuffle needs a copy
        rng.shuffle(idx)
        n_val = int(round(len(idx) * val_frac))
        val_idx.extend(idx[:n_val].tolist())
    val_mask = df.index.isin(val_idx)
    return df[~val_mask].reset_index(drop=True), df[val_mask].reset_index(drop=True)


# ----------------------------------------------------------------------------------------------
# Dataset
# ----------------------------------------------------------------------------------------------
def build_augmentations(size: int):
    """Augmentations applied to the cropped, resized uint8 image BEFORE normalization.
    Phase 0: the basics. Phase 2 adds adapter-style reflections and vignetting (spec §4.3)."""
    import albumentations as A
    return A.Compose([
        A.HorizontalFlip(p=0.5),
        A.VerticalFlip(p=0.5),
        A.Rotate(limit=180, border_mode=0, p=0.8),
        A.RandomBrightnessContrast(brightness_limit=0.2, contrast_limit=0.2, p=0.7),
        A.HueSaturationValue(hue_shift_limit=5, sat_shift_limit=15, val_shift_limit=10, p=0.3),
    ])


class FundusDataset(torch.utils.data.Dataset):
    """One item = (tensor[3,S,S] float32, grade int, path str).

    Preprocessing a 3000x3000 photo takes ~0.3 s, far too slow to repeat every epoch on Colab's
    CPU, so the cropped+resized uint8 image is cached once under `cache_dir` (inside ml/data/,
    which git ignores). The cache holds real APTOS pixels and must never be committed.
    """

    def __init__(self, df: pd.DataFrame, cfg: PreprocessConfig, augment: bool = False,
                 cache_dir: str | Path | None = None):
        self.df = df.reset_index(drop=True)
        self.cfg = cfg
        self.aug = build_augmentations(cfg.size) if augment else None
        self.cache_dir = Path(cache_dir) if cache_dir else None
        if self.cache_dir:
            self.cache_dir.mkdir(parents=True, exist_ok=True)

    def __len__(self) -> int:
        return len(self.df)

    def _cache_path(self, row) -> Path | None:
        if not self.cache_dir:
            return None
        return self.cache_dir / f"{row.id_code}_{self.cfg.size}.png"

    def load_square_uint8(self, i: int) -> np.ndarray:
        """Steps 1-4 of preprocess.py, rounded to uint8 (so it can be cached as a PNG)."""
        import cv2
        row = self.df.iloc[i]
        cp = self._cache_path(row)
        if cp is not None and cp.exists():
            bgr = cv2.imread(str(cp), cv2.IMREAD_COLOR)
            return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
        small = crop_and_resize(load_rgb(row.path), self.cfg)
        img = np.clip(np.rint(small), 0, 255).astype(np.uint8)
        if cp is not None:
            cv2.imwrite(str(cp), cv2.cvtColor(img, cv2.COLOR_RGB2BGR))
        return img

    def __getitem__(self, i: int):
        img = self.load_square_uint8(i)
        if self.aug is not None:
            img = self.aug(image=img)["image"]
        tensor = torch.from_numpy(normalize(img, self.cfg.mean, self.cfg.std))
        row = self.df.iloc[i]
        return tensor, int(row.diagnosis), row.path


def compute_dataset_mean_std(ds: FundusDataset, max_images: int = 300, seed: int = 0):
    """Per-channel mean/std of pixel/255 over a sample of the TRAINING images only."""
    rng = np.random.RandomState(seed)
    idx = rng.choice(len(ds), size=min(max_images, len(ds)), replace=False)
    s = np.zeros(3, dtype=np.float64)
    s2 = np.zeros(3, dtype=np.float64)
    n = 0
    for i in idx:
        x = ds.load_square_uint8(int(i)).astype(np.float64) / 255.0
        s += x.reshape(-1, 3).sum(axis=0)
        s2 += (x.reshape(-1, 3) ** 2).sum(axis=0)
        n += x.shape[0] * x.shape[1]
    mean = s / n
    std = np.sqrt(np.maximum(s2 / n - mean ** 2, 1e-12))
    return tuple(float(round(m, 4)) for m in mean), tuple(float(round(v, 4)) for v in std)


# ----------------------------------------------------------------------------------------------
# Model
# ----------------------------------------------------------------------------------------------
class DRModel(nn.Module):
    """timm backbone -> feature map -> global average pool -> Linear(C, 6).

    Outputs (all returned, all exported):
      logits   [N,5]        5-class DR grade scores
      reg      [N,1]        regression estimate of the grade (ordinal auxiliary head, spec §4.3)
      features [N,C,h,w]    last conv feature map. Because the head is linear on the pooled
                            features, Grad-CAM for class k equals sum_c W[k,c] * features[c],
                            so the app can draw the heat map from this output plus the head
                            weights (saved in model_card.json) without running backprop.
    """

    def __init__(self, arch: str = "efficientnet_b3", pretrained: bool = True, drop_rate: float = 0.2):
        super().__init__()
        import timm
        self.backbone = timm.create_model(arch, pretrained=pretrained, num_classes=0, global_pool="")
        with torch.no_grad():
            c = self.backbone(torch.zeros(1, 3, 64, 64)).shape[1]
        self.num_features = c
        self.drop = nn.Dropout(drop_rate)
        self.head = nn.Linear(c, NUM_GRADES + 1)

    def forward(self, x):
        features = self.backbone(x)                       # [N,C,h,w]
        pooled = features.mean(dim=(2, 3))                 # [N,C]
        out = self.head(self.drop(pooled))                 # [N,6]
        logits, reg = out[:, :NUM_GRADES], out[:, NUM_GRADES:]
        return logits, reg, features

    def head_weights(self) -> dict:
        w = self.head.weight.detach().cpu().numpy()
        b = self.head.bias.detach().cpu().numpy()
        return {"class_weights": w[:NUM_GRADES].tolist(), "class_bias": b[:NUM_GRADES].tolist(),
                "reg_weights": w[NUM_GRADES].tolist(), "reg_bias": float(b[NUM_GRADES])}


def referable_prob_from_logits(logits: torch.Tensor) -> torch.Tensor:
    """P(grade >= 2) = sum of softmax over moderate, severe, proliferative."""
    return torch.softmax(logits, dim=1)[:, REFERABLE_MIN_GRADE:].sum(dim=1)


# ----------------------------------------------------------------------------------------------
# Metrics
# ----------------------------------------------------------------------------------------------
def compute_metrics(y_true: np.ndarray, probs: np.ndarray, reg: np.ndarray) -> dict:
    """y_true [N] ints, probs [N,5] softmax, reg [N] regression output."""
    from sklearn.metrics import cohen_kappa_score, confusion_matrix, roc_auc_score
    y_true = np.asarray(y_true).astype(int)
    pred_cls = probs.argmax(axis=1)
    pred_reg = np.clip(np.rint(reg), 0, NUM_GRADES - 1).astype(int)
    referable_true = (y_true >= REFERABLE_MIN_GRADE).astype(int)
    referable_prob = probs[:, REFERABLE_MIN_GRADE:].sum(axis=1)
    referable_pred = (referable_prob >= 0.5).astype(int)
    tp = int(((referable_pred == 1) & (referable_true == 1)).sum())
    tn = int(((referable_pred == 0) & (referable_true == 0)).sum())
    fp = int(((referable_pred == 1) & (referable_true == 0)).sum())
    fn = int(((referable_pred == 0) & (referable_true == 1)).sum())
    sens = tp / (tp + fn) if (tp + fn) else float("nan")
    spec = tn / (tn + fp) if (tn + fp) else float("nan")
    try:
        auc = float(roc_auc_score(referable_true, referable_prob))
    except ValueError:            # only one class present (tiny smoke runs)
        auc = float("nan")
    return {
        "n": int(len(y_true)),
        "accuracy": float((pred_cls == y_true).mean()),
        "kappa_quadratic": float(cohen_kappa_score(y_true, pred_cls, weights="quadratic")) if len(set(y_true)) > 1 else float("nan"),
        "kappa_quadratic_regression_head": float(cohen_kappa_score(y_true, pred_reg, weights="quadratic")) if len(set(y_true)) > 1 else float("nan"),
        "referable_sensitivity": float(sens),
        "referable_specificity": float(spec),
        "referable_auc": auc,
        "referable_threshold": 0.5,
        "confusion_matrix": confusion_matrix(y_true, pred_cls, labels=list(range(NUM_GRADES))).tolist(),
        "referable_counts": {"tp": tp, "tn": tn, "fp": fp, "fn": fn},
    }


def save_confusion_matrix(cm, path: str | Path, title: str = "DR grade confusion matrix") -> None:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    cm = np.asarray(cm)
    fig, ax = plt.subplots(figsize=(5, 4.5))
    im = ax.imshow(cm, cmap="Blues")
    ax.set_xticks(range(NUM_GRADES)); ax.set_yticks(range(NUM_GRADES))
    ax.set_xticklabels(GRADE_NAMES, rotation=45, ha="right"); ax.set_yticklabels(GRADE_NAMES)
    ax.set_xlabel("predicted"); ax.set_ylabel("true"); ax.set_title(title)
    for i in range(NUM_GRADES):
        for j in range(NUM_GRADES):
            ax.text(j, i, str(cm[i, j]), ha="center", va="center",
                    color="white" if cm[i, j] > cm.max() / 2 else "black", fontsize=9)
    fig.colorbar(im, ax=ax)
    fig.tight_layout(); fig.savefig(path, dpi=150); plt.close(fig)


def save_roc_curve(y_true, referable_prob, path: str | Path) -> None:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from sklearn.metrics import roc_curve
    y = (np.asarray(y_true) >= REFERABLE_MIN_GRADE).astype(int)
    fig, ax = plt.subplots(figsize=(4.5, 4.5))
    if len(set(y.tolist())) > 1:
        fpr, tpr, _ = roc_curve(y, referable_prob)
        ax.plot(fpr, tpr, lw=2)
    ax.plot([0, 1], [0, 1], "--", color="gray", lw=1)
    ax.set_xlabel("1 - specificity"); ax.set_ylabel("sensitivity"); ax.set_title("Referable DR ROC")
    ax.set_xlim(0, 1); ax.set_ylim(0, 1)
    fig.tight_layout(); fig.savefig(path, dpi=150); plt.close(fig)


def write_json(obj, path: str | Path) -> None:
    Path(path).write_text(json.dumps(obj, indent=2, default=_json_default), encoding="utf-8")


def _json_default(o):
    if isinstance(o, (np.floating, np.integer)):
        return o.item()
    if isinstance(o, np.ndarray):
        return o.tolist()
    raise TypeError(f"not JSON serializable: {type(o)}")
