# DECISIONS.md — Foresight decision log

Dated log of design decisions, answers to the spec's open questions (§11), and every deviation from
`FORESIGHT_SPEC.md` with the reason and the alternative chosen. Newest entries at the bottom.
Format: `## YYYY-MM-DD — Title` · **Decision** · **Why** · **Consequences / follow-ups**.

---

## 2026-11-03 — Q1: App stack is Kotlin native
**Decision.** Kotlin, Jetpack Compose, CameraX with Camera2 interop, LiteRT. Not Flutter.
**Why.** Team lead chose it. The spec's default; Phase 2 auto-capture depends on manual focus distance, fixed
exposure/ISO and torch control, which Camera2 interop exposes and cross-platform frameworks expose poorly.
Samsung competition, Samsung devices.
**Consequences.** Repo layout exactly as §9. Design-system module is Compose (`app/.../ui/theme/`). CI builds with
Gradle on `ubuntu-latest`, which ships the Android SDK.

## 2026-11-03 — Q3: First adapter build uses the 20D lens
**Decision.** 20D Volk-style aspheric condensing lens, nominal 50 mm diameter, as the OpenSCAD default.
28D remains a documented parameter variant.
**Why.** Team lead chose it. Larger aerial image (~3x), standard lens, easier for the phone camera to focus on.
**Consequences.** `hardware/adapter.scad` default `lens_diameter = 50`, aerial image ≈ 50 mm in front of the lens;
BOM lists the 20D lens with the 28D as an alternative row.

## 2026-11-03 — Q2 (default): Partner clinic and signing clinician are TBD
**Decision.** Placeholder partner: Gardner Health Services (named in the spec as an example), to be confirmed.
`hardware/SAFETY.md` carries an unsigned sign-off block. No human is imaged until it is signed.
**Why.** Team has not yet secured a clinician. The spec's §6 and §7.2 gates are unaffected: they simply stay closed.
**Follow-up.** Team lead to confirm clinic + clinician before Phase 2 acceptance.

## 2026-11-03 — Q4 (default): Share-to-PDF cards first, thermal printer optional
**Decision.** Phase 1 referral card is generated as a PDF and shared from the phone (print, save, message).
A Bluetooth ESC/POS thermal receipt printer (~$40) is an optional Phase 3 add-on behind the same `CardPrinter`
interface.
**Why.** PDF needs no hardware, works offline, and covers the competition demo. Printer adds instant paper cards
for participants without phones, which matters in the field, so it stays on the plan.

## 2026-11-03 — Q5 (default): Assume images may be used for research publication
**Decision.** Write the consent form with a separate, optional, default-off research-use clause, and start the IRB
conversation now (district + partner clinic / university IRB if a partner is academic).
**Why.** Reversing this later is impossible: data collected without research consent cannot be published. Opt-in
costs nothing if the team never publishes.
**Follow-up.** Teacher to ask the district about IRB/consent review before Phase 3 dress rehearsal.

## 2026-11-03 — Q6 (default): Pitch-video event is TBD
**Decision.** Default plan: film a school parent night or Veggielution screening with signed releases. Decide at
Phase 3.
**Why.** The pitch video is Phase 4; nothing in Phase 0–2 depends on it.

## 2026-11-03 — Python version: 3.14 locally, 3.11 on Colab (deviation from §2.1 "Python 3.11")
**Decision.** The developer machine only has Python 3.14; the spec asks for 3.11. The `ml/` code targets 3.11+
and is verified on 3.14 locally; Colab's runtime (3.11/3.12) is the reference for the `--small` classroom run.
**Why.** Installing a second interpreter is not required for Phase 0 as long as every dependency has 3.14 wheels.
If a dependency (notably TensorFlow for the TFLite converter) lacks a 3.14 wheel, the export step documents the
Colab path and the local step uses an alternative converter. See the TFLite export entry below once resolved.

## 2026-11-03 — Kaggle data not available on this machine during Phase 0
**Decision.** The team lead does not yet have a Kaggle API token. `train_dr.py --small`, `eval.py` and
`make_fixtures.py` are written against the APTOS 2019 layout and documented in `ml/data/download.md`; they are
smoke-tested locally on randomly generated tensors that are never written to `fixtures/` and never called images.
The real end-to-end Phase 0 acceptance run ("student produces a model + metrics on Colab in under an hour") must
be performed by the team once APTOS is downloaded.
**Why.** §13.1 forbids synthetic retina images anywhere in the pipeline, and APTOS cannot be downloaded without
accepting the competition rules under a Kaggle account.
**Follow-up.** Team lead: create Kaggle account, accept APTOS 2019 rules, place `kaggle.json`, run the commands in
`ml/data/download.md`, then `make_fixtures.py` and `train_dr.py --small`.

## 2026-11-03 — Fixtures are built locally from APTOS by script, not committed (deviation from §8)
**Decision.** `fixtures/` in git holds only `README.md`, `fixtures_manifest.json` (APTOS image ids + grades) and
the generator script's expected outputs list. `ml/make_fixtures.py` builds the ~40 real images and their degraded
variants locally from the downloaded APTOS data. Tests skip (not fail) when fixtures are absent, and CI runs them
only when the fixtures artifact is present.
**Why.** APTOS 2019 is distributed under Kaggle competition rules that allow use for the competition and
non-commercial research but do not clearly allow redistribution in a public repository. Committing images would
risk the dataset license and would also break the "no participant-like data in git" habit we want students to
have. The spec's intent (deterministic tests on real images) is preserved: the manifest pins exact image ids so
every machine builds identical fixtures.
**Alternative considered.** Genuinely public-domain fundus images (e.g. STARE, Wikimedia Commons) — too few with
DR grades to cover each grade; kept as an option if the team wants a tiny committed subset.

## 2026-11-03 — Spec inconsistencies noted (not changed)
- M1 speed: §4.3 target ≥ 25 fps, §8 performance test ≥ 15 fps. Treated as target vs. floor: the perf test asserts
  ≥ 15 fps; the model card reports measured fps against the 25 fps target.
- §3.3 age ranges omit "Under 18" while §6 requires that option to refuse minors. The intake adds "Under 18" as the
  first option; selecting it ends the session with the refusal screen.
- §7.1 BOM ≤ $100 while a new 20D lens is ≈ $150. `hardware/bom.md` will show both totals (used/educational lens
  vs. new) as the spec itself requests.
- §0.3 "no account beyond free Kaggle/GitHub" vs. Messidor-2 (ADCIS registration) and RETFound (GitHub request
  form). Both are free registrations; documented as such in `ml/data/download.md`. Messidor-2 is test-only and
  RETFound is optional, so Phase 0–1 need neither.

## 2026-11-03 — Phase 0 Higgsfield scope limited by the team lead
**Decision.** Phase 0 media generation is limited to the §13.3a mood boards (3 directions × 6 images) and the
§13.2 logo concept directions (3 concepts × 3–5 variants). App icon set, palette extraction, upscales,
background removal and all Phase 1 assets wait until the team lead picks a direction.
**Why.** Explicit instruction from the team lead on 2026-11-03; also §13.1.6 cost discipline. Design-system
tokens in `ui/theme/` are scaffolded with provisional values marked `// PROVISIONAL until direction chosen`.

## 2026-11-03 — TFLite conversion runs on Colab/CI, not on the team lead's Windows machine
**Decision.** `ml/export_tflite.py` always writes ONNX and verifies it with onnxruntime; the ONNX → TensorFlow
→ `.tflite` (fp16 + int8) step runs wherever TensorFlow imports (Colab, the Linux CI job, most Macs). On
this Windows machine TensorFlow's native DLL is blocked by a Windows Application Control policy
(`DLL load failed ... An Application Control policy has blocked this file`) and `onnx2tf` has no Python
3.14 wheel, so the local smoke run stops at ONNX and prints the Colab command. `export_status.json` records
which happened.
**Why.** Silent failure is forbidden (§0.8); a hard requirement on local TensorFlow would block every student
on a locked-down laptop. The CI job (`.github/workflows/ci.yml`, Python 3.11 on Ubuntu) is the place where the
full chain is exercised on every push.
**Follow-up.** Confirm in CI that `model_int8.tflite` is produced by the smoke run; then verify the layout
(`input_layout` NHWC) when the Kotlin runner is written in Phase 1.

## 2026-11-03 — Fonts: Be Vietnam Pro + IBM Plex Mono (both OFL)
**Decision.** Be Vietnam Pro (Light/Regular/Medium/SemiBold) for all text; IBM Plex Mono for numerals.
**Why.** Spec §3.10 offers Inter/Noto Sans/Be Vietnam Pro; Be Vietnam Pro was designed for Vietnamese
diacritics first. Google Fonts ships Inter and JetBrains Mono only as variable fonts, which complicate
Android font resources; IBM Plex Mono has static weights and matches the instrument feel.

## 2026-11-03 — Design-system lint is a JVM unit test, not a custom Android Lint rule
**Decision.** `DesignTokenLintTest` scans `ui/**` (excluding `ui/theme/`) for `Color(0x…)`, built-in Color
constants and `.dp/.sp` literals; `I18nCompletenessTest` checks keys across en/es/vi and the medical-string
separation. Both run in `gradlew test` and CI.
**Why.** A custom Lint module is more machinery than a student should have to understand; a 60-line test
with a clear failure message does the same job.

## 2026-11-03 — Design direction: A "Instrument"; first logo round rejected
**Decision.** Team lead chose mood-board direction A (precision optical instrument: charcoal, pale-amber
hairlines, matte black with one amber accent). The 12 first-round logo concepts (sunrise iris, aperture cone,
vessel F) were all rejected; the new brief is a mark that **fuses an eye with a camera shutter**, explored
across several styles.
**Consequences.** `Color.kt` tokens are final for direction A (they already matched it). Style core and
reference images recorded in `assets/ASSETS.md`. Round-1 logo files stay in `assets/brand/concepts/round1/`
for the record; round 2 goes in `round2/`.

## 2026-11-03 — Phase 0 status against the §10 acceptance criteria
| Criterion | Status | Evidence |
|---|---|---|
| Repo skeleton, CLAUDE.md, DECISIONS.md | done | this repo |
| CI running lint + unit tests | written, **not yet run** (no GitHub remote yet) | `.github/workflows/ci.yml` |
| Brand kit via Higgsfield | **partial by instruction**: logo concept directions only | `assets/brand/concepts/`, `ASSETS.md` |
| Design direction chosen from mood boards | **waiting on the team**: 3 × 6 mood boards delivered | `assets/ui/mockups/moodboards/` |
| Compose design-system module scaffolded | done with provisional tokens; 8 components with previews; token lint + i18n tests pass; debug APK builds | `app/app/src/main/java/.../ui/theme/` |
| `ml/data/download.md`, `preprocess.py` + tests, `make_fixtures.py` | done; 14 unit tests pass; fixture tests skip until APTOS is present | `ml/` |
| `train_dr.py --small` end-to-end, `eval.py`, `model_card.json` | code done and verified with `--smoke` (random tensors) locally; ONNX export verified; **real APTOS run not yet possible here** (no Kaggle token); TFLite step runs on Colab/CI | `ml/runs/<ts>-smoke/` |
| Accept: student clones, follows README, gets a model + metrics on Colab in < 1 h | **not yet verified** — needs the team's Kaggle account | README quickstart B |

Team to-do before Phase 0 can be accepted: (1) create the GitHub repo and push so CI runs; (2) Kaggle
account + APTOS download + `make_fixtures.py` + `train_dr.py --small` on Colab, timed; (3) pick a mood-board
direction and a logo concept; (4) a non-author teammate reviews the gallery screen on a 6.1" phone in a dark room.

## 2026-11-03 — Brand-asset workflow: Higgsfield's bundled brand workflow is used for reference, not as the gate
**Decision.** `get_workflow_instructions("brand-asset-creation")` was loaded as the server instructs. Its full
interactive flow (palette approval → Recraft SVG logos → brandbook) is designed for a finished identity. For
Phase 0 we only need exploratory concept directions, so logo concepts are generated with `generate_image_batch`
as §13.2 specifies, and the Recraft vector route is reserved for finalizing the chosen direction (SVG export is
exactly what §13.2 asks for).
**Why.** Keeps Phase 0 at "directions to compare", matching the team lead's instruction; avoids spending credits on
a brandbook before a direction exists.
