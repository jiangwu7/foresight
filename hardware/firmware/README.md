# firmware/ — Mode B adapter controller (spec §3.5, §7.2)

Phase 3 (optional). A Seeed XIAO (nRF52840 or RP2040) exposing a BLE characteristic that accepts
`ALIGN_ON`, `CAPTURE_FLASH(ms)`, `OFF`. Flash duration (≤ 50 ms) and LED current are capped **in firmware**
and cannot be raised from the app. Mode A (manual switch) needs no firmware and is what Phase 2 uses.
