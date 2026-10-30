package com.asterion.art

import com.asterion.art.video.VideoPlanStore
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoMasterDurationTest {
    @Test fun acceptsTenTwentyThirtySeconds() {
        assertEquals(10,VideoPlanStore.requireDuration(5,2))
        assertEquals(20,VideoPlanStore.requireDuration(5,4))
        assertEquals(30,VideoPlanStore.requireDuration(5,6))
    }

    @Test fun rejectsPreviouslyPermittedTwoSecondVideo() {
        try {
            VideoPlanStore.requireDuration(2,1)
            throw AssertionError("Short sample was treated as completed video")
        } catch(expected:IllegalArgumentException) {
            assert(expected.message.orEmpty().contains("10–30"))
        }
    }

    @Test fun rejectsPreviouslyPermittedFortySecondVideo() {
        try {
            VideoPlanStore.requireDuration(5,8)
            throw AssertionError("Excessively long output was permitted")
        } catch(expected:IllegalArgumentException) {
            assert(expected.message.orEmpty().contains("40 seconds"))
        }
    }
}
