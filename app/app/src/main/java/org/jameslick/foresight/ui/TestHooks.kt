package org.jameslick.foresight.ui

import android.graphics.Bitmap

/**
 * Seam for the instrumented end-to-end test (spec §8): when `bitmapProvider` is set, the capture screen's
 * "Choose a photo" button uses it instead of opening the system photo picker, so a fixture image can be
 * injected through the real gallery-import path. Null in normal use.
 */
object TestHooks {
    @Volatile var bitmapProvider: ((eye: String) -> Bitmap?)? = null
}
