package com.asterion.art

import com.asterion.art.methodology.DesignBlueprintEngine
import org.junit.Assert.*
import org.junit.Test

class DesignBlueprintEngineTest {
    @Test fun threeSheetsContainFifteenDistinctFrozenSpecs() {
        val concepts=listOf("Antimatter","Cloud","Oni")
        val designs=DesignBlueprintEngine.population(concepts)
        assertEquals(15,designs.size)
        assertEquals(15,designs.map { it.id }.distinct().size)
        assertEquals(15,designs.map { it.morphology }.distinct().size)
        assertEquals(15,designs.map { it.faceAndHair }.distinct().size)
        assertTrue(designs.all { it.conceptFusion.contains("Antimatter") &&
            it.conceptFusion.contains("Cloud") && it.conceptFusion.contains("Oni") })
        val first=DesignBlueprintEngine.sheetPrompt(concepts,0)
        val second=DesignBlueprintEngine.sheetPrompt(concepts,1)
        val third=DesignBlueprintEngine.sheetPrompt(concepts,2)
        assertTrue("D01" in first && "D05" in first && "D06" !in first)
        assertTrue("D06" in second && "D10" in second && "D11" !in second)
        assertTrue("D11" in third && "D15" in third && "D01" !in third)
    }

    @Test fun inputConceptsAreNotDropped() {
        val concepts=listOf("Dragon","Galaxy","Chaos","Void")
        assertTrue(DesignBlueprintEngine.population(concepts,15)
            .all { blueprint -> concepts.all { it in blueprint.conceptFusion } })
    }

    @Test fun repeatedInputsDoNotCountAsTwoConcepts() {
        assertTrue(runCatching {
            DesignBlueprintEngine.population(listOf("Oni","Oni"))
        }.isFailure)
    }
}
