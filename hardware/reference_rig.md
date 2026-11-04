# Reference rig — Nikon Z8 + 24-120 mm f/4 S (spec §7.3) — lab use only

Purpose: high-quality reference photographs of the *same* eyes (model eye, or consenting adult team member
after `SAFETY.md` is signed) as the phone adapter, for the resolution experiment (§4.6) and for judging
how much the $100 adapter loses. Never a screening device. Same safety rules as the adapter.

## Optical layout
```
eye ── 50 mm ── 20D lens ── 50 mm ── [aerial image] ── ~300 mm ── Z8 + 24-120 @ 120 mm
                          ╲
                           45° beamsplitter ← polarizer ← LEDs (same illumination module as the adapter)
```
- The 20D lens sits ≈ 50 mm from the cornea (eyecup from the adapter, no phone clamp).
- The aerial image forms ≈ 50 mm on the camera side of the lens; the Z8 focuses on it at ≈ 0.35 m from the
  sensor, 120 mm focal length → ≈ 0.39× magnification, ≈ 3,500 px across the fundus.
- Illumination: the adapter's illumination module on a small stand, beamsplitter centred on the axis.
  The Z8's sensitivity means the alignment LED alone (no flash) is often enough: dimmer than the phone,
  never brighter.

## Camera settings (starting point, record what you actually used in the log)
| Setting | Value | Why |
|---|---|---|
| Mode | M, manual focus | nothing hunts during capture |
| Focal length | 120 mm | fills the frame with the aerial image |
| Aperture | f/5.6 | depth of field around the aerial plane; f/4 if light is short |
| Shutter | 1/60 s with the alignment LED; 1/200 s with the flash | ≤ flash window |
| ISO | 3200–6400 | the Z8 is clean here; prefer ISO over brighter light |
| White balance | 3000 K fixed | matches the warm LED; keep constant across sessions |
| File | RAW + JPEG fine | RAW for the resolution experiment |
| Flash | **built-in / Godox: OFF** | spec §7.2 — photographic flashes are never used |
| Stabilization | off, on a tripod | |

## Procedure (two people; one is the operator, the other the participant or holds the model eye)
1. Room lights off; red safelight only. Operator and participant seated; chin on the rest.
2. Alignment LED on (dim). Centre the pupil in the lens; find the red retinal glow in the viewfinder.
3. Focus the Z8 on the aerial image (vessels sharp), not on the lens surface.
4. Take 3 frames per eye at 1/60 s with the alignment LED; if too dark, one frame with the capture flash
   (press the Mode A button while the shutter is open at 1/15 s; the flash is 40 ms).
5. Log: date, operator, participant/model-eye id, eye, settings, LED current, file names, any discomfort.
6. Same eye immediately after with the phone adapter, same illumination settings, for the paired comparison.

## Log
`hardware/rig_log.csv` (created at first use): `date,operator,subject,eye,frame,shutter,iso,aperture,led,notes`.
