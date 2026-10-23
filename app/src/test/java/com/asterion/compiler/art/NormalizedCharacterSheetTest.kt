package com.asterion.compiler.art

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizedCharacterSheetTest {
    private fun sheet(form: String, weapon: String? = null) = NormalizedCharacterSheet(
        sheetId = form, revision = 1, identity = CanonicalCharacterForm("artoria", form, "fate"),
        references = setOf("identity-$form", "weapon-$form"),
        panels = listOf(CharacterPanel("primary", CharacterPanelKind.PRIMARY_IDENTITY,
            setOf("identity-$form"), 100, requiredInRender = true)) +
            listOfNotNull(weapon?.let { CharacterPanel(it, CharacterPanelKind.SIGNATURE_WEAPON,
                setOf("weapon-$form"), 85, identitySignificant = true) })
    )
    @Test fun saberAndLancerHaveIndependentFormIds() {
        val saber = sheet("saber", "Excalibur")
        val lancer = sheet("lancer", "Rhongomyniad")
        assertFalse(saber.identity == lancer.identity)
        assertTrue(CharacterSheetArchitectureV1.validate(saber).isEmpty())
        assertTrue(CharacterSheetArchitectureV1.validate(lancer).isEmpty())
    }
    @Test fun weaponIsOptional() {
        val withoutWeapon = sheet("francesca")
        assertTrue(CharacterSheetArchitectureV1.validate(withoutWeapon).isEmpty())
        assertEquals(1, CharacterSheetArchitectureV1.orderedPanels(withoutWeapon).size)
    }
    @Test fun optionalPanelNeedsEvidence() {
        val base = sheet("lancer")
        val invalid = base.copy(panels = base.panels + CharacterPanel(
            "invented", CharacterPanelKind.WINGS, emptySet(), 99))
        assertTrue(CharacterSheetArchitectureV1.validate(invalid).isNotEmpty())
    }
    @Test fun diagnosticCalloutsRespectLimit() {
        val base = sheet("saber")
        val five = base.copy(panels = base.panels + (1..5).map { i ->
            CharacterPanel("detail-$i", CharacterPanelKind.DIAGNOSTIC_CALLOUT,
                setOf("identity-saber"), i) })
        assertTrue(CharacterSheetArchitectureV1.validate(five).isNotEmpty())
        assertTrue(CharacterSheetArchitectureV1.validate(
            five.copy(exceptionalCalloutsAllowed = true)).isEmpty())
    }
}
