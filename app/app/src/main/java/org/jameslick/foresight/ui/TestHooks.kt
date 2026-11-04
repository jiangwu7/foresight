package org.jameslick.foresight.ui

import android.graphics.Bitmap

/**
 * Seams for the instrumented tests (spec §8). Null in normal use.
 *
 * - [bitmapProvider]: the gallery-import capture screen's "Choose a photo" uses it instead of the system
 *   photo picker, so a fixture image goes through the real import path (Phase 1 end-to-end test).
 * - [frameProvider]: the live capture screen reads preview "frames" from it instead of the camera, at the
 *   normal analysis rate, and uses the same bitmap as the captured still. Lets the auto-capture test loop a
 *   good frame (must trigger) and a degraded one (must never trigger) on a device without an adapter.
 */
object TestHooks {
    @Volatile var bitmapProvider: ((eye: String) -> Bitmap?)? = null
    @Volatile var frameProvider: (() -> Bitmap?)? = null
}
