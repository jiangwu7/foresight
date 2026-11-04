package org.jameslick.foresight.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import ar.com.hjg.pngj.ImageLineInt
import ar.com.hjg.pngj.PngReader
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Python <-> Kotlin preprocessing equivalence (spec §4.2): for each fixture image, the Kotlin pipeline
 * must match the .bin tensor Python wrote to fixtures/expected within 1e-3.
 *
 * Runs on the JVM with javax.imageio (no Android). Skips when the fixtures have not been built on this
 * machine (see fixtures/README.md); CI without APTOS therefore passes but does not exercise this test.
 */
class PreprocessEquivalenceTest {

    private val repoRoot: File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "FORESIGHT_SPEC.md").exists() }

    /** Decodes an 8-bit RGB/RGBA PNG into 0xAARRGGBB ints, the same layout android.graphics.Bitmap.getPixels gives. */
    private fun argbPixels(file: File): Triple<IntArray, Int, Int> {
        val reader = PngReader(file)
        val info = reader.imgInfo
        require(info.bitDepth == 8 && info.channels >= 3) { "fixture must be 8-bit RGB(A): ${file.name}" }
        val w = info.cols; val h = info.rows; val ch = info.channels
        val px = IntArray(w * h)
        for (y in 0 until h) {
            val line = (reader.readRow() as ImageLineInt).scanline
            for (x in 0 until w) {
                val o = x * ch
                px[y * w + x] = (0xFF shl 24) or (line[o] shl 16) or (line[o + 1] shl 8) or line[o + 2]
            }
        }
        reader.end()
        return Triple(px, w, h)
    }

    private fun readFloats(file: File): FloatArray {
        val bytes = file.readBytes()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 4) { buf.getFloat(it * 4) }
    }

    @Test
    fun `kotlin preprocessing matches python on fixture images within 1e-3`() {
        val index = File(repoRoot, "fixtures/expected/index.json")
        assumeTrue("fixtures not built on this machine", index.exists())
        val json = index.readText()
        // Tiny hand parser: avoids a JSON dependency in the test. index.json is written by make_fixtures.py.
        val size = Regex(""""size":\s*(\d+)""").find(json)!!.groupValues[1].toInt()
        val mean = Regex(""""mean":\s*\[([^\]]+)\]""").find(json)!!.groupValues[1].split(",").map { it.trim().toFloat() }.toFloatArray()
        val std = Regex(""""std":\s*\[([^\]]+)\]""").find(json)!!.groupValues[1].split(",").map { it.trim().toFloat() }.toFloatArray()
        val items = Regex(""""image":\s*"([^"]+)",\s*"tensor":\s*"([^"]+)"""").findAll(json).toList()
        assumeTrue("no fixture items", items.isNotEmpty())
        val cfg = PreprocessConfig(size = size, mean = mean, std = std)

        var checked = 0
        var worst = 0f
        for (m in items) {
            val image = File(repoRoot, m.groupValues[1]); val tensor = File(repoRoot, m.groupValues[2])
            assumeTrue("fixture files missing", image.exists() && tensor.exists())
            val (px, w, h) = argbPixels(image)
            val got = Preprocess.runChw(px, w, h, cfg)
            val want = readFloats(tensor)
            assertEquals("tensor length for ${image.name}", want.size, got.size)
            var maxDiff = 0f
            for (i in got.indices) maxDiff = maxOf(maxDiff, abs(got[i] - want[i]))
            assertTrue("${image.name}: max abs diff $maxDiff exceeds 1e-3", maxDiff < 1e-3f)
            worst = maxOf(worst, maxDiff); checked++
        }
        println("PreprocessEquivalenceTest: $checked images, worst max-abs-diff $worst")
        assertTrue(checked >= 20)
    }

    @Test
    fun `bbox finds an offset disc and ignores a dark frame`() {
        val w = 40; val h = 30
        val px = IntArray(w * h)
        for (y in 5..24) for (x in 10..29) px[y * w + x] = 0xFF8040A0.toInt()
        assertTrue(Preprocess.findFundusBbox(px, w, h, 10).contentEquals(intArrayOf(10, 5, 30, 25)))
        assertTrue(Preprocess.findFundusBbox(IntArray(w * h), w, h, 10).contentEquals(intArrayOf(0, 0, w, h)))
    }

    @Test
    fun `constant image stays constant through resize and normalizes by the formula`() {
        val w = 37; val h = 37
        val px = IntArray(w * h) { 0xFF4D4D4D.toInt() }   // 77,77,77
        val cfg = PreprocessConfig(16, floatArrayOf(0.5f, 0.5f, 0.5f), floatArrayOf(0.25f, 0.25f, 0.25f))
        val out = Preprocess.runChw(px, w, h, cfg)
        val expected = (77f / 255f - 0.5f) / 0.25f
        out.forEach { assertEquals(expected, it, 1e-5f) }
    }
}
