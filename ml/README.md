# ml/ — training, evaluation and export

Everything here runs on a laptop CPU (slowly), a Colab free-tier T4 (the classroom path), or a
workstation GPU. Python 3.11+.

## 10-minute start (no data needed)
```bash
# from the repo root
python -m venv .venv
.venv/Scripts/pip install -r ml/requirements.txt        # Windows;  .venv/bin/pip on macOS/Linux
.venv/Scripts/python -m pytest ml/tests -q               # unit tests for preprocessing
.venv/Scripts/python ml/train_dr.py --smoke              # whole pipeline on random tensors, ~1 min
```
The smoke run writes `ml/runs/<timestamp>-smoke/` with every artifact a real run has (config,
metrics, plots, ONNX, model card). It knows nothing about retinas; it only proves the code works.

## Classroom run (APTOS, Colab T4, < 30 min)
1. Get APTOS 2019: `ml/data/download.md` (free Kaggle account).
2. `python ml/make_fixtures.py --data-dir ml/data/aptos` — builds `fixtures/` from real images.
3. `python ml/train_dr.py --small --data-dir ml/data/aptos`
4. `python ml/eval.py --run ml/runs/<timestamp>-small --check-card`

On Colab, clone the repo, upload `kaggle.json`, and run the same commands with `!`. The first epoch
spends a few minutes caching cropped copies of the 3,662 images under `ml/data/aptos/_cache/`
(git-ignored; real pixels, never commit).

## Files
| File | What it does |
|---|---|
| `preprocess.py` | The one definition of image preprocessing (§4.2). Ported to Kotlin in the app. |
| `common.py` | Labels, dataset, `DRModel`, metrics, plots. |
| `train_dr.py` | Train M2. `--small` classroom preset, `--smoke` code check, full mode. |
| `eval.py` | Metrics on the held-out split; `--check-card` verifies the model card within ±0.01. |
| `export_tflite.py` | PyTorch → ONNX → TFLite fp16/int8. Falls back to ONNX-only where TensorFlow can't import. |
| `make_model_card.py` | Writes `model_card.json` (shown in the app's Settings → About models). |
| `make_fixtures.py` | Builds `fixtures/` from APTOS (real images + degraded copies + expected tensors). |
| `data/download.md` | How to obtain every dataset and its license. |
| `experiments/resolution_ablation.py` | Phase 2 (stub). |
| `tests/` | pytest unit tests. Fixture-dependent tests skip when `fixtures/` is empty. |

Phase 2 adds `train_quality.py` (M1) and the full EyePACS run.

## Outputs of a run (`ml/runs/<timestamp>/`)
`config.json` · `split.json` · `history.json` · `metrics.json` · `confusion_matrix.png` · `roc_curve.png`
· `best.pt` · `model.onnx` · `model_fp16.tflite` · `model_int8.tflite` · `export_status.json` · `model_card.json`

To ship a model, copy `model_int8.tflite` and `model_card.json` into `app/app/src/main/assets/models/`.

## The model
`DRModel` = timm backbone (EfficientNet-B3 by default) → global average pool → one linear layer with
6 outputs: 5 grade logits + 1 regression estimate. `referable_prob = P(grade ≥ 2)`. The exported graph
also returns the last feature map, so the app can draw the heat map as a weighted sum of channels
(for a linear head on pooled features, that is exactly Grad-CAM) without any backprop on the phone.

## Rules that apply here (from CLAUDE.md)
- Real images only. Never generate retina images for training, validation, test or fixtures.
- Raw data and caches stay in `ml/data/` (git-ignored). Never commit an image.
- Metrics go in the model card honestly, including the bias section and known gaps.
