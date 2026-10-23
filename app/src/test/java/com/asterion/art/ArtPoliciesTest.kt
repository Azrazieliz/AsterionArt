package com.asterion.art

import com.asterion.art.model.ArtPolicies
import com.asterion.art.model.SheetRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtPoliciesTest {
    @Test fun gradesAreBounded() {
        assertEquals(0, ArtPolicies.grade(-11))
        assertEquals(5, ArtPolicies.grade(19))
    }
    @Test fun automationRequiresCoreReferenceTypes() {
        assertFalse(ArtPolicies.eligibleForAutomation(mapOf(SheetRole.CHARACTER to 1)))
        assertTrue(ArtPolicies.eligibleForAutomation(mapOf(
            SheetRole.CHARACTER to 2,
            SheetRole.ART_STYLE to 1,
            SheetRole.THEME to 1
        )))
    }
    @Test fun noTranslationUtilityAndOneAnimeRoute() {
        assertEquals(4, ArtPolicies.sheetRoles.size)
        assertEquals("Rin Flanime", ArtPolicies.modelRanking.first())
        assertTrue(ArtPolicies.creationModes.none { it.name.contains("TRANSLATION") })
    }
}
