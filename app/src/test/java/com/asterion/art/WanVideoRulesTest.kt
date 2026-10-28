package com.asterion.art

import com.asterion.art.video.WanVideoRules
import com.asterion.art.video.WanVideoModelPack
import org.junit.Assert.*
import org.junit.Test

class WanVideoRulesTest {
    @Test fun framesAlwaysUseGenuineWanTemporalCounts() {
        (1..5).forEach{seconds->
            val frames=WanVideoRules.frameCount(seconds)
            assertEquals(seconds*24+1,frames)
            assertEquals(1,frames%4)
            assertEquals(24,WanVideoRules.outputFps(seconds))
        }
    }
    @Test fun distinctSourceImagesShareTheFirstImageCanvas() {
        val portrait=WanVideoRules.canvas(768,1365)
        assertEquals(704 to 1280,portrait)
        assertEquals(1280 to 704,WanVideoRules.canvas(1500,1500))
        assertEquals(1280 to 704,WanVideoRules.canvas(1500,720))
    }
    @Test fun runtimeNeedsEveryMatchingVideoComponent() {
        assertEquals(3,WanVideoModelPack.components.size)
        assertEquals(3,WanVideoModelPack.components.map{it.file}.distinct().size)
        assertTrue(WanVideoModelPack.components.all{
            it.repo.startsWith("off-grid-ai/") && it.file.isNotBlank()
        })
    }
}
