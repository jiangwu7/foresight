package org.jameslick.foresight

import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.jameslick.foresight.ml.DrModelRunner
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shipped model, run on the phone through the real runner, must react to its input and get two clear
 * fixtures right (spec §8, §10 Phase 1). Added after the first device run returned the same output for
 * every image (DECISIONS.md 2026-11-04). Push the two fixtures first:
 *
 *   adb push fixtures/gradable/grade2_000c1434d8d7.png /sdcard/Download/foresight_fixture.png
 *   adb push fixtures/gradable/grade0_002c21358ce6.png /sdcard/Download/foresight_fixture_grade0.png
 *
 * Skips when they are missing. Raw outputs are logged under the tag ForesightParity for comparison with
 * ml/verify_tflite.py on the desktop.
 */
@RunWith(AndroidJUnit4::class)
class ModelParityTest {
    private val grade2 = "/sdcard/Download/foresight_fixture.png"
    private val grade0 = "/sdcard/Download/foresight_fixture_grade0.png"

    private fun readViaShell(path: String): ByteArray {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cat $path")
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private fun run(runner: DrModelRunner, path: String): DrModelRunner.Output {
        val bytes = readViaShell(path)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val w = bmp.width; val h = bmp.height
        val px = IntArray(w * h).also { bmp.getPixels(it, 0, w, 0, 0, w, h) }
        val out = runner.run(px, w, h)
        Log.i("ForesightParity", "file=${path.substringAfterLast('/')} size=${w}x$h grade=${out.grade} " +
            "probs=${out.probs.joinToString(",") { "%.4f".format(it) }} referable=${"%.4f".format(out.referableProb)} " +
            "reg=${"%.3f".format(out.reg)} runtimeMs=${out.runtimeMs}")
        return out
    }

    @Test fun shipped_model_reacts_to_input_and_grades_two_clear_fixtures() {
        assumeTrue("fixtures not pushed", readViaShell(grade2).isNotEmpty() && readViaShell(grade0).isNotEmpty())
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        DrModelRunner.load(ctx).use { runner ->
            val a = run(runner, grade0)
            val b = run(runner, grade2)
            assertNotEquals("model output does not depend on the input image", a.probs.toList(), b.probs.toList())
            assertTrue("grade-0 fixture should not be referable (got ${a.referableProb})", a.referableProb < 0.5f)
            assertTrue("grade-2 fixture should be referable (got ${b.referableProb})", b.referableProb >= 0.5f)
            assertTrue("inference budget 1.5 s (got ${b.runtimeMs} ms)", b.runtimeMs <= 1500)
        }
    }
}
