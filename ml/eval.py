"""
Evaluate a trained run on its held-out split and print the metrics (spec §4.4, §8).

    python ml/eval.py --run ml/runs/<timestamp>                 # PyTorch weights, held-out APTOS split
    python ml/eval.py --run ml/runs/<timestamp> --backend onnx  # the exported ONNX file
    python ml/eval.py --run ml/runs/<timestamp> --check-card    # exit 1 if metrics drift > 0.01 from model_card.json

Phase 2 adds `--split messidor2` for the external test set.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import numpy as np
import torch
from tqdm import tqdm

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import DRModel, FundusDataset, compute_metrics, read_aptos_labels, write_json  # noqa: E402
from preprocess import PreprocessConfig  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
CHECKED_KEYS = ("accuracy", "kappa_quadratic", "referable_sensitivity", "referable_specificity", "referable_auc")


def load_split(run_dir: Path, data_dir: Path):
    config = json.loads((run_dir / "config.json").read_text())
    split = json.loads((run_dir / "split.json").read_text())
    cfg = PreprocessConfig.from_dict(config["preprocess"])
    if split["mode"] == "smoke":
        from train_dr import RandomTensorDataset
        return config, cfg, RandomTensorDataset(16, cfg.size, config["seed"] + 1)
    df = read_aptos_labels(data_dir)
    val_df = df[df["id_code"].isin(set(split["val"]))]
    return config, cfg, FundusDataset(val_df, cfg, augment=False, cache_dir=data_dir / "_cache")


def predict(run_dir: Path, config: dict, ds, backend: str, batch_size: int = 16):
    loader = torch.utils.data.DataLoader(ds, batch_size=batch_size, shuffle=False)
    ys, ps, rs = [], [], []
    if backend == "torch":
        ckpt = torch.load(run_dir / "best.pt", map_location="cpu", weights_only=False)
        model = DRModel(config["arch"], pretrained=False)
        model.load_state_dict(ckpt["state_dict"]); model.eval()
        with torch.no_grad():
            for x, y, _ in tqdm(loader, desc="eval", leave=False):
                logits, reg, _ = model(x)
                ys.append(np.asarray(y)); ps.append(torch.softmax(logits, 1).numpy()); rs.append(reg.squeeze(1).numpy())
    elif backend == "onnx":
        import onnxruntime as ort
        sess = ort.InferenceSession(str(run_dir / "model.onnx"), providers=["CPUExecutionProvider"])
        for x, y, _ in tqdm(loader, desc="eval(onnx)", leave=False):
            for i in range(x.shape[0]):   # the ONNX graph has a fixed batch of 1
                logits, reg, _ = sess.run(None, {"image": x[i:i + 1].numpy()})
                ys.append(np.asarray([int(y[i])])); ps.append(_softmax(logits)); rs.append(reg[:, 0])
    else:
        raise ValueError(backend)
    return np.concatenate(ys), np.concatenate(ps), np.concatenate(rs)


def _softmax(z):
    z = z - z.max(axis=1, keepdims=True)
    e = np.exp(z)
    return e / e.sum(axis=1, keepdims=True)


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--run", required=True)
    ap.add_argument("--data-dir", default=str(ROOT / "ml" / "data" / "aptos"))
    ap.add_argument("--backend", choices=["torch", "onnx"], default="torch")
    ap.add_argument("--check-card", action="store_true", help="compare with model_card.json within +/-0.01")
    args = ap.parse_args(argv)
    run_dir = Path(args.run)
    config, cfg, ds = load_split(run_dir, Path(args.data_dir))
    y, p, r = predict(run_dir, config, ds, args.backend)
    m = compute_metrics(y, p, r)
    print(f"\nheld-out metrics ({args.backend}, n={m['n']}):")
    for k in CHECKED_KEYS:
        print(f"  {k:28s} {m[k]:.4f}")
    print("  confusion matrix (rows = true grade 0..4):")
    for row in m["confusion_matrix"]:
        print("   ", row)
    write_json(m, run_dir / f"eval_{args.backend}.json")
    if args.check_card:
        card = json.loads((run_dir / "model_card.json").read_text())["metrics_heldout"]
        bad = []
        for k in CHECKED_KEYS:
            a, b = m.get(k), card.get(k)
            if a is None or b is None or (np.isnan(a) and np.isnan(b)):
                continue
            if abs(a - b) > 0.01:
                bad.append(f"{k}: eval {a:.4f} vs card {b:.4f}")
        if bad:
            print("MODEL CARD MISMATCH:\n  " + "\n  ".join(bad))
            return 1
        print("model card metrics reproduced within 0.01")
    return 0


if __name__ == "__main__":
    sys.exit(main())
