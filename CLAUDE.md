# CLAUDE.md — Foresight non-negotiables

Foresight is a $100 smartphone retinal **screening** kit built by James Lick High School students for
Samsung Solve for Tomorrow 2026-27. `FORESIGHT_SPEC.md` is the single source of truth; this file is the
short list of rules every session must obey. Section numbers refer to the spec. Decisions and deviations
go in `DECISIONS.md` — never silently change a requirement.

## How to work (§0)
1. Build in phase order (§10). Do not start Phase N+1 until Phase N's acceptance criteria pass. Ask before skipping.
2. Everything runs on a student laptop and a mid-range Android phone. No paid cloud, no GPU server, no accounts
   beyond free Kaggle/GitHub. Training scripts run on Colab free tier (T4) with `--small` (< 30 min) and a full mode.
3. Privacy is a build constraint (§6). If a shortcut would send an image off the device, stop and redesign.
4. Medical language is a build constraint (§1.3). All user-facing strings live in i18n resource files, never inline.
   Every condition-mentioning string is checked against the allowed phrasing.
5. Write tests as you go, using the real-image fixtures in `fixtures/` (§8).
6. Keep it understandable by a high school student: straightforward code, a README per module (< 10 min to run).
7. If something in the spec is impossible or a bad idea, say so in `DECISIONS.md` with the alternative chosen.
8. Use the Higgsfield MCP tools for all visual/video assets (§13). Generated media is for communication and UI only.
9. Aesthetics are a first-class requirement (§3.10). A working screen that looks like a default Material template is not done.
10. Commit after each meaningful step with clear messages.

## Medical framing (§1.3) — MUST
Foresight is a screening and risk-stratification tool, **not** a diagnostic device.
- **Never** say "you have diabetes", "diagnosis", "you have retinopathy", or "you are healthy".
- **Always** frame as: *"Your photo shows signs that are often seen in people with ___. We recommend a follow-up
  exam / blood test."* or *"No signs of ___ were detected in this photo. This does not replace a full eye exam."*
- Every result screen and printed card includes: **"Foresight is a screening tool operated by students. It is not a
  medical diagnosis. Please see a healthcare provider."**
- A human referral is always in the loop: every positive flag produces a referral action, never just a label.
- "Possible undiagnosed diabetes" and "cardiometabolic risk" flags are research outputs labeled **Experimental**
  until a partner clinician signs off.
- Ungradable images produce "We couldn't get a clear enough photo" plus a recommendation to see an eye-care
  provider — never a negative result.
- Medical result strings live in `strings_medical.xml` per language with an approved-wording header; do not edit
  without clinician review.

## Privacy, security, ethics (§6) — MUST
- On-device inference only. No image leaves the phone unless the participant ticked "share with clinic", and then
  only via the password-protected clinic export handed over in person.
- No analytics SDKs, no crash reporters that upload images, `android:allowBackup="false"`, excluded from auto-backup.
- Encryption at rest for images and contact info (Android Keystore + SQLCipher). Screener PIN login; auto-lock after
  5 min idle.
- "Delete my record" on the results screen removes images, contact info and inference results, keeping only an
  anonymous count.
- Consent text in all three languages says plainly: *"A photo of your retina is unique to you, like a fingerprint."*
- Before any screening of real people: district-reviewed consent form; clinician review of illumination (§7.2) and
  wording (§1.3); check IRB need if data will be published. `hardware/SAFETY.md` must be complete and signed first.
- Minors are not screened. The app refuses to proceed if "Under 18" is selected (the option exists only to catch it).
- Bias check: report M2 metrics by image source/camera and by age group where labels exist; document gaps honestly.
- Two separate exports (§3.7): `*_deid.json` (no images, contact, free text; ages bucketed) and `*_clinic.zip`
  (consented participants only, password-protected, never auto-uploaded).

## Visual design language (§3.10) — MUST
Brief: futuristic, sleek, professional — a precision optical instrument, calm, dark, luminous. A judge wants to
photograph it; a grandmother finds it calming and obvious.
1. **Dark, luminous, quiet.** Near-black default theme (not pure #000), one luminous accent from the brand and the
   retina's palette (deep reds, amber, warm disc glow). Light is the metaphor: thin glowing rings, soft gradients.
2. **Few elements, large.** One primary action per screen. Headline ≥ 32sp, body ≥ 18sp, 48–64dp targets.
3. **Precision details.** Hairlines, concentric rings/reticles, monospaced numerals, small-caps micro-labels, 8dp grid.
4. **Motion with purpose.** Physics-based Compose transitions; ring breathes, snaps and glows on capture; result card
   unfolds. Nothing decorative. Respect "reduce motion".
5. **Results stay humane.** Desaturated green/yellow/red paired with icons and words; red is serious, not alarming.
6. **Consistent across surfaces.** App, dashboard, printed card, video and adapter finish share palette, type, ring motif.

Implementation: one Compose design-system module `ui/theme/` (color/type/shape/elevation/motion tokens; light
fallback passes WCAG AA; Inter/Noto Sans/Be Vietnam Pro for Vietnamese diacritics; monospaced numerals). No ad-hoc
colors or sizes elsewhere — lint for it. Components: `GlowRing`, `InstrumentCard`, `BigAction`, `StatusPill`,
`MetricReadout`, `LanguageTile`, `ResultCard`, `HeatmapOverlay`, each with a preview and screenshot test.
Higgsfield produces mood boards and mockups that the team approves **before** UI is coded; UI is then built in
Compose to match. Design review gate: side-by-side against approved mockups, by a teammate who did not build the
screen, on a 6.1" phone, in a dark room.

**Anti-patterns (reject on sight):** default Material purple/teal, stock icons, system dialogs for key flows,
dense forms, light-gray-on-white text, neon rainbow gradients, HUD clutter, fake "scanning" effects over the retina
image, anything that looks like a template.

## Generated media (§13.1) — MUST
1. **Never generate fundus/retinal images for the ML pipeline.** `fixtures/`, training, validation and test data
   are real images from the licensed public datasets in §4.1 only.
2. **Never show a generated image where a real retinal photo or real result belongs.** Eye illustrations in
   onboarding/explainers must be obviously illustrative (flat/graphic), never photoreal.
3. **Never upload participant data to Higgsfield** — no retinal photos, no identifiable participants, no signatures.
   Only team members with signed media releases appear in uploaded footage.
4. **No fabricated evidence.** Generated video may dramatize *how* screening works, never a result, statistic,
   partnership or testimonial that didn't happen. Every generated pitch clip carries an on-screen "Illustration" tag;
   `assets/ASSETS.md` records real vs. generated.
5. **No real people's likenesses** other than consenting team members; no brand logos (Samsung, clinics, datasets);
   no depictions of minors as participants.
6. **Cost discipline.** Call `balance` at the start of a media session; log credits per asset group in `ASSETS.md`.
   Use batch tools + `jobs_wait` + one `show_generation_by_ids`. Generate at working resolution; upscale finalists only.
7. **Provenance.** Every file in `assets/` has an `ASSETS.md` entry: tool, model, prompt, job id, date, license, usage.
Before the first Higgsfield call of a session, call `get_workflow_instructions` with no argument.

## Voices (§13.8) — decided
All spoken audio is recorded by native-speaking students (EN/ES/VI). Android TTS is the development placeholder
only. Scripts come from `strings.xml` / `strings_medical.xml` via `docs/audio_manifest.csv`. **Never** clone,
synthesize or modify student voices with Higgsfield or any other tool beyond local level normalization and
noise reduction. No `generate_audio`, `dubbing` or `voice_change` on voice; `generate_audio` is for music/SFX only.

## Current decisions (see DECISIONS.md for the log)
- Stack: Kotlin native (Jetpack Compose, CameraX + Camera2 interop, LiteRT). Lens: 20D for the first build.
- Python env: `.venv` at repo root (`python -m venv .venv`), requirements in `ml/requirements.txt`.
- Local Android SDK for this machine: `C:\Users\jiang\Android\Sdk` (platform 35, build-tools 35.0.0); set in
  `app/local.properties` (ignored by git).
