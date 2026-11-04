# Mode A wiring — manual LED control with hardware limits (spec §3.5, §7.2)

Mode A needs no microcontroller. The screener flips the alignment LED on, aligns, and presses the capture
button when the app says "Hold still". The app never drives the LEDs; it only tells the screener when.

## Why this circuit is safe by construction
- **Current is fixed by series resistors.** Nothing in software, and no button, can raise it.
- **The capture flash is fixed by a one-shot timer.** A 555 in monostable mode turns the white LED on for a
  fixed ~40 ms per button press, however long the button is held. Holding or hammering the button cannot
  make a longer exposure (the 555 ignores re-triggers while timing).
- **Power is 5 V from a USB power bank**: no exposed mains, easy to replace.

## Parts
| Ref | Part | Value | Notes |
|---|---|---|---|
| D1 | Alignment LED | 5 mm 660 nm deep red | mounted in the illumination module's left seat |
| D2 | Capture LED | 5 mm warm white, CRI ≥ 90 | right seat |
| R1 | Series resistor for D1 | **620 Ω** | (5 V − 2.0 V) / 620 Ω ≈ 4.8 mA — dim on purpose |
| R2 | Series resistor for D2 | **100 Ω** | (5 V − 3.1 V) / 100 Ω ≈ 19 mA |
| SW1 | Toggle switch | SPST | alignment LED on/off |
| SW2 | Momentary push button | NO | capture trigger |
| U1 | 555 timer | NE555 / TLC555 | monostable |
| R3 | Timing resistor | **36 kΩ** | t = 1.1 · R3 · C1 = 1.1 · 36 kΩ · 1 µF ≈ **40 ms** (< 50 ms limit) |
| C1 | Timing capacitor | 1 µF film/ceramic (X7R) | not electrolytic (tolerance) |
| C2 | Control-pin capacitor | 10 nF | pin 5 to GND |
| R4 | Trigger pull-up | 10 kΩ | pin 2 to +5 V; SW2 pulls pin 2 to GND |
| C3 | Decoupling | 100 nF | across U1 supply |

If the measured flash is longer than 50 ms (tolerances), use R3 = 33 kΩ. Never exceed 43 kΩ with C1 = 1 µF.

## Schematic (text)
```
+5V ──┬────────────────────────────── U1 pin 8 (VCC), pin 4 (RESET)
      │
      ├── SW1 ── R1 (620 Ω) ── D1 (red, anode→cathode) ── GND        alignment LED, manual on/off
      │
      ├── R4 (10 kΩ) ──┬── U1 pin 2 (TRIG)
      │                └── SW2 (push) ── GND                           press = trigger
      │
      ├── R3 (36 kΩ) ──┬── U1 pin 6 (THR) & pin 7 (DIS)
      │                └── C1 (1 µF) ── GND
      │
      U1 pin 5 ── C2 (10 nF) ── GND          U1 pin 1 ── GND
      U1 pin 3 (OUT) ── R2 (100 Ω) ── D2 (white) ── GND                  one 40 ms flash per press
```
The 555 output sources ~20 mA directly; no transistor needed at this current. Keep LED leads short and
run both LEDs in matte-black heat-shrink so no stray light leaves the module.

## Bring-up checklist (bench, no eye)
1. Power from the USB bank. Alignment LED on with SW1: measure the voltage across R1 → I = V/620 Ω ≈ 4.8 mA.
2. Press SW2 while watching the white LED: a brief flash. Measure the pulse with the phone's slow-motion
   video (240 fps → one frame ≈ 4 ms; the flash should span ≤ 12 frames) or an oscilloscope. Record it in
   `SAFETY.md` §5.
3. Hold SW2 down for 5 s: the LED must flash once and stay off.
4. Measure corneal-plane irradiance of both LEDs with the lux/irradiance meter at 50 mm from the lens;
   enter the values in `SAFETY.md` §5.

## What the app does in Mode A
`AdapterLink.ManualSwitch`: "Turn on the red light" prompt on entering the capture screen; "Press the
capture button" + haptic when the quality model says the frame is good; the still is taken ~150 ms later
to coincide with the flash (the app keeps the shutter open through the flash window using a fixed exposure).
