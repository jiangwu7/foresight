package org.jameslick.foresight.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * The de-identified export `foresight_export_<event>_deid.json` (spec §3.7, §5).
 *
 * Contains aggregates and one row per participant with NO images, NO contact, NO free text, NO names and
 * ages only as buckets. It is built from `ExportDao.deidRows`, a SQL projection that never selects the
 * encrypted columns, and then checked by `assertNoDisallowedKeys` before it is written. The dashboard
 * re-checks on load. Pure Kotlin so the schema test runs on the JVM.
 */
object DeidExport {
    const val SCHEMA = "foresight-deid-v1"

    /** Keys that must never appear anywhere in a de-identified file (dashboard rejects them too). */
    val DISALLOWED_KEYS = setOf(
        "name", "first_name", "last_name", "contact", "phone", "email", "address", "dob", "birth",
        "image", "images", "image_path", "photo", "signature", "notes", "free_text", "comment",
        "contactEncrypted", "consentSignatureBlob", "imagePathEncrypted", "heatmapPath",
    )

    @Serializable
    data class EventInfo(val id: String, val name: String, val location_label: String, val date: Long, val lat: Double? = null, val lng: Double? = null)

    @Serializable
    data class ModelInfo(val name: String, val version: String)

    @Serializable
    data class ParticipantRow(
        val language: String,
        val age_range: String?,
        val dx_diabetes: String?,
        val last_exam: String?,
        val has_doctor: Boolean?,
        val outcome: String?,
        val gradable_eyes: Int,
        val captured_eyes: Int,
        val card_printed: Boolean?,
        val follow_up_status: String?,
        val record_deleted: Boolean,
    )

    @Serializable
    data class Summary(
        val screened: Int,
        val records_deleted: Int,
        val with_result: Int,
        val gradability_rate: Double?,
        val pct_no_exam_2y_or_never: Double?,
        val pct_green: Double?,
        val pct_yellow: Double?,
        val pct_red: Double?,
        val pct_ungradable: Double?,
        val referrals_printed: Int,
        val follow_ups_completed: Int,
    )

    @Serializable
    data class File(
        val schema: String,
        val exported_at: Long,
        val app_version: String,
        val model: ModelInfo,
        val event: EventInfo,
        val summary: Summary,
        val participants: List<ParticipantRow>,
    )

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun build(event: Event, rows: List<DeidRow>, model: ModelInfo, appVersion: String, now: Long): File {
        val participants = rows.map { r ->
            if (r.deleted) ParticipantRow(r.language, null, null, null, null, null, 0, 0, null, null, record_deleted = true)
            else ParticipantRow(r.language, r.ageRange, r.dxDiabetes, r.lastExam, r.hasDoctor, r.outcome,
                r.gradableEyes, r.capturedEyes, r.cardPrinted, r.followUpStatus, record_deleted = false)
        }
        val live = participants.filter { !it.record_deleted }
        val withResult = live.filter { it.outcome != null }
        fun pct(n: Int, d: Int): Double? = if (d == 0) null else Math.round(1000.0 * n / d) / 10.0
        val capturedEyes = live.sumOf { it.captured_eyes }
        val summary = Summary(
            screened = participants.size,
            records_deleted = participants.count { it.record_deleted },
            with_result = withResult.size,
            gradability_rate = pct(live.sumOf { it.gradable_eyes }, capturedEyes),
            pct_no_exam_2y_or_never = pct(live.count { it.last_exam == "over_2y" || it.last_exam == "never" }, live.count { it.last_exam != null }),
            pct_green = pct(withResult.count { it.outcome == "green" }, withResult.size),
            pct_yellow = pct(withResult.count { it.outcome == "yellow" }, withResult.size),
            pct_red = pct(withResult.count { it.outcome == "red" }, withResult.size),
            pct_ungradable = pct(withResult.count { it.outcome == "ungradable" }, withResult.size),
            referrals_printed = live.count { it.card_printed == true },
            follow_ups_completed = live.count { it.follow_up_status == "completed" },
        )
        return File(SCHEMA, now, appVersion, model,
            EventInfo(event.id, event.name, event.locationLabel, event.date, event.lat, event.lng), summary, participants)
    }

    fun encode(file: File): String {
        val text = json.encodeToString(file)
        assertNoDisallowedKeys(json.parseToJsonElement(text))
        return text
    }

    fun fileName(event: Event): String = "foresight_export_${event.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}_deid.json"

    /** The only places a "name" may appear: the event's display name and the model's name. Same list as dashboard.js. */
    val ALLOWED_PATHS = setOf("$.event.name", "$.model.name")

    /** Walks the whole JSON tree; throws if any object key is on the disallowed list. */
    fun assertNoDisallowedKeys(element: JsonElement, path: String = "$") {
        when (element) {
            is JsonObject -> element.entries.forEach { (k, v) ->
                val p = "$path.$k"
                check(k.lowercase() !in DISALLOWED_KEYS || p in ALLOWED_PATHS) { "de-identified export contains disallowed key '$k' at $p" }
                assertNoDisallowedKeys(v, p)
            }
            is JsonArray -> element.forEachIndexed { i, v -> assertNoDisallowedKeys(v, "$path[$i]") }
            else -> Unit
        }
    }
}
