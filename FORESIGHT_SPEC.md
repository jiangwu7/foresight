# Foresight — Build Specification for Claude Code

> **Foresight** is a $100 smartphone-based retinal screening kit that spots diabetic retinopathy, flags possible undiagnosed diabetes, and surfaces other eye and cardiometabolic risk signs in one 90-second photo of the eye — built by James Lick High School students (East Side Union High School District, San José, CA) for the Samsung Solve for Tomorrow 2026-27 competition.

This document is the single source of truth for building Foresight. Read it completely before writing code. Where it says **MUST**, treat it as a hard requirement. Where it says **SHOULD**, use judgment and explain deviations in `DECISIONS.md`.

---

## 0. How to work on this project (instructions for Claude Code)

1. **Start by creating the repo skeleton in §9**, a `CLAUDE.md` summarizing this spec's non-negotiables (§1.3, §6), and a `DECISIONS.md` log. Commit early and often with clear messages.
2. **Build in the phase order in §10.** Do not start Phase N+1 until Phase N's acceptance criteria pass. Ask before skipping a phase.
3. **Everything must run on a student laptop and a mid-range Android phone.** No step may require a paid cloud service, a GPU server, or an account beyond free Kaggle/GitHub. If training needs a GPU, write the training script so it runs on Google Colab's free tier or on a local workstation, and document both.
4. **Privacy is a build constraint, not a feature** (§6). If a shortcut would send an image off the device, stop and choose a different design.
5. **Medical language is a build constraint** (§1.3). Every user-facing string that mentions a condition must be checked against the allowed phrasing list. Put all user-facing strings in i18n resource files, never inline.
6. **Write tests as you go.** Each phase lists what must be tested. Prefer small, deterministic tests using the synthetic fixtures in §8.
7. **Keep it understandable by a high school student.** Favor straightforward code and clear READMEs over cleverness. Each module gets a README explaining what it does and how to run it in under 10 minutes.
8. **When something in this spec is impossible or a bad idea, say so** in `DECISIONS.md` with the alternative you chose. Do not silently drop requirements.
9. **Use the connected Higgsfield MCP tools for all visual and video assets** (logo, icons, illustrations, UI onboarding art, 3D renders, explainer animations, pitch-video footage) following §13. Generated media is for communication and UI only — **never** for training data, test fixtures, or anything a participant or clinician could mistake for a real retinal photo or real result.
10. **Aesthetics are a first-class requirement, not polish.** The app must look futuristic, sleek, and professional — the kind of interface a judge photographs — while staying usable by a 70-year-old with poor vision. §3.10 defines the design language; every screen is reviewed against it before its phase is accepted. If a feature works but looks like a default Material template, it is not done.

---

## 1. Product overview

### 1.1 The problem
- Diabetic retinopathy (DR) is the leading cause of blindness in working-age adults. ~85% of diabetics over 40 develop it; vision loss is largely preventable with timely detection.
- Only about half of US diabetics get their recommended annual eye exam; rates are lowest among Medi-Cal patients, farmworkers, and non-English speakers.
- ~1 in 4 US adults with diabetes is undiagnosed. In Santa Clara County, more than 25% of Latino adults over 45 have diabetes, and in September 2025 the county declared a public health crisis for Latino residents, citing elevated diabetes death rates. Most of that population lives in East San José — Foresight's home community.
- Clinical fundus (retinal) cameras cost $5,000–15,000 and live in clinics people never reach.

### 1.2 The solution
A screening kit operated by trained student volunteers at churches, flea markets, Veggielution, school parent nights, and clinics:

- **Hardware:** a 3D-printed adapter holding a 20D condensing lens, a dim deep-red alignment LED, and a low-power white capture LED with polarizers, clamped to a smartphone. Target build cost ≤ $100 including the lens.
- **Mobile app (Android-first):** guides alignment, auto-captures when the retina is in focus and well lit, runs AI models **entirely on-device**, and shows a plain-language result in English, Spanish, or Vietnamese with a visual heat map.
- **Referral:** a printed/QR card with the result and a warm handoff to partner clinics (e.g., Gardner Health, Valley Health Center East Valley) for a confirmatory exam or A1c blood test.
- **Dashboard (web, local-first):** de-identified aggregate stats per screening event — people screened, % never examined, % flagged, referrals completed — producing a map of unmet need.

### 1.3 Non-negotiable medical framing (MUST)
Foresight is a **screening and risk-stratification tool**, not a diagnostic device. These rules govern every string, screen, report, and README:

- **Never** say "you have diabetes," "diagnosis," "you have retinopathy," or "you are healthy."
- **Always** frame results as: *"Your photo shows signs that are often seen in people with ___. We recommend a follow-up exam / blood test."* or *"No signs of ___ were detected in this photo. This does not replace a full eye exam."*
- Every result screen and printed card **MUST** include: "Foresight is a screening tool operated by students. It is not a medical diagnosis. Please see a healthcare provider."
- A **human referral is always in the loop**: every positive flag produces a referral action, never just a label.
- The "possible undiagnosed diabetes" and "cardiometabolic risk" flags are **research outputs** and MUST be labeled "Experimental" in the UI until a partner clinician signs off on their use.
- Ungradable images produce the result "We couldn't get a clear enough photo" and a recommendation to see an eye-care provider — never a negative result.

### 1.4 Users
| User | Needs |
|---|---|
| **Screener** (student volunteer, 15–18) | A flow that cannot be done wrong: big buttons, live guidance, automatic capture, one-tap language switch, offline operation. |
| **Participant** (community member, often 40–80, Spanish/Vietnamese-speaking, may have low literacy or poor vision) | Large text, audio playback of results, plain language, printed card, dignity and privacy. |
| **Partner clinician** (optometrist/FQHC staff) | Access to the images only when the participant consents; clear flag thresholds; exportable referral summary. |
| **Team lead / teacher** | Event setup, device fleet management, de-identified stats, data export for the competition report. |

### 1.5 Success metrics (what the competition report will show)
Primary: people screened; % reporting no eye exam in 2+ years; % flagged referable DR; % flagged "possible diabetes"; referrals completed; confirmed new diagnoses (via partner clinic, with consent).
Technical: gradability rate (% of participants with a gradable image on first attempt, target ≥ 85%); DR model sensitivity/specificity on held-out public data (target ≥ 0.85 / ≥ 0.80 for referable DR); capture-to-result time (target ≤ 90 s per participant, both eyes).

---

## 2. System architecture

```
┌──────────────────────────────┐        ┌───────────────────────────────┐
│  Hardware adapter            │        │  Android app "Foresight"       │
│  20D lens + red LED + white  │──────▶ │  Kotlin · Jetpack Compose      │
│  LED + polarizers + phone    │  photo │  CameraX (+Camera2 interop)    │
│  clamp  (hardware/)          │        │  LiteRT on-device inference    │
└──────────────────────────────┘        │  Room DB (encrypted)           │
                                        │  i18n: en / es / vi            │
                                        └──────────────┬────────────────┘
                                                       │ optional, consented,
                                                       │ de-identified JSON export
        ┌─────────────────────────┐                    ▼
        │  ML pipeline (ml/)      │        ┌───────────────────────────────┐
        │  Python · PyTorch       │        │  Dashboard (dashboard/)        │
        │  public datasets →      │        │  Static web app, reads JSON    │
        │  trained models →       │        │  exports; aggregate stats and  │
        │  .tflite exports        │──────▶ │  maps; no PHI                  │
        │  (ship in app/assets)   │        └───────────────────────────────┘
        └─────────────────────────┘
```

### 2.1 Technology choices (and why)
- **Android, Kotlin, Jetpack Compose, CameraX with Camera2 interop.** The competition is Samsung's; prizes are Samsung devices; the team has Galaxy phones. Camera2 interop is required for manual focus distance, fixed exposure/ISO, and torch control, which cross-platform frameworks expose poorly. *Alternative if the team prefers JS:* Flutter with the `camera` plugin and `tflite_flutter`; document the trade-off in `DECISIONS.md` if chosen.
- **LiteRT (formerly TensorFlow Lite) with the GPU/NNAPI delegate** for on-device inference. Models are exported from PyTorch via ONNX → TFLite (or via `ai-edge-torch`). Int8 quantization is required for the shipping models; keep fp16 variants for accuracy comparison.
- **Room database with SQLCipher** for encrypted local storage.
- **Python 3.11, PyTorch, timm, scikit-learn, albumentations** for training. Training scripts must run on Colab free tier (T4) with a `--small` flag that completes in < 30 minutes for classroom use, and a full mode for the workstation.
- **Dashboard: plain static web app** (vanilla JS or a light framework; no server required). Reads de-identified JSON exports from a local folder or an uploaded file. Must work by opening `index.html`.
- **Hardware CAD in OpenSCAD** so the adapter is parametric (phone width, lens diameter, camera offset) and students can regenerate STLs for any phone.

---

## 3. Mobile app specification

### 3.1 Screens and flow
```
Home → New Event / Resume Event
  └─ Screening session
       1. Language picker (EN/ES/VI; big flags + text; persists)
       2. Consent (plain-language, audio playback, checkboxes; signature optional)
       3. Intake (≤ 6 questions, all optional; see 3.3)
       4. Capture — Right eye
       5. Capture — Left eye
       6. Processing (≤ 5 s target)
       7. Results (per eye + overall; heat map toggle; audio playback)
       8. Referral & card (QR + print via Bluetooth receipt printer or share-to-PDF)
       9. Done → next participant (auto-wipes screen state)
Settings → device check, LED test, model versions, export, clinic list, screener login
```

### 3.2 Capture screen (the core of the app)
**MUST:**
- Full-screen live preview with an overlay ring showing where the retina/optic disc should sit and a traffic-light quality indicator.
- Lock camera to: manual focus at the aerial-image plane (calibrated per phone model in Settings), fixed exposure and ISO, torch **off** (illumination comes from the adapter's LEDs), white balance locked.
- Dim-red alignment mode: the screen itself goes dark red (minimize phone-screen light reaching the pupil), preview shown with gain boost.
- **Auto-capture:** run a lightweight quality model (§4.3) on preview frames at ~5 fps; when the quality score exceeds threshold for 3 consecutive frames, trigger the capture LED via the adapter (see 3.5) and take a full-resolution still, then return to red mode. Manual shutter button always available as fallback.
- Immediate gradability check on the still; if ungradable, show *why* (too dark / blurry / off-center / reflection) with a one-line fix tip, and offer retake (max 3 per eye before "we couldn't get a clear photo").
- Haptic + spoken cues ("Look at the green dot", "Hold still", "Got it") in the selected language, so the screener can watch the participant instead of the screen.
- Gallery import option (Settings → "Import test image") for development and for photos from the Z8 reference rig (§7.3).

### 3.3 Intake questions (all optional, tap-to-answer, large targets)
1. Age range (18–39 / 40–59 / 60+)
2. "Have you ever been told you have diabetes?" (Yes / No / Not sure)
3. "When was your last eye exam?" (Within 1 year / 1–2 years / More than 2 years / Never)
4. "Do you have regular access to a doctor?" (Yes / No)
5. Preferred contact for referral (phone number; optional; stored encrypted, deletable)
6. Consent to share images with partner clinic (Yes / No) — default **No**

### 3.4 Results screen
- Per eye: DR grade (None / Mild / Moderate / Severe / Proliferative mapped to plain language), referable-DR flag (Moderate+ or any DME suspicion), gradability.
- Overall card: one of three outcomes, each with fixed approved wording in all three languages:
  - **Green** — "No signs detected in this photo." + reminder it does not replace an exam.
  - **Yellow** — "Some signs that are worth a closer look." + referral within 3 months.
  - **Red** — "Signs that need a prompt eye exam." + referral within 2–4 weeks, clinic contact shown.
- **Experimental** section (collapsed by default, badge "Experimental"): possible-undiagnosed-diabetes flag (only shown if intake Q2 ≠ Yes), optic-disc cupping suspicion (glaucoma), hypertensive vessel changes. Each with the approved phrasing from §1.3.
- Heat map (Grad-CAM) toggle over the fundus image, with the sentence "Highlighted areas influenced the result" — never "lesion" labels.
- "Read aloud" button for the full result in the selected language (Android TTS).

### 3.5 Adapter control
The adapter's LEDs are driven one of two ways — implement both behind an interface:
- **Mode A (simplest):** LEDs on a manual switch; the app just tells the screener when to press. Works on day one.
- **Mode B:** a tiny microcontroller (e.g., Seeed XIAO / RP2040) with BLE; the app sends `ALIGN_ON`, `CAPTURE_FLASH(ms)`, `OFF`. Flash duration and LED current are capped in firmware per §7.2 and cannot be raised from the app.

### 3.6 Data model (Room)
```
Event(id, name, location_label, date, screener_ids[], notes)
Participant(id, event_id, created_at, language, age_range, dx_diabetes, last_exam, has_doctor,
            contact_encrypted?, consent_share_images, consent_signature_blob?)
Capture(id, participant_id, eye[R/L], image_path_encrypted, quality_score, gradable, retake_n,
        device_model, focus_cal, created_at)
Inference(id, capture_id, model_name, model_version, dr_grade, dr_probs[5], referable_prob,
          experimental_json, heatmap_path?, runtime_ms)
Referral(id, participant_id, outcome[green/yellow/red], clinic_id?, card_printed, follow_up_status)
```
Images and contact info are AES-256 encrypted at rest (Android Keystore). Default retention: images auto-delete 30 days after an event unless the participant consented to share; aggregates are kept.

### 3.7 Export
Settings → Export produces **two separate files**:
- `foresight_export_<event>_deid.json` — de-identified aggregates and per-participant rows with no images, no contact, no free text, ages bucketed. This is what the dashboard reads.
- `foresight_export_<event>_clinic.zip` — only participants who consented; images + result summary; password-protected; for hand-off to the partner clinic. Never auto-uploaded.

### 3.8 Internationalization
All strings in `res/values/strings.xml`, `values-es/`, `values-vi/`. Medical result strings live in a **separate file `strings_medical.xml`** per language with a comment header stating they are approved wording and must not be edited without clinician review. Numbers, dates, and TTS locale follow the selected language, not the phone locale.

### 3.9 Accessibility
Minimum 18sp body text, 48dp touch targets, TalkBack labels on every control, high-contrast theme, results readable aloud. The participant may have very poor vision — design for it. Accessibility and the design language in §3.10 are not in tension: large type, generous spacing, and high contrast *are* the sleek look.

### 3.10 Visual design language (MUST)
**Brief:** futuristic, sleek, professional. The feel of a precision optical instrument — calm, dark, luminous — not a sci-fi movie prop and not a hospital form. A judge should want to photograph the screen; a grandmother should find it calming and obvious.

**Principles**
1. **Dark, luminous, quiet.** Default theme is near-black (not pure #000) with a single luminous accent drawn from the brand (§13.2) and the retina's own palette (deep reds, amber, the optic disc's warm glow). Light is the metaphor: thin glowing rings, soft gradients, subtle vignettes. The capture screen is already dark red for optical reasons — make the whole app feel like it belongs to that moment.
2. **Few elements, large.** One primary action per screen. Oversized type (headline ≥ 32sp, body ≥ 18sp), wide margins, 48–64dp targets. Negative space is the main design material.
3. **Precision details.** Thin hairlines, concentric rings and reticles echoing the alignment overlay, monospaced numerals for measurements and timers, micro-labels in small caps. Everything aligned to an 8dp grid.
4. **Motion with purpose.** Smooth, physics-based transitions (Compose `animate*` / shared-element transitions); the alignment ring breathes while waiting, snaps and glows on capture, the result card unfolds. Nothing bounces or decorates; every animation communicates state. Respect the system "reduce motion" setting.
5. **Results stay humane.** Green/yellow/red are desaturated and paired with icons and words (§3.4); the red state is serious, not alarming — no flashing, no alarm color floods.
6. **Consistent across surfaces.** The app, the dashboard, the printed card, the pitch video and the adapter's physical finish (matte black with one accent) share the same palette, type and ring motif.

**Implementation**
- Build a single Compose design system module (`ui/theme/`): color tokens (dark default + a light fallback that passes WCAG AA), typography scale (Inter/Noto Sans or Be Vietnam Pro for full Vietnamese diacritics; a monospaced numeral face), shape tokens, elevation/glow styles, motion specs. No ad-hoc colors or sizes anywhere else — lint for it.
- Components: `GlowRing`, `InstrumentCard`, `BigAction`, `StatusPill`, `MetricReadout`, `LanguageTile`, `ResultCard`, `HeatmapOverlay`. Each has a Compose preview and a screenshot test.
- **Higgsfield's role (§13.3a):** generate mood boards, high-fidelity concept mockups of every screen, backgrounds, iconography and ring/reticle motifs that the team reviews and selects *before* UI is coded; then the UI is **built in Compose to match**, not shipped as images. Generated backgrounds/illustrations are exported as assets; layout, type and controls are always real, accessible components.
- **Design review gate:** each phase's acceptance includes a side-by-side of the running app against the approved mockups, checked by a teammate who did not build the screen, on a 6.1" phone at arm's length and under a dark room's lighting (the real screening condition).

**Anti-patterns (reject on sight):** default Material purple/teal, stock icons, system dialogs for key flows, dense forms, light-gray-on-white text, neon rainbow gradients, HUD clutter, fake "scanning" effects over the retina image, anything that looks like a template.

---

## 4. Machine learning specification (`ml/`)

### 4.1 Datasets (public, free, non-commercial research use)
| Dataset | Use | Notes |
|---|---|---|
| **APTOS 2019** (Kaggle) | DR grading, 3,662 imgs, 5 grades | Small; good for the `--small` classroom run |
| **Kaggle Diabetic Retinopathy Detection (EyePACS)** | DR grading, ~88k imgs | Main training set; noisy labels; varied cameras — good for robustness |
| **Messidor-2** | DR external validation, 1,748 imgs | Requires registration; use as a held-out test set only |
| **IDRiD** | DR + lesion segmentation + optic disc location | For heat-map sanity checks and disc detection |
| **DDR** | DR grading incl. "ungradable" label | Train the quality/gradability model |
| **DRIMDB / EyeQ** | Image quality labels | Quality model |
| **REFUGE / ORIGA** | Glaucoma, disc/cup segmentation | Experimental glaucoma flag |
| **RETFound** weights (GitHub, research use) | Optional teacher backbone (ViT-L pretrained on 1.6M retinal images) | Too large for phone; use for distillation or as an accuracy ceiling |

`ml/data/download.md` MUST document exactly how to obtain each dataset (Kaggle CLI commands, registration links) and the license terms. Raw data is never committed. Note: the "diabetes from a healthy-looking eye" task (§4.5) needs images from confirmed **non-diabetic** people, which these datasets largely lack — see that section.

### 4.2 Preprocessing (shared by training and the app — implement once in Python, port exactly to Kotlin, and test equivalence)
1. Detect the circular fundus region; crop to its bounding box; pad to square.
2. Resize to 512×512 (DR model) / 256×256 (quality model).
3. Normalize per-channel with dataset mean/std stored in `model_card.json`.
4. Optional local contrast enhancement (Ben Graham–style Gaussian subtraction) — if used in training, MUST be used identically on device.
Equivalence test: 20 fixture images processed in Python and in Kotlin must match within a tolerance of 1e-3 after normalization.

### 4.3 Models
**M1 — Quality / gradability (runs on live preview).**
MobileNetV3-Small, 256×256, outputs: `gradable_prob`, and a 4-way "problem" head (dark, blurry, off-center, reflection). Target: ≥ 25 fps on a mid-range phone with int8. Also outputs an approximate optic-disc center (regression head) for the alignment overlay.

**M2 — Diabetic retinopathy grading (primary).**
EfficientNet-B3 (or ConvNeXt-Tiny), 512×512, 5-class ordinal output with a regression auxiliary head; derive `referable_prob` = P(grade ≥ 2). Train on EyePACS + APTOS with heavy augmentation (rotation, flips, brightness/contrast, simulated reflections and vignetting that mimic the phone adapter). Evaluate on Messidor-2 and a held-out APTOS split. Report quadratic-weighted kappa, sensitivity/specificity for referable DR, and AUC. Must ship ≤ 30 MB int8 and run ≤ 1.5 s per image on device.

**M3 — Experimental flags (Phase 4 only).**
- Glaucoma suspicion: disc/cup segmentation (REFUGE) → vertical cup-to-disc ratio; flag if > 0.6.
- Hypertensive changes: arteriole/venule ratio from vessel segmentation (DRIVE/HRF for pretraining) — flag if AVR < 0.66. Clearly experimental.
- Diabetes-from-clean-retina: see §4.5.

**Explainability.** Grad-CAM for M2 on the last conv block, exported as a 16×16 heat map upsampled on device. Fixture test: heat maps on IDRiD images with known lesion masks should overlap lesions above a chance baseline (report IoU in the model card).

### 4.4 Training scripts and reproducibility
- `ml/train_quality.py`, `ml/train_dr.py`, `ml/eval.py`, `ml/export_tflite.py`, `ml/make_model_card.py`.
- Every run writes `runs/<timestamp>/` with config, metrics JSON, confusion matrices, ROC curves (PNG), and the exported `.tflite`.
- `--small` flag: APTOS only, 224px, 3 epochs, finishes on Colab T4 in < 30 minutes. This is the version students run in class.
- `model_card.json` per shipped model: version, training data, metrics, preprocessing constants, intended use, limitations, and the §1.3 disclaimer. The app shows the card in Settings → About models.

### 4.5 Research track: "possible undiagnosed diabetes" from retinas without visible DR
Published work reports AUC ≈ 0.83–0.86 for detecting diabetes from fundus photos that show no retinopathy — a screening-grade signal, not a diagnosis. The blocker is data: public DR datasets come almost entirely from diabetics. Plan:
1. Phase 2: build the training harness using DR grade-0 images labeled "diabetic" vs. any available non-diabetic fundus sets (e.g., healthy controls from glaucoma/AMD datasets), and measure the (optimistic, confounded) AUC honestly.
2. Phase 4: pursue a data-sharing agreement with a partner clinic or EyePACS for de-identified images with diabetes status; retrain; re-evaluate.
3. Also implement the **"retinal age gap"** regression (predict age from fundus; report predicted − actual) as a secondary experimental signal, since age labels exist in several public sets.
Until step 2 is done, this flag ships only behind the "Experimental" badge and a Settings toggle that defaults **off**.

### 4.6 Resolution experiment (for the competition report)
`ml/experiments/resolution_ablation.py`: take the Messidor-2 test set, progressively degrade it (downsample to 1024/512/384/256/128 px; add Gaussian blur σ = 1–4; add synthetic corneal reflection and vignetting), and plot M2 sensitivity/specificity vs. degradation. This quantifies how good the $100 adapter's photos need to be and is a key figure in the application.

---

## 5. Dashboard specification (`dashboard/`)

Static web app; opening `index.html` works offline. Loads one or more `*_deid.json` exports.
- **Event summary cards:** screened, gradability rate, % never/2+ years since exam, % yellow, % red, % experimental flags, referrals printed, follow-ups completed.
- **Map:** events plotted by location label (hand-entered lat/lng per event in Settings), colored by flag rate.
- **Trend:** screened per event over time; cumulative totals.
- **Export:** CSV of the aggregate table; PNG of each chart for the competition slides.
- MUST contain no names, contact info, images, or free text. Enforce by schema validation on load: reject files containing disallowed fields.

---

## 6. Privacy, security, and ethics (MUST)

- **On-device inference only.** No image leaves the phone unless the participant ticked "share with clinic," and then only via the password-protected clinic export, handed over in person.
- **No analytics SDKs, no crash reporters that upload images, no cloud backup of the app's data directory** (`android:allowBackup="false"`, exclude from auto-backup).
- Encryption at rest for images and contact info (Android Keystore + SQLCipher). Screener login (PIN) required to open the app; auto-lock after 5 minutes idle.
- Participant can withdraw at any time: a "Delete my record" action on the results screen removes images, contact info, and inference results, keeping only an anonymous count.
- Retinal images are biometric identifiers. The consent text (in all three languages) MUST say this plainly: *"A photo of your retina is unique to you, like a fingerprint."*
- Before any screening of real people: written consent form reviewed by the district; a partnering optometrist/clinician reviews the illumination design (§7.2) and the result wording (§1.3); check whether the activity needs IRB review if data will be published.
- Minors are **not** screened in the pilot. The app refuses to proceed if age range "Under 18" is selected (include that option only to catch it).
- Bias check: report M2 metrics separately by image source/camera and, where labels exist, by age group; include in the model card. Document known gaps (e.g., limited images from Vietnamese or Latino populations in public data) honestly.

---

## 7. Hardware specification (`hardware/`)

### 7.1 Adapter design (OpenSCAD, parametric)
- Phone clamp (parameter: phone width, camera module offset from edge).
- Lens holder for a **20D Volk-style aspheric condensing lens** (parameter: lens diameter, nominally 50 mm; 28D or 40D variants for smaller pupils / wider field).
- Lens-to-phone-camera distance adjustable (the aerial image forms ≈ 50 mm in front of the 20D lens; the phone camera focuses on that plane — typically 60–120 mm from the aerial image depending on the phone's minimum focus distance). Provide a sliding rail with a locking screw.
- Illumination module: a **deep-red (≈ 660 nm) low-power LED** for alignment and a **warm-white LED** for capture, both routed through a 45° glass beamsplitter (or a half-mirror slide) so the light enters the pupil coaxially; **linear polarizer** on the LED path and a crossed polarizer on the camera path to suppress corneal reflection.
- Soft eyecup / forehead rest so the participant can steady against it; chin-rest clamp for tabletop use.
- Bill of materials with sources and prices, target total ≤ $100 (lens ≈ $150 new is the exception; list used/educational sources and note the realistic BOM both ways).

### 7.2 Light safety (MUST)
- LED currents are hard-limited in hardware (series resistor) and, in Mode B, firmware; the app cannot increase them.
- Capture flash ≤ 50 ms. Alignment LED at the lowest level that gives a usable preview.
- `hardware/SAFETY.md` documents the irradiance calculation against **ISO 15004-2 (Group 1, non-hazardous) limits** for both LEDs, with measured values once a lux/irradiance meter is available, and a sign-off line for the partnering clinician. **Do not use the device on a person until this file is complete and signed.**
- The Godox speedlight or any photographic flash is **not** to be used as the capture light.

### 7.3 Reference rig (Nikon Z8 + 24-120 mm f/4 S) — lab use only
Documented in `hardware/reference_rig.md`: same 20D lens and beamsplitter illumination, camera at ~0.35 m focusing on the aerial image (0.39× magnification → ~3,500 px across the fundus). Purpose: capture high-quality reference images of the same eyes as the phone adapter for the §4.6 resolution experiment and for quality comparison. Same safety rules apply; the Z8's low-light performance should allow *dimmer* illumination than the phone, not brighter.

### 7.4 Anterior-segment mode (optional, safe, day-one)
An app mode that photographs the external eye without the condensing lens or pupil illumination (ordinary room light / diffused flash) for future external-eye signals (scleral color, arcus, pupil size). Store images with the same privacy rules. No model required in Phase 1 — this is data collection for later.

---

## 8. Testing and fixtures

- `fixtures/` contains ~40 public-domain or synthetic fundus images: gradable at each DR grade (from APTOS, with license noted), plus synthetic ungradable variants (dark, blurred, off-center, reflection) generated by `ml/make_fixtures.py`.
- **Unit tests:** preprocessing equivalence (Python ↔ Kotlin); model loads and outputs expected shapes; result-outcome mapping (grade/probability → green/yellow/red) is a pure function with exhaustive tests; i18n completeness (every key exists in all three languages; every medical string is in `strings_medical.xml`); export schema validation (clinic fields never appear in deid export).
- **Instrumented tests:** screening flow end-to-end with a fixture image injected through the gallery-import path; auto-capture triggers on a looped good-quality video and never triggers on an ungradable one.
- **ML tests:** `eval.py` reproduces model-card metrics within ±0.01 on the held-out set; Grad-CAM lesion overlap test (§4.3).
- **Performance budget tests:** M1 ≥ 15 fps and M2 ≤ 1.5 s on a reference mid-range device (document which).

---

## 9. Repository layout

```
foresight/
├── CLAUDE.md                 # non-negotiables summary for Claude Code
├── DECISIONS.md              # dated log of design decisions and deviations
├── README.md                 # 10-minute quickstart for students
├── app/                      # Android app (Kotlin, Compose, CameraX, LiteRT)
│   ├── app/src/main/java/org/jameslick/foresight/
│   │   ├── capture/          # camera control, auto-capture, adapter interface (Mode A/B)
│   │   ├── ml/               # preprocessing (ported), model runners, Grad-CAM
│   │   ├── data/             # Room entities, encryption, export
│   │   ├── ui/               # Compose screens, accessibility
│   │   │   └── theme/        # design system: color/type/shape/motion tokens, GlowRing, InstrumentCard… (§3.10)
│   │   └── i18n/             # helpers; strings live in res/
│   └── app/src/main/assets/models/   # .tflite + model_card.json
├── ml/                       # Python training/eval/export
│   ├── data/download.md
│   ├── preprocess.py
│   ├── train_quality.py · train_dr.py · eval.py · export_tflite.py
│   ├── experiments/resolution_ablation.py
│   └── make_fixtures.py
├── dashboard/                # static web app
├── hardware/
│   ├── adapter.scad · bom.md · SAFETY.md · reference_rig.md · firmware/ (Mode B)
├── fixtures/                 # REAL public-domain/licensed fundus images only — never generated
├── assets/                   # Higgsfield-generated media (see §13)
│   ├── brand/                # logo, wordmark, color palette, app icon set
│   ├── ui/                   # onboarding illustrations, empty states, result-card art
│   │   └── mockups/          # mood boards + approved per-screen mockups (the design target, §13.3a)
│   ├── audio/master/         # student voice recordings, WAV (not shipped; app ships Opus in res/raw) (§13.8)
│   ├── hardware/             # 3D renders and exploded views of the adapter
│   ├── explainers/           # animated clips: how the eye is photographed, what DR looks like
│   ├── video/                # pitch video b-roll, title cards, final cuts (en/es/vi, student-narrated)
│   └── ASSETS.md             # prompt log + provenance for every generated file
└── docs/
    ├── consent_en.md · consent_es.md · consent_vi.md
    ├── screener_training.md   # 1-page how-to for volunteers
    ├── audio_manifest.csv     # generated from string resources; drives voice recording sessions (§13.8)
    └── competition/           # metrics snapshots, figures for the SFT application
```

---

## 10. Phased build plan with acceptance criteria

Key external dates: SFT application due **Nov 23, 2026**; semifinalists announced **Jan 20, 2027**; national finalists **Mar 18, 2027**; final event **April 2027**.

### Phase 0 — Foundations (target: 1 week)
- Repo skeleton, `CLAUDE.md`, `DECISIONS.md`, CI running lint + unit tests.
- Brand kit via Higgsfield (§13.2): logo/wordmark, app icon set, palette; `assets/ASSETS.md` started.
- Design direction chosen from Higgsfield mood boards (§13.3a); Compose design-system module (`ui/theme/`) scaffolded with tokens from the chosen direction (§3.10).
- `ml/data/download.md`; `preprocess.py` with tests; `make_fixtures.py` producing `fixtures/`.
- `train_dr.py --small` runs end-to-end on APTOS and exports a (weak) `.tflite`; `eval.py` prints metrics; `model_card.json` generated.
**Accept when:** a student can clone, follow README, and produce a model + metrics on Colab in under an hour.

### Phase 1 — App MVP with imported images (target: 2–3 weeks)
- Full screening flow (language → consent → intake → capture via gallery import → results → card → next).
- M2 (small-run model) and Grad-CAM running on device; outcome mapping; i18n complete in EN/ES/VI; TTS.
- Encrypted Room storage; de-identified export; dashboard reads it and shows summary cards.
- High-fidelity screen mockups via Higgsfield (§13.3a) approved **before** screens are coded; UI built in Compose to match; UI illustration set (§13.3): onboarding, consent, "look at the green dot" guidance art, result-card backgrounds, empty states — every one checked against the §13.1 rules.
**Accept when:** end-to-end instrumented test passes on a fixture image; i18n completeness test passes; export schema test passes; a non-technical teammate can run a mock screening in < 90 s; **design review gate (§3.10) passes for every screen** — running app matches approved mockups, no anti-patterns present.

### Phase 2 — Live capture and adapter v1 (target: 3–4 weeks)
- OpenSCAD adapter printed; Mode A illumination; `SAFETY.md` calculations drafted.
- CameraX/Camera2 manual controls; red-mode preview; M1 quality model; auto-capture; retake guidance.
- Full M2 training run on EyePACS + APTOS, evaluated on Messidor-2; adapter-style augmentations.
- Reference rig documented; resolution ablation (§4.6) run on public data and plotted.
- Hardware renders via Higgsfield (§13.4): photoreal and exploded-view renders of the adapter from the OpenSCAD model, for the BOM doc, the application, and the pitch.
**Accept when:** gradable photos of a model eye (or consenting adult team member **after** clinician sign-off of `SAFETY.md`) are captured hands-free ≥ 80% of attempts; M2 referable-DR sensitivity ≥ 0.85 / specificity ≥ 0.80 on Messidor-2; ablation figure produced.

### Phase 3 — Field readiness (target: 2–3 weeks)
- Mode B BLE adapter firmware (optional); Bluetooth receipt-printer card printing or share-to-PDF; clinic list and referral flow; screener PIN + auto-lock; "Delete my record".
- Consent forms in three languages reviewed by the district; screener training doc; partner clinic identified.
- Dashboard map + trend + CSV/PNG export.
- Explainer animations via Higgsfield (§13.5) embedded in the consent and results screens (how the photo is taken; what a referral means), in all three languages.
**Accept when:** a dress-rehearsal screening of 10 consenting adults completes with ≥ 85% gradability and a filled dashboard; privacy checklist (§6) passes line by line.

### Phase 4 — Research track and polish (ongoing to April)
- Experimental flags (§4.3 M3, §4.5) behind badges and toggles; retinal-age regression; bias report in model cards.
- Anterior-segment mode; English, Spanish and Vietnamese audio recorded by native-speaking students (§13.8) replaces TTS throughout the app.
- Competition figures: screened counts, gradability, referral completion, ablation plot, model cards.
- Pitch video (§13.6): script, Higgsfield b-roll and title cards, real footage of the team and device, English/Spanish/Vietnamese versions narrated by student voices; 3-minute cut for the national-finalist round.

---

## 11. Open questions for the team (answer in `DECISIONS.md`)
1. Kotlin-native vs. Flutter — who on the team will maintain the app, and what do they know?
2. Which partner clinic and which clinician will sign `SAFETY.md` and review the wording?
3. 20D vs. 28D lens for the first build (28D = wider field, smaller aerial image, better through undilated pupils).
4. Printer: Bluetooth thermal receipt printer (~$40, instant cards) vs. PDF-to-phone only?
5. Will images ever be used for research publication? If yes, start the IRB conversation now.
6. Pitch video: which real screening event will be filmed (with releases)? (Voices are decided: native-speaking students record English, Spanish and Vietnamese — see §13.8.)

---

## 12. Glossary
- **Fundus:** the back of the eye (retina, optic disc, macula, vessels) as seen through the pupil.
- **Diabetic retinopathy (DR):** damage to retinal blood vessels from diabetes; graded 0–4 (none → proliferative). "Referable DR" = moderate or worse, or suspected macular edema.
- **Condensing lens (20D):** a 20-diopter aspheric lens that forms a real, inverted, ~3× magnified aerial image of the retina about 50 mm in front of itself.
- **Non-mydriatic:** imaging without dilating drops; relies on a dark room and dim red/IR alignment light so the pupil stays wide.
- **Gradability:** whether an image is clear enough to assess; ungradable ≠ negative.
- **Oculomics:** using eye images to infer systemic (whole-body) health signals.
- **ISO 15004-2:** the light-hazard safety standard for ophthalmic instruments.

---

## 13. Generated media assets — Higgsfield (MCP tools connected in Claude Code)

The team lead has an Ultra-tier Higgsfield subscription linked to Claude Code. Use its MCP tools (`generate_image`, `generate_image_batch`, `generate_video`, `generate_3d`, `upscale_image`, `remove_background`, `reframe`, `outpaint_image`, `generate_audio` for music/SFX only, explainer presets, 3D scene builder) for every visual and video asset in the project, and for the design exploration in §13.3a that drives the app's look (§3.10). Voices are **not** generated — see §13.8. Before the first Higgsfield call of a session, call `get_workflow_instructions` with no argument and, for any multi-step video, follow the workflow it returns. For 3D/exploded renders of the adapter, start with `get_preset_instructions` with `{ preset: "/3d-jutsu" }` as the server instructs.

### 13.1 Hard rules for generated media (MUST)
1. **Never generate fundus/retinal images for the ML pipeline.** `fixtures/`, training, validation, and test data are real images from the licensed public datasets in §4.1 only. A synthetic retina in the pipeline would invalidate every metric in the application.
2. **Never show a generated image where a real retinal photo or real result belongs.** Result screens, printed cards, heat maps, model cards, and the dashboard display only the participant's actual photo and actual output. Stylized eye *illustrations* are fine in onboarding and explainers, but they must be obviously illustrative (flat/graphic style, not photoreal) so no one mistakes them for a scan.
3. **Never upload participant data to Higgsfield** — no retinal photos, no event photos containing identifiable participants, no consent signatures. Only team members who have signed a media release appear in footage sent for editing, dubbing, or motion work.
4. **No fabricated evidence.** Generated video may dramatize *how* a screening works; it may not depict a result, a statistic, a clinic partnership, or a testimonial that didn't happen. Every generated clip in the pitch video gets an on-screen "Illustration" tag, and `assets/ASSETS.md` records which shots are real and which are generated — the competition judges may ask.
5. **No real people's likenesses** other than consenting team members; no brand logos (Samsung, clinics, datasets) generated into imagery; no depictions of minors as participants.
6. **Cost discipline.** Call `balance` at the start of a media session and log credits spent per asset group in `ASSETS.md`. Use `generate_image_batch` / `generate_video_batch` for variations and `jobs_wait`, then one `show_generation_by_ids`. Generate at working resolution, pick, then `upscale_image` only the finalists.
7. **Provenance.** Every file in `assets/` has an entry in `ASSETS.md`: tool, model, prompt, seed/job id, date, license note, and where it is used. Favor styles that stay on-brand (§13.2) across the whole project rather than one-off looks.

### 13.2 Brand kit (Phase 0)
- **Logo and wordmark "Foresight."** Concept directions to generate and compare (3–5 variants each): an eye whose iris is a sunrise/horizon; a pupil as a lens aperture with a forward-pointing light cone; a minimal retina-vessel tree forming the letter F. Flat vector-like style, works in one color, legible at 48 px for the app icon.
- **Palette:** derive from the chosen logo; must pass WCAG AA contrast for 18sp text (§3.9); the result colors green/yellow/red (§3.4) are fixed and must remain distinguishable for color-blind users (add shape/icon cues, not just color).
- **App icon set** (adaptive icon foreground/background, 512 px store icon) and favicon for the dashboard. Run finalists through `remove_background`, export PNG + SVG-traced versions into `assets/brand/`.
- **Type:** choose a free, multilingual font with full Vietnamese diacritics (e.g., Noto Sans or Be Vietnam Pro) — do not generate typography as an image.

### 13.3a Design exploration and screen mockups (Phase 0–1, before UI code)
This is where Higgsfield does the most for the project's look.
1. **Mood boards (Phase 0):** 3 directions × 6 images each, prompted from the §3.10 brief ("precision optical instrument," "dark luminous interface," "retina-inspired palette"). Team picks one direction; record the choice and the winning prompts in `ASSETS.md` so every later asset reuses them.
2. **High-fidelity screen mockups (Phase 1):** one image per screen in §3.1 (language, consent, intake, capture with alignment ring, processing, results green/yellow/red, card, settings) plus the dashboard, in the chosen direction, at phone aspect ratio, with placeholder text in the correct language. Generate variants with `generate_image_batch`, pick, `upscale_image` the finalists, store in `assets/ui/mockups/`.
3. **Motion references:** short `generate_video` clips of the alignment ring breathing/snapping and the result card unfolding, used as the spec for Compose animations (timing, easing, glow).
4. **Reusable visual assets:** ring/reticle motifs, background gradients and vignettes, the icon family (one consistent style, exported via `remove_background` as transparent PNGs/SVG-traced), card textures for print.
Then the UI is **built in Compose to match the mockups** (§3.10 Implementation). Mockups are the target, never the shipped UI; text, controls and layout are always real components so they stay accessible and translatable.

### 13.3 UI illustration set (Phase 1)
Generate a consistent set (same style prompt, same palette) for:
- Onboarding (3 screens): "What Foresight does," "How the photo works," "What happens with your results."
- Consent screen header art and the "your retina is unique like a fingerprint" illustration.
- Capture guidance: friendly diagram of where to rest the forehead, "look at the green dot," "hold still."
- Result-card backgrounds for green / yellow / red outcomes (calm, non-alarming even for red).
- Empty states and the "we couldn't get a clear photo" screen.
- Screener-training one-pager figures (`docs/screener_training.md`).
Style guidance: flat, warm, East San José setting cues (church hall, mercado, school gym) without identifiable landmarks or logos; participants depicted as diverse adults 40+; no photoreal eyes. Export at 2× and 3× densities into `app/src/main/res/drawable-*`.

### 13.4 Hardware renders (Phase 2)
- Render the OpenSCAD adapter from `hardware/adapter.scad` (export STL/GLB, import with `scene_builder_3d_import_asset`, then generate product-shot angles and an exploded view labeling lens, beamsplitter, polarizers, LEDs, phone clamp).
- A "how the light path works" diagram: red alignment light and white capture flash entering the pupil coaxially, aerial image forming in front of the 20D lens. This is the single most useful figure for judges and for the clinician reviewing `SAFETY.md`; label it as a schematic.
- Store in `assets/hardware/`; reference from `hardware/bom.md` and the application.

### 13.5 Explainer animations (Phase 3)
Short (10–20 s), silent-with-captions loops embedded in the app:
1. Consent screen: what the device does and what happens to the photo (stays on the phone; deleted after 30 days unless you share).
2. Capture screen: how to position and where to look.
3. Results screen: what green/yellow/red mean and what a referral is.
Produce with the Higgsfield explainer presets (`get_explainer_presets` → `resolve_explainer_preset`) or `generate_video` from the §13.3 illustrations for visual continuity. Caption tracks in EN/ES/VI from the approved `strings_medical.xml` wording — the animations must not introduce new medical phrasing. Keep each file ≤ 2 MB (loop as WebP/MP4) so the app stays small and offline.

### 13.6 Pitch video (Phase 4; 3 minutes max)
Structure: problem (30 s) → the device and a real screening (60 s) → results and referral path (30 s) → evidence: gradability, model metrics, ablation figure, people screened (40 s) → team and ask (20 s).
- **Real footage first.** Film the team, the adapter, and a consenting adult screening with the Z8 (§7.3 kit doubles as the video camera). Generated media fills the gaps: title cards, the light-path animation, a map/zoom of East San José (no identifiable people), motion graphics for metrics, b-roll where filming isn't possible — each tagged "Illustration" per §13.1.
- **Multilingual cuts.** English, Spanish and Vietnamese versions are each narrated by a native-speaking student (§13.8) — three separate recordings from three scripts, not a dub of the English. Captions in all three languages on every cut.
- **Music and sound design** via `generate_audio` only (no copyrighted tracks); keep the score quiet under narration and consistent with the §3.10 aesthetic.
- Deliver `assets/video/foresight_pitch_en.mp4`, `_es.mp4`, `_vi.mp4`, plus a 60-second social cut, and a shot list in `ASSETS.md` marking real vs. generated.

### 13.7 Things Higgsfield is *not* used for
Training data, fixtures, synthetic patients, result imagery, consent-form text, medical wording, dashboard charts (those come from real exported data via the dashboard's own charting), any image that would be sent to a clinician as part of a referral, or **any voice** (§13.8).

### 13.8 Voices — recorded by native-speaking students (decided)
All spoken audio in the project — in-app prompts ("Look at the green dot," "Hold still," "Got it"), consent read-aloud, result read-aloud, explainer narration, and the pitch video — is recorded by native-speaking students: one each for English, Spanish and Vietnamese. Android TTS is the development placeholder only and is replaced in Phase 4.
- **Scripts first.** Every line comes from `strings.xml` / `strings_medical.xml` so the audio never drifts from the approved wording. Generate a recording manifest (`docs/audio_manifest.csv`: key, language, text, filename) from the string resources; the app looks up audio by string key and falls back to TTS if a clip is missing.
- **Recording.** Quiet room, phone or USB mic, 48 kHz WAV; two takes per line; export to Opus/OGG for the app (~24 kbps speech) and keep WAVs in `assets/audio/master/` (not shipped). Normalize loudness across languages (−16 LUFS) so switching language doesn't change volume.
- **Review.** A second native speaker (student, parent, or staff) checks each language for naturalness and correct medical register before release; log sign-off in `ASSETS.md`.
- **Consent and credit.** Each student voice actor signs a media release (and a parent signs if under 18) covering use in the app, video and competition materials; credit them by name in the app's About screen and the video.
- **Never** clone, modify, or synthesize these students' voices with Higgsfield or any other tool, and do not use `generate_audio`/`dubbing`/`voice_change` on their recordings beyond level normalization and noise reduction done locally.
