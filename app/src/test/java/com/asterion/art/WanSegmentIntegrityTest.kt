package com.asterion.art

import com.asterion.art.video.WanSegmentIntegrity
import org.junit.Assert.*
import org.junit.Test

class WanSegmentIntegrityTest {
    @Test fun rejectsInterruptedPartialMp4InsteadOfRecoveringIt() {
        assertFalse(WanSegmentIntegrity.valid(1,0,0))
        assertFalse(WanSegmentIntegrity.valid(10,0,375000))
        assertFalse(WanSegmentIntegrity.valid(24,0,500000))
        assertFalse(WanSegmentIntegrity.valid(24,0,3000000))
    }
    @Test fun accepts24fpsOneSecondNeuralSequence() {
        assertTrue(WanSegmentIntegrity.valid(24,0,958333))
        assertTrue(WanSegmentIntegrity.valid(25,0,1000000))
    }
}
