package org.jameslick.foresight.capture

import android.content.Context

/**
 * Per-phone capture calibration (spec §3.2 "calibrated per phone model in Settings") and the adapter mode.
 * Plain SharedPreferences: a handful of numbers, nothing sensitive.
 *
 * Focus is set as the distance from the camera to the aerial image in millimetres, which is what a student
 * can measure with a ruler on the rail; the camera wants diopters (1000 / mm). Exposure for the alignment
 * phase is long and high-ISO (the red LED is dim on purpose, spec §7.2). In Mode A the capture phase opens a
 * long, low-ISO window (default 500 ms) so the screener's 40 ms button-press flash lands inside it without
 * exact timing; Mode B (Phase 3) fires the flash itself and can use a short window. Phones cap the exposure
 * time; the controller clamps to the camera's range.
 */
data class CaptureSettings(
    val focusMm: Float = 90f,
    val alignIso: Int = 1600,
    val alignShutterMs: Float = 33f,
    val captureIso: Int = 200,
    val captureShutterMs: Float = 500f,
    val adapterMode: AdapterMode = AdapterMode.MANUAL,
    /** Use the gallery import flow instead of the camera (development, reference-rig photos). */
    val importMode: Boolean = false,
    val redMode: Boolean = true,
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
            )
        }

        fun save(context: Context, s: CaptureSettings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat("focus_mm", s.focusMm).putInt("align_iso", s.alignIso).putFloat("align_shutter_ms", s.alignShutterMs)
                .putInt("capture_iso", s.captureIso).putFloat("capture_shutter_ms", s.captureShutterMs)
                .putString("adapter_mode", s.adapterMode.name).putBoolean("import_mode", s.importMode).putBoolean("red_mode", s.redMode)
                .apply()
        }
    }
}
