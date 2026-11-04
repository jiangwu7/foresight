package org.jameslick.foresight.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Parses the model card that actually ships in assets, so a card format change breaks the build, not the app. */
class ModelCardTest {

    private val cardFile = DesignTokenLintTestPath.moduleFile("src/main/assets/models/dr_m2_model_card.json")

    @Test
    fun `shipped model card parses and carries what inference needs`() {
        assertTrue("model card missing: ${cardFile.absolutePath}", cardFile.isFile)
        val card = ModelCard.parse(cardFile.readText())
        assertEquals("foresight-dr-m2", card.name)
        assertEquals(224, card.preprocess.size)
        assertEquals(3, card.preprocess.mean.size)
        assertEquals(3, card.preprocess.std.size)
        assertTrue(card.inputLayoutNhwc)
        assertEquals(listOf("none", "mild", "moderate", "severe", "proliferative"), card.classes)
        assertEquals(5, card.classWeights.size)
        assertEquals(card.featureChannels, card.classWeights[0].size)
        assertEquals(0.5f, card.referableThreshold, 1e-6f)
        assertTrue(card.disclaimer.startsWith("Foresight is a screening tool operated by students."))
        assertTrue(card.metrics.containsKey("referable_sensitivity"))
        val cfg = card.toPreprocessConfig()
        assertEquals(224, cfg.size)
        assertEquals(null, cfg.benGrahamSigma)
    }
}

/** Shared helper so tests find module files whether Gradle runs them from the module or the root. */
object DesignTokenLintTestPath {
    fun moduleFile(relative: String): File {
        val direct = File(relative)
        if (direct.exists()) return direct
        return File("app", relative)
    }
}
