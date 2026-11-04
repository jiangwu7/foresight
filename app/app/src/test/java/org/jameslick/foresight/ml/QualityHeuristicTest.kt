package org.jameslick.foresight.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The provisional gradability check must pass every real gradable fixture and name the problem on every
 * degraded one (spec §8: "auto-capture ... never triggers on an ungradable one" — this is the still-image half).
 * Skips when the fixtures are not built on this machine.
 */
class QualityHeuristicTest {

    @Test
    fun `all 25 gradable fixtures are gradable`() {
        assumeTrue(FixturePngs.built())
        val files = FixturePngs.list("gradable")
        assertTrue(files.size >= 25)
        val failures = files.mapNotNull { f ->
            val (px, w, h) = FixturePngs.argb(f)
            val r = QualityHeuristic.assess(px, w, h)
            if (r.gradable) null else "${f.name}: ${r.problem} ${r.details}"
        }
        assertTrue("gradable fixtures rejected:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `each degradation is detected with the right label`() {
        assumeTrue(FixturePngs.built())
        val expect = mapOf("dark_" to QualityHeuristic.Problem.Dark, "blur_" to QualityHeuristic.Problem.Blurry,
                           "offcenter_" to QualityHeuristic.Problem.OffCenter, "reflection_" to QualityHeuristic.Problem.Reflection)
        val failures = mutableListOf<String>()
        for ((prefix, want) in expect) {
            val files = FixturePngs.list("ungradable", prefix)
            assertTrue("no $prefix fixtures", files.isNotEmpty())
            for (f in files) {
                val (px, w, h) = FixturePngs.argb(f)
                val r = QualityHeuristic.assess(px, w, h)
                if (r.gradable || r.problem != want) failures += "${f.name}: got ${r.problem} gradable=${r.gradable} ${r.details}"
            }
        }
        assertTrue("ungradable fixtures misjudged:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `score is in 0 to 1`() {
        assumeTrue(FixturePngs.built())
        val f = FixturePngs.list("gradable").first()
        val (px, w, h) = FixturePngs.argb(f)
        val r = QualityHeuristic.assess(px, w, h)
        assertTrue(r.score in 0f..1f)
        assertEquals(null, r.problem)
    }
}
