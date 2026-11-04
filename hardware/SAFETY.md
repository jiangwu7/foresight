# SAFETY.md — Light-hazard assessment for the Foresight adapter (spec §7.2)

> **STATUS: DRAFT CALCULATION (Phase 2), UNSIGNED. Do not use the device on any person.**
> The numbers below are estimates from assumed datasheet values (every one is tagged ASSUMED). They become
> a real assessment only after (a) the real LED datasheets are substituted, (b) the corneal-plane
> irradiance is measured, and (c) the partnering clinician signs §7. Until then the only subject is a
> model eye.

## 1. Standard
ISO 15004-2:2007 *Ophthalmic instruments — Fundamental requirements and test methods — Part 2: Light hazard
protection*. Target classification: **Group 1** (no potential light hazard) for both light sources.
Group 1 limits used here (verify against the purchased text of the standard before signing):

| Quantity | Symbol | Group 1 limit |
|---|---|---|
| Weighted photochemical retinal irradiance (aphakic weighting A(λ)) | E_A-R | ≤ 2.2 mW/cm² |
| Unweighted visible + infrared retinal irradiance | E_VIR-R | ≤ 0.7 W/cm² |
| Corneal/lens UV-A and IR irradiance | | no UV or IR emission from these LEDs → negligible |

## 2. Light sources
| Source | Part | Peak wavelength | Drive current (hard limit) | Duty | Role |
|---|---|---|---|---|---|
| Alignment LED | ASSUMED 5 mm AlGaInP, Kingbright WP7113SRC/J-type | ≈ 660 nm | **4.8 mA** (5 V, 620 Ω series) | continuous, dim, ≤ 120 s per eye | alignment while the pupil stays wide |
| Capture LED | ASSUMED 5 mm warm white 2700–3000 K, CRI ≥ 90 | white | **19 mA** (5 V, 100 Ω series) | **40 ms** one-shot (555 monostable), ≤ 6 per participant | single still |

Hard limits: LED currents are fixed by series resistors (Mode A, `wiring_mode_a.md`) and additionally capped
in firmware (Mode B, `firmware/`). The flash length is fixed by a 555 one-shot in Mode A (holding the button
cannot extend it) and by `MAX_FLASH_MS = 50` in Mode B. The app cannot raise any of them. The Godox
speedlight or any photographic flash is never used.

## 3. Optical path
LED → linear polarizer (T ≈ 0.38, ASSUMED) → 45° plate beamsplitter (R ≈ 0.50, ASSUMED) → 20D condensing
lens (T ≈ 0.92, ASSUMED) → pupil → retina. The LED is imaged into the pupil plane (Maxwellian view), so the
light that passes the pupil is spread over the illuminated retinal patch (≈ 45° field → 14.1 mm diameter →
1.56 cm²). Geometric collection into the pupil image is ASSUMED at 10 % of the LED's emitted power; the
real figure is set by the port geometry and will be bounded by measurement.
Diagram: `assets/hardware/light_path.png` (Phase 2, §13.4). Geometry: `adapter.scad` → `renders/`.

## 4. Calculations (draft, `safety_calc.py` → `safety_calc.xlsx` / `safety_calc.json`)
Method: E_retina = P_pupil / A_retina, with P_pupil = P_LED × T_pol × R_bs × T_lens × f_collect.
E_A-R applies the aphakic weighting: A(660 nm) ≈ 0.001 (ISO table); for the warm-white LED a
spectrum-averaged weight of 0.20 is ASSUMED (conservative; replace by the integral over the measured
spectrum). Worst case: 8 mm pupil; alignment 120 s continuous per eye; capture 40 ms × 6 flashes.

| Source | P_LED (mW, at drive current) | P_pupil (mW) | E_VIR-R (W/cm²) | margin to 0.7 | E_A-R (mW/cm²) | margin to 2.2 | H_A-R dose (J/cm²) | Group 1? |
|---|---|---|---|---|---|---|---|---|
| Alignment LED (660 nm, 4.8 mA) | 1.0 (ASSUMED 4 mW @ 20 mA) | 0.0175 | 1.1 × 10⁻⁵ | ≈ 60 000× | 1.1 × 10⁻⁵ | ≈ 200 000× | < 10⁻⁵ | yes |
| Capture LED (warm white, 19 mA) | 15 (ASSUMED ≈ 5 lm) | 0.26 | 1.7 × 10⁻⁴ | ≈ 4 000× | 0.034 | ≈ 65× | 1 × 10⁻⁵ | yes |

Reading: even if the collection fraction were 100 % instead of 10 % and the white LED three times brighter
than assumed, both sources stay inside Group 1 by more than 2×. The capture LED's photochemical term is
the binding one (its blue content); the deep-red alignment LED is negligible by every measure. This is
why the adapter aligns in red and flashes white only briefly.

Assumptions to replace before signing: LED radiant power and spectrum (datasheets), polarizer and
beamsplitter transmission (datasheets), collection fraction (measurement, §5), flash length (measured
with slow-motion video or a scope, `wiring_mode_a.md` step 2).

## 5. Measured values (once a lux/irradiance meter is available)
| Source | Measured irradiance at the corneal plane (mW/cm²) | Instrument | Date | By |
|---|---|---|---|---|
| Alignment LED | — | — | — | — |
| Capture LED (per flash) | — | — | — | — |
| Flash duration (ms) | — | — | — | — |

Convert corneal irradiance to retinal irradiance with E_retina = E_cornea × A_pupil / A_retina
(8 mm pupil: A_pupil = 0.503 cm²; A_retina = 1.56 cm² → × 0.32) and compare with §1.

## 6. Operating rules
- Alignment LED at the lowest level that gives a usable preview (4.8 mA design value).
- Max 3 capture attempts per eye; the app enforces this. Flashes are ≥ 0.5 s apart (firmware) and in
  practice ≥ 10 s apart (re-alignment).
- Minors are never screened. Participants who report photosensitivity, recent eye surgery or dilation
  drops are not screened.
- Any discomfort reported by a participant ends the session.
- Never look into the capture LED directly during bench tests; test flashes against a wall.

## 7. Sign-off
I have reviewed the sources, the calculations and the measured values above and agree that the device, as
built and configured, falls within ISO 15004-2 Group 1 limits for both light sources.

Clinician name / credentials: ______________________________  Clinic: ______________________

Signature: ______________________________  Date: ____________

Teacher / team lead: ______________________________  Date: ____________
