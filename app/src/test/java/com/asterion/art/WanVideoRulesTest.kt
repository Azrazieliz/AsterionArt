package com.asterion.art

import com.asterion.art.video.WanVideoRules
import com.asterion.art.video.WanVideoModelPack
import org.junit.Assert.*
import org.junit.Test

class WanVideoRulesTest {
    @Test fun framesAlwaysUseGenuineWanTemporalCounts() {
        (1..5).forEach{seconds->
            val frames=WanVideoRules.frameCount(seconds)
            assertTrue(frames in 5..29)
            assertEquals(1,frames%4)
            assertTrue(WanVideoRules.outputFps(seconds) in 6..24)
        }
    }
    @Test fun distinctSourceImagesShareTheFirstImageCanvas() {
        val portrait=WanVideoRules.canvas(768,1365)
        assertEquals(0,portrait.first%64)
        assertEquals(0,portrait.second%64)
        assertTrue(portrait.first in 256..960)
        assertTrue(portrait.second in 256..960)
        assertTrue(WanVideoRules.canvas(1500,1500).first==512)
    }
    @Test fun runtimeNeedsEveryMatchingVideoComponent() {
        assertEquals(3,WanVideoModelPack.components.size)
        assertEquals(3,WanVideoModelPack.components.map{it.file}.distinct().size)
        assertTrue(WanVideoModelPack.components.all{
            it.repo.startsWith("off-grid-ai/") && it.file.isNotBlank()
        })
    }
}
