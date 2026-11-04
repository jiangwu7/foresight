package org.jameslick.foresight

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.jameslick.foresight.FlowDriver.reachCapture
import org.jameslick.foresight.capture.CameraController
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Diagnostic for the real camera path (spec §3.2 manual controls): grants nothing itself, so run
 *   adb shell pm grant org.jameslick.foresight android.permission.CAMERA
 * first. Reaches the live capture screen with the camera bound, logs the phone's capabilities (tag
 * ForesightCamera), waits a few seconds so the preview and the quality loop run, then takes a screenshot
 * through the shell to /sdcard/Download/foresight_live.png for the design review against mockup 503.
 * Skips when the camera permission has not been granted.
 */
@RunWith(AndroidJUnit4::class)
class LiveCameraDiagnosticTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun live_screen_binds_the_camera_and_survives_ten_seconds() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val granted = ctx.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        assumeTrue("camera permission not granted (adb shell pm grant ... CAMERA)", granted)
        val caps = CameraController.probe(ctx)
        Log.i("ForesightCamera", "probe: $caps")
        rule.reachCapture()
        rule.onNodeWithText("Take photo", ignoreCase = true).assertIsDisplayed()
        Thread.sleep(6_000)
        rule.waitForIdle()
        FlowDriver.readViaShell("/dev/null")   // no-op keeps the helper warm
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("screencap -p /sdcard/Download/foresight_live.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        Thread.sleep(4_000)
        rule.waitForIdle()
        rule.onNodeWithText("Take photo", ignoreCase = true).assertIsDisplayed()   // still alive, no crash
        Log.i("ForesightCamera", "live screen survived 10 s with the camera bound")
    }
}
