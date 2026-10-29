package com.asterion.art

import com.asterion.art.brain.StructuredPlanOutput
import org.junit.Assert.*
import org.junit.Test

class StructuredPlanOutputTest {
    @Test fun rejectsUnstructuredReasoningInsteadOfSendingItToDiffusion() {
        assertThrows(IllegalArgumentException::class.java) {
            StructuredPlanOutput.validate("Here is some design advice.","")
        }
    }

    @Test fun acceptsOneCompleteImageSpecificationUnmodified() {
        val full="A deeply specified source-faithful original illustration showing " +
            "consistent anatomy, exact character identity, environment, lighting and surfaces."
        val parsed=StructuredPlanOutput.validate(
            "[POSITIVE]"+full+"[/POSITIVE][NEGATIVE]blurry[/NEGATIVE]","")
        assertEquals(full,parsed.positive)
        assertEquals("blurry",parsed.negative)
        assertTrue(parsed.sheetPrompts.isEmpty())
    }

    @Test fun requiresAllThreeCharacterPopulationSheets() {
        val answer="[SHEET_1]"+ "Identity and anatomical details. ".repeat(5)+
            "[/SHEET_1][SHEET_2]"+ "Distinct character detail. ".repeat(5)+
            "[/SHEET_2]"
        assertThrows(IllegalArgumentException::class.java) {
            StructuredPlanOutput.validate(answer,"",true)
        }
    }
}
