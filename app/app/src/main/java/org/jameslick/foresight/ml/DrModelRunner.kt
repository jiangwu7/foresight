package org.jameslick.foresight.ml

import android.content.Context
import android.os.SystemClock
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.exp

/**
 * Runs the DR grading model (M2, spec §4.3) on device with LiteRT. Everything stays on the phone (§6).
 *
 * Inputs are ARGB pixels straight from a Bitmap; preprocessing is `Preprocess` (the verified port of the
 * Python pipeline) with constants from the model card. Outputs are matched by shape, not by position,
 * because converters reorder them: [1,5] logits, [1,1] regression, 4-D feature map.
 *
 * Phase 1 uses the CPU (XNNPACK) path. The GPU/NNAPI delegates are evaluated against the 1.5 s budget in
 * Phase 2 together with the full int8 model.
 */
class DrModelRunner private constructor(
    val card: ModelCard,
    private val interpreter: Interpreter,
) : Closeable {

    class Output(
        /** softmax over the 5 grades */
        val probs: FloatArray,
        val referableProb: Float,
        /** regression head estimate of the grade (0..4) */
        val reg: Float,
        /** class-activation heat map for referable DR, row-major 0..1 */
        val heatmap: FloatArray,
        val heatRows: Int,
        val heatCols: Int,
        val runtimeMs: Long,
    ) {
        val grade: Int get() = EyeResult.argmax(probs)
        fun toEyeResult(gradable: Boolean = true) = EyeResult(gradable, probs, referableProb)
    }

    private val cfg = card.toPreprocessConfig()
    private val inputIndex = 0
    private val logitsIndex: Int
    private val regIndex: Int
    private val featIndex: Int
    private val featShape: IntArray
    private val featNhwc: Boolean

    init {
        val outs = (0 until interpreter.outputTensorCount).map { it to interpreter.getOutputTensor(it).shape() }
        logitsIndex = outs.first { it.second.contentEquals(intArrayOf(1, 5)) }.first
        regIndex = outs.first { it.second.contentEquals(intArrayOf(1, 1)) }.first
        val feat = outs.first { it.second.size == 4 }
        featIndex = feat.first
        featShape = feat.second
        featNhwc = featShape[3] == card.featureChannels
        require(featNhwc || featShape[1] == card.featureChannels) { "feature map ${featShape.toList()} does not match card channels ${card.featureChannels}" }
        val inShape = interpreter.getInputTensor(inputIndex).shape()
        require(inShape.contentEquals(intArrayOf(1, cfg.size, cfg.size, 3))) { "model input ${inShape.toList()} != [1,${cfg.size},${cfg.size},3]; card and model disagree" }
    }

    /** Full pipeline on one eye photo. Thread-safe only if called from one thread at a time. */
    fun run(pixels: IntArray, width: Int, height: Int): Output {
        val t0 = SystemClock.elapsedRealtime()
        val input = Preprocess.runNhwc(pixels, width, height, cfg)
        val inBuf = ByteBuffer.allocateDirect(4 * input.size).order(ByteOrder.nativeOrder())
        inBuf.asFloatBuffer().put(input)

        val logits = Array(1) { FloatArray(5) }
        val reg = Array(1) { FloatArray(1) }
        val featCount = featShape.fold(1) { a, b -> a * b }
        val featBuf = ByteBuffer.allocateDirect(4 * featCount).order(ByteOrder.nativeOrder())
        interpreter.runForMultipleInputsOutputs(arrayOf(inBuf), mapOf(logitsIndex to logits, regIndex to reg, featIndex to featBuf))

        val probs = softmax(logits[0])
        val referable = OutcomeMapper.referableProb(probs)
        featBuf.rewind()
        val feats = FloatArray(featCount).also { featBuf.asFloatBuffer().get(it) }
        val (rows, cols, channels, nhwc) = if (featNhwc) Quad(featShape[1], featShape[2], featShape[3], feats)
            else Quad(featShape[2], featShape[3], featShape[1], nchwToNhwc(feats, featShape[1], featShape[2], featShape[3]))
        val heat = ClassActivationMap.referable(nhwc, rows, cols, channels, card.classWeights, probs)
        return Output(probs, referable, reg[0][0], heat, rows, cols, SystemClock.elapsedRealtime() - t0)
    }

    override fun close() = interpreter.close()

    private data class Quad(val rows: Int, val cols: Int, val channels: Int, val data: FloatArray)

    companion object {
        const val MODEL_ASSET = "models/dr_m2_int8.tflite"
        const val CARD_ASSET = "models/dr_m2_model_card.json"

        fun load(context: Context, modelAsset: String = MODEL_ASSET, cardAsset: String = CARD_ASSET, threads: Int = 4): DrModelRunner {
            val card = ModelCard.parse(context.assets.open(cardAsset).bufferedReader().readText())
            val model = context.assets.openFd(modelAsset).use { fd ->
                fd.createInputStream().channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
            val options = Interpreter.Options().setNumThreads(threads)
            return DrModelRunner(card, Interpreter(model, options))
        }

        fun softmax(z: FloatArray): FloatArray {
            val m = z.max()
            val e = FloatArray(z.size) { exp(z[it] - m) }
            val s = e.sum()
            return FloatArray(z.size) { e[it] / s }
        }

        fun nchwToNhwc(src: FloatArray, c: Int, h: Int, w: Int): FloatArray {
            val out = FloatArray(src.size)
            for (ch in 0 until c) for (y in 0 until h) for (x in 0 until w) {
                out[(y * w + x) * c + ch] = src[(ch * h + y) * w + x]
            }
            return out
        }
    }
}
