package com.asterion.art

import com.asterion.art.video.VideoTimelineMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTimelineMathTest {
    @Test fun timestampsNormalizeNonZeroSourceOrigins() {
        assertEquals(3_000_000L,
            VideoTimelineMath.presentationTime(850_000,850_000,3_000_000,-1))
        assertEquals(3_040_000L,
            VideoTimelineMath.presentationTime(890_000,850_000,3_000_000,3_000_000))
    }
    @Test fun subsequentSegmentsCannotOverlapPreviousTracks() {
        val firstEnd=VideoTimelineMath.presentationTime(130_000,0,0,100_000)
        val next=VideoTimelineMath.nextStart(0,firstEnd,40_000)
        assertTrue(next>firstEnd)
        assertEquals(next,VideoTimelineMath.presentationTime(200_000,200_000,next,firstEnd))
    }
    @Test fun duplicateEncodedTimestampsStayMonotonic() {
        assertEquals(1001L,VideoTimelineMath.presentationTime(0,0,0,1000))
    }
}
