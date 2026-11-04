package org.jameslick.foresight

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * "No ad-hoc colors or sizes anywhere else — lint for it" (spec §3.10 Implementation).
 *
 * Scans every Kotlin file under ui/ EXCEPT ui/theme/ for raw Color literals and .dp/.sp numbers. Those may
 * only live in the token files. A failure lists file:line so the fix is obvious: use a token.
 *
 * Runs as a plain JVM unit test so it is part of `gradlew test` and CI, no Android device needed.
 */
class DesignTokenLintTest {

    private val forbidden = listOf(
        Regex("""\bColor\(0x[0-9A-Fa-f]+""") to "raw Color literal — add a token in ui/theme/Color.kt",
        Regex("""\bColor\.(Red|Green|Blue|Yellow|Magenta|Cyan|Gray|LightGray|DarkGray|White|Black)\b""") to "built-in Color constant — use a token",
        Regex("""(?<![\w.])\d+(\.\d+)?\.(dp|sp)\b""") to "raw dp/sp literal — add a token in ui/theme/Dimens.kt or Type.kt",
    )

    @Test
    fun `screens use design tokens, never raw colors or sizes`() {
        val uiDir = moduleFile("src/main/java/org/jameslick/foresight/ui")
        assertTrue("ui/ directory not found at ${uiDir.absolutePath}", uiDir.isDirectory)
        val themeDir = File(uiDir, "theme")
        val violations = mutableListOf<String>()
        uiDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && !it.absolutePath.startsWith(themeDir.absolutePath) }
            .forEach { file ->
                file.readLines().forEachIndexed { i, line ->
                    val code = line.substringBefore("//")
                    forbidden.forEach { (regex, why) ->
                        if (regex.containsMatchIn(code)) violations += "${file.name}:${i + 1}: $why\n    $line"
                    }
                }
            }
        assertTrue("Design-token violations:\n" + violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `theme tokens exist and are the only place with literals`() {
        val themeDir = moduleFile("src/main/java/org/jameslick/foresight/ui/theme")
        listOf("Color.kt", "Type.kt", "Dimens.kt", "Motion.kt", "Theme.kt").forEach {
            assertTrue("missing token file $it", File(themeDir, it).isFile)
        }
    }

    companion object {
        /** Gradle runs unit tests with the module directory as the working directory. */
        fun moduleFile(relative: String): File {
            val direct = File(relative)
            if (direct.exists()) return direct
            return File("app", relative) // when run from the Gradle root instead
        }
    }
}
