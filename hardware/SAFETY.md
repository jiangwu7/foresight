# SAFETY.md — Light-hazard assessment for the Foresight adapter (spec §7.2)

> **STATUS: INCOMPLETE AND UNSIGNED. Do not use the device on any person.**
> This file is completed in Phase 2 and must be signed by the partnering clinician before any human —
> including a consenting team member — is imaged.

## 1. Standard
ISO 15004-2:2007 *Ophthalmic instruments — Fundamental requirements and test methods — Part 2: Light hazard
protection*. Target classification: **Group 1** (no potential light hazard) for both light sources.

## 2. Light sources
| Source | Part | Peak wavelength | Drive current (hard limit) | Duty | Role |
|---|---|---|---|---|---|
| Alignment LED | TBD (≈ 660 nm deep red) | TBD | TBD mA (series resistor) | continuous, dim | alignment while pupil stays wide |
| Capture LED | TBD (warm white, CRI ≥ 90) | TBD | TBD mA (series resistor) | ≤ 50 ms flash | single still |

Hard limits: LED currents are fixed by series resistors (Mode A) and additionally capped in firmware (Mode B).
The app cannot raise them. Capture flash ≤ 50 ms. The Godox speedlight or any photographic flash is never used.

## 3. Optical path
Light path: LED → linear polarizer → 45° beamsplitter → 20D condensing lens → pupil → retina.
Retinal irradiance depends on LED radiance, the beamsplitter transmission, lens losses, and pupil area.
Diagram: `assets/hardware/light_path.png` (Phase 2, §13.4).

## 4. Calculations (to complete in Phase 2)
For each source, per ISO 15004-2 §5:
- Weighted retinal irradiance (photochemical, aphakic weighting) E_A-R = … W/cm²  (limit Group 1: ≤ 2.2 mW/cm²)
- Retinal thermal irradiance E_VIR-R = … W/cm²  (limit: ≤ 0.7 W/cm² for ≥ 1 s... use the time-dependent limit for the 50 ms flash)
- Corneal/lens UV and IR irradiance (expected negligible: no UV or IR emission)
- Assumptions: pupil diameter 8 mm (dark-adapted, worst case), exposure duration worst case (alignment:
  continuous for 120 s; capture: 50 ms × 6 flashes per participant).
Worked numbers, datasheets and spreadsheet: `hardware/safety_calc.xlsx` (Phase 2).

## 5. Measured values (once a lux/irradiance meter is available)
| Source | Measured irradiance at the corneal plane | Instrument | Date | By |
|---|---|---|---|---|
| Alignment LED | — | — | — | — |
| Capture LED | — | — | — | — |

## 6. Operating rules
- Alignment LED at the lowest level that gives a usable preview.
- Max 3 capture attempts per eye; the app enforces this.
- Minors are never screened.
- Any discomfort reported by a participant ends the session.

## 7. Sign-off
I have reviewed the sources, the calculations and the measured values above and agree that the device, as
built and configured, falls within ISO 15004-2 Group 1 limits for both light sources.

Clinician name / credentials: ______________________________  Clinic: ______________________

Signature: ______________________________  Date: ____________

Teacher / team lead: ______________________________  Date: ____________
