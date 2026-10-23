package com.asterion.art

import com.asterion.art.model.SheetLayouts
import com.asterion.art.model.SheetRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetLayoutsTest {
    @Test fun everyRoleHasAUniqueRequiredPrimaryPanel() {
        SheetRole.entries.forEach { role ->
            val slots = SheetLayouts.forRole(role)
            assertEquals("PRIMARY", slots.first().id)
            assertEquals(slots.size, slots.map { it.id }.distinct().size)
            assertTrue(SheetLayouts.isValid(role, "PRIMARY"))
        }
    }
    @Test fun noInventedWeaponForFrancescaOrUnrelatedPanel() {
        assertTrue(SheetLayouts.isValid(SheetRole.CHARACTER, "DISTINCTIVE"))
        assertFalse(SheetLayouts.isValid(SheetRole.THEME, "DISTINCTIVE"))
    }
    @Test fun allReferenceTypesHaveExplicitLabels() {
        SheetRole.entries.forEach { role ->
            SheetLayouts.forRole(role).forEach { slot ->
                assertTrue(SheetLayouts.label(role, slot.id).isNotBlank())
            }
        }
    }
}
