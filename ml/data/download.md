# Getting the datasets (spec §4.1)

Raw data is **never committed**. Everything downloads into `ml/data/<name>/`, which `.gitignore`
excludes. All datasets below are free for non-commercial research/education; read each license
when you download it, and record the date you accepted the terms in your lab notebook. If a term
below turns out to be out of date, the license page wins and this file should be fixed.

> Phase 0 only needs **APTOS 2019**. Everything else is for Phase 2 and later.

## 0. One-time Kaggle setup (free account)
1. Create an account at https://www.kaggle.com and verify your phone number (required to download
   competition data).
2. Account → Settings → API → **Create New Token**. This downloads `kaggle.json`.
3. Put it where the CLI looks for it:
   - Windows: `C:\Users\<you>\.kaggle\kaggle.json`
   - macOS/Linux: `~/.kaggle/kaggle.json` then `chmod 600 ~/.kaggle/kaggle.json`
   - Colab: upload it and run `!mkdir -p ~/.kaggle && cp kaggle.json ~/.kaggle/ && chmod 600 ~/.kaggle/kaggle.json`
4. `pip install kaggle` (inside the project `.venv`).
5. For each **competition** dataset you must first open the competition page in a browser and
   click **"Late Submission" / "Join Competition"** to accept its rules, or the download fails
   with a 403.

## 1. APTOS 2019 Blindness Detection — DR grading, 3,662 images (Phase 0, `--small` run)
- Page: https://www.kaggle.com/competitions/aptos2019-blindness-detection
- Terms: Kaggle competition rules (non-commercial, research/education; redistribution not
  permitted — which is why `fixtures/` is built locally and not committed).
- Labels: `train.csv` with `id_code,diagnosis` (0 none, 1 mild, 2 moderate, 3 severe, 4 proliferative).
```bash
kaggle competitions download -c aptos2019-blindness-detection -p ml/data/aptos
cd ml/data/aptos && unzip -q aptos2019-blindness-detection.zip && rm aptos2019-blindness-detection.zip
# Expect: ml/data/aptos/train.csv, ml/data/aptos/train_images/*.png  (~9.5 GB unzipped)
```
Then build the fixtures and run the classroom training:
```bash
.venv/Scripts/python ml/make_fixtures.py --data-dir ml/data/aptos
.venv/Scripts/python ml/train_dr.py --small --data-dir ml/data/aptos
```

## 2. Kaggle Diabetic Retinopathy Detection (EyePACS) — ~88k images (Phase 2 main training set)
- Page: https://www.kaggle.com/competitions/diabetic-retinopathy-detection
- Terms: Kaggle competition rules (non-commercial research). ~88 GB; use the workstation, not Colab.
- Labels: `trainLabels.csv` with `image,level`. Labels are noisy and cameras vary (good for robustness).
```bash
kaggle competitions download -c diabetic-retinopathy-detection -p ml/data/eyepacs
# The archive is split: train.zip.001 ... use 7-Zip:  7z x train.zip.001
```

## 3. Messidor-2 — 1,748 images, external TEST set only (Phase 2)
- Images: register (free) at https://www.adcis.net/en/third-party/messidor2/ — ADCIS emails a
  download link. License: research use, cite Decencière et al. 2014; no redistribution.
- Adjudicated DR grades (Krause et al. 2018) are on Kaggle:
  https://www.kaggle.com/datasets/google-brain/messidor2-dr-grades
```bash
kaggle datasets download -d google-brain/messidor2-dr-grades -p ml/data/messidor2 --unzip
# Put the ADCIS images in ml/data/messidor2/images/
```
**Rule:** Messidor-2 is never used for training or model selection. `eval.py --split messidor2` only.

## 4. IDRiD — DR grades + lesion masks + optic-disc centers (Phase 2 heat-map checks)
- Page: https://ieee-dataport.org/open-access/indian-diabetic-retinopathy-image-dataset-idrid
- License: CC BY 4.0. Free IEEE DataPort account needed to download.
- Put it in `ml/data/idrid/` keeping the original folder names (`A. Segmentation`, `B. Disease Grading`, `C. Localization`).

## 5. DDR — DR grading incl. an "ungradable" class (Phase 2 quality model)
- Page: https://github.com/nkicsl/DDR-dataset (links to Baidu/Google Drive mirrors)
- License: research use, cite Li et al. 2019.
- Put it in `ml/data/ddr/`. The grade `5` = ungradable is what M1 learns from.

## 6. EyeQ / DRIMDB — image-quality labels (Phase 2 quality model)
- EyeQ: https://github.com/HzFu/EyeQ — quality labels (Good/Usable/Reject) for 28,792 EyePACS
  images; you need EyePACS (#2) for the pixels. License: research use.
- DRIMDB: https://doi.org/10.6084/m9.figshare.c.3936936 (figshare, CC BY 4.0), 216 images labeled
  good/bad/outlier.
- Put them in `ml/data/eyeq/` and `ml/data/drimdb/`.

## 7. REFUGE (and ORIGA) — glaucoma disc/cup segmentation (Phase 4 experimental flag)
- REFUGE: https://refuge.grand-challenge.org/ — free grand-challenge.org account; CC BY-NC-ND 4.0.
- ORIGA is no longer publicly distributed; skip unless a partner provides access.
- Put REFUGE in `ml/data/refuge/`.

## 8. DRIVE / HRF — vessel segmentation pretraining (Phase 4 experimental flag)
- DRIVE: https://drive.grand-challenge.org/ (free account; research use).
- HRF: https://www5.cs.fau.de/research/data/fundus-images/ (CC BY 4.0).

## 9. RETFound weights — optional teacher backbone (Phase 2+, research use)
- https://github.com/rmaphoh/RETFound_MAE — fill in the (free) access request form linked from
  the README; weights are released under CC BY-NC 4.0 for research only. Too large for the phone;
  used only for distillation or as an accuracy ceiling.

## Notes on §0.3 ("no accounts beyond free Kaggle/GitHub")
Messidor-2 (ADCIS), IDRiD (IEEE DataPort), REFUGE/DRIVE (grand-challenge.org) and RETFound all need
a free registration. This is logged in `DECISIONS.md`; none of them is needed before Phase 2.

## Folder layout after downloads
```
ml/data/
├── download.md          (this file — the only tracked file in ml/data)
├── aptos/               train.csv, train_images/, test_images/
├── eyepacs/             trainLabels.csv, train/, test/
├── messidor2/           images/, messidor_data.csv
├── idrid/, ddr/, eyeq/, drimdb/, refuge/, drive/, hrf/
```
