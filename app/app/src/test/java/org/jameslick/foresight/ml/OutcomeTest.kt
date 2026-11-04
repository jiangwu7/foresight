package org.jameslick.foresight.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exhaustive tests for the outcome mapping (spec §8: "a pure function with exhaustive tests"). */
class OutcomeTest {

    private fun probsFor(grade: Int, confidence: Float = 0.9f): FloatArray {
        val rest = (1f - confidence) / 4f
        return FloatArray(5) { if (it == grade) confidence else rest }
    }

    private fun eye(grade: Int, referable: Float? = null, gradable: Boolean = true) =
        EyeResult(gradable = gradable, probs = probsFor(grade), referableProb = referable)

    @Test
    fun `every grade maps to the documented eye outcome`() {
        val expected = mapOf(0 to EyeOutcome.Green, 1 to EyeOutcome.Yellow, 2 to EyeOutcome.Yellow,
                             3 to EyeOutcome.Red, 4 to EyeOutcome.Red)
        for ((g, want) in expected) assertEquals("grade $g", want, OutcomeMapper.eyeOutcome(eye(g)))
    }

    @Test
    fun `referable probability alone can raise grade 0 to yellow but never to red`() {
        assertEquals(EyeOutcome.Green, OutcomeMapper.eyeOutcome(eye(0, referable = 0.49f)))
        assertEquals(EyeOutcome.Yellow, OutcomeMapper.eyeOutcome(eye(0, referable = 0.5f)))
        assertEquals(EyeOutcome.Yellow, OutcomeMapper.eyeOutcome(eye(0, referable = 0.99f)))
        assertNotEquals(EyeOutcome.Red, OutcomeMapper.eyeOutcome(eye(0, referable = 1f)))
    }

    @Test
    fun `custom threshold is honored`() {
        assertEquals(EyeOutcome.Green, OutcomeMapper.eyeOutcome(eye(0, referable = 0.6f), threshold = 0.7f))
        assertEquals(EyeOutcome.Yellow, OutcomeMapper.eyeOutcome(eye(0, referable = 0.7f), threshold = 0.7f))
    }

    @Test
    fun `referable probability derived from probs sums moderate and worse`() {
        val p = floatArrayOf(0.5f, 0.1f, 0.2f, 0.1f, 0.1f)
        assertEquals(0.4f, OutcomeMapper.referableProb(p), 1e-6f)
    }

    @Test
    fun `ungradable is never green and missing probs count as ungradable`() {
        for (g in 0..4) assertEquals(EyeOutcome.Ungradable, OutcomeMapper.eyeOutcome(eye(g, gradable = false)))
        assertEquals(EyeOutcome.Ungradable, OutcomeMapper.eyeOutcome(EyeResult(gradable = true, probs = null)))
    }

    @Test
    fun `overall is the more serious eye, exhaustively`() {
        val rank = mapOf(EyeOutcome.Green to 0, EyeOutcome.Ungradable to 1, EyeOutcome.Yellow to 2, EyeOutcome.Red to 3)
        val toOverall = mapOf(EyeOutcome.Green to OverallOutcome.Green, EyeOutcome.Ungradable to OverallOutcome.Ungradable,
                              EyeOutcome.Yellow to OverallOutcome.Yellow, EyeOutcome.Red to OverallOutcome.Red)
        for (r in EyeOutcome.entries) for (l in EyeOutcome.entries) {
            val worse = if (rank.getValue(r) >= rank.getValue(l)) r else l
            assertEquals("$r + $l", toOverall.getValue(worse), OutcomeMapper.overall(r, l))
            assertEquals("symmetry $r + $l", OutcomeMapper.overall(r, l), OutcomeMapper.overall(l, r))
        }
    }

    @Test
    fun `one ungradable eye beside a green eye is not a negative screen`() {
        assertEquals(OverallOutcome.Ungradable, OutcomeMapper.overall(eye(0), eye(0, gradable = false)))
        assertEquals(OverallOutcome.Yellow, OutcomeMapper.overall(eye(1), eye(0, gradable = false)))
        assertEquals(OverallOutcome.Red, OutcomeMapper.overall(eye(4), eye(0, gradable = false)))
    }

    @Test
    fun `outcome never gets less serious as the grade rises`() {
        val order = listOf(EyeOutcome.Green, EyeOutcome.Yellow, EyeOutcome.Red)
        var last = -1
        for (g in 0..4) {
            val idx = order.indexOf(OutcomeMapper.eyeOutcome(eye(g)))
            assertTrue("grade $g went backwards", idx >= last)
            last = idx
        }
    }

    @Test
    fun `argmax picks the first maximum`() {
        assertEquals(0, EyeResult.argmax(floatArrayOf(0.4f, 0.4f, 0.2f, 0f, 0f)))
        assertEquals(4, EyeResult.argmax(floatArrayOf(0f, 0f, 0f, 0f, 1f)))
        assertEquals(DrGrade.Moderate, eye(2).grade)
    }
}
