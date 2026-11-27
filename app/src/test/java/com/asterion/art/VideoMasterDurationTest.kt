package com.asterion.art

import com.asterion.art.video.VideoPlanStore
import org.junit.Assert.*
import org.junit.Test

class VideoMasterDurationTest {
    @Test fun noArbitraryTenToThirtySecondProductCeiling() {
        assertEquals(2,VideoPlanStore.requireDuration(2,1))
        assertEquals(10,VideoPlanStore.requireDuration(5,2))
        assertEquals(40,VideoPlanStore.requireDuration(5,8))
        assertEquals(600,VideoPlanStore.requireDuration(5,120))
    }
    @Test fun rejectsOnlyInvalidOrUnrepresentableDurations() {
        assertThrows(IllegalArgumentException::class.java){
            VideoPlanStore.requireDuration(0,2)
        }
        assertThrows(IllegalArgumentException::class.java){
            VideoPlanStore.requireDuration(2,0)
        }
        assertThrows(IllegalArgumentException::class.java){
            VideoPlanStore.requireDuration(5,Int.MAX_VALUE)
        }
    }
}
