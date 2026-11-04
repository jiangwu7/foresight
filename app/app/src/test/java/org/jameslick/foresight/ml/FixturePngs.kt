package org.jameslick.foresight.ml

import ar.com.hjg.pngj.ImageLineInt
import ar.com.hjg.pngj.PngReader
import java.io.File

/** Test helper: locate the repo's fixtures and decode PNGs into ARGB ints (what Bitmap.getPixels returns). */
object FixturePngs {
    val repoRoot: File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "FORESIGHT_SPEC.md").exists() }

    val fixturesDir = File(repoRoot, "fixtures")

    fun built(): Boolean = File(fixturesDir, "expected/index.json").exists() && File(fixturesDir, "gradable").listFiles()?.isNotEmpty() == true

    fun list(sub: String, prefix: String = ""): List<File> =
        File(fixturesDir, sub).listFiles { f -> f.name.endsWith(".png") && f.name.startsWith(prefix) }?.sorted() ?: emptyList()

    fun argb(file: File): Triple<IntArray, Int, Int> {
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
}
