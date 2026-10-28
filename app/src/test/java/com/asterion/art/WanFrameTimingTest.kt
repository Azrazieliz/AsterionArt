package com.asterion.art

import com.asterion.art.video.WanFrameTiming
import org.junit.Assert.*
import org.junit.Test

class WanFrameTimingTest {
    @Test fun native25FrameSequenceExportsExactlyOneSecondAt24fps() {
        val samples=WanFrameTiming.selectFrameIndices(25,1,24)
        assertEquals(24,samples.size)
        assertEquals(0,samples.first())
        assertEquals(24,samples.last())
        assertEquals(24,samples.toSet().size)
        assertTrue(samples.zipWithNext().all{(a,b)->b>a})
    }
    @Test fun supportsFullFiveSecondsWithoutInaccurateDuration() {
        val samples=WanFrameTiming.selectFrameIndices(121,5,24)
        assertEquals(120,samples.size)
        assertEquals(0,samples.first())
        assertEquals(120,samples.last())
    }
    @Test fun doesNotDuplicateFramesToFakeLongerVideo() {
        assertTrue(runCatching{
            WanFrameTiming.selectFrameIndices(11,1,24)
        }.isFailure)
    }
}
