package com.asterion.art

import com.asterion.art.nativeengine.MemoryProfile
import com.asterion.art.nativeengine.NativeInferenceProfiles
import org.junit.Assert.*
import org.junit.Test

class NativeInferenceProfilesTest {
    private val gib=1024L*1024*1024
    @Test fun defaultSafeModePreservesSequentialAnima() {
        val d=NativeInferenceProfiles.decide("anima",MemoryProfile.AUTO,
            16*gib,10*gib)
        assertTrue(d.lowRam)
        assertTrue(d.sequentialDiT)
    }
    @Test fun optInFastModeNeedsActualMemoryHeadroom() {
        val fast=NativeInferenceProfiles.decide("anima",MemoryProfile.PERFORMANCE,
            16*gib,6*gib)
        assertFalse(fast.lowRam)
        assertFalse(fast.sequentialDiT)
        val fallback=NativeInferenceProfiles.decide("anima",MemoryProfile.PERFORMANCE,
            12*gib,3*gib)
        assertTrue(fallback.lowRam)
        assertTrue(fallback.sequentialDiT)
    }
    @Test fun otherBackendsKeepDedicatedNativeGraphPolicy() {
        val qwen=NativeInferenceProfiles.decide("qwen21",MemoryProfile.PERFORMANCE,
            12*gib,6*gib)
        assertFalse(qwen.lowRam)
        assertFalse(qwen.sequentialDiT)
        val sdxl=NativeInferenceProfiles.decide("sdxl",MemoryProfile.AUTO,
            12*gib,6*gib)
        assertTrue(sdxl.lowRam)
        assertFalse(sdxl.sequentialDiT)
    }
}
