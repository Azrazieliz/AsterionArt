package com.asterion.art

import com.asterion.art.engine.OriginalOutputPolicy
import com.asterion.art.model.CreationMode
import org.junit.Assert.*
import org.junit.Test

class OriginalOutputPolicyTest {
    @Test fun keepsOneCataloguePerOriginal() {
        CreationMode.entries.filter{it!=CreationMode.CHARACTER}.forEach{
            assertEquals(it.name,1,OriginalOutputPolicy.imageCount(it,false,null,8))
        }
    }
    @Test fun characterPhaseOneRespectsChosenSheetCount() {
        assertEquals(30,OriginalOutputPolicy.imageCount(
            CreationMode.CHARACTER,false,30,3))
        assertEquals(1,OriginalOutputPolicy.imageCount(
            CreationMode.CHARACTER,false,1,3))
        assertTrue(runCatching{OriginalOutputPolicy.imageCount(
            CreationMode.CHARACTER,false,null,3)}.isFailure)
    }
    @Test fun artworkRetainsBatchWhileSelectedDesignGetsOneImage() {
        assertEquals(5,OriginalOutputPolicy.imageCount(null,false,null,5))
        assertEquals(1,OriginalOutputPolicy.imageCount(
            CreationMode.OUTFIT,true,null,8))
    }
}
