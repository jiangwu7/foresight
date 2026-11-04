package org.jameslick.foresight.capture

import org.jameslick.foresight.capture.AutoCapture.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §3.2: trigger after 3 consecutive good frames; never on ungradable ones; no strobing. */
class AutoCaptureTest {

    private fun run(ac: AutoCapture, verdicts: List<Boolean>, startMs: Long = 0, stepMs: Long = AutoCapture.FRAME_INTERVAL_MS): List<Event> =
        verdicts.mapIndexed { i, g -> ac.feed(g, startMs + i * stepMs) }

    @Test
    fun `three consecutive good frames trigger once`() {
        val ev = run(AutoCapture(), listOf(true, true, true))
        assertEquals(listOf(Event.None, Event.Settling, Event.Trigger), ev)
    }

    @Test
    fun `a bad frame resets the count`() {
        val ev = run(AutoCapture(), listOf(true, true, false, true, true, true))
        assertEquals(listOf(Event.None, Event.Settling, Event.None, Event.None, Event.Settling, Event.Trigger), ev)
    }

    @Test
    fun `never triggers on an ungradable sequence`() {
        val ev = run(AutoCapture(), List(50) { it % 3 != 2 })   // never three good in a row
        assertTrue(ev.none { it == Event.Trigger })
        val ev2 = run(AutoCapture(), List(50) { false })
        assertTrue(ev2.none { it != Event.None })
    }

    @Test
    fun `cooldown prevents a second trigger while the still is reviewed`() {
        val ac = AutoCapture(cooldownMs = 4000)
        val ev = run(ac, List(30) { true })                     // 30 good frames over 6 s
        val triggers = ev.withIndex().filter { it.value == Event.Trigger }.map { it.index }
        assertEquals(listOf(2, 24), triggers)                    // cooldown ends at frame 22, then three more good frames
    }
}
