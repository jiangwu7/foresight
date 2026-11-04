# app/ — the Foresight Android app

Kotlin · Jetpack Compose · CameraX (+ Camera2 interop) · LiteRT. Phase 0 contains the design system and a
gallery screen; the screening flow arrives in Phase 1.

## Run it in under 10 minutes
1. Install Android Studio (Ladybug or newer) with SDK Platform 35.
2. `File → Open → app/` (this folder, the one with `settings.gradle.kts`).
3. Plug in a phone with USB debugging on, press Run. You should see the dark component gallery.

From a terminal (needs `local.properties` with `sdk.dir=` pointing at your SDK):
```bash
./gradlew :app:testDebugUnitTest   # design-token lint + i18n completeness
./gradlew :app:lintDebug           # Android lint (AllowBackup and HardcodedText are errors)
./gradlew :app:assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

## Where things live
```
app/src/main/java/org/jameslick/foresight/
├── MainActivity.kt            Phase 0: hosts the gallery
├── ui/DesignGalleryScreen.kt  every component with live state
└── ui/theme/                  THE design system (§3.10). Tokens + components:
    Color.kt  Type.kt  Dimens.kt  Motion.kt  Glow.kt  Theme.kt
    GlowRing  InstrumentCard  BigAction  StatusPill  MetricReadout  LanguageTile  ResultCard  HeatmapOverlay
app/src/main/res/
├── values/strings.xml, values-es/, values-vi/      UI strings
├── values*/strings_medical.xml                      approved medical wording (clinician-reviewed)
├── font/                                            Be Vietnam Pro + IBM Plex Mono (OFL)
└── xml/data_extraction_rules.xml                    no backups (§6)
app/src/test/                  JVM tests: DesignTokenLintTest, I18nCompletenessTest
```

## Rules enforced by the build
- **No raw colors or sizes outside `ui/theme/`** — `DesignTokenLintTest` fails the build.
- **Every string key exists in en/es/vi; medical keys only in `strings_medical.xml`** — `I18nCompletenessTest`.
- **No inline sentences in UI code** — same test.
- **`allowBackup="false"`** and no hard-coded text — Android lint errors.

## Design tokens are provisional
`Color.kt` is marked PROVISIONAL until the team picks a mood-board direction (see `assets/ASSETS.md`).
Only that file should change when the direction is chosen.
