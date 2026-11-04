# firmware/ — Mode B adapter controller (spec §3.5, §7.2)

Mode B is optional (Phase 2 ships Mode A, see `../wiring_mode_a.md`). `foresight_adapter.ino` is the
reference sketch for a Seeed XIAO nRF52840 with the ArduinoBLE library.

## The rule
Every safety limit is a constant in the sketch and in the series resistors on the LED lines. The app can
ask for a shorter flash or no light; it cannot ask for more than `MAX_FLASH_MS` (50 ms) or a brighter
alignment light than `MAX_RED_DUTY`. Changing those constants is a hardware change that reopens `SAFETY.md`.

## Protocol (ASCII over one BLE write characteristic; replies on the notify characteristic)
| Command | Effect | Reply |
|---|---|---|
| `ALIGN_ON` / `ALIGN_OFF` | alignment LED at the capped duty / off | `ALIGN_ON` / `ALIGN_OFF` |
| `CAPTURE_FLASH:<ms>` | white LED for min(ms, 50) ms, alignment LED off meanwhile | `FLASHED:<ms>` or `BUSY` (< 500 ms since last) |
| `OFF` | everything off | `OFF` |
| `PING` | liveness | `OK <version>` |

The app side is `AdapterLink` (`app/.../capture/AdapterLink.kt`): `ManualSwitch` (Mode A, prompts the
screener) and `Ble` (Mode B, sends these commands). Both expose the same three calls.

## Build
Arduino IDE → Boards Manager "Seeed nRF52 Boards" → XIAO nRF52840; Library Manager → ArduinoBLE. Flash,
then test from any BLE scanner app: write `PING`, expect `OK 0.1.0`. The UUIDs in the sketch are
placeholders; generate real ones (uuidgen) before a release and mirror them in `AdapterLink.Ble`.
