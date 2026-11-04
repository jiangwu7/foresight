# Bill of materials — Foresight adapter v1 (spec §7.1)

Status: **Phase 2 draft with researched prices (2026-11-04).** Prices are asking prices from catalog pages
and marketplace listings, not purchase receipts; confirm before ordering. Two totals as the spec requires.

| # | Part | Spec | Qty | Source (checked) | Price (used/edu) | Price (new) | Notes |
|---|---|---|---|---|---|---|---|
| 1 | Condensing lens | 20D double-aspheric, ≈ 50 mm (Volk 20D or equivalent) | 1 | used: DOTmed listings $210–215; Ketchum (edu) $394; new: volk.com $399–532 | **$210** | **$399** | The whole budget problem. Generic "20D aspheric" lenses exist on Alibaba for far less but are **unverified** optically; one should be bought and compared on the reference rig before it is specified. |
| 2 | Alignment LED | 5 mm 660 nm AlGaInP (e.g. Kingbright WP7113SRC/J) | 1 | Digi-Key / Mouser | $0.50 | $0.50 | current fixed by R1 = 620 Ω → 4.8 mA |
| 3 | Capture LED | 5 mm warm white 2700–3000 K; **CRI ≥ 90 not found in 5 mm through-hole** (Cree C513A warm white is CRI ≈ 80) | 1 | Digi-Key / element14 | $1 | $1 | Accept CRI 80 for v1, or use a CRI 90+ 3 mm/SMD warm-white (e.g. Nichia/Lumileds) on a tiny carrier; ≤ 40 ms flash, R2 = 100 Ω → 19 mA |
| 4 | Beamsplitter | 50:50 plate, 25 × 38 mm, VIS, 1–3 mm | 1 | Edmund Optics #37-198 (1 mm) / #31-436 (3 mm float glass, ≈ ₹5,220 ≈ $60) / #47-023 N-BK7 €156 | $15 (surplus / half-mirror microscope slide) | $60 | Budget alternative: a 25 × 36 mm piece cut from an acrylic "two-way mirror" sheet (≈ $10); more loss, same geometry |
| 5 | Linear polarizer film | 2 pieces (LED path 16 × 36 mm, camera path 30 × 30 mm) | 1 sheet | Edmund / Amazon (sheet 100 × 100 mm) | $6 | $6 | cut with scissors; mark the axis |
| 6 | Mode A electronics | NE555, R 620/100/36k/10k Ω, C 1 µF/10 nF/100 nF, SPST toggle, push button, proto board, USB cable | 1 set | Digi-Key / Tayda | $4 | $4 | `wiring_mode_a.md` |
| 7 | Microcontroller (Mode B, optional) | Seeed XIAO nRF52840 | 1 | Seeed | $10 | $10 | `firmware/` |
| 8 | Power | USB power bank (any) or 3 × AA holder | 1 | | $5 | $8 | 5 V |
| 9 | 3D-printed body | 5 parts from `adapter.scad`, matte black PLA/PETG, ≈ 180 g | 1 | school printer | $4 | $4 | `stl/` |
| 10 | Eyecup pad / forehead pad | foam or silicone, self-adhesive | 1 | | $3 | $3 | |
| 11 | Hardware | M3 × 8 (10), M3 × 12 (4), heat-shrink, black tape | — | | $3 | $3 | |
| | **Total without lens** | | | | **≈ $41** | **≈ $100** | |
| | **Total with lens** | | | | **≈ $250** | **≈ $500** | |

## The honest number
Everything except the lens fits the $100 target with room to spare. A used Volk 20D costs about $210 on the
secondary market today, not the $40–60 assumed in Phase 0, so the all-in cost of adapter v1 is **≈ $250**
with a used lens. Options, in the order we will try them:
1. **Borrowed/donated lens** from the partner clinic or an optometry school for the pilot (cost to the
   team: $0; the BOM stays ≈ $41).
2. **Generic 20D aspheric** (Alibaba/AliExpress, $25–60): buy one, image the model eye on the reference
   rig with it and with a Volk, and compare sharpness and field. If it is within 10 % it becomes the spec.
3. **Volk 20D used** ($210) as the reference lens the team keeps regardless.
The competition narrative should state the two totals plainly: ≈ $41 for the adapter, plus the lens.

Sources checked 2026-11-04: [DOTmed used 20D listing](https://www.dotmed.com/listing/ophthalmology-general/volk/20d/double-aspheric-lens/5344775),
[Ketchum store](https://mbkustore.ketchum.edu/products/cond-lens-volk-20d), [Volk 20D collection](https://www.volk.com/collections/all/20d),
[Edmund 25 × 38 mm 50:50 1 mm](https://www.edmundoptics.com/p/25-x-38mm-50-50rt-vis-plate-beamsplitter/37198/),
[Edmund 25 × 38 mm N-BK7](https://www.edmundoptics.eu/f/PdfExport/12584), [Cree C513A warm white](https://my.element14.com/cree-led/c513a-msn-cw0z0132/led-5mm-12cd-warm-white/dp/2579583).

The adapter finish is matte black with one accent color from the chosen brand direction (§3.10.6): the
amber from the logo on the rail scale and the eyecup rim.
