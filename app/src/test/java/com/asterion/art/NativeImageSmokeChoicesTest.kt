package com.asterion.art

import com.asterion.art.engine.DreamModel
import com.asterion.art.work.NativeImageSmokeChoices
import org.junit.Assert.assertEquals
import org.junit.Test

class NativeImageSmokeChoicesTest {
    private fun model(base:Int,res:List<Pair<Int,Int>> = emptyList())=DreamModel(
        "installed","User checkpoint",base,true,false,true,"",res)
    @Test fun respectsInstalledNativeModelGeometry() {
        assertEquals(512 to 768,NativeImageSmokeChoices.dimensions(model(1024,
            listOf(1024 to 1024,512 to 768))))
    }
    @Test fun usesNativeNominalResolutionWithoutFakingSupport() {
        assertEquals(1024 to 1024,NativeImageSmokeChoices.dimensions(model(1024)))
    }
    @Test fun rejectsUnusableAdvertisedResolutions() {
        assertEquals(512 to 512,NativeImageSmokeChoices.dimensions(model(512,
            listOf(32 to 32,4096 to 4096))))
    }
}
