package com.asterion.art

import com.asterion.art.video.VideoContinuitySchedule
import org.junit.Assert.*
import org.junit.Test

class VideoContinuityScheduleTest {
    @Test fun oneKeyframeCanProduceProgressiveConditionedSegments(){
        val clips=VideoContinuitySchedule.plan(1,5,17,true,6)
        assertEquals(6,clips.size)
        assertFalse(clips[0].usePreviousEndFrame)
        assertTrue(clips.drop(1).all { it.usePreviousEndFrame })
        assertEquals(17L,clips[0].seed)
        assertEquals(1026L,clips[1].seed)
    }
    @Test fun multipleReferencesKeepIndependentAnchorAuthority(){
        val clips=VideoContinuitySchedule.plan(3,2,10,false)
        assertEquals(3,clips.size)
        assertTrue(clips.none{it.usePreviousEndFrame})
    }
    @Test fun multiAnchorContinuationMustNotDiscardSources(){
        assertTrue(runCatching {
            VideoContinuitySchedule.plan(2,5,0,true,6)
        }.isFailure)
    }
}
