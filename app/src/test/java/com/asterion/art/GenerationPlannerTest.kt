package com.asterion.art

import com.asterion.art.engine.GenerationPlanner
import com.asterion.art.engine.SourceEvidence
import com.asterion.art.model.CreationMode
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
    @Test fun longAuthoritativeSheetDescriptionsSurviveUnchanged() {
        val required="Canonical multi-component source identity: " +
            "precise facial geometry, hair architecture, body construction, " +
            "material and distinctive traits preserved with no substitution. ".repeat(5)
        val source=SourceEvidence("long-source",SheetRole.CHARACTER,
            mapOf("PRIMARY" to listOf(required)))
        val recipe=GenerationPlanner.compile(
            listOf(source)+sheets.filter{it.role!=SheetRole.CHARACTER},
            RenderMode.ANIME,GeneratorSettings())
        assertTrue(recipe.passes.all { it.positive.contains(required.trim()) })
    }

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
    @Test fun embeddedReadableRinLabelResolvesEvenWithRandomQnnId() {
        val nativeModels=listOf(
            "qnn_123456789012345678901234" to "Rin Flanime v1 Turbo (Anima QNN)",
            "qnn_abcdefabcdefabcdefabcdef" to "MiaoMiao v1.4 (Anima QNN)")
        assertEquals("qnn_123456789012345678901234",
            GenerationPlanner.selectModelId(nativeModels,RenderMode.ANIME))
    }
    @Test fun aSingleSourceImageRemainsAvailableToIndependentImageEditing() {
        val selected=GeneratorSettings(denoise=0.62f)
        val recipe=GenerationPlanner.compileFromSource(RenderMode.ANIME,selected)
        assertEquals(5,recipe.passes.size)
        assertEquals(0.62f,recipe.passes.first().denoise,0.0001f)
        assertTrue(recipe.passes.first().positive.contains("SOURCE IMAGE PIXELS"))
        assertTrue(recipe.passes.all{it.positive.contains("identity")})
    }
    @Test fun artworkRequiresCharacterThemeAndArtStyleAuthorities() {
        assertTrue(runCatching {
            GenerationPlanner.compileVisual(
                emptyList(),RenderMode.ANIME,GeneratorSettings(),
                sourceIllustration=true)
        }.isFailure)
        val complete=GenerationPlanner.compileVisual(
            sheets,RenderMode.ANIME,GeneratorSettings())
        assertEquals(5,complete.passes.size)
        assertTrue(runCatching {
            GenerationPlanner.compileVisual(
                sheets.filter{it.role!=SheetRole.THEME},
                RenderMode.ANIME,GeneratorSettings(),
                sourceIllustration=true)
        }.isFailure)
    }
    @Test fun eachOriginalUsesItsOwnSingleCompleteNativePrompt() {
        // No Original is a beneficiary of Artwork's five image-quality phases.
        val female=GenerationPlanner.compileVisual(
            emptyList(),RenderMode.ANIME,GeneratorSettings(),
            creation=CreationMode.FEMINISATION,sourceIllustration=true)
        assertEquals(1,female.passes.size)
        assertEquals(1,female.passes.first().number)
        val creature=GenerationPlanner.compileConcepts(
            listOf("Rose"),CreationMode.CREATURE,RenderMode.ANIME,GeneratorSettings())
        assertEquals(1,creature.passes.size)
        assertTrue(creature.passes.first().positive.contains("Rose"))
    }

}
