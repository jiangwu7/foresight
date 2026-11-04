package org.jameslick.foresight

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry

/** Shared steps for the instrumented tests: reading fixtures through the shell and driving the flow to the capture screen. */
object FlowDriver {
    const val FIXTURE_GOOD = "/sdcard/Download/foresight_fixture.png"
    const val FIXTURE_GRADE0 = "/sdcard/Download/foresight_fixture_grade0.png"
    const val FIXTURE_DARK = "/sdcard/Download/foresight_fixture_dark.png"

    /** The app has no storage permission (and must not); the instrumentation's shell can read Download. */
    fun readViaShell(path: String): ByteArray {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cat $path")
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    fun bitmap(path: String): Bitmap? = readViaShell(path).takeIf { it.isNotEmpty() }?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

    /** Case-insensitive: pills and micro-labels render their text in small caps. */
    fun ComposeTestRule.nodesWithText(text: String) = try { onAllNodes(hasText(text, ignoreCase = true)).fetchSemanticsNodes() } catch (e: Throwable) { emptyList() }

    /** Home → (create event) → language (English) → consent → intake (skipped) → right-eye capture screen. */
    fun ComposeTestRule.reachCapture() {
        val hasEvent = try { waitUntil(10_000) { nodesWithText("Continue").isNotEmpty() }; true } catch (e: Throwable) { false }
        if (!hasEvent) {
            onNodeWithText("New event").performClick()
            onNodeWithText("Event name").performTextInput("Flow test")
            onNodeWithText("Create event").performClick()
            waitUntil(10_000) { nodesWithText("Continue").isNotEmpty() || nodesWithText("English").isNotEmpty() }
        }
        if (nodesWithText("Continue").isNotEmpty()) onNodeWithText("Continue").performClick()
        onNodeWithText("English").performClick()
        onNodeWithText("Continue").performClick()
        onNodeWithText("I understand this is a screening, not a diagnosis").performClick()
        onNodeWithText("I agree to have my eyes photographed").performClick()
        onNodeWithText("I am 18 or older").performClick()
        onNodeWithText("I agree").performClick()
        // Intake questions are off by default (Settings switch); if a device has them on, skip through.
        if (nodesWithText("Skip this question").isNotEmpty()) {
            repeat(5) { onNodeWithText("Skip this question").performClick() }
            onNodeWithText("Next").performClick()
        }
    }
}
