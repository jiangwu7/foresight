package org.jameslick.foresight.ml

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Phase 1 gradability check: a hand-written stand-in for the M1 quality model (spec §4.3), which arrives in
 * Phase 2 with the live camera. It answers the one question the flow needs now — "is this still clear enough
 * to grade?" — and names the problem the same four ways M1 will (dark / blurry / off-center / reflection), so
 * the UI and strings are final even though the detector is provisional.
 *
 * Works on the 256-px square produced by `Preprocess.cropAndResize` (float RGB 0..255, planar):
 *   dark        mean brightness inside the fundus disc below a floor
 *   reflection  too many near-white pixels inside the disc
 *   off-center  the fundus bounding box touches two or more image edges, or its area is tiny
 *   blurry      low Laplacian variance of the green channel (classic focus measure)
 * Thresholds were set by hand on the fixture set; `QualityHeuristicTest` pins them.
 */
object QualityHeuristic {
    enum class Problem { Dark, Blurry, OffCenter, Reflection }

    data class Result(val gradable: Boolean, val score: Float, val problem: Problem?, val details: Map<String, Float>)

    const val SIZE = 256
    // Thresholds pinned by QualityHeuristicTest on the APTOS fixtures (measured ranges in comments).
    const val DARK_MEAN_MAX = 28f          // mean of max(R,G,B) inside the disc; gradable fixtures ≥ 58
    const val REFLECTION_FRAC_MAX = 0.012f // fraction of disc pixels with all channels > 240; gradable = 0.000, reflection fixtures ≥ 0.022
    const val BLUR_VAR_MIN = 35f           // Laplacian variance (green); gradable ≥ 51, blur fixtures ≤ 20
    const val MIN_DISC_FRAC = 0.35f        // disc must fill at least this much of the frame's longer side
    const val MAX_CENTER_OFFSET = 0.12f    // fundus center may drift this far (fraction of frame) from the middle

    /** @param pixels ARGB from the original photo; `cfg` only needs the size and threshold. */
    fun assess(pixels: IntArray, width: Int, height: Int): Result {
        val bbox = Preprocess.findFundusBbox(pixels, width, height, 10)
        val bw = bbox[2] - bbox[0]; val bh = bbox[3] - bbox[1]
        val discFrac = max(bw, bh).toFloat() / max(width, height).toFloat()
        // Clinical photos legitimately touch the frame edges (tight crops), so "off-center" means the fundus
        // CENTER has drifted away from the frame center, or the fundus is tiny.
        val cx = (bbox[0] + bbox[2]) / 2f / width; val cy = (bbox[1] + bbox[3]) / 2f / height
        val centerOffset = max(abs(cx - 0.5f), abs(cy - 0.5f))

        val cfg = PreprocessConfig(SIZE, floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 1f, 1f))
        val img = Preprocess.cropAndResize(pixels, width, height, cfg)
        val r = img.data[0]; val g = img.data[1]; val b = img.data[2]
        val n = SIZE * SIZE

        // Disc mask on the resized square: pixels brighter than the background threshold.
        var inDisc = 0; var sumMax = 0f; var bright = 0
        for (i in 0 until n) {
            val m = max(r[i], max(g[i], b[i]))
            if (m > 10f) {
                inDisc++; sumMax += m
                if (r[i] > 240f && g[i] > 240f && b[i] > 240f) bright++
            }
        }
        val meanMax = if (inDisc > 0) sumMax / inDisc else 0f
        val reflectionFrac = if (inDisc > 0) bright.toFloat() / inDisc else 0f

        // Laplacian variance of the green channel, computed only inside the disc interior.
        var lapSum = 0.0; var lapSq = 0.0; var lapN = 0
        for (y in 1 until SIZE - 1) for (x in 1 until SIZE - 1) {
            val i = y * SIZE + x
            if (max(r[i], max(g[i], b[i])) <= 10f) continue
            val lap = 4f * g[i] - g[i - 1] - g[i + 1] - g[i - SIZE] - g[i + SIZE]
            lapSum += lap; lapSq += lap.toDouble() * lap; lapN++
        }
        val lapVar = if (lapN > 0) (lapSq / lapN - (lapSum / lapN) * (lapSum / lapN)).toFloat() else 0f

        val details = mapOf("mean_brightness" to meanMax, "reflection_frac" to reflectionFrac, "laplacian_var" to lapVar,
                            "disc_frac" to discFrac, "center_offset" to centerOffset)
        val problem = when {
            meanMax < DARK_MEAN_MAX -> Problem.Dark
            reflectionFrac > REFLECTION_FRAC_MAX -> Problem.Reflection
            centerOffset > MAX_CENTER_OFFSET || discFrac < MIN_DISC_FRAC -> Problem.OffCenter
            lapVar < BLUR_VAR_MIN -> Problem.Blurry
            else -> null
        }
        // A rough 0..1 score for the readout: sharpness and brightness, each clamped.
        val score = (minOf(1f, lapVar / (4f * BLUR_VAR_MIN)) * 0.6f + minOf(1f, meanMax / 120f) * 0.4f).let { if (problem == null) it else it * 0.5f }
        return Result(problem == null, score, problem, details)
    }

    @Suppress("unused")
    private fun stddev(values: FloatArray): Float {
        val mean = values.average().toFloat()
        var acc = 0f
        for (v in values) acc += (v - mean) * (v - mean)
        return sqrt(acc / values.size)
    }

    @Suppress("unused")
    private fun absf(v: Float) = abs(v)
}
