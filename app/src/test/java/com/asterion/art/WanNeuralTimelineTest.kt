package com.asterion.art

import com.asterion.art.video.WanNeuralTimeline
import org.junit.Assert.*
import org.junit.Test

class WanNeuralTimelineTest {
    @Test fun oneFiveSecondAnimationIsFiveGenuineOneSecondClips() {
        val list=WanNeuralTimeline.plan(1,5,1,42L)
        assertEquals(5,list.size)
        assertTrue(list.all{it.expectedSeconds==1})
        assertFalse(list.first().usePreviousEndFrame)
        assertTrue(list.drop(1).all{it.usePreviousEndFrame})
        assertEquals(42L,list.first().seed)
    }
    @Test fun multipleSourceImagesEachStartTheirOwnChain() {
        val frames=WanNeuralTimeline.plan(3,2,3,17L)
        assertEquals(6,frames.size)
        assertEquals(listOf(0,0,1,1,2,2),frames.map{it.sourceIndex})
        assertEquals(listOf(false,true,false,true,false,true),
            frames.map{it.usePreviousEndFrame})
    }
    @Test fun rejectsAmbiguousPartialMultiImageChains() {
        assertTrue(runCatching{WanNeuralTimeline.plan(3,2,2,11)}.isFailure)
        assertTrue(runCatching{WanNeuralTimeline.plan(1,0,2,11)}.isFailure)
    }
}
