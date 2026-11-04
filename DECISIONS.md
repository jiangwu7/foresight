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

## 2026-11-03 — Logo chosen: round-2 `i304_soft_friendly` (eye with a four-blade shutter iris)
**Decision.** Team lead picked the Ideogram vector mark. Used as-is (no refinement round). Brand kit built
locally by `assets/brand/build_brand_kit.py`: clean SVGs, PNGs, lockups with the wordmark outlined from
Be Vietnam Pro, Android adaptive/legacy icons, dashboard favicon, palette with contrast checks.
**Deviation from §13.2.** `remove_background` / SVG tracing were not used: the source is already vector, so a
local, deterministic build is cleaner and free. The accent token moved from `#F2B55B` to the mark's exact
`#F3B661` (contrast on Ink 10.8:1).
**Consequences.** Phase 0 brand-kit item is complete pending the team's own look at the icon on a phone.

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
| Accept: student clones, follows README, gets a model + metrics on Colab in < 1 h | **passed 2026-11-04**: team lead, Colab T4, 57.1 min wall-clock from clone to `eval.py --check-card`. That hour included three bugs found and fixed live (a `%` in a format string, fp16 NaN in validation, onnx2tf 2.x output layout) and two re-runs of the training cell; training itself took 1.2 min and a clean session is expected to take ~20 min, dominated by the 10 GB download. Held-out APTOS: kappa 0.903, referable sens 0.943 / spec 0.931. | `docs/competition/phase0_small_run_colab_t4/` (once the run zip is dropped in) |

Team to-do before Phase 0 can be accepted: (1) ~~create the GitHub repo and push so CI runs~~ done 2026-11-04,
CI green; (2) ~~Kaggle account + APTOS download + `make_fixtures.py` + `train_dr.py --small` on Colab, timed~~
done 2026-11-04, 57.1 min; (3) ~~pick a mood-board direction and a logo concept~~ done 2026-11-03;
(4) a non-author teammate reviews the gallery screen on a 6.1" phone in a dark room — **still open**.

## 2026-11-04 — Phase 0 accepted; Phase 1 started
**Decision.** All §10 Phase 0 criteria are met (table above). The remaining human review of the gallery
screen on a phone is the team's standing action and does not block Phase 1. Team lead instructed: "start
phase 1".
**Shipped model for Phase 1.** `app/app/src/main/assets/models/dr_m2_int8.tflite` (11.7 MB) and its model
card come from the Colab T4 `--small` run (`docs/competition/phase0_small_run_colab_t4/`). Held-out APTOS:
kappa 0.903, referable sens 0.943 / spec 0.931, AUC 0.985. It is the weak classroom model by design and is
used only to build and test the on-device pipeline. It must never be used on a person.
**Deviation (§2.1 int8).** onnx2tf 2.6.9's calibrated int8 path failed ("flatbuffer_direct fast path
failed"); the fallback produced a **dynamic-range** quantized model (int8 weights, float32 activations).
Good enough for Phase 1 on CPU; full int8 (activations too, for the GPU/NNAPI delegates and the 1.5 s
budget) is re-attempted in Phase 2 with the full model. `export_status.json` records which was produced.
**ONNX vs PyTorch.** Max abs difference 0.065, on the raw `features` output (large-magnitude activations);
logits agree far more closely. The Phase 1 on-device test compares TFLite outcomes against the Python
reference on the fixture images rather than raw tensors.
**Phase 1 order.** (1) High-fidelity screen mockups in direction A for approval (gate per §3.10 / §13.3a);
(2) in parallel, the non-UI core: Kotlin preprocessing port + equivalence test, outcome mapping + exhaustive
tests, LiteRT runner + CAM heat map, Room/SQLCipher data layer, de-identified export, dashboard reader;
(3) screens in Compose only after mockup approval.

## 2026-11-04 — Phase 1 mockups approved; language codes shown as EN / ESP / VN
**Decision.** Team lead approved the round-1 mockups as recommended (list in `assets/ASSETS.md` §4). Language
tiles display "EN", "ESP", "VN". These are display labels chosen by the team; resource qualifiers stay the
BCP-47 codes `es` / `vi`, and TTS locales stay `es-US` / `vi-VN`.
**Consequences.** Compose screens are built to these mockups. Known corrections applied in code rather than by
regeneration: result glyphs are circle / triangle / square (never the logo); capture preview stays abstract and
dim; dashboard uses a real map.

## 2026-11-04 — Toolchain: AGP 8.11.2, Gradle 8.13, Kotlin 2.2.10, compileSdk 36; library pins
**Decision.** Upgraded from AGP 8.7 / Kotlin 2.0.21 / compileSdk 35 so Room 2.8.5, LiteRT 1.4.2 and
kotlinx-serialization 1.9 (built with Kotlin 2.2) resolve. Navigation is pinned to 2.9.8 and Lifecycle to 2.9.4
because the 2.10 / 2.11 releases demand compileSdk 37 and AGP 9.1, which are not yet stable.
**Why.** The newest AndroidX releases moved faster than the stable AGP; pinning keeps students on Android Studio
stable. Revisit when AGP 9.1 ships.

## 2026-11-04 — Phase 1 status after the first build day
Built and compiling, 31 JVM tests green, lint clean, debug APK builds: full screening flow (home → new event →
language → consent → intake ×6 with the under-18 refusal → right eye → left eye → processing → results →
card → done), Settings (model card, exports, retention purge), de-identified export + validator, clinic zip,
encrypted storage, LiteRT inference with the heat map, trilingual strings (ES/VI drafts), PDF card with QR,
print + share, TTS read-aloud, dashboard reader.
**Not yet verified on a device.** Nothing here has run on a phone: SQLCipher/Keystore, LiteRT, the photo
picker, print and share are exercised only by compilation and unit tests. The instrumented end-to-end test
(`ScreeningFlowTest`) is written and needs a device or emulator with a fixture pushed; CI has no emulator
job yet (Phase 1 follow-up).
**Phase 1 acceptance still open:** instrumented test run, non-technical teammate mock screening in < 90 s,
design review gate on a 6.1" phone against the mockups, clinician review of the draft medical strings.

## 2026-11-04 — Phase 1 gradability check is a heuristic, not M1
**Decision.** `QualityHeuristic` (brightness, saturated-pixel fraction, fundus-center offset, Laplacian
variance) decides gradable / dark / reflection / off-center / blurry on still images until the M1 quality
model arrives in Phase 2. Thresholds are pinned by a test on the 25 gradable + 16 degraded fixtures.
**Why.** The flow, strings and "ungradable is never negative" behaviour must be final in Phase 1; the detector
can be swapped later behind the same result type. Known gap: tuned on clinical-camera crops, so it will be
re-tuned on adapter photos in Phase 2.

## 2026-11-04 — First run on a phone (Galaxy S23 Ultra, Android 16): two bugs fixed, end-to-end test passes
**What happened.** The team lead installed the debug build from Android Studio and walked the flow. Two defects
that no JVM test could catch showed up immediately:
1. **Black text on the near-black background** on every screen. Material's default content color is pure
   black until a `Surface` sets it; our screens draw straight onto the background, so every `Text` without an
   explicit color was invisible. Fix: `ForesightTheme` now provides `LocalContentColor = colors.onBackground`
   at the root. Text inside cards/tiles was already fine because those are Surfaces.
2. **Crash on the capture screen** (`No ActivityResultRegistryOwner was provided`). `Localized` wrapped the
   screens in a bare `createConfigurationContext` Context, so the photo picker could not find the Activity;
   the share sheet (`startActivity`) and `PrintManager.print` would have failed the same way. Fix: the
   localized Context is now a `ContextWrapper` around the Activity that only overrides `getResources()`, and
   the result-registry and back-dispatcher owners are re-provided explicitly.
**Test harness fixes.** `ScreeningFlowTest` now (a) waits for the event list to load from the encrypted
database before deciding whether to create an event, and creates one itself ("Flow test") if none exists;
(b) reads the fixture through the instrumentation's shell access, because on Android 11+ the app cannot open
`/sdcard/Download` directly (EACCES) and must not gain a storage permission to do so. `gradle.properties`
sets `android.injected.androidTest.leaveApksInstalledAfterRun=true`: Gradle's default uninstalls the app
after a connected test run and that wiped the team lead's first event (database gone with the app).
**Result.** `connectedDebugAndroidTest`: 1 test, 0 failures, 0 skipped, 5.4 s on the device. LiteRT loaded
the int8 model with the XNNPACK CPU delegate; no app errors in Logcat. Home screen checked against mockup
511 (wordmark crossing the ring matches the mockup). Phase 1 acceptance items still open: teammate mock
screening in < 90 s, dark-room design review of every screen, clinician review of ES/VI medical strings.

## 2026-11-04 — Shipped model was broken: every image graded red. Replaced with a verified fp16 export
**What happened.** On the team lead's first hand-run, both eyes came back red, including a clean grade-0
fixture. A diagnostic test on the phone showed the model returned the *same* output for every image (near
uniform 20 % per grade, regression ≈ 0). The file `dr_m2_int8.tflite` from the Colab run had been shipped
unverified. Desktop LiteRT refuses to load it (per-channel quantization on the wrong axis, `conv.cc:751`);
the Android runtime loads it silently and computes garbage. Cause: onnx2tf 2.6.9's "flatbuffer_direct"
writer. Re-exporting with it in WSL reproduced the damage in every mode (fp16 unloadable, int8 NaN).
**Fix.**
- `ml/verify_tflite.py` (new): loads a `.tflite` with ai-edge-litert (Windows wheels exist) or
  `tf.lite`, runs the fixtures through it and the ONNX model, and requires: loads, input-dependent, argmax
  agreement ≥ 0.95 (fp16) / ≥ 0.80 (int8), referable-prob diff ≤ 0.05 / ≤ 0.15. `export_tflite.py` runs it
  on every file it writes and renames failures `*.BROKEN.tflite` so they cannot be shipped.
- `ml/requirements-export.txt` pinned to onnx2tf 1.x + TensorFlow < 2.20 (+ tf_keras, onnx-graphsurgeon,
  sng4onnx, psutil). On that line onnx2tf writes a SavedModel and the official TFLite converter does fp16
  and calibrated int8; onnx2tf's own quantizer is bypassed (it downloads a sample batch that current NumPy
  rejects; the script now writes that file itself).
- Export runs locally after all: TensorFlow is blocked on Windows but not in WSL Ubuntu (ml/README.md).
- `ml/tests/test_shipped_model.py` (new, desktop) and `ModelParityTest` (new, on the phone) fail if the
  shipped asset does not load, does not react to its input, or misgrades the clear fixtures.
**Shipped now:** `app/.../assets/models/dr_m2.tflite` = **fp16** export of the local CPU `--small` run
`20261104-082149` (kappa 0.907, referable sens 0.950 / spec 0.931, AUC 0.985 — marginally better than the
Colab run's 0.903 / 0.943 / 0.931). Verification: 12 fixtures, argmax agreement 1.00, max referable-prob
diff 0.0023 vs ONNX. On the Galaxy S23 Ultra: grade-0 fixture → grade 0 (p 0.986, referable 0.008),
grade-2 fixture → grade 2 (p 0.819, referable 0.981), 97–264 ms per image. APK 83 MB.
**Team lead's walk-through on the phone (same day, after the fix):** speckled grade-2 image → yellow, clean
grade-0 image → green, overall yellow, as expected. Time per participant not yet measured.
**Deviation from spec §4.3 ("≤ 30 MB int8").** 21.4 MB, so within the size budget, but fp16 rather than
int8: the calibrated int8 export loads and reacts to input but degrades accuracy (argmax agreement 0.75;
a mild grade-1 fixture moved from referable 0.05 to 0.64), so the guard rejected it. Proper int8
(quantization-aware training or selective float fallback) is a Phase 2 task alongside the delegate
benchmark. The Colab run's ONNX is not on this machine; if the team wants that exact model shipped,
download `model.onnx` from the Colab run and re-export with the pinned toolchain.

## 2026-11-04 — Hand-run #2: 88 s per participant; face photos were graded, so a "not a retina photo" rule was added
**Results of the team lead's run on the Galaxy.** Full flow timed at **88 s** (target < 90 s, §10 Phase 1).
Retina fixtures: grade-2 → yellow, grade-0 → green, overall yellow (correct). Print opens Samsung's print
dialog, which offers "Share as PDF" rather than "Save as PDF": that is the phone's print service, not the
app; the app's own Share button covers saving the card. **Failure:** two photos of a person's face were
graded (red and green, overall red) instead of being refused. The Phase 1 heuristic only asked "is this
fundus photo clear enough?", never "is this a fundus photo?".
**Fix.** `QualityHeuristic` gains `Problem.NotFundus`: the four 8 % corner patches of the original photo
must be dark (mean max-channel ≤ 40; the 25 gradable fixtures measure ≤ 15, ordinary photos and screenshots
≥ 180) and the disc must be red-dominant (R/B ≥ 0.95, R/G ≥ 0.85; fixtures ≥ 1.02 / 0.95). Checked after
"dark" and "off-center" (a fundus pushed into a corner is still a fundus) and before "reflection" (a white
screenshot would otherwise read as glare). New strings `problem_not_fundus(_tip)` in EN/ES/VI (UI wording,
not medical). `QualityHeuristicTest` adds synthetic non-retina images (face on light background,
screenshot, sky/grass): all refused; all 25 gradable and 16 degraded fixtures keep their labels. Lint,
32 JVM tests and both device tests green.
**Re-tested by the team lead on the phone:** face photos for both eyes refused on all three attempts; the
session ended as ungradable, not with a color.
**Known gap, documented in the code:** a warm-toned object photographed on a black background (e.g. a face
in a dark room) can still pass; that needs the M1 quality model (Phase 2). The screener briefing should
say: only photos taken through the adapter.

## 2026-11-04 — Phase 1 accepted (as reported by the team lead); Phase 2 may start
**Acceptance items (§10 Phase 1) and who verified them, per the team lead's report:**
- Fixture image reaches a result and a card: instrumented test on the Galaxy S23 Ultra, green.
- Non-technical teammate mock screening < 90 s: team lead measured 88 s; a teammate who did not build the
  app ran it cold (reported done).
- Design review gate on a 6.1" phone in a dark room against the approved mockups: reported done by a
  teammate.
- Clinician review of the draft ES/VI medical strings and the outcome thresholds: reported done.
- "Delete my record" keeps the anonymous count; ES and VI runs show no cut-off text or missing diacritics:
  team lead.
- Non-retina photos refused; model verified on device (see the two entries above).
**Not re-verified by the assistant:** the teammate review and the clinician review are the team lead's
report; no written sign-off is in the repo yet. Suggested: add the clinician's name/date to the header of
each `strings_medical.xml` when the wording is final.
**Phase 2 gating (§6, CLAUDE.md):** nobody is imaged until `hardware/SAFETY.md` is complete and signed and
the illumination is clinician-reviewed. Phase 2 camera work starts on a model eye.

## 2026-11-04 — Phase 2 plan and order of work
Phase 2 (§10) deliverables, in the order they are built, one commit per step:
1. **Hardware v1 (gates everything else):** `adapter.scad` full parametric geometry (phone clamp, rail with
   locking screw, 20D lens holder, coaxial illumination module with 45° beamsplitter and crossed polarizers,
   eyecup/forehead rest) exported to STL; `SAFETY.md` irradiance calculations drafted against ISO 15004-2
   Group 1 with every assumption marked and a spreadsheet; Mode A wiring (switch + series resistors) and
   the Mode B firmware skeleton with the capped command set; `bom.md` with concrete parts.
2. **Live capture in the app:** CameraX + Camera2 interop (manual focus distance calibrated per phone in
   Settings, fixed exposure/ISO, torch off, WB locked), full-screen preview with the ring overlay and
   traffic-light quality, dim-red alignment mode, auto-capture on 3 consecutive good preview frames with a
   manual shutter fallback, Mode A/B adapter interface, haptic + spoken cues, gallery import moved to
   Settings → "Import test image" (kept on the capture screen only in debug builds for the fixture test).
   Instrumented tests: auto-capture fires on a looped good frame sequence and never on an ungradable one.
3. **M1 quality model:** `train_quality.py` (MobileNetV3-Small, 256 px; gradable + 4-way problem head +
   disc-center head), trained on APTOS with the same synthetic degradations as `make_fixtures.py` plus
   non-fundus negatives; int8 export through the new verification guard; `QualityModelRunner` behind the
   same `Result` type as the heuristic (which stays as the fallback); ≥ 15 fps budget test on the Galaxy.
4. **M2 full run infrastructure:** EyePACS + APTOS loaders, Messidor-2 evaluation, adapter-style
   augmentations (reflection, vignetting, blur), `--datasets`; the full run itself needs EyePACS (~88 GB)
   and GPU hours and is scheduled by the team on Colab; proper int8 (QAT or selective float fallback).
5. **Resolution ablation (§4.6):** implemented against Messidor-2; run on the APTOS held-out split as a
   stand-in until Messidor-2 is obtained, figure produced either way.
6. **Hardware renders (§13.4) via Higgsfield** from the STL: product shots, exploded view, light-path
   schematic for SAFETY.md and the application; `assets/hardware/`, logged in ASSETS.md.
**Gap carried from Phase 1:** the §13.3 UI illustration set (onboarding, consent header, capture guidance,
result-card backgrounds) was never generated; mockups left slots for it. Scheduled after item 6, scoped
with the team lead before any credits are spent.
**Data reality:** only APTOS is on this machine. Messidor-2 (registration) and EyePACS (Kaggle, 88 GB) are
team downloads; DDR/EyeQ for M1 likewise. Everything is built to run on them, and verified on APTOS now.
**Safety reality:** nobody is imaged in Phase 2 until `SAFETY.md` is complete and signed; camera work is
tested on a model eye (a printed fundus photo behind a 20D lens is enough for alignment/auto-capture).

## 2026-11-04 — Phase 2 step 1: adapter v1 geometry, safety draft, Mode A/B, BOM reality
**Built.** `hardware/adapter.scad` is now a full parametric model (phone clamp, illumination module with
45° beamsplitter slot, LED side port and two polarizer slots, slotted rail with a mm scale, lens carriage
with locking screw and three set screws, eyecup with forehead-rest arm) with a `part=` switch; five STLs
and three preview renders are generated by `hardware/Makefile` (OpenSCAD 2021.01 installed with winget).
`SAFETY.md` has a complete draft calculation from `safety_calc.py` (Maxwellian-view model, ISO 15004-2
Group 1 limits 2.2 mW/cm² weighted / 0.7 W/cm² unweighted); every input is tagged ASSUMED and the result
has > 60× margin for the white flash and > 10⁴× for the red alignment LED, so the design is sound even if
several assumptions are off by 3×. Still unsigned, still no human subjects. Mode A is a switch plus a
555 one-shot that fixes the flash at ≈ 40 ms in hardware; Mode B is a compiling Arduino sketch for the
XIAO nRF52840 with the caps in constants. `reference_rig.md` has the Z8 procedure.
**Decision: Mode A ships in Phase 2; Mode B stays a reference until Phase 3.** Adding BLE permissions to
the app for an untested path is not worth it; the app's `AdapterLink` interface keeps both doors open.
**BOM reality (researched 2026-11-04).** A used Volk 20D is ≈ $210 on the secondary market, not the
$40–60 assumed in Phase 0; new is $399–532. Everything else totals ≈ $41. The honest statement for the
application is "≈ $41 adapter + the lens", with three routes to a cheap lens listed in `bom.md`
(borrowed from the clinic, a generic 20D aspheric validated on the reference rig, or a used Volk). The
spec's ≤ $100 target is met only without the lens, or with a donated one. No CRI ≥ 90 warm-white 5 mm
through-hole LED was found; v1 accepts CRI ≈ 80 (Cree C513A) and notes the SMD alternative.
**Known model warnings.** OpenSCAD reports the clamp and assembly as possibly non-manifold (coincident
faces between the jaws and lips); slicers repair this automatically. Geometry has not been printed yet.

## 2026-11-04 — Phase 2 step 2: live capture built (device verification pending)
**Built.** `capture/` package: `CameraController` (CameraX 1.5.3 + Camera2 interop: AF off with the focus
distance from Settings in diopters, AE off with two exposure programs, torch off, AWB locked; ~5 fps RGBA
analysis stream; full-resolution still), `AutoCapture` (pure state machine: 3 consecutive gradable frames →
trigger, bad frame resets, 4 s cooldown; unit-tested), `AdapterLink` (Mode A `ManualSwitchLink` prompts the
screener; Mode B `BleLink` placeholder with the firmware command strings, no Bluetooth permission yet),
`CaptureSettings` (per-phone calibration in SharedPreferences). `LiveCaptureScreen`: full-screen preview
under a dim-red scrim, the GlowRing with a green fixation dot, traffic-light pill, live problem tip,
adapter prompt card, manual "Take photo", haptic + spoken "Look at the green dot / Hold still / Got it",
then the same review block as the import flow (`EyeReviewBlock`, shared). Settings → Camera: focus mm,
ISO/shutter for both programs, dim-red toggle, adapter mode, "import test images" switch, device check
(manual focus / manual exposure / AWB lock) and the Mode A LED test text. Only new permission: CAMERA.
**Mode A timing decision.** The app cannot know when the screener presses the flash button, so the
capture program opens a long low-ISO window (default 500 ms at ISO 200) 400 ms after the spoken prompt;
the 40 ms flash lands inside it. A missed press yields a dark still → "too dark" → retake. Mode B fixes
this properly in Phase 3. Exposure is clamped to the phone's advertised range.
**Tests.** `AutoCaptureTest` (4 JVM tests). `LiveCaptureTest` (device): the screen loops a fixture through
`TestHooks.frameProvider`; a good frame must reach "Got it", a dark frame must never trigger in 6 s.
**Not yet run on the phone** (unplugged at the time): `LiveCaptureTest`, the real camera path (manual
controls on the Galaxy S23 Ultra), and the bench test with a printed fundus photo in a dark room. Those are
the next action when the phone is back; a screenshot review of the live screen against mockup 503 too.
**Gallery import** stays available for the end-to-end test hook and via Settings (spec §3.2 last bullet).

## 2026-11-04 — Live capture verified on the Galaxy S23 Ultra; Mode A capture redesigned around the phone's limits
**Device facts (main back camera, from `dumpsys media.camera` and `CameraController.probe`):** manual focus
yes, manual exposure yes, AWB lock yes; closest focus **100 mm** (10 D); ISO 50–3200; exposure
**0.054–150 ms**. Two consequences:
1. **Rail geometry.** The aerial image must be ≥ 100 mm from the camera, so the lens sits ≥ 150 mm away.
   `adapter.scad` now defaults to `rail_min_gap = 140`, `rail_travel = 80` (lens 140–220 mm, aerial image
   90–170 mm); the README tells students to read their phone's closest focus from Settings → Device check
   and set the rail from it. STLs and renders regenerated.
2. **Mode A capture.** A 500 ms "catch the press" shutter is impossible (150 ms cap). The app now watches the
   analysis stream at full rate (1280×960) during a 4 s window after the prompt and keeps the frame in which
   the screener's 40 ms flash appears (`FlashCatcher`: brightness ≥ 1.8× the dim-red baseline and ≥ 12
   levels above it, brightest frame of the flash wins; 5 JVM tests). If no flash is seen the app takes an
   ordinary still, which reads as too dark → retake. The grading model sees 224 px, so a 1280×960 frame is
   not a quality compromise for M2; the full-resolution still returns with Mode B in Phase 3, when the
   firmware fires the flash and the app can synchronise.
**Verified on the phone:** `LiveCaptureTest` (good frame → "Got it"; dark frame never triggers in 6 s),
`LiveCameraDiagnosticTest` (camera bound with the manual controls, live for 10 s, no crash), plus the
Phase 1 tests. Screenshot of the live screen with the real camera:
`docs/competition/phase2_device/live_capture_galaxy_s23u.png` — dim-red scrim, ring + fixation dot,
traffic-light pill, Mode A prompt, manual shutter; to be reviewed by a teammate against mockup 503.
**Still untested:** an actual flash catch (no adapter built yet) and auto-capture on a printed fundus
photo in a dark room. Both are bench tests for the team once parts arrive; the 555 flash can be checked
against the app with just the LED circuit and a sheet of paper.

## 2026-11-04 — User-test feedback: intake screens cut from the default flow (deviation from spec §3.3, team lead's call)
**Feedback.** Testers said there were too many optional questions before the photos. The team lead asked
to keep only what is essential.
**Decision.** Default participant flow is now **language → consent → right eye → left eye → processing →
results → card** (7 steps; the step counter adapts). The six intake questions (§3.3) still exist and run
exactly as before when **Settings → Camera → "Ask the optional intake questions"** is on (research events,
bias analysis by age group per §6); off by default. What was essential moved:
- **Age gate (§6, minors never screened):** a required third checkbox on the consent screen, "I am 18 or
  older", plus a link "The participant is under 18" to the refusal screen. "I agree" stays disabled until
  all three boxes are ticked.
- **Phone number and share-with-clinic consent:** a "For the clinic (optional)" card on the results screen,
  saved when the participant moves on to the card. Share still defaults to No (§6); the contact number is
  still stored encrypted and deleted by "Delete my record". Shown only when the intake questions are off.
**Not collected by default any more:** age range, diabetes history, last exam, access to a doctor. The
de-identified export writes them as null; the dashboard's age/history breakdowns are empty unless an event
turns the questions on. Noted for the competition report.
**Verified:** 41 JVM tests, lint, 5 device tests (flow drivers updated for the consent checkbox).

## 2026-11-03 — Brand-asset workflow: Higgsfield's bundled brand workflow is used for reference, not as the gate
**Decision.** `get_workflow_instructions("brand-asset-creation")` was loaded as the server instructs. Its full
interactive flow (palette approval → Recraft SVG logos → brandbook) is designed for a finished identity. For
Phase 0 we only need exploratory concept directions, so logo concepts are generated with `generate_image_batch`
as §13.2 specifies, and the Recraft vector route is reserved for finalizing the chosen direction (SVG export is
exactly what §13.2 asks for).
**Why.** Keeps Phase 0 at "directions to compare", matching the team lead's instruction; avoids spending credits on
a brandbook before a direction exists.
