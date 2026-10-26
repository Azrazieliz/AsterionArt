package com.asterion.art

import com.asterion.art.methodology.*
import com.asterion.art.model.CreationMode
import com.asterion.art.model.SheetRole
import org.junit.Assert.*
import org.junit.Test

class DetailedMethodologyContractTest {
    @Test fun characterCreationNeedsNoPriorCharacterOrThemeSheet() {
        val start=MethodologyRequest(CreationMode.CHARACTER,conceptIds=listOf("SC084","SC016"))
        assertTrue(DetailedMethodologies.validate(start).isEmpty())
        val layout=DetailedMethodologies.geometry(start)
        assertEquals("16:9",layout.ratio)
        assertEquals(3,layout.imageCount)
        assertEquals(5,layout.designsPerImage)
        assertTrue(layout.frontAndRearViews)
    }
    @Test fun styleCorrectionOnlyNeedsArtworkAndTargetStyle() {
        val request=MethodologyRequest(CreationMode.STYLE_CORRECTION,
            sheetRoles=setOf(SheetRole.ART_STYLE),sourceIllustrationPresent=true)
        assertTrue(DetailedMethodologies.validate(request).isEmpty())
        assertEquals("SOURCE",DetailedMethodologies.geometry(request).ratio)
        assertFalse(DetailedMethodologies.geometry(request).whiteNeutralField)
        assertTrue(DetailedMethodologies.validate(request.copy(sourceIllustrationPresent=false)).isNotEmpty())
    }
    @Test fun outfitAndWeaponUseDifferentAuthoritativePresentation() {
        val source=MethodologyRequest(CreationMode.OUTFIT,sourceIllustrationPresent=true)
        val weapon=source.copy(mode=CreationMode.WEAPON)
        assertTrue(DetailedMethodologies.validate(source).isEmpty())
        assertTrue(DetailedMethodologies.validate(weapon).isEmpty())
        assertEquals(10,DetailedMethodologies.geometry(source).designsPerImage)
        assertEquals(2,DetailedMethodologies.geometry(weapon).rows)
        assertTrue(DetailedMethodologies.all.getValue(CreationMode.WEAPON)
            .rendererDirective.contains("ISOLATED"))
    }
    @Test fun characterCreationIsOnlyPhaseOne() {
        val base=MethodologyRequest(CreationMode.CHARACTER,conceptIds=listOf("SC084","SC016"))
        assertTrue(DetailedMethodologies.validate(base).isEmpty())
        assertTrue(DetailedMethodologies.validate(base.copy(action=StageAction.OUTFIT)).isNotEmpty())
        assertTrue(DetailedMethodologies.validate(base.copy(action=StageAction.WEAPON)).isNotEmpty())
        assertTrue(DetailedMethodologies.validate(base.copy(action=StageAction.ASSEMBLY)).isNotEmpty())
    }

    @Test fun feminisationRequiresSourceImageAndOneFullBodyOutput() {
        val base=MethodologyRequest(CreationMode.FEMINISATION,sourceIllustrationPresent=true)
        assertTrue(DetailedMethodologies.validate(base).isEmpty())
        assertEquals("9:16",DetailedMethodologies.geometry(base).ratio)
        assertTrue(DetailedMethodologies.validate(base.copy(sourceIllustrationPresent=false)).isNotEmpty())
    }
    @Test fun mutationAndAlternateUniverseAreNotOneGenericMode() {
        val mutation=DetailedMethodologies.all.getValue(CreationMode.MUTATION)
        val au=DetailedMethodologies.all.getValue(CreationMode.ALTERNATE_UNIVERSE)
        assertTrue(mutation.rendererDirective.contains("70 percent"))
        assertFalse(au.rendererDirective.contains("70 percent"))
        assertTrue(DetailedMethodologies.validate(MethodologyRequest(
            CreationMode.CREATURE,conceptIds=listOf("SC016"))).isEmpty())
    }
}
