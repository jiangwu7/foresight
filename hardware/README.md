# hardware/ — the Foresight adapter (spec §7)

A 3D-printed clamp that holds a 20D condensing lens in front of a phone camera, lights the eye coaxially
through a beamsplitter, and rests on the participant's brow. Everything is parametric: change the phone
numbers at the top of `adapter.scad` and reprint.

## Print it in under 10 minutes of your time
1. Install OpenSCAD (`winget install OpenSCAD.OpenSCAD` or openscad.org).
2. Edit the **PARAMETERS** block in `adapter.scad`: `phone_width`, `phone_thickness`, `camera_offset_x/y`
   (measure the main camera's centre from the phone's left and top edges with the screen up).
3. `make` (or run the `openscad` lines in `Makefile` by hand) → `stl/*.stl`, one file per part.
4. Print all five parts in **matte black** PLA/PETG, 0.2 mm layers, 3 walls, 20 % infill. Supports only
   for the eyecup. Order of assembly: clamp → illumination module (3 × M3) → rail (plugs in) → carriage
   (slides, M3 locking screw) → lens (3 × M3 set screws) → eyecup (4 × M3).
5. Wire Mode A per `wiring_mode_a.md`. Do the bench checklist there. Do **not** point it at anyone.

## Files
| File | What |
|---|---|
| `adapter.scad` | the model; `part="assembly"` for the whole thing, or one part name for printing |
| `stl/` | exported parts (regenerate after editing parameters) |
| `renders/` | OpenSCAD previews of the assembly, exploded view and illumination module |
| `wiring_mode_a.md` | LEDs on a switch + a 555 one-shot that fixes the flash at 40 ms |
| `firmware/` | Mode B (BLE) reference sketch with the limits in code |
| `SAFETY.md` | ISO 15004-2 assessment: draft numbers from `safety_calc.py`, sign-off block |
| `safety_calc.py` → `.xlsx/.json` | the calculation, every assumption tagged |
| `bom.md` | parts, sources, two totals |
| `reference_rig.md` | the Nikon Z8 lab rig and its procedure |

## How the optics work (one paragraph for the team)
A 20D lens held ≈ 50 mm from the eye forms a real, inverted image of the retina ≈ 50 mm on the other side of
the lens, floating in air (the "aerial image"). The phone focuses on that plane, 60–120 mm from the camera
depending on its minimum focus distance, which is what the sliding rail sets. To see the retina you must
light it through the same pupil the camera looks through, so a dim deep-red LED (the pupil barely reacts to
it) is bounced off a 45° beamsplitter onto the optical axis; the camera looks straight through the plate.
Crossed polarizers on the LED and camera paths kill the bright reflection off the cornea. A brief warm-white
flash takes the actual picture. `SAFETY.md` shows both lights are far below the no-hazard limit.
