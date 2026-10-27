package com.asterion.art

import com.asterion.art.engine.*
import com.asterion.art.learning.DesignFusionRules
import com.asterion.art.model.*
import org.junit.Assert.*
import org.junit.Test

class UniversalCreativePlannerTest {
    @Test fun everyCreationStudioHasIndependentDomainAndNegative() {
        for(mode in CreationMode.entries) {
            val domain=UniversalCreativePlanner.domain(mode)
            val plan=UniversalCreativePlanner.interpret(DesignIntent(domain,
                listOf("Oni","Antimatter")))
            assertTrue(mode.name,plan.positive.length>100)
            assertTrue(mode.name,plan.negative.length>10)
            assertTrue(mode.name,"Oni" in plan.positive)
            assertTrue(mode.name,"Antimatter" in plan.positive)
        }
        assertEquals(9,CreationMode.entries.size)
    }

    @Test fun allFourSheetTypesAndVideoHaveDedicatedContracts() {
        for(role in SheetRole.entries) {
            val info=UniversalCreativePlanner.interpret(
                DesignIntent(UniversalCreativePlanner.sheetDomain(role),
                    referenceRoles=listOf(role)))
            assertTrue(info.positive.isNotBlank())
            assertTrue(info.negative.isNotBlank())
            assertTrue(info.sourceAuthority.isNotEmpty())
        }
        val motion=UniversalCreativePlanner.motion("Camera pan","wind lifts the hair")
        assertTrue(motion.positive.contains("temporal continuity"))
        assertTrue(motion.positive.contains("wind lifts the hair"))
        assertTrue(motion.negative.contains("flicker"))
        val edit=UniversalCreativePlanner.interpret(DesignIntent(CreativeDomain.IMAGE_EDIT))
        assertTrue(edit.positive.contains("selected target region"))
    }

    @Test fun dynamicRecipeAugmentsAllPassesAndPreservesOriginalText() {
        val settings=GeneratorSettings()
        val recipe=GenerationRecipe(RenderMode.ANIME,
            listOf(GenerationPass(1,"original authority","existing negative",1f)),
            emptyList(),settings)
        val result=UniversalCreativePlanner.enrich(recipe,CreationMode.OUTFIT,
            listOf("Crystal","Armor"),emptyList())
        assertEquals(1,result.passes.size)
        assertTrue(result.passes[0].positive.startsWith("original authority"))
        assertTrue(result.passes[0].positive.contains("Crystal"))
        assertTrue(result.passes[0].negative.contains("existing negative"))
        assertTrue(result.passes[0].negative.contains("unwearable"))
    }

    @Test fun crossSheetFusionRetainsBothSourcesAndCustomIntent() {
        val prompt=DesignFusionRules.fuse(
            listOf("D02: armored horned character","D13: transparent rib cage"),
            "preserve the second face")
        assertTrue(prompt.contains("D02"))
        assertTrue(prompt.contains("D13"))
        assertTrue(prompt.contains("preserve the second face"))
        assertTrue(DesignFusionRules.alter("Original identity","change only hair")
            .contains("change only hair"))
    }

    @Test fun allVariantsReceiveDifferentDevelopmentFocus() {
        val variants=(0 until 8).map {
            UniversalCreativePlanner.individual(CreativeDomain.WEAPON,
                listOf("Sword","Crystal"),it,"Frozen weapon design")
        }
        assertEquals(8,variants.distinct().size)
        assertTrue(variants.all { it.contains("Frozen weapon design") })
        assertTrue(variants.all { it.contains("Sword") && it.contains("Crystal") })
    }
}
