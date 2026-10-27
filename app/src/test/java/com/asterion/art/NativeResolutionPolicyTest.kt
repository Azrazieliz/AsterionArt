package com.asterion.art

import com.asterion.art.nativeengine.NativeResolutionPolicy
import com.asterion.art.engine.DreamModel
import org.junit.Assert.*
import org.junit.Test

class NativeResolutionPolicyTest {
    private fun anima(resolutions:List<Pair<Int,Int>> = emptyList())=
        DreamModel("anima","Anima QNN",1024,true,false,true,"",resolutions)
    private fun dit(resolutions:List<Pair<Int,Int>> = emptyList())=
        DreamModel("dit","Dynamic DiT",1024,false,false,true,"qwen21",resolutions)

    @Test fun animaAndSdxlAlwaysLoadSquareFixedQnnCanvas() {
        for(ratio in listOf("1:1","3:4","4:3","9:16","16:9","16:45")) {
            assertEquals(1024 to 1024,NativeResolutionPolicy.choose(anima(),ratio))
            val sdxl=DreamModel("sdxl","Fixed SDXL",1024,false,true,true,"",emptyList())
            assertEquals(1024 to 1024,NativeResolutionPolicy.choose(sdxl,ratio))
        }
    }
    @Test fun dynamicDitRatiosUseRequestedNativeCanvas() {
        val selected=listOf("1:1","3:4","4:3","9:16","2:3","3:2","16:9")
        val expected=listOf(1.0,.75,4.0/3.0,9.0/16.0,2.0/3.0,1.5,16.0/9.0)
        selected.zip(expected).forEach{(ratio,target)->
            val (w,h)=NativeResolutionPolicy.choose(dit(),ratio)
            assertTrue(w in 256..2048&&h in 256..2048)
            assertTrue("Wrong dynamic aspect for "+ratio,
                kotlin.math.abs(w.toDouble()/h-target)<.025)
            assertEquals(0,w%32);assertEquals(0,h%32)
        }
    }
    @Test fun dynamicIndividualTriptychUsesTallNativeCanvas() {
        val (w,h)=NativeResolutionPolicy.choose(dit(),"16:45")
        assertTrue(h>w*2)
    }
    @Test fun actualDynamicModelResolutionsTakePriorityOverGuesses() {
        val options=listOf(1024 to 1024,576 to 1024)
        assertEquals(576 to 1024,NativeResolutionPolicy.choose(dit(options),"9:16"))
    }
    @Test(expected=IllegalStateException::class)
    fun refusesUnsupportedRatio() {
        NativeResolutionPolicy.choose(anima(),"panorama")
    }
}
