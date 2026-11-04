package org.jameslick.foresight.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Export schema validation (spec §8): clinic/personal fields never appear in the de-identified export. */
class DeidExportTest {

    private val event = Event(id = "ev-1", name = "Parent Night", locationLabel = "James Lick HS", date = 1_700_000_000_000L, lat = 37.35, lng = -121.83)
    private val model = DeidExport.ModelInfo("foresight-dr-m2", "20261104-172135+61fb3fa")

    private fun row(outcome: String?, lastExam: String? = "never", gradable: Int = 2, captured: Int = 2, deleted: Boolean = false, printed: Boolean = false, follow: String = "none") =
        DeidRow("p-${System.nanoTime()}", "es", "40_59", "no", lastExam, true, deleted, outcome, printed, follow, gradable, captured)

    @Test
    fun `export has the schema, counts and percentages`() {
        val rows = listOf(
            row("green", lastExam = "within_1y"), row("yellow", printed = true), row("red", printed = true, follow = "completed"),
            row("ungradable", gradable = 1), row(null, captured = 0, gradable = 0), row("green", deleted = true),
        )
        val f = DeidExport.build(event, rows, model, "0.1.0", now = 1L)
        assertEquals(DeidExport.SCHEMA, f.schema)
        assertEquals(6, f.summary.screened)
        assertEquals(1, f.summary.records_deleted)
        assertEquals(4, f.summary.with_result)
        assertEquals(2, f.summary.referrals_printed)
        assertEquals(1, f.summary.follow_ups_completed)
        assertEquals(25.0, f.summary.pct_green!!, 1e-9)
        assertEquals(25.0, f.summary.pct_red!!, 1e-9)
        // gradable eyes 2+2+2+1+0 = 7 of 8 captured -> 87.5%
        assertEquals(87.5, f.summary.gradability_rate!!, 1e-9)
        // no exam in 2+ years: 4 of 5 live rows with an answer
        assertEquals(80.0, f.summary.pct_no_exam_2y_or_never!!, 1e-9)
    }

    @Test
    fun `deleted records keep only a count`() {
        val f = DeidExport.build(event, listOf(row("red", deleted = true)), model, "0.1.0", 1L)
        val p = f.participants.single()
        assertTrue(p.record_deleted)
        assertNull(p.outcome); assertNull(p.age_range); assertNull(p.last_exam); assertEquals(0, p.captured_eyes)
    }

    @Test
    fun `encoded file contains no disallowed keys and no participant ids`() {
        val text = DeidExport.encode(DeidExport.build(event, listOf(row("yellow")), model, "0.1.0", 1L))
        val lower = text.lowercase()
        for (k in DeidExport.DISALLOWED_KEYS - "name") assertFalse("found '$k'", lower.contains("\"$k\""))
        // "name" is allowed only as the event's and the model's display name
        assertEquals(2, Regex("\"name\"").findAll(lower).count())
        assertFalse(lower.contains("participantid"))
        val root = Json.parseToJsonElement(text).jsonObject
        assertEquals(DeidExport.SCHEMA, root["schema"]!!.jsonPrimitive.content)
    }

    @Test
    fun `validator rejects a file with a personal field anywhere in the tree`() {
        val bad = buildJsonObject {
            put("schema", DeidExport.SCHEMA)
            put("participants", kotlinx.serialization.json.buildJsonArray { add(buildJsonObject { put("language", "en"); put("phone", "408") }) })
        }
        try {
            DeidExport.assertNoDisallowedKeys(bad)
            assertTrue("expected rejection", false)
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("phone"))
        }
    }

    @Test
    fun `file name is derived from the event name`() {
        assertEquals("foresight_export_parent-night_deid.json", DeidExport.fileName(event))
    }
}
