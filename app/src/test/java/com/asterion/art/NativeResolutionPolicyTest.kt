package com.asterion.art

import com.asterion.art.nativeengine.NativeResolutionPolicy
import com.asterion.art.engine.DreamModel
import org.junit.Assert.*
import org.junit.Test

class NativeResolutionPolicyTest {
    private fun native(resolutions:List<Pair<Int,Int>> = emptyList())=
        DreamModel("installed","Anima checkpoint",1024,true,false,true,"",resolutions)
    @Test fun everyUserRatioWorksWithoutNativeSizeManifest() {
        val selected=listOf("1:1","3:4","4:3","9:16","2:3","3:2","16:9")
        val expectations=listOf(1.0,.75,4.0/3.0,9.0/16.0,
            2.0/3.0,1.5,16.0/9.0)
        selected.zip(expectations).forEach{(ratio,expected)->
            val (w,h)=NativeResolutionPolicy.choose(native(),ratio)
            assertTrue(w in 256..2048 && h in 256..2048)
            assertTrue("Wrong aspect for "+ratio,
                kotlin.math.abs(w.toDouble()/h-expected)<.025)
            assertEquals(0,w%32);assertEquals(0,h%32)
        }
    }
    @Test fun characterIndividualTriptychRemainsTallAndPortrait() {
        val (w,h)=NativeResolutionPolicy.choose(native(),"16:45")
        assertTrue(h>w*2)
    }
    @Test fun actualModelResolutionsTakePriorityOverGuesses() {
        val options=listOf(1024 to 1024,576 to 1024)
        assertEquals(576 to 1024,NativeResolutionPolicy.choose(native(options),"9:16"))
    }
    @Test(expected=IllegalStateException::class)
    fun refusesUnsupportedRatio() {
        NativeResolutionPolicy.choose(native(),"panorama")
    }
}
