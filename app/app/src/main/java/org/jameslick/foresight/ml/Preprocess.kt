package org.jameslick.foresight.ml

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Kotlin port of `ml/preprocess.py` (spec §4.2). The two implementations must agree within 1e-3 on the
 * fixture images; `PreprocessEquivalenceTest` checks exactly that against the .bin tensors in fixtures/expected.
 *
 * Pure Kotlin on purpose: it takes ARGB pixels (what `Bitmap.getPixels` returns) and never touches
 * Android classes, so it runs in plain JVM unit tests. Steps, same order and arithmetic as Python:
 *   1. findFundusBbox      bright-pixel bounding box (max of R,G,B > threshold)
 *   2. crop + pad to square, then
 *   3. block-mean downsample by the largest integer factor that keeps the image >= size,
 *      then bilinear (half-pixel centers) to size x size  (steps 2-3 fused into one pass to save memory)
 *   4. optional Ben Graham local contrast (4*img - 4*blur + 128)
 *   5. normalize (v/255 - mean) / std
 *
 * Float32 everywhere, in the same operation order as NumPy, so results match to float precision.
 */
class PreprocessConfig(
    val size: Int,
    val mean: FloatArray,
    val std: FloatArray,
    val bboxThreshold: Int = 10,
    val benGrahamSigma: Float? = null,
) {
    init {
        require(mean.size == 3 && std.size == 3) { "mean/std must have 3 channels" }
        require(size > 0)
    }
}

/** A float RGB image in planar form: `data[c][y * width + x]`, values 0..255 (or normalized). */
class PlanarImage(val width: Int, val height: Int, val data: Array<FloatArray>) {
    init {
        require(data.size == 3 && data.all { it.size == width * height })
    }
}

object Preprocess {

    /** Bounding box (x0, y0, x1Exclusive, y1Exclusive) of pixels whose brightest channel exceeds threshold. */
    fun findFundusBbox(pixels: IntArray, width: Int, height: Int, threshold: Int): IntArray {
        var x0 = Int.MAX_VALUE; var y0 = Int.MAX_VALUE; var x1 = -1; var y1 = -1
        var i = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val p = pixels[i++]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                if (max(r, max(g, b)) > threshold) {
                    if (x < x0) x0 = x
                    if (x > x1) x1 = x
                    if (y < y0) y0 = y
                    if (y > y1) y1 = y
                }
            }
        }
        if (x1 < 0) return intArrayOf(0, 0, width, height)   // fully dark frame: use the whole image
        return intArrayOf(x0, y0, x1 + 1, y1 + 1)
    }

    /**
     * Steps 1-4: ARGB pixels -> float RGB image of size x size in 0..255.
     * Matches `preprocess.crop_and_resize` in Python.
     */
    fun cropAndResize(pixels: IntArray, width: Int, height: Int, cfg: PreprocessConfig): PlanarImage {
        require(pixels.size == width * height) { "pixel buffer does not match width*height" }
        val bb = findFundusBbox(pixels, width, height, cfg.bboxThreshold)
        val x0 = bb[0]; val y0 = bb[1]; val x1 = bb[2]; val y1 = bb[3]
        val cropW = x1 - x0; val cropH = y1 - y0
        val side = max(cropW, cropH)
        val oy = (side - cropH) / 2            // where the crop sits inside the padded square
        val ox = (side - cropW) / 2

        // Block-mean downsample of the padded square by k, trimming to a multiple of k (as NumPy does).
        val k = max(1, side / cfg.size)
        val m = side / k                       // block-mean output side
        val planes = Array(3) { FloatArray(m * m) }
        val kk = (k * k).toFloat()
        for (by in 0 until m) {
            for (bx in 0 until m) {
                var sr = 0; var sg = 0; var sb = 0
                for (yy in by * k until by * k + k) {
                    val sy = yy - oy + y0
                    if (sy < y0 || sy >= y1) continue
                    val row = sy * width
                    for (xx in bx * k until bx * k + k) {
                        val sx = xx - ox + x0
                        if (sx < x0 || sx >= x1) continue
                        val p = pixels[row + sx]
                        sr += (p shr 16) and 0xFF
                        sg += (p shr 8) and 0xFF
                        sb += p and 0xFF
                    }
                }
                val idx = by * m + bx
                planes[0][idx] = sr.toFloat() / kk
                planes[1][idx] = sg.toFloat() / kk
                planes[2][idx] = sb.toFloat() / kk
            }
        }
        var img = PlanarImage(m, m, planes)
        img = bilinearResize(img, cfg.size)
        cfg.benGrahamSigma?.let { img = benGraham(img, it) }
        return img
    }

    /** Bilinear resize to size x size with half-pixel centers (align_corners=false), same math as Python. */
    fun bilinearResize(src: PlanarImage, size: Int): PlanarImage {
        if (src.width == size && src.height == size) return src
        val (yi0, yi1, fy) = coords(src.height, size)
        val (xi0, xi1, fx) = coords(src.width, size)
        val out = Array(3) { FloatArray(size * size) }
        for (c in 0 until 3) {
            val s = src.data[c]; val d = out[c]
            for (y in 0 until size) {
                val r0 = yi0[y] * src.width; val r1 = yi1[y] * src.width
                val wy = fy[y]; val wy0 = 1f - wy
                for (x in 0 until size) {
                    val wx = fx[x]; val wx0 = 1f - wx
                    val top = s[r0 + xi0[x]] * wx0 + s[r0 + xi1[x]] * wx
                    val bottom = s[r1 + xi0[x]] * wx0 + s[r1 + xi1[x]] * wx
                    d[y * size + x] = top * wy0 + bottom * wy
                }
            }
        }
        return PlanarImage(size, size, out)
    }

    private data class Coords(val i0: IntArray, val i1: IntArray, val frac: FloatArray)

    private fun coords(nSrc: Int, nDst: Int): Coords {
        val i0 = IntArray(nDst); val i1 = IntArray(nDst); val frac = FloatArray(nDst)
        val scale = nSrc.toFloat() / nDst.toFloat()
        for (i in 0 until nDst) {
            var pos = (i.toFloat() + 0.5f) * scale - 0.5f
            pos = min(max(pos, 0f), (nSrc - 1).toFloat())
            val f = floor(pos).toInt()
            i0[i] = f
            i1[i] = min(f + 1, nSrc - 1)
            frac[i] = pos - f.toFloat()
        }
        return Coords(i0, i1, frac)
    }

    /** Separable Gaussian blur with edge replication; float32; same kernel as Python. */
    fun gaussianBlur(src: PlanarImage, sigma: Float): PlanarImage {
        val radius = ceil(3f * sigma).toInt()
        val kernel = FloatArray(2 * radius + 1)
        var sum = 0f
        for (i in kernel.indices) {
            val x = (i - radius).toFloat()
            kernel[i] = exp(-(x * x) / (2f * sigma * sigma)); sum += kernel[i]
        }
        for (i in kernel.indices) kernel[i] /= sum
        val w = src.width; val h = src.height
        val out = Array(3) { FloatArray(w * h) }
        val tmp = FloatArray(w * h)
        for (c in 0 until 3) {
            val s = src.data[c]
            // vertical pass
            for (y in 0 until h) for (x in 0 until w) {
                var acc = 0f
                for (i in kernel.indices) {
                    val yy = min(max(y + i - radius, 0), h - 1)
                    acc += kernel[i] * s[yy * w + x]
                }
                tmp[y * w + x] = acc
            }
            // horizontal pass
            for (y in 0 until h) for (x in 0 until w) {
                var acc = 0f
                for (i in kernel.indices) {
                    val xx = min(max(x + i - radius, 0), w - 1)
                    acc += kernel[i] * tmp[y * w + xx]
                }
                out[c][y * w + x] = acc
            }
        }
        return PlanarImage(w, h, out)
    }

    fun benGraham(src: PlanarImage, sigma: Float): PlanarImage {
        val blur = gaussianBlur(src, sigma)
        val out = Array(3) { c ->
            FloatArray(src.width * src.height) { i ->
                min(max(4f * src.data[c][i] - 4f * blur.data[c][i] + 128f, 0f), 255f)
            }
        }
        return PlanarImage(src.width, src.height, out)
    }

    /** Step 5 in CHW layout (what Python writes to fixtures/expected). */
    fun normalizeChw(img: PlanarImage, cfg: PreprocessConfig): FloatArray {
        val n = img.width * img.height
        val out = FloatArray(3 * n)
        for (c in 0 until 3) {
            val mean = cfg.mean[c]; val std = cfg.std[c]; val s = img.data[c]
            for (i in 0 until n) out[c * n + i] = (s[i] / 255f - mean) / std
        }
        return out
    }

    /** Step 5 in NHWC layout (what the TFLite model consumes, see model_card.json `layout_tflite`). */
    fun normalizeNhwc(img: PlanarImage, cfg: PreprocessConfig): FloatArray {
        val n = img.width * img.height
        val out = FloatArray(3 * n)
        for (c in 0 until 3) {
            val mean = cfg.mean[c]; val std = cfg.std[c]; val s = img.data[c]
            for (i in 0 until n) out[i * 3 + c] = (s[i] / 255f - mean) / std
        }
        return out
    }

    /** The whole pipeline to CHW, for tests and comparisons with Python. */
    fun runChw(pixels: IntArray, width: Int, height: Int, cfg: PreprocessConfig): FloatArray =
        normalizeChw(cropAndResize(pixels, width, height, cfg), cfg)

    /** The whole pipeline to NHWC, for the model. */
    fun runNhwc(pixels: IntArray, width: Int, height: Int, cfg: PreprocessConfig): FloatArray =
        normalizeNhwc(cropAndResize(pixels, width, height, cfg), cfg)
}
