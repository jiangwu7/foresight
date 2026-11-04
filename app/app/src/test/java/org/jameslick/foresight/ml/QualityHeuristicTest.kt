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

    /** Synthetic non-retina pictures (never retina images, spec §13.1): the check must refuse to grade them. */
    @Test
    fun `ordinary photos and screenshots are not fundus`() {
        val w = 512; val h = 512; val rnd = java.util.Random(1)
        fun ch(v: Int, noise: Int) = (v + (rnd.nextGaussian() * noise).toInt()).coerceIn(0, 255)
        fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        fun oval(i: Int): Boolean { val x = i % w - 256.0; val y = i / w - 256.0; return (y / 200) * (y / 200) + (x / 140) * (x / 140) < 1.0 }
        val face = IntArray(w * h) { i -> if (oval(i)) argb(ch(224, 12), ch(172, 12), ch(140, 12)) else argb(ch(200, 12), ch(200, 12), ch(200, 12)) }
        val screenshot = IntArray(w * h) { if (rnd.nextDouble() < 0.08) argb(20, 20, 20) else argb(245, 245, 245) }
        val outdoor = IntArray(w * h) { i -> if (i / w < 256) argb(120, 170, 230) else argb(60, 140, 50) }
        val cases = mapOf("face on light background" to face, "screenshot" to screenshot, "outdoor sky and grass" to outdoor)
        val failures = cases.mapNotNull { (name, px) ->
            val r = QualityHeuristic.assess(px, w, h)
            if (!r.gradable && r.problem == QualityHeuristic.Problem.NotFundus) null else "$name: gradable=${r.gradable} ${r.problem} ${r.details}"
        }
        assertTrue("non-retina images not refused:\n" + failures.joinToString("\n"), failures.isEmpty())
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
