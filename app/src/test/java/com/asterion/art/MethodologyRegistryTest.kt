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
        assertTrue(errors.any { it.contains("concept") })
        assertTrue(MethodologyRegistry.validate(CreationMode.ALTERNATE_UNIVERSE,
            mapOf(SheetRole.CHARACTER to 1), conceptCount=1).isEmpty())
        assertTrue(MethodologyRegistry.validate(CreationMode.CHARACTER,
            emptyMap(), conceptCount=2).isEmpty())
        assertTrue(MethodologyRegistry.validate(CreationMode.STYLE_CORRECTION,
            mapOf(SheetRole.ART_STYLE to 1),
            sourceIllustration=true).isEmpty())
        val creation=MethodologyRegistry.modes.getValue(CreationMode.CHARACTER)
        assertEquals(15,creation.layout.totalDesigns)
        assertEquals(3,creation.layout.pageCount)
        assertEquals("16:9",creation.layout.ratio)
    }

    @Test fun characterV3RequiresThreeViewsForEachIdentity() {
        val character=MethodologyRegistry.modes.getValue(CreationMode.CHARACTER)
        assertEquals(15,character.layout.totalDesigns)
        assertEquals(3,character.layout.pageCount)
        assertTrue(character.directive.contains("FIFTEEN full-body depictions per sheet"))
        assertTrue(character.directive.contains("90-degree side profile"))
        assertTrue(character.directive.contains("direct rear"))
        assertTrue(character.required.isEmpty())
    }
    @Test fun styleCorrectionRequiresOnlyIllustrationAndStyleSheet() {
        val expected=MethodologyRegistry.modes.getValue(CreationMode.STYLE_CORRECTION)
        assertEquals(setOf(SheetRole.ART_STYLE),expected.required)
        assertTrue(expected.requiresIllustration)
        assertFalse(expected.changeBodyAllowed)
        assertTrue(MethodologyRegistry.validate(CreationMode.STYLE_CORRECTION,
            mapOf(SheetRole.ART_STYLE to 1),sourceIllustration=true).isEmpty())
        assertFalse(MethodologyRegistry.validate(CreationMode.STYLE_CORRECTION,
            mapOf(SheetRole.ART_STYLE to 1),sourceIllustration=false).isEmpty())
    }
}
