package org.jameslick.foresight.capture

/**
 * Mode A has no wire to the capture LED: the screener presses a button and the 555 fires a ~40 ms flash
 * at a moment the app cannot predict. Phones also cap the exposure time (the Galaxy S23 Ultra: 150 ms), so
 * "open a long shutter and hope" does not work. Instead the app watches the preview stream at full rate
 * during the capture window and keeps the frame in which the flash appears: brightness jumps far above
 * the dim-red baseline, peaks for one or two frames, then drops.
 *
 * Pure logic, fed one luma value per frame; the caller keeps the pixels of whichever frame this class
 * marks as the keeper. Resolves after the flash has passed (so the brightest frame wins) or on timeout.
 */
class FlashCatcher(
    private val baselineFrames: Int = BASELINE_FRAMES,
    private val ratio: Float = FLASH_RATIO,
    private val maxFlashFrames: Int = MAX_FLASH_FRAMES,
) {
    enum class State { Baseline, Waiting, InFlash, Done }

    var state: State = State.Baseline
        private set
    /** Index (0-based, in feed order) of the frame to keep once [state] is Done; null if none. */
    var keeper: Int? = null
        private set
    var baseline: Float = 0f
        private set

    private var n = 0
    private var sum = 0f
    private var peak = 0f
    private var framesInFlash = 0

    /** @return true when this frame should be kept as the current best candidate (the caller copies it). */
    fun feed(luma: Float): Boolean {
        val i = n++
        when (state) {
            State.Baseline -> {
                sum += luma
                if (n >= baselineFrames) { baseline = sum / n; state = State.Waiting }
                return false
            }
            State.Waiting -> {
                if (luma > threshold()) { state = State.InFlash; peak = luma; keeper = i; framesInFlash = 1; return true }
                // keep the baseline tracking slow drift (the red LED, hand movement)
                baseline = baseline * 0.9f + luma * 0.1f
                return false
            }
            State.InFlash -> {
                framesInFlash++
                if (luma > peak) { peak = luma; keeper = i; if (framesInFlash <= maxFlashFrames) return true }
                if (luma <= threshold() || framesInFlash >= maxFlashFrames) state = State.Done
                return false
            }
            State.Done -> return false
        }
    }

    fun threshold(): Float = maxOf(baseline * ratio, baseline + MIN_JUMP)

    companion object {
        const val BASELINE_FRAMES = 3
        /** A flash frame must be at least this many times brighter than the dim-red baseline. */
        const val FLASH_RATIO = 1.8f
        /** ...and at least this much brighter in absolute terms (0..255 luma), so noise on a black frame never counts. */
        const val MIN_JUMP = 12f
        const val MAX_FLASH_FRAMES = 4
        /** How long the catch window stays open after the prompt (Mode A: the screener's reaction time). */
        const val WINDOW_MS = 4000L

        /** Mean luma of an ARGB frame, sampled on a grid for speed. */
        fun luma(pixels: IntArray, width: Int, height: Int, step: Int = 8): Float {
            var sum = 0L; var count = 0
            var y = 0
            while (y < height) {
                var x = 0
                val row = y * width
                while (x < width) {
                    val p = pixels[row + x]
                    sum += (77 * ((p shr 16) and 0xFF) + 150 * ((p shr 8) and 0xFF) + 29 * (p and 0xFF)) shr 8
                    count++; x += step
                }
                y += step
            }
            return if (count == 0) 0f else sum.toFloat() / count
        }
    }
}
