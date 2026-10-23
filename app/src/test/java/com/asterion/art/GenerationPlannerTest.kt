package com.asterion.art

import com.asterion.art.engine.GenerationPlanner
import com.asterion.art.engine.SourceEvidence
import com.asterion.art.model.GeneratorSettings
import com.asterion.art.model.RenderMode
import com.asterion.art.model.SheetRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationPlannerTest {
    private val sheets = listOf(
        SourceEvidence("character", SheetRole.CHARACTER,
            mapOf("PRIMARY" to listOf("Silver braided hair", "Distinctive green eyes"),
                "OUTFIT" to listOf("Blue armor"), "DISTINCTIVE" to listOf("Golden lance"))),
        SourceEvidence("style", SheetRole.ART_STYLE,
            mapOf("PRIMARY" to listOf("High contrast anime paint"),
                "MATERIAL" to listOf("Glossy metal"))),
        SourceEvidence("theme", SheetRole.THEME,
            mapOf("PRIMARY" to listOf("Monumental throne setting"),
                "POSE" to listOf("Symmetrical seating"))),
    )
    @Test fun fivePassesCarryCompleteFoundation() {
        val recipe = GenerationPlanner.compile(sheets, RenderMode.ANIME, GeneratorSettings())
        assertEquals(5, recipe.passes.size)
        recipe.passes.forEach { stage ->
            assertTrue(stage.positive.contains("Silver braided hair"))
            assertTrue(stage.positive.contains("Monumental throne setting"))
            assertTrue(stage.positive.contains("Blue armor"))
        }
        assertTrue(recipe.passes.last().denoise < recipe.passes[1].denoise)
    }
    @Test fun noImagesWithoutAuthoritativeEvidence() {
        val invalid = sheets.map { it.copy(byPanel = emptyMap()) }
        val failure = runCatching {
            GenerationPlanner.compile(invalid, RenderMode.ANIME, GeneratorSettings())
        }
        assertTrue(failure.isFailure)
    }
    @Test fun oneAutoModelRouteKeepsUsersPreferredOrder() {
        val models = listOf("hassaku-xl" to "Hassaku XL", "rin_flanime_1" to "Rin Flanime")
        assertEquals("rin_flanime_1", GenerationPlanner.selectModelId(models, RenderMode.ANIME))
        assertFalse(GenerationPlanner.selectModelId(emptyList(), RenderMode.ANIME) != null)
    }
}
