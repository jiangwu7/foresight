package org.jameslick.foresight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * i18n completeness (spec §8, §3.8):
 *  1. every string key exists in English, Spanish and Vietnamese;
 *  2. every key that starts with "medical_" lives in strings_medical.xml, and strings_medical.xml holds
 *     nothing else;
 *  3. no user-facing Kotlin code under ui/ contains a quoted sentence (a crude guard against inline text).
 */
class I18nCompletenessTest {

    private val languages = mapOf("en" to "values", "es" to "values-es", "vi" to "values-vi")
    private val keyRegex = Regex("""<string\s+name="([^"]+)"""")

    private fun keys(dir: String, file: String): Set<String> {
        val f = DesignTokenLintTest.moduleFile("src/main/res/$dir/$file")
        assertTrue("missing ${f.path}", f.isFile)
        return keyRegex.findAll(f.readText()).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `every key exists in all three languages`() {
        val perLang = languages.mapValues { (_, dir) -> keys(dir, "strings.xml") + keys(dir, "strings_medical.xml") }
        val en = perLang.getValue("en")
        perLang.forEach { (lang, set) ->
            val missing = en - set
            val extra = set - en
            assertTrue("$lang is missing keys: $missing", missing.isEmpty())
            assertTrue("$lang has keys English lacks: $extra", extra.isEmpty())
        }
    }

    @Test
    fun `medical keys live only in strings_medical`() {
        languages.values.forEach { dir ->
            val general = keys(dir, "strings.xml")
            val medical = keys(dir, "strings_medical.xml")
            assertTrue("$dir/strings.xml contains medical keys: ${general.filter { it.startsWith("medical_") }}",
                general.none { it.startsWith("medical_") })
            assertTrue("$dir/strings_medical.xml contains non-medical keys: ${medical.filterNot { it.startsWith("medical_") }}",
                medical.all { it.startsWith("medical_") })
            assertTrue("$dir/strings_medical.xml must contain the §1.3 disclaimer", "medical_disclaimer" in medical)
        }
    }

    @Test
    fun `medical files carry the approved-wording header`() {
        languages.values.forEach { dir ->
            val text = DesignTokenLintTest.moduleFile("src/main/res/$dir/strings_medical.xml").readText()
            assertTrue("$dir/strings_medical.xml lacks the approved-wording header", text.contains("APPROVED MEDICAL WORDING"))
        }
    }

    @Test
    fun `no inline sentences in ui code`() {
        val uiDir = DesignTokenLintTest.moduleFile("src/main/java/org/jameslick/foresight/ui")
        // A "sentence" = a quoted string with at least three words and a space, outside previews and comments.
        val sentence = Regex(""""[A-Za-z][^"]*\s[^"]*\s[^"]*"""")
        val offenders = mutableListOf<String>()
        uiDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            var inPreview = false
            var inBlockComment = false
            file.readLines().forEachIndexed { i, raw ->
                // Skip comments: KDoc / block comments and line comments are documentation, not UI text.
                if (inBlockComment) {
                    if (raw.contains("*/")) inBlockComment = false
                    return@forEachIndexed
                }
                if (raw.trimStart().startsWith("/*")) {
                    if (!raw.contains("*/")) inBlockComment = true
                    return@forEachIndexed
                }
                val line = raw.substringBefore("//")
                if (line.contains("@Preview")) inPreview = true
                if (inPreview && line.trimStart().startsWith("@Composable")) { /* still preview */ }
                if (inPreview && line.startsWith("}") ) inPreview = false
                if (!inPreview && !line.contains("require(") && !line.contains("label =") && sentence.containsMatchIn(line)) {
                    offenders += "${file.name}:${i + 1}: $raw"
                }
            }
        }
        assertEquals("Inline user-facing text found; move it to strings.xml:\n" + offenders.joinToString("\n"), 0, offenders.size)
    }
}
