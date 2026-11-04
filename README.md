# Foresight

A $100 smartphone retinal **screening** kit built by James Lick High School students (East Side Union High
School District, San José) for Samsung Solve for Tomorrow 2026-27. One 90-second photo of the eye, graded
on the phone, with a plain-language result in English, Spanish or Vietnamese and a warm handoff to a clinic.

> Foresight is a screening tool operated by students. It is not a medical diagnosis. Please see a healthcare provider.

**Start here:** `FORESIGHT_SPEC.md` is the single source of truth. `CLAUDE.md` is the short list of rules.
`DECISIONS.md` is where every choice and deviation is logged. Current phase: **Phase 0 — Foundations**.

## 10-minute quickstart

### A. The ML pipeline (no data needed to try it)
```bash
git clone <this repo> foresight && cd foresight
python -m venv .venv
.venv/Scripts/pip install -r ml/requirements.txt     # Windows;  .venv/bin/pip on macOS/Linux
.venv/Scripts/python -m pytest ml/tests -q
.venv/Scripts/python ml/train_dr.py --smoke          # ~1 minute on CPU
```
Windows users: run these in PowerShell **one line at a time**. PowerShell 5 does not support `&&`, and
`unzip`/`rm` are not Windows commands (use `tar -xf` and `Remove-Item`); `ml/data/download.md` has the
PowerShell versions.
Then look inside `ml/runs/<timestamp>-smoke/`: metrics, plots, an ONNX model and a `model_card.json`.

### B. The classroom run on Colab (real data, < 1 hour)
1. Open a Colab notebook with a **T4 GPU** runtime.
2. `!git clone <this repo> && %cd foresight && pip install -r ml/requirements.txt -r ml/requirements-export.txt`
3. Follow `ml/data/download.md` to get **APTOS 2019** (free Kaggle account).
4. `!python ml/make_fixtures.py --data-dir ml/data/aptos`
5. `!python ml/train_dr.py --small --data-dir ml/data/aptos`  (≈ 20–30 min)
6. `!python ml/eval.py --run ml/runs/<timestamp>-small --check-card`

### C. The Android app
Open `app/` in Android Studio (Ladybug or newer) and run the `app` configuration on a phone. Phase 0 shows
the design-system gallery. From a terminal:
```bash
cd app && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

## Repository map
| Folder | What | Phase |
|---|---|---|
| `ml/` | Preprocessing, training, evaluation, TFLite export, fixtures | 0 ✅ |
| `app/` | Android app (Kotlin, Compose, CameraX, LiteRT). Phase 0: design system + gallery | 0 ✅ |
| `assets/` | Higgsfield-generated media with provenance in `assets/ASSETS.md` | 0 (mood boards, logo concepts) |
| `fixtures/` | Real-image test fixtures, built locally from APTOS | 0 (built by script) |
| `dashboard/` | Static web dashboard reading de-identified exports | 1 |
| `hardware/` | OpenSCAD adapter, BOM, light-safety doc, reference rig | 2 (templates now) |
| `docs/` | Consent forms, screener training, audio manifest, competition figures | 3–4 (templates now) |

## Rules everyone follows
- Real retina images only in the ML pipeline; generated media is for communication and UI.
- Nothing leaves the phone. No analytics, no backups, no uploads.
- Medical wording lives in `strings_medical.xml` and is never edited without clinician review.
- Nobody is imaged until `hardware/SAFETY.md` is complete and signed.
