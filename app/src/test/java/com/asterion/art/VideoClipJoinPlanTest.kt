package com.asterion.art

import com.asterion.art.video.VideoClipJoinPlan
import org.junit.Assert.*
import org.junit.Test

class VideoClipJoinPlanTest {
    private val id="join_"+("a".repeat(32))
    @Test fun roundTripPreservesAllInputVideosInOriginalOrder() {
        val clips=listOf("content://media/external/video/media/1",
            "content://media/external/video/media/7",
            "content://media/external/video/media/12")
        val saved=VideoClipJoinPlan(id,clips)
        assertEquals(saved,VideoClipJoinPlan.fromJson(saved.json()))
    }
    @Test fun rejectsDuplicateInputsAndNonMediaUris() {
        val first="content://media/external/video/media/2"
        assertTrue(runCatching{VideoClipJoinPlan(id,listOf(first,first))}.isFailure)
        assertTrue(runCatching{VideoClipJoinPlan(id,listOf(first,"file:///hidden.mp4"))}.isFailure)
    }
    @Test fun avoidsOneFilePretendingToBeJoinedVideo() {
        assertTrue(runCatching{VideoClipJoinPlan(id,
            listOf("content://media/external/video/media/1"))}.isFailure)
    }
}
