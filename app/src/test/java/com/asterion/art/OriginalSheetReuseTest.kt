package com.asterion.art

import com.asterion.art.learning.OriginalSheetReuse
import com.asterion.art.learning.SavedDesign
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import com.asterion.art.model.SheetRole
import org.junit.Assert.assertEquals
import org.junit.Test

class OriginalSheetReuseTest {
    private fun design(
        original: CreationMode?,
        preview: String?="content://asterion/generated/design.png",
        sheet: String?="content://asterion/generated/catalogue.png",
        status: String="CANDIDATE"
    ) = SavedDesign(
        id="design-001", jobId="job-001",
        mode=RenderMode.ANIME, creationMode=original,
        ordinal=1, sheetNumber=1,
        prompt="canonical independent design", negative="",
        sheetUri=sheet, status=status, previewUri=preview
    )

    @Test fun feminineDesignDefaultsToCharacterSheet() {
        val value=design(CreationMode.FEMINISATION)
        assertEquals(SheetRole.CHARACTER,OriginalSheetReuse.suggestedRole(value))
        assertEquals(value.previewUri,OriginalSheetReuse.sourcePixels(value))
    }

    @Test fun weaponAndStyleOutputRetainSpecificSuggestedRoles() {
        assertEquals(SheetRole.ELEMENT,
            OriginalSheetReuse.suggestedRole(design(CreationMode.WEAPON)))
        assertEquals(SheetRole.ART_STYLE,
            OriginalSheetReuse.suggestedRole(design(CreationMode.STYLE_CORRECTION)))
    }

    @Test fun catalogueNeverBecomesIndividualDesignWithoutCrop() {
        val item=design(CreationMode.CHARACTER,preview=null)
        try {
            OriginalSheetReuse.sourcePixels(item)
            throw AssertionError("A whole catalogue was accepted as a character")
        } catch(expected:IllegalArgumentException) {
            assert(expected.message.orEmpty().contains("no individually saved image"))
        }
    }

    @Test fun completedSingleImageMayBeUsedWithoutSeparatePreview() {
        val item=design(CreationMode.FEMINISATION,preview=null)
        assertEquals(item.sheetUri,OriginalSheetReuse.sourcePixels(item))
    }

    @Test fun unrenderedFusionIsNotSilentlyConvertedToReference() {
        val item=design(null,preview=null,sheet=null,status="FUSION")
        try {
            OriginalSheetReuse.sourcePixels(item)
            throw AssertionError("A prompt-only design was treated as a generated image")
        } catch(expected:IllegalArgumentException) {
            assert(expected.message.orEmpty().contains("Render it"))
        }
    }

    @Test fun videoNeedsAnActualSavedFrame() {
        val item=design(null).copy(mode=RenderMode.ANIMATION)
        try {
            OriginalSheetReuse.sourcePixels(item)
            throw AssertionError("The video itself was accepted as a still image")
        } catch(expected:IllegalArgumentException) {
            assert(expected.message.orEmpty().contains("not a still-image"))
        }
    }
}
