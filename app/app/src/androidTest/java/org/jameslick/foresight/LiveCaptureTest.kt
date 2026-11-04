package org.jameslick.foresight

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.jameslick.foresight.FlowDriver.nodesWithText
import org.jameslick.foresight.FlowDriver.reachCapture
import org.jameslick.foresight.ui.TestHooks
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spec §8: "auto-capture triggers on a looped good-quality video and never triggers on an ungradable one".
 * The live capture screen reads its preview frames from [TestHooks.frameProvider] here, so this runs on
 * any phone without the adapter. Push the fixtures first:
 *
 *   adb push fixtures/gradable/grade2_000c1434d8d7.png /sdcard/Download/foresight_fixture.png
 *   adb push fixtures/ungradable/dark_<id>.png          /sdcard/Download/foresight_fixture_dark.png
 */
@RunWith(AndroidJUnit4::class)
class LiveCaptureTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @After fun unhook() { TestHooks.frameProvider = null }

    @Test
    fun auto_capture_fires_on_a_looped_good_frame() {
        val good = FlowDriver.bitmap(FlowDriver.FIXTURE_GOOD)
        assumeTrue("good fixture not pushed", good != null)
        TestHooks.frameProvider = { good }
        rule.reachCapture()
        // Three good frames at 5 fps → capture → analysis → "Got it" (spec §3.2 cue) on the review block.
        rule.waitUntil(timeoutMillis = 30_000) { rule.nodesWithText("Got it").isNotEmpty() }
        rule.onNodeWithText("Got it", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun auto_capture_never_fires_on_a_looped_dark_frame() {
        val dark = FlowDriver.bitmap(FlowDriver.FIXTURE_DARK)
        assumeTrue("dark fixture not pushed", dark != null)
        TestHooks.frameProvider = { dark }
        rule.reachCapture()
        // 6 s = 30 frames; a trigger would have replaced the shutter button with the review block.
        rule.mainClock.autoAdvance = true
        Thread.sleep(6_000)
        rule.waitForIdle()
        assertTrue("capture fired on an ungradable frame", rule.nodesWithText("Got it").isEmpty())
        rule.onNodeWithText("Take photo").assertIsDisplayed()
    }
}
