# app/ — the Foresight Android app

Kotlin · Jetpack Compose · LiteRT · Room + SQLCipher. Phase 1 = the full screening flow with imported
photos (the live camera, red mode and auto-capture arrive in Phase 2).

## Run it in under 10 minutes
1. Install Android Studio (Narwhal or newer) with SDK Platform 36.
2. `File → Open → app/` (this folder, the one with `settings.gradle.kts`).
3. Plug in a phone with USB debugging on, press Run. Create an event, then walk the flow; on the capture
   screens pick any fixture image from `fixtures/gradable/` (copy a few to the phone first).

From a terminal (needs `local.properties` with `sdk.dir=` pointing at your SDK; on Windows write it as
`sdk.dir=C\:/Users/you/Android/Sdk`):
```bash
./gradlew :app:testDebugUnitTest   # 31 JVM tests: tokens, i18n, preprocessing equivalence, outcomes, CAM, export, quality
./gradlew :app:lintDebug           # Android lint (AllowBackup and HardcodedText are errors)
./gradlew :app:assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
# with a device/emulator and a fixture pushed to /sdcard/Download/foresight_fixture.png:
./gradlew :app:connectedDebugAndroidTest
```

## Where things live
```
app/src/main/java/org/jameslick/foresight/
├── MainActivity.kt                 hosts ForesightNav
├── ui/ForesightNav.kt              routes per spec §3.1
├── ui/ScreeningViewModel.kt        one session's state; nextParticipant() wipes it
├── ui/FlowScreen.kt                the screen skeleton every step uses
├── ui/screens/                     Home, NewEvent, Language, Consent, Intake, Under18, Capture, Processing, Results, Card, Done, Settings
├── ui/ReferralCardPdf.kt           the printed/shared card (PDF + QR), via FileProvider
├── ui/theme/                       design system (§3.10): tokens + GlowRing, InstrumentCard, BigAction, StatusPill,
│                                   MetricReadout, LanguageTile, ResultTile, OptionTile, CheckRow, ResultCard, HeatmapOverlay
├── i18n/Localized.kt, Speech.kt    session language (not the phone's) for strings, dates and TTS
├── ml/Preprocess.kt                bit-exact port of ml/preprocess.py
├── ml/DrModelRunner.kt             LiteRT inference; outputs matched by shape
├── ml/ClassActivationMap.kt        heat map from features × head weights (no backprop)
├── ml/Outcome.kt                   grade/probability → green/yellow/red/ungradable (pure, exhaustively tested)
├── ml/QualityHeuristic.kt          Phase 1 gradability check (M1 replaces it in Phase 2)
└── data/                           Room entities (§3.6), SQLCipher database, Keystore crypto, encrypted images,
                                    de-identified export + validator, clinic zip, ScreeningRepository
app/src/main/assets/models/         dr_m2_int8.tflite + dr_m2_model_card.json (from the Colab run)
app/src/main/res/values*/           strings.xml (UI) and strings_medical.xml (approved wording, clinician-reviewed)
app/src/test/                       JVM tests;  app/src/androidTest/  the end-to-end flow test
```

## Rules enforced by the build
- **No raw colors or sizes outside `ui/theme/`** — `DesignTokenLintTest`.
- **Every string key exists in en/es/vi; medical keys only in `strings_medical.xml`; no inline sentences** — `I18nCompletenessTest`.
- **Kotlin preprocessing == Python preprocessing** on the fixtures — `PreprocessEquivalenceTest` (skips without fixtures).
- **The de-identified export never contains a personal field** — `DeidExportTest`.
- **`allowBackup="false"`, no INTERNET permission, no hard-coded text** — manifest + Android lint.

## Privacy map (spec §6)
Photos → PNG → AES-GCM (Android Keystore key) → `filesDir/images/*.enc`. Contact number → AES-GCM blob in
the SQLCipher database. Database passphrase → random, stored encrypted by the Keystore key. Exports →
`filesDir/exports/`, shared only through the FileProvider when the screener taps Export. Nothing has a
network path: the app does not request INTERNET.
