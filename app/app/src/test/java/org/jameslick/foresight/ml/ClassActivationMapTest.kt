package org.jameslick.foresight.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassActivationMapTest {

    @Test
    fun `heat map equals the weighted channel sum, relu'd and scaled to max 1`() {
        // 2x2 grid, 3 channels, NHWC
        val features = floatArrayOf(
            1f, 0f, 0f,   // (0,0)
            0f, 1f, 0f,   // (0,1)
            0f, 0f, 1f,   // (1,0)
            -1f, -1f, -1f // (1,1)
        )
        val weights = listOf(
            listOf(0f, 0f, 0f),
            listOf(0f, 0f, 0f),
            listOf(2f, 0f, 0f),   // class 2 fires on channel 0
            listOf(0f, 1f, 0f),   // class 3 fires on channel 1
            listOf(0f, 0f, 4f),   // class 4 fires on channel 2
        )
        val heat = ClassActivationMap.compute(features, 2, 2, 3, weights, intArrayOf(2, 3, 4))
        // raw: (0,0)=2, (0,1)=1, (1,0)=4, (1,1)=-7 -> relu 0 ; scaled by 4
        assertEquals(0.5f, heat[0], 1e-6f)
        assertEquals(0.25f, heat[1], 1e-6f)
        assertEquals(1f, heat[2], 1e-6f)
        assertEquals(0f, heat[3], 1e-6f)
    }

    @Test
    fun `class probabilities weight the classes`() {
        val features = floatArrayOf(1f, 1f)          // 1x1 grid, 2 channels
        val weights = listOf(listOf(0f, 0f), listOf(0f, 0f), listOf(1f, 0f), listOf(0f, 1f), listOf(0f, 0f))
        val probs = floatArrayOf(0f, 0f, 0.9f, 0.1f, 0f)
        val heat = ClassActivationMap.referable(features, 1, 1, 2, weights, probs)
        assertEquals(1f, heat[0], 1e-6f)              // single cell is always the max after scaling
    }

    @Test
    fun `all-negative evidence gives an all-zero map, not NaN`() {
        val features = floatArrayOf(1f, 1f, 1f, 1f)
        val weights = listOf(listOf(-1f), listOf(-1f), listOf(-1f), listOf(-1f), listOf(-1f))
        val heat = ClassActivationMap.compute(features, 2, 2, 1, weights, intArrayOf(2, 3, 4))
        assertTrue(heat.all { it == 0f })
    }

    @Test
    fun `channel mismatch is rejected`() {
        val features = FloatArray(4)
        try {
            ClassActivationMap.compute(features, 2, 2, 1, listOf(listOf(1f, 2f)), intArrayOf(0))
            assertTrue("expected an exception", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("channels"))
        }
    }
}
