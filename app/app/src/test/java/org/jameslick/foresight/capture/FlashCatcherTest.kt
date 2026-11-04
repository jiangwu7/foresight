package org.jameslick.foresight.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mode A flash catching: keep the brightest frame of the flash; ignore noise and slow drift; time out cleanly. */
class FlashCatcherTest {

    private fun run(lumas: List<Float>): FlashCatcher = FlashCatcher().also { fc -> lumas.forEach { fc.feed(it) } }

    @Test
    fun `keeps the brightest frame of a two-frame flash`() {
        //            baseline x3     wait   flash  flash  back
        val fc = run(listOf(20f, 21f, 19f, 20f, 140f, 180f, 22f))
        assertEquals(FlashCatcher.State.Done, fc.state)
        assertEquals(5, fc.keeper)
    }

    @Test
    fun `noise on a dark scene never counts as a flash`() {
        val fc = run(listOf(4f, 5f, 4f, 6f, 9f, 11f, 5f, 4f))      // ratio exceeded (11 > 1.8 × 5) but jump < 12
        assertNull(fc.keeper)
        assertTrue(fc.state == FlashCatcher.State.Waiting)
    }

    @Test
    fun `slow brightening tracks the baseline instead of triggering`() {
        val fc = run((0 until 40).map { 20f + it * 0.5f })        // 20 → 40 over 40 frames
        assertNull(fc.keeper)
    }

    @Test
    fun `a long flash is cut after the max frames so the window closes`() {
        val fc = run(listOf(20f, 20f, 20f, 200f, 200f, 200f, 200f, 200f, 200f))
        assertEquals(FlashCatcher.State.Done, fc.state)
        assertEquals(3, fc.keeper)
    }

    @Test
    fun `luma of a flat frame is its gray level`() {
        val px = IntArray(64 * 64) { (0xFF shl 24) or (100 shl 16) or (100 shl 8) or 100 }
        assertEquals(100f, FlashCatcher.luma(px, 64, 64), 1f)
    }
}
