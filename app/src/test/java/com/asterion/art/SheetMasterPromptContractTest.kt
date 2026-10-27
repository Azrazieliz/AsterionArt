package com.asterion.art

import com.asterion.art.render.SheetMasterPrompts
import com.asterion.art.model.SheetRole
import com.asterion.art.model.CreationMode
import org.junit.Assert.*
import org.junit.Test

class SheetMasterPromptContractTest {
    @Test fun standardCharacterHasSameIdentityAndReconstructedViews() {
        val p=SheetMasterPrompts.positive(SheetRole.CHARACTER)
        for(needle in listOf("ONE finished character master sheet",
            "SOLE canonical identity","PRIMARY IDENTITY VIEW",
            "FACE IDENTITY","HAIR CONSTRUCTION","ANATOMICAL PROFILE",
            "OUTFIT CONSTRUCTION","COLOR PALETTE",
            "Reconstruct unknown views"))assertTrue(needle,p.contains(needle))
    }
    @Test fun neverTruncateActualVisualEvidence() {
        val evidence="Source observation, distinct markings and anatomy. ".repeat(120)
        assertTrue(SheetMasterPrompts.positive(SheetRole.CHARACTER,evidence)
            .endsWith(evidence))
    }
    @Test fun sourceOriginalSelectsItsOwnSheetMethod() {
        val outfit=SheetMasterPrompts.positive(SheetRole.CHARACTER,"",
            CreationMode.OUTFIT)
        val weapon=SheetMasterPrompts.positive(SheetRole.ELEMENT,"",
            CreationMode.WEAPON)
        val creature=SheetMasterPrompts.positive(SheetRole.CHARACTER,"",
            CreationMode.CREATURE)
        val feminisation=SheetMasterPrompts.positive(SheetRole.CHARACTER,"",
            CreationMode.FEMINISATION)
        assertTrue(outfit.contains("OUTFIT CONSTRUCTION MASTER SHEET"))
        assertTrue(weapon.contains("SIGNATURE WEAPON MASTER SHEET"))
        assertTrue(creature.contains("CREATURE MORPHOLOGY MASTER SHEET"))
        assertTrue(feminisation.contains("FEMINISATION RECONSTRUCTION MASTER SHEET"))
        assertFalse(weapon.contains("PRIMARY IDENTITY VIEW"))
    }
    @Test fun allFourRolesHaveDistinctDirections() {
        assertEquals(4,SheetRole.entries.map{SheetMasterPrompts.positive(it)}
            .distinct().size)
    }
}
