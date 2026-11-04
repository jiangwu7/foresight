package org.jameslick.foresight.ml

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The parts of `model_card.json` (written by ml/make_model_card.py) that the app needs at runtime, plus the
 * descriptive fields shown in Settings -> About models (spec §4.4). Parsed leniently: the card is produced by
 * our own script, but we only hard-fail on the fields inference cannot work without.
 */
@Serializable
data class ModelCard(
    val name: String,
    val version: String,
    val architecture: String,
    val task: String,
    val preprocess: PreprocessSpec,
    val inputLayoutNhwc: Boolean,
    val classes: List<String>,
    val referableThreshold: Float,
    val featureChannels: Int,
    /** head.class_weights[k][c] — lets the app draw the class-activation heat map without backprop. */
    val classWeights: List<List<Float>>,
    val metrics: Map<String, Float>,
    val intendedUse: String,
    val limitations: List<String>,
    val disclaimer: String,
    val trainingData: String,
    val exportStatus: String,
) {
    @Serializable
    data class PreprocessSpec(
        val size: Int,
        val mean: List<Float>,
        val std: List<Float>,
        val bboxThreshold: Int,
        val benGrahamSigma: Float?,
    )

    fun toPreprocessConfig() = PreprocessConfig(
        size = preprocess.size,
        mean = preprocess.mean.toFloatArray(),
        std = preprocess.std.toFloatArray(),
        bboxThreshold = preprocess.bboxThreshold,
        benGrahamSigma = preprocess.benGrahamSigma,
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun parse(text: String): ModelCard {
            val root = json.parseToJsonElement(text).jsonObject
            val pre = root.getValue("preprocess").jsonObject
            val outputs = root.getValue("outputs").jsonObject
            val head = root.getValue("head").jsonObject
            val metricsObj = root["metrics_heldout"]?.jsonObject ?: JsonObject(emptyMap())
            val layout = root["input"]?.jsonObject?.get("layout_tflite")?.jsonPrimitive?.contentOrNull ?: "NHWC"
            return ModelCard(
                name = root.str("name"),
                version = root.str("version"),
                architecture = root.str("architecture"),
                task = root.str("task"),
                preprocess = PreprocessSpec(
                    size = pre.getValue("size").jsonPrimitive.int,
                    mean = pre.getValue("mean").jsonArray.map { it.jsonPrimitive.float },
                    std = pre.getValue("std").jsonArray.map { it.jsonPrimitive.float },
                    bboxThreshold = pre["bbox_threshold"]?.jsonPrimitive?.int ?: 10,
                    benGrahamSigma = pre["ben_graham_sigma"]?.takeUnless { it.isNull() }?.jsonPrimitive?.float,
                ),
                inputLayoutNhwc = layout.startsWith("NHWC"),
                classes = outputs.getValue("logits").jsonObject.getValue("classes").jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" },
                referableThreshold = metricsObj["referable_threshold"]?.jsonPrimitive?.float ?: OutcomeMapper.DEFAULT_REFERABLE_THRESHOLD,
                featureChannels = outputs.getValue("features").jsonObject.getValue("channels").jsonPrimitive.int,
                classWeights = head.getValue("class_weights").jsonArray.map { row -> row.jsonArray.map { it.jsonPrimitive.float } },
                metrics = metricsObj.entries
                    .filter { (_, v) -> v is kotlinx.serialization.json.JsonPrimitive && v.contentOrNull?.toFloatOrNull() != null && !v.isString }
                    .associate { (k, v) -> k to v.jsonPrimitive.float },
                intendedUse = root.str("intended_use"),
                limitations = root["limitations"]?.jsonArray?.map { it.jsonPrimitive.contentOrNull ?: "" } ?: emptyList(),
                disclaimer = root.str("disclaimer"),
                trainingData = root["training_data"]?.toString() ?: "",
                exportStatus = root.str("export_status"),
            )
        }

        private fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.contentOrNull ?: ""
        private fun JsonElement.isNull(): Boolean = this is kotlinx.serialization.json.JsonNull
    }
}
