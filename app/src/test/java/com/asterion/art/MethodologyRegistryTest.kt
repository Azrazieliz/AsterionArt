package com.asterion.art

import com.asterion.art.methodology.MethodologyRegistry
import com.asterion.art.model.CreationMode
import com.asterion.art.model.SheetRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MethodologyRegistryTest {
    @Test fun allUserRequestedCreationModesRegistered() {
        assertEquals(CreationMode.entries.size,MethodologyRegistry.modes.size)
    }
    @Test fun outfitCannotRedesignSourceCharacter() {
        val outfit=MethodologyRegistry.modes.getValue(CreationMode.OUTFIT)
        assertTrue(outfit.protectIdentity)
        assertFalse(outfit.changeBodyAllowed)
    }
    @Test fun mutationIsNotSameAsTransformation() {
        val transform=MethodologyRegistry.modes.getValue(CreationMode.TRANSFORMATION)
        val mutant=MethodologyRegistry.modes.getValue(CreationMode.MUTATION)
        assertTrue(transform.protectIdentity)
        assertFalse(mutant.protectIdentity)
    }
    @Test fun requiresTheCorrectAuthorities() {
        val errors=MethodologyRegistry.validate(CreationMode.ALTERNATE_UNIVERSE,
            mapOf(SheetRole.CHARACTER to 1))
        assertTrue(errors.any { it.contains("Theme") })
    }
}
