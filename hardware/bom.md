# Bill of materials — Foresight adapter v1 (spec §7.1)

Status: **skeleton (Phase 0)**. Prices and sources are filled in Phase 2 when parts are ordered. Two totals
are reported as the spec requires: with a used/educational lens and with a new lens.

| # | Part | Spec | Qty | Source | Price (used/edu) | Price (new) | Notes |
|---|---|---|---|---|---|---|---|
| 1 | Condensing lens | 20D aspheric, ~50 mm (Volk-style); 28D alternative | 1 | eBay / Volk edu / surplus | ~$40–60 | ~$150 | Decision: 20D first (DECISIONS.md) |
| 2 | Alignment LED | ≈ 660 nm deep red, low power | 1 | Digi-Key / Adafruit | ~$1 | ~$1 | current fixed by series resistor |
| 3 | Capture LED | warm white, high CRI | 1 | Digi-Key / Adafruit | ~$2 | ~$2 | ≤ 50 ms flash |
| 4 | Beamsplitter | 45° glass plate 50:50, ~25 × 36 mm (or half-mirror slide) | 1 | Edmund / surplus | ~$8 | ~$15 | |
| 5 | Linear polarizer film | 2 pieces (LED path + camera path, crossed) | 1 sheet | Edmund / Amazon | ~$6 | ~$6 | |
| 6 | Series resistors, switch (Mode A) | | — | | ~$2 | ~$2 | |
| 7 | Microcontroller (Mode B, optional) | Seeed XIAO nRF52840 / RP2040 | 1 | Seeed | ~$10 | ~$10 | BLE |
| 8 | Battery / USB power | coin cell or small LiPo | 1 | | ~$5 | ~$5 | |
| 9 | 3D-printed body | PLA/PETG matte black, from `adapter.scad` | 1 | school printer | ~$3 | ~$3 | |
| 10 | Eyecup / forehead rest | soft silicone or foam | 1 | | ~$4 | ~$4 | |
| 11 | Rail locking screw, M3 hardware | | — | | ~$2 | ~$2 | |
| | **Total** | | | | **≈ $85–105** | **≈ $200** | target ≤ $100 is met only with a used/edu lens |

The adapter finish is matte black with one accent color from the chosen brand direction (§3.10.6).
