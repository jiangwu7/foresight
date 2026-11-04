"""
Train the Foresight DR grading model (M2, spec §4.3) and write a complete run folder.

Classroom run (APTOS only, 224 px, 3 epochs, < 30 min on a Colab T4):
    python ml/train_dr.py --small --data-dir ml/data/aptos

Full run (workstation GPU; Phase 2 adds EyePACS):
    python ml/train_dr.py --data-dir ml/data/aptos --img-size 512 --epochs 20 --arch efficientnet_b3

Code smoke test (no data, random tensors, CPU, ~1 minute; this is what CI runs):
    python ml/train_dr.py --smoke

Every run writes ml/runs/<timestamp>/ containing:
    config.json, split.json, metrics.json, confusion_matrix.png, roc_curve.png, best.pt,
    model.onnx (+ model_fp16.tflite / model_int8.tflite when TensorFlow is available),
    export_status.json, model_card.json
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from tqdm import tqdm

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import (DRModel, FundusDataset, compute_dataset_mean_std, compute_metrics,  # noqa: E402
                    read_aptos_labels, save_confusion_matrix, save_roc_curve, stratified_split,
                    write_json, NUM_GRADES)
from preprocess import IMAGENET_MEAN, IMAGENET_STD, PreprocessConfig  # noqa: E402
import export_tflite  # noqa: E402
import make_model_card  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]

# Some libraries print emoji; on a Windows console with a legacy code page that raises
# UnicodeEncodeError and would abort an otherwise healthy run.
for _stream in (sys.stdout, sys.stderr):
    if hasattr(_stream, "reconfigure"):
        _stream.reconfigure(errors="replace")


def parse_args(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--data-dir", default=str(ROOT / "ml" / "data" / "aptos"))
    ap.add_argument("--runs-dir", default=str(ROOT / "ml" / "runs"))
    ap.add_argument("--small", action="store_true", help="classroom preset: APTOS, 224px, 3 epochs")
    ap.add_argument("--smoke", action="store_true", help="no data; random tensors; tiny model; checks the code path only")
    ap.add_argument("--arch", default="efficientnet_b3", help="any timm model name (efficientnet_b3, convnext_tiny, ...)")
    ap.add_argument("--img-size", type=int, default=512)
    ap.add_argument("--epochs", type=int, default=20)
    ap.add_argument("--batch-size", type=int, default=16)
    ap.add_argument("--lr", type=float, default=3e-4)
    ap.add_argument("--workers", type=int, default=2)
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--val-frac", type=float, default=0.2)
    ap.add_argument("--reg-weight", type=float, default=0.5, help="weight of the regression auxiliary loss")
    ap.add_argument("--ben-graham-sigma", type=float, default=None, help="enable local contrast step (spec §4.2.4)")
    ap.add_argument("--no-pretrained", action="store_true", help="skip ImageNet weights (offline machines)")
    ap.add_argument("--limit", type=int, default=None, help="use only N training images (debugging)")
    ap.add_argument("--int8-calib", type=int, default=100, help="images used to calibrate the int8 export")
    args = ap.parse_args(argv)
    if args.small:
        args.img_size = 224
        args.epochs = 3
        args.batch_size = 32
    if args.smoke:
        args.arch = "mobilenetv3_small_100"
        args.img_size = 64
        args.epochs = 1
        args.batch_size = 8
        args.no_pretrained = True
        args.workers = 0
        args.int8_calib = 4
    return args


class RandomTensorDataset(torch.utils.data.Dataset):
    """Smoke-test stand-in: random noise tensors with random grades. These are NOT images and are
    never written to disk, so §13.1 (no synthetic retinas in the pipeline) is respected."""

    def __init__(self, n: int, size: int, seed: int):
        g = torch.Generator().manual_seed(seed)
        self.x = torch.randn(n, 3, size, size, generator=g)
        self.y = torch.randint(0, NUM_GRADES, (n,), generator=g)

    def __len__(self):
        return len(self.y)

    def __getitem__(self, i):
        return self.x[i], int(self.y[i]), f"random-{i}"


def git_sha() -> str:
    try:
        return subprocess.check_output(["git", "rev-parse", "--short", "HEAD"], cwd=ROOT, text=True).strip()
    except Exception:
        return "unknown"


def run_epoch(model, loader, device, optimizer=None, scaler=None, reg_weight=0.5, desc="", scheduler=None):
    train = optimizer is not None
    model.train(train)
    all_y, all_p, all_r = [], [], []
    total_loss, n = 0.0, 0
    for x, y, _ in tqdm(loader, desc=desc, leave=False):
        x, y = x.to(device, non_blocking=True), y.to(device)
        with torch.set_grad_enabled(train), torch.autocast(device_type=device.type, enabled=(device.type == "cuda")):
            logits, reg, _ = model(x)
            loss = F.cross_entropy(logits, y) + reg_weight * F.smooth_l1_loss(reg.squeeze(1), y.float())
        if train:
            optimizer.zero_grad(set_to_none=True)
            if scaler is not None:
                scaler.scale(loss).backward(); scaler.step(optimizer); scaler.update()
            else:
                loss.backward(); optimizer.step()
            if scheduler is not None:
                scheduler.step()
        total_loss += float(loss) * len(y)
        n += len(y)
        all_y.append(y.cpu().numpy())
        all_p.append(torch.softmax(logits.float(), dim=1).detach().cpu().numpy())
        all_r.append(reg.squeeze(1).float().detach().cpu().numpy())
    y = np.concatenate(all_y); p = np.concatenate(all_p); r = np.concatenate(all_r)
    metrics = compute_metrics(y, p, r)
    metrics["loss"] = total_loss / max(n, 1)
    return metrics, (y, p, r)


def main(argv=None) -> Path:
    args = parse_args(argv)
    torch.manual_seed(args.seed); np.random.seed(args.seed)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    run_dir = Path(args.runs_dir) / (stamp + ("-smoke" if args.smoke else "-small" if args.small else ""))
    run_dir.mkdir(parents=True, exist_ok=False)
    print(f"run dir: {run_dir}   device: {device}")

    # ---- data --------------------------------------------------------------------------------
    cfg = PreprocessConfig(size=args.img_size, ben_graham_sigma=args.ben_graham_sigma)
    if args.smoke:
        train_ds = RandomTensorDataset(32, args.img_size, args.seed)
        val_ds = RandomTensorDataset(16, args.img_size, args.seed + 1)
        split = {"mode": "smoke", "train": [], "val": []}
        sources = {"smoke": {"n_train": 32, "n_val": 16}}
    else:
        df = read_aptos_labels(args.data_dir)
        train_df, val_df = stratified_split(df, args.val_frac, args.seed)
        if args.limit:
            train_df = train_df.iloc[:args.limit]
        cache_dir = Path(args.data_dir) / "_cache"
        probe = FundusDataset(train_df, cfg, augment=False, cache_dir=cache_dir)
        print("preparing images (first run caches the cropped/resized copies) ...")
        for i in tqdm(range(len(probe)), desc="cache train", leave=False):
            probe.load_square_uint8(i)
        mean, std = compute_dataset_mean_std(probe)
        cfg = PreprocessConfig(size=args.img_size, mean=mean, std=std, ben_graham_sigma=args.ben_graham_sigma)
        train_ds = FundusDataset(train_df, cfg, augment=True, cache_dir=cache_dir)
        val_ds = FundusDataset(val_df, cfg, augment=False, cache_dir=cache_dir)
        split = {"mode": "aptos_stratified", "seed": args.seed, "val_frac": args.val_frac,
                 "train": train_df["id_code"].tolist(), "val": val_df["id_code"].tolist()}
        sources = {"aptos2019": {"n_train": len(train_df), "n_val": len(val_df),
                                 "train_grade_counts": train_df["diagnosis"].value_counts().sort_index().tolist(),
                                 "val_grade_counts": val_df["diagnosis"].value_counts().sort_index().tolist()}}
    write_json(split, run_dir / "split.json")

    loader_kw = dict(batch_size=args.batch_size, num_workers=args.workers, pin_memory=(device.type == "cuda"))
    train_loader = torch.utils.data.DataLoader(train_ds, shuffle=True, drop_last=False, **loader_kw)
    val_loader = torch.utils.data.DataLoader(val_ds, shuffle=False, **loader_kw)

    # ---- model -------------------------------------------------------------------------------
    model = DRModel(args.arch, pretrained=not args.no_pretrained).to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-4)
    steps = max(1, args.epochs * len(train_loader))
    scheduler = torch.optim.lr_scheduler.OneCycleLR(optimizer, max_lr=args.lr, total_steps=steps, pct_start=0.15)
    scaler = torch.amp.GradScaler("cuda") if device.type == "cuda" else None

    config = {
        "arch": args.arch, "img_size": args.img_size, "epochs": args.epochs, "batch_size": args.batch_size,
        "lr": args.lr, "seed": args.seed, "reg_weight": args.reg_weight, "pretrained": not args.no_pretrained,
        "small": args.small, "smoke": args.smoke, "preprocess": cfg.to_dict(), "sources": sources,
        "device": str(device), "torch": torch.__version__, "git_sha": git_sha(), "started": stamp,
    }
    write_json(config, run_dir / "config.json")

    # ---- train -------------------------------------------------------------------------------
    best_kappa, best_epoch, history = -2.0, -1, []
    t0 = time.time()
    for epoch in range(args.epochs):
        tr, _ = run_epoch(model, train_loader, device, optimizer, scaler, args.reg_weight,
                          desc=f"train {epoch + 1}/{args.epochs}", scheduler=scheduler)
        va, (y, p, r) = run_epoch(model, val_loader, device, desc=f"val   {epoch + 1}/{args.epochs}")
        score = va["kappa_quadratic"] if not np.isnan(va["kappa_quadratic"]) else va["accuracy"]
        history.append({"epoch": epoch + 1, "train_loss": tr["loss"], "val_loss": va["loss"],
                        "val_kappa": va["kappa_quadratic"], "val_accuracy": va["accuracy"],
                        "val_referable_sens": va["referable_sensitivity"], "val_referable_spec": va["referable_specificity"]})
        print(f"epoch {epoch + 1}: train loss {tr['loss']:.3f} | val loss {va['loss']:.3f} "
              f"kappa {va['kappa_quadratic']:.3f} acc {va['accuracy']:.3f} "
              f"ref sens {va['referable_sensitivity']:.3f} spec {va['referable_specificity']:.3f}")
        if score > best_kappa:
            best_kappa, best_epoch = score, epoch + 1
            torch.save({"state_dict": model.state_dict(), "config": config}, run_dir / "best.pt")
            write_json(va, run_dir / "metrics.json")
            save_confusion_matrix(va["confusion_matrix"], run_dir / "confusion_matrix.png")
            save_roc_curve(y, p[:, 2:].sum(axis=1), run_dir / "roc_curve.png")
    train_minutes = (time.time() - t0) / 60.0
    write_json({"history": history, "best_epoch": best_epoch, "train_minutes": round(train_minutes, 2)}, run_dir / "history.json")
    print(f"best epoch {best_epoch}, trained in {train_minutes:.1f} min")

    # ---- export + model card ---------------------------------------------------------------
    ckpt = torch.load(run_dir / "best.pt", map_location="cpu", weights_only=False)
    model.load_state_dict(ckpt["state_dict"])
    model.to("cpu").eval()
    calib = None if args.smoke else _calibration_batches(val_ds, args.int8_calib, args.batch_size)
    export_tflite.export_run(model, run_dir, args.img_size, calibration=calib)
    make_model_card.build(run_dir, model)
    print(f"done. run folder: {run_dir}")
    return run_dir


def _calibration_batches(ds, n_images: int, batch_size: int):
    """Yields float32 numpy batches [B,3,S,S] from the (un-augmented) validation set for int8 calibration."""
    def gen():
        xs = []
        for i in range(min(n_images, len(ds))):
            x, _, _ = ds[i]
            xs.append(x.numpy())
            if len(xs) == batch_size:
                yield np.stack(xs); xs = []
        if xs:
            yield np.stack(xs)
    return gen


if __name__ == "__main__":
    main()
