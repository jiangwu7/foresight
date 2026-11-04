package org.jameslick.foresight.capture

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** How the adapter's LEDs are driven (spec §3.5). */
enum class AdapterMode { MANUAL, BLE }

/** What the screen must tell the screener right now (Mode A has no wire to the LEDs; the screener is the wire). */
enum class AdapterPrompt { TurnOnRed, PressCapture, TurnOffRed }

/**
 * The app's only way to talk to the adapter. Both modes expose the same three calls so the capture screen
 * does not care which is plugged in. Flash length is capped here too ([MAX_FLASH_MS]) even though the real
 * limit is in hardware/firmware (spec §7.2): the app can only ever ask for less.
 */
interface AdapterLink : AutoCloseable {
    val mode: AdapterMode
    val prompt: StateFlow<AdapterPrompt?>
    fun alignOn()
    fun alignOff()
    /** Fires (or asks the screener to fire) the capture flash and returns when the flash window has passed. */
    suspend fun captureFlash(ms: Int = DEFAULT_FLASH_MS)
    fun off()

    companion object {
        const val MAX_FLASH_MS = 50
        const val DEFAULT_FLASH_MS = 40
        /** Mode A: delay between the prompt and opening the still's (long) shutter window; the press is expected inside that window. */
        const val MANUAL_PRESS_LATENCY_MS = 400L

        fun create(mode: AdapterMode): AdapterLink = when (mode) {
            AdapterMode.MANUAL -> ManualSwitchLink()
            AdapterMode.BLE -> BleLink()
        }
    }
}

/** Mode A: LEDs on a switch and a 555 one-shot (hardware/wiring_mode_a.md). The app only prompts. */
class ManualSwitchLink : AdapterLink {
    override val mode = AdapterMode.MANUAL
    private val _prompt = MutableStateFlow<AdapterPrompt?>(null)
    override val prompt: StateFlow<AdapterPrompt?> = _prompt

    override fun alignOn() { _prompt.value = AdapterPrompt.TurnOnRed }
    override fun alignOff() { _prompt.value = AdapterPrompt.TurnOffRed }
    override suspend fun captureFlash(ms: Int) {
        _prompt.value = AdapterPrompt.PressCapture
        delay(AdapterLink.MANUAL_PRESS_LATENCY_MS)
        _prompt.value = null
    }
    override fun off() { _prompt.value = AdapterPrompt.TurnOffRed }
    override fun close() { _prompt.value = null }
}

/**
 * Mode B placeholder (Phase 3): the BLE transport is not implemented and the app declares no Bluetooth
 * permission yet. It behaves exactly like Mode A so the rest of the app can be exercised, and documents
 * the command strings the firmware (hardware/firmware/) expects.
 */
class BleLink : AdapterLink {
    override val mode = AdapterMode.BLE
    private val manual = ManualSwitchLink()
    override val prompt: StateFlow<AdapterPrompt?> = manual.prompt
    override fun alignOn() = manual.alignOn()
    override fun alignOff() = manual.alignOff()
    override suspend fun captureFlash(ms: Int) = manual.captureFlash(minOf(ms, AdapterLink.MAX_FLASH_MS))
    override fun off() = manual.off()
    override fun close() = manual.close()

    companion object {
        const val CMD_ALIGN_ON = "ALIGN_ON"
        const val CMD_ALIGN_OFF = "ALIGN_OFF"
        const val CMD_CAPTURE_FLASH = "CAPTURE_FLASH:"
        const val CMD_OFF = "OFF"
    }
}
