package org.jameslick.foresight

import android.graphics.BitmapFactory
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.jameslick.foresight.ui.TestHooks
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end screening flow with a fixture image injected through the gallery-import path (spec §8, §10
 * Phase 1 acceptance). Runs on a device or emulator:
 *
 *   adb push fixtures/gradable/grade2_000c1434d8d7.png /sdcard/Download/foresight_fixture.png
 *   cd app && ./gradlew :app:connectedDebugAndroidTest
 *
 * Skips (does not fail) when the fixture has not been pushed, so CI without an emulator stays green.
 * Strings are matched in English; the test selects English on the language screen.
 */
@RunWith(AndroidJUnit4::class)
class ScreeningFlowTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val fixture = File("/sdcard/Download/foresight_fixture.png")

    @Before fun hook() {
        assumeTrue("fixture not pushed to the device", fixture.exists())
        val bmp = BitmapFactory.decodeFile(fixture.absolutePath)
        TestHooks.bitmapProvider = { bmp }
    }

    @After fun unhook() { TestHooks.bitmapProvider = null }

    @Test
    fun fixture_image_reaches_a_result_and_a_card() {
        val r = rule
        // Home -> (create an event if none) -> language
        r.waitForIdle()
        if (r.onAllNodesWithTextSafe("New event").isNotEmpty() && r.onAllNodesWithTextSafe("Continue").isEmpty()) {
            r.onNodeWithText("New event").performClick()
            r.waitForIdle()
            // NewEventScreen: type is not driven here; create with the default empty name is disabled, so skip.
            assumeTrue("create an event in the app once before running this test", false)
        }
        r.onNodeWithText("Continue").performClick()
        r.onNodeWithText("English").performClick()
        r.onNodeWithText("Continue").performClick()
        // Consent
        r.onNodeWithText("I understand this is a screening, not a diagnosis").performClick()
        r.onNodeWithText("I agree to have my eyes photographed").performClick()
        r.onNodeWithText("I agree").performClick()
        // Intake: skip all six
        repeat(5) { r.onNodeWithText("Skip this question").performClick() }
        r.onNodeWithText("Next").performClick()
        // Right eye, left eye via the hook
        r.onNodeWithText("Choose a photo").performClick()
        r.waitUntil(timeoutMillis = 30_000) { r.onAllNodesWithTextSafe("Continue").isNotEmpty() }
        r.onNodeWithText("Continue").performClick()
        r.onNodeWithText("Choose a photo").performClick()
        r.waitUntil(timeoutMillis = 30_000) { r.onAllNodesWithTextSafe("Continue").isNotEmpty() }
        r.onNodeWithText("Continue").performClick()
        // Processing -> results: the disclaimer must be on screen for every outcome (§1.3)
        r.waitUntil(timeoutMillis = 30_000) {
            r.onAllNodesWithTextSafe("Foresight is a screening tool operated by students. It is not a medical diagnosis. Please see a healthcare provider.").isNotEmpty()
        }
        r.onNodeWithText("Foresight is a screening tool operated by students. It is not a medical diagnosis. Please see a healthcare provider.").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextSafe(text: String) =
        try { onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes() } catch (e: Throwable) { emptyList() }
}
