package org.jameslick.foresight

import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.jameslick.foresight.ui.TestHooks
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end screening flow with a fixture image injected through the gallery-import path (spec §8, §10
 * Phase 1 acceptance). Runs on a device or emulator:
 *
 *   adb push fixtures/gradable/grade2_000c1434d8d7.png /sdcard/Download/foresight_fixture.png
 *   cd app && ./gradlew :app:connectedDebugAndroidTest
 *
 * Skips (does not fail) when the fixture has not been pushed, so CI without an emulator stays green.
 * Strings are matched in English; the test selects English on the language screen. If the app has no
 * event yet the test creates one ("Flow test"). Note: by default Gradle UNINSTALLS the app after a
 * connected test run, wiping its database; gradle.properties sets leaveApksInstalledAfterRun to keep it.
 */
@RunWith(AndroidJUnit4::class)
class ScreeningFlowTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val fixturePath = "/sdcard/Download/foresight_fixture.png"

    /**
     * Reads a file through the instrumentation's shell access. The app itself has no storage permission
     * (and must not: it never reads the gallery directly), so on Android 11+ a plain File read of
     * Download is denied with EACCES even though exists() says true. Empty bytes = file not pushed.
     */
    private fun readViaShell(path: String): ByteArray {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cat $path")
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    @Before fun hook() {
        val bytes = readViaShell(fixturePath)
        assumeTrue("fixture not pushed to the device ($fixturePath)", bytes.isNotEmpty())
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assumeTrue("fixture is not a decodable image", bmp != null)
        TestHooks.bitmapProvider = { bmp }
    }

    @After fun unhook() { TestHooks.bitmapProvider = null }

    @Test
    fun fixture_image_reaches_a_result_and_a_card() {
        val r = rule
        // Home -> (create an event if none) -> language
        // The event list loads from the encrypted database asynchronously; give it a moment before deciding
        // whether an event exists ("Continue" only appears once one is loaded). Without one, create it.
        val hasEvent = try {
            r.waitUntil(timeoutMillis = 10_000) { r.onAllNodesWithTextSafe("Continue").isNotEmpty() }; true
        } catch (e: Throwable) { false }
        if (!hasEvent) {
            r.onNodeWithText("New event").performClick()
            r.onNodeWithText("Event name").performTextInput("Flow test")
            r.onNodeWithText("Create event").performClick()
            r.waitUntil(timeoutMillis = 10_000) {
                r.onAllNodesWithTextSafe("Continue").isNotEmpty() || r.onAllNodesWithTextSafe("English").isNotEmpty()
            }
        }
        if (r.onAllNodesWithTextSafe("Continue").isNotEmpty()) r.onNodeWithText("Continue").performClick()
        r.onNodeWithText("English").performClick()
        r.onNodeWithText("Continue").performClick()
        // Consent
        r.onNodeWithText("I understand this is a screening, not a diagnosis").performClick()
        r.onNodeWithText("I agree to have my eyes photographed").performClick()
        r.onNodeWithText("I am 18 or older").performClick()
        r.onNodeWithText("I agree").performClick()
        // Intake questions are off by default since the 2026-11-04 user tests; skip through if a device has them on.
        if (r.onAllNodesWithTextSafe("Skip this question").isNotEmpty()) {
            repeat(5) { r.onNodeWithText("Skip this question").performClick() }
            r.onNodeWithText("Next").performClick()
        }
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
