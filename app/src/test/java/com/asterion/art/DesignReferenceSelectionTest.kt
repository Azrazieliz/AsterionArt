package com.asterion.art

import com.asterion.art.learning.DesignReferenceSelection
import com.asterion.art.learning.DesignFusionRules
import com.asterion.art.learning.SavedDesign
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignReferenceSelectionTest {
    private fun design(id:String,uri:String?,preview:String?=null,
                       mode:CreationMode?=null,parents:List<String> = emptyList(),
                       status:String="CANDIDATE")=SavedDesign(
        id=id,jobId="source-job",mode=RenderMode.ANIME,
        creationMode=mode,ordinal=1,sheetNumber=1,prompt="Identified silhouette",
        negative="",sheetUri=uri,previewUri=preview,status=status,parents=parents)

    @Test fun regenerationUsesSingleDesignCropNotWholeCatalogue() {
        val parent=design("outfit1","content://whole-sheet","file:///crop.png",
            CreationMode.OUTFIT)
        assertEquals(listOf("file:///crop.png"),
            DesignReferenceSelection.images(parent,listOf(parent)))
    }
    @Test fun multiParentFusionRetainsEverySourcePixelUriInOrder() {
        val left=design("a","file:///first.png")
        val right=design("b","file:///second.png")
        val fusion=design("fusion",null,parents=listOf("a","b"))
        assertEquals(listOf("file:///first.png","file:///second.png"),
            DesignReferenceSelection.images(fusion,listOf(left,right,fusion)))
    }
    @Test fun longParentDesignAuthorityIsNotSilentlyTruncated() {
        val source="SOURCE-CONCEPT-START "+("a".repeat(18000))+" END-FROZEN-MORPHOLOGY"
        val fused=DesignFusionRules.fuse(listOf(source,"second full concept"),"")
        assertTrue(fused.contains("END-FROZEN-MORPHOLOGY"))
        assertTrue(fused.contains(source))
    }
    @Test fun missingIndividualCatalogueCropIsNotSilentlyReplacedByWholeSheet() {
        val parent=design("missing","file:///unseparated.png",mode=CreationMode.WEAPON)
        assertTrue(runCatching {
            DesignReferenceSelection.images(parent,listOf(parent))
        }.isFailure)
    }
}
