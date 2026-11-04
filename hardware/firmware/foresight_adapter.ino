/*
 * Foresight adapter — Mode B firmware skeleton (spec §3.5, §7.2). Target: Seeed XIAO nRF52840 (Arduino,
 * ArduinoBLE library). Phase 2 ships Mode A; this sketch is the reference for Mode B and compiles as-is.
 *
 * Safety limits live HERE and nowhere else. The app can only ask for less, never more:
 *   - MAX_FLASH_MS   capture flash hard cap (spec §7.2: <= 50 ms)
 *   - MAX_RED_DUTY   alignment LED PWM cap (dim on purpose)
 *   - MAX_WHITE_DUTY capture LED PWM cap
 *   - series resistors on the LED lines fix the peak current regardless of this code.
 *
 * BLE: one service, one write characteristic (commands) and one notify characteristic (status).
 * Commands are short ASCII strings so students can test them from any BLE scanner app:
 *   "ALIGN_ON"          alignment LED on at the capped duty
 *   "ALIGN_OFF"
 *   "CAPTURE_FLASH:40"  white LED on for min(40, MAX_FLASH_MS) ms, then off; alignment LED off during the flash
 *   "OFF"               everything off
 *   "PING"              reply "OK" + firmware version
 */
#include <ArduinoBLE.h>

static const char* FW_VERSION = "0.1.0";
static const int PIN_RED = D0;        // alignment LED (660 nm) through its series resistor
static const int PIN_WHITE = D1;      // capture LED (warm white) through its series resistor

static const uint32_t MAX_FLASH_MS = 50;     // ISO 15004-2 planning limit (spec §7.2); do not raise
static const uint8_t MAX_RED_DUTY = 64;      // of 255 (~25 %), dim alignment light
static const uint8_t MAX_WHITE_DUTY = 255;   // full, but only for MAX_FLASH_MS
static const uint32_t MIN_FLASH_GAP_MS = 500; // refuse flashes closer than this (no strobing)

BLEService adapterService("8f6a0001-5a6c-4b4e-9b3a-foresight000");            // placeholder UUIDs: regenerate before release
BLEStringCharacteristic cmdChar("8f6a0002-5a6c-4b4e-9b3a-foresight000", BLEWrite, 32);
BLEStringCharacteristic statusChar("8f6a0003-5a6c-4b4e-9b3a-foresight000", BLERead | BLENotify, 32);

static uint32_t lastFlashEnd = 0;

void allOff() { analogWrite(PIN_RED, 0); analogWrite(PIN_WHITE, 0); }

void flash(uint32_t requestedMs) {
  uint32_t ms = requestedMs > MAX_FLASH_MS ? MAX_FLASH_MS : requestedMs;   // the cap, enforced here
  if (millis() - lastFlashEnd < MIN_FLASH_GAP_MS) { statusChar.writeValue("BUSY"); return; }
  analogWrite(PIN_RED, 0);
  analogWrite(PIN_WHITE, MAX_WHITE_DUTY);
  delay(ms);                                   // blocking on purpose: nothing can extend it
  analogWrite(PIN_WHITE, 0);
  lastFlashEnd = millis();
  statusChar.writeValue(String("FLASHED:") + ms);
}

void handle(const String& cmd) {
  if (cmd == "ALIGN_ON") { analogWrite(PIN_RED, MAX_RED_DUTY); statusChar.writeValue("ALIGN_ON"); }
  else if (cmd == "ALIGN_OFF") { analogWrite(PIN_RED, 0); statusChar.writeValue("ALIGN_OFF"); }
  else if (cmd.startsWith("CAPTURE_FLASH:")) { flash((uint32_t)cmd.substring(14).toInt()); }
  else if (cmd == "OFF") { allOff(); statusChar.writeValue("OFF"); }
  else if (cmd == "PING") { statusChar.writeValue(String("OK ") + FW_VERSION); }
  else { statusChar.writeValue("ERR"); }
}

void setup() {
  pinMode(PIN_RED, OUTPUT); pinMode(PIN_WHITE, OUTPUT); allOff();
  if (!BLE.begin()) { while (true) { delay(1000); } }
  BLE.setLocalName("Foresight-Adapter");
  BLE.setAdvertisedService(adapterService);
  adapterService.addCharacteristic(cmdChar);
  adapterService.addCharacteristic(statusChar);
  BLE.addService(adapterService);
  statusChar.writeValue("READY");
  BLE.advertise();
}

void loop() {
  BLEDevice central = BLE.central();
  if (central) {
    while (central.connected()) {
      if (cmdChar.written()) handle(cmdChar.value());
    }
    allOff();                                   // link lost: lights off
  }
}
