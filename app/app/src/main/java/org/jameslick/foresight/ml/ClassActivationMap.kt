package org.jameslick.foresight.ml

import kotlin.math.max

/**
 * Heat map from the model's last feature map (spec §4.3 Explainability).
 *
 * The DR model's head is a single linear layer on globally-pooled features, so Grad-CAM for class k reduces
 * exactly to  heat[y][x] = sum_c W[k][c] * features[y][x][c]. No gradients, no second model run: the app
 * reads `features` (an output of the .tflite) and `head.class_weights` from the model card.
 *
 * The map is ReLU'd and scaled to 0..1 (max = 1), then upsampled on screen by `HeatmapOverlay`. It is an
 * attention map, never a lesion detector; the UI caption says only "Highlighted areas influenced the result".
 */
object ClassActivationMap {

    /**
     * @param features flat NHWC feature map of shape [rows, cols, channels] (batch dropped)
     * @param classWeights [numClasses][channels] from the model card
     * @param classIndices which classes to combine; e.g. the referable classes 2..4, weighted by their probabilities
     * @param classProbs softmax probabilities, used to weight the chosen classes (null = equal weights)
     * @return row-major grid in 0..1, length rows * cols
     */
    fun compute(
        features: FloatArray,
        rows: Int,
        cols: Int,
        channels: Int,
        classWeights: List<List<Float>>,
        classIndices: IntArray,
        classProbs: FloatArray? = null,
    ): FloatArray {
        require(features.size == rows * cols * channels) { "features size ${features.size} != $rows*$cols*$channels" }
        require(classIndices.isNotEmpty())
        // Combined channel weights: w[c] = sum_k p_k * W[k][c]
        val w = FloatArray(channels)
        for (k in classIndices) {
            val row = classWeights[k]
            require(row.size == channels) { "class_weights[$k] has ${row.size} channels, expected $channels" }
            val pk = classProbs?.get(k) ?: 1f
            for (c in 0 until channels) w[c] += pk * row[c]
        }
        val heat = FloatArray(rows * cols)
        var maxV = 0f
        for (y in 0 until rows) for (x in 0 until cols) {
            val base = (y * cols + x) * channels
            var acc = 0f
            for (c in 0 until channels) acc += w[c] * features[base + c]
            val v = max(acc, 0f)          // ReLU: only evidence *for* the class is shown
            heat[y * cols + x] = v
            if (v > maxV) maxV = v
        }
        if (maxV > 0f) for (i in heat.indices) heat[i] /= maxV
        return heat
    }

    /** Heat map for "referable DR" = classes moderate, severe, proliferative, weighted by their probabilities. */
    fun referable(features: FloatArray, rows: Int, cols: Int, channels: Int, classWeights: List<List<Float>>, probs: FloatArray): FloatArray =
        compute(features, rows, cols, channels, classWeights, intArrayOf(2, 3, 4), probs)
}
