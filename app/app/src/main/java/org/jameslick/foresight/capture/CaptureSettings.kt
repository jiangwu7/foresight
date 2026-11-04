package org.jameslick.foresight.capture

import android.content.Context

/**
 * Per-phone capture calibration (spec §3.2 "calibrated per phone model in Settings") and the adapter mode.
 * Plain SharedPreferences: a handful of numbers, nothing sensitive.
 *
 * Focus is set as the distance from the camera to the aerial image in millimetres, which is what a student
 * can measure with a ruler on the rail; the camera wants diopters (1000 / mm). Exposure for the alignment
 * phase is long and high-ISO (the red LED is dim on purpose, spec §7.2). The capture phase is a short,
 * low-ISO exposure run at full frame rate: in Mode A the app watches those frames and keeps the one where
 * the screener's 40 ms flash appears ([FlashCatcher]); Mode B (Phase 3) will fire the flash itself.
 * Phones cap the exposure time (S23 Ultra: 150 ms) and the closest focus (S23 Ultra: 100 mm); the
 * controller clamps to the camera's advertised ranges.
 */
data class CaptureSettings(
    val focusMm: Float = 100f,
    val alignIso: Int = 1600,
    val alignShutterMs: Float = 33f,
    val captureIso: Int = 200,
    val captureShutterMs: Float = 33f,
    val adapterMode: AdapterMode = AdapterMode.MANUAL,
    /** Use the gallery import flow instead of the camera (development, reference-rig photos). */
    val importMode: Boolean = false,
    val redMode: Boolean = true,
    /** Ask the six optional intake questions (spec §3.3) between consent and capture. Off by default since the
     *  2026-11-04 user tests: the default flow is language → consent → camera. Research events turn it on. */
    val askIntake: Boolean = false,
) {
    val focusDiopters: Float get() = 1000f / focusMm.coerceAtLeast(10f)
    val alignShutterNs: Long get() = (alignShutterMs * 1_000_000f).toLong()
    val captureShutterNs: Long get() = (captureShutterMs * 1_000_000f).toLong()

    companion object {
        private const val PREFS = "capture_settings"

        fun load(context: Context): CaptureSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val d = CaptureSettings()
            return CaptureSettings(
                focusMm = p.getFloat("focus_mm", d.focusMm),
                alignIso = p.getInt("align_iso", d.alignIso),
                alignShutterMs = p.getFloat("align_shutter_ms", d.alignShutterMs),
                captureIso = p.getInt("capture_iso", d.captureIso),
                captureShutterMs = p.getFloat("capture_shutter_ms", d.captureShutterMs),
                adapterMode = runCatching { AdapterMode.valueOf(p.getString("adapter_mode", d.adapterMode.name)!!) }.getOrDefault(d.adapterMode),
                importMode = p.getBoolean("import_mode", d.importMode),
                redMode = p.getBoolean("red_mode", d.redMode),
                askIntake = p.getBoolean("ask_intake", d.askIntake),
            )
        }

        fun save(context: Context, s: CaptureSettings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat("focus_mm", s.focusMm).putInt("align_iso", s.alignIso).putFloat("align_shutter_ms", s.alignShutterMs)
                .putInt("capture_iso", s.captureIso).putFloat("capture_shutter_ms", s.captureShutterMs)
                .putString("adapter_mode", s.adapterMode.name).putBoolean("import_mode", s.importMode).putBoolean("red_mode", s.redMode).putBoolean("ask_intake", s.askIntake)
                .apply()
        }
    }
}
