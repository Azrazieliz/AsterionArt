package com.asterion.art

import com.asterion.art.engine.DreamModel
import com.asterion.art.model.SheetRole
import com.asterion.art.nativeengine.SheetModelSelection
import org.junit.Assert.*
import org.junit.Test

class SheetModelSelectionTest {
    private fun model(id:String,kind:String=""):DreamModel =
        DreamModel(id,id,1024,kind=="anima",kind=="sdxl",true,
            if(kind in setOf("qwen21","klein"))kind else "",emptyList())

    @Test fun choosesReferenceAwareNativeModelWhenAvailable() {
        val anima=model("anima","anima")
        val qwen=model("qwen","qwen21")
        assertEquals(qwen,SheetModelSelection.select(SheetRole.CHARACTER,
            listOf(anima,qwen),null,anima,1))
        assertEquals(qwen,SheetModelSelection.select(SheetRole.THEME,
            listOf(anima,qwen),null,anima,2))
    }

    @Test fun sheetSpecificOwnerAssignmentOverridesDefaultModelSelection() {
        val qwen=model("qwen","qwen21")
        val klein=model("klein","klein")
        assertEquals(klein,SheetModelSelection.select(SheetRole.CHARACTER,
            listOf(qwen,klein),klein,qwen,3))
    }

    @Test fun neverDiscardsReferencesToUseSingleImageModel() {
        val anima=model("anima","anima")
        assertThrows(IllegalArgumentException::class.java){
            SheetModelSelection.select(SheetRole.ART_STYLE,
                listOf(anima),null,anima,2)
        }
        assertThrows(IllegalArgumentException::class.java){
            SheetModelSelection.select(SheetRole.CHARACTER,
                listOf(anima),null,anima,0)
        }
        val qwen=model("qwen","qwen21")
        assertThrows(IllegalArgumentException::class.java){
            SheetModelSelection.select(SheetRole.CHARACTER,
                listOf(anima,qwen),anima,qwen,2)
        }
    }

    @Test fun remainsCompatibleWithOneOriginalImg2imgSource() {
        val anima=model("anima","anima")
        assertEquals(anima,SheetModelSelection.select(SheetRole.ELEMENT,
            listOf(anima),null,anima,1))
    }
}
