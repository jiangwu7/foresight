package org.jameslick.foresight.ml

/**
 * "Is this frame a gradable retina photo?" — the one question the capture screen asks of every preview
 * frame and of the final still (spec §3.2, §4.3 M1). Phase 1/2 answer it with [QualityHeuristic]; the M1
 * model implements the same interface so the screen does not change when it arrives.
 */
fun interface QualityAssessor {
    fun assess(pixels: IntArray, width: Int, height: Int): QualityHeuristic.Result
}

/** The hand-written Phase 1 rules behind the interface. */
object HeuristicAssessor : QualityAssessor {
    override fun assess(pixels: IntArray, width: Int, height: Int): QualityHeuristic.Result = QualityHeuristic.assess(pixels, width, height)
}
