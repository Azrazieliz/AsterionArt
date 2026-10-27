package com.asterion.art

import com.asterion.art.learning.DesignFeatureEncoder
import com.asterion.art.learning.OnlinePreferenceTraining
import com.asterion.art.learning.PreferenceModelState
import org.junit.Assert.*
import org.junit.Test

class OnlinePreferenceModelTest {
    @Test fun keepAndRejectActuallyUpdateWeights() {
        val gold="crystalline horn structures and nuanced iris with tensile wing supports"
        val bad="generic silhouette, uniform armor and disconnected decorative emblems"
        var model=PreferenceModelState()
        val beforeGold=OnlinePreferenceTraining.score(model,gold)
        val beforeBad=OnlinePreferenceTraining.score(model,bad)
        repeat(55) {
            model=OnlinePreferenceTraining.pairwise(model,gold,bad)
        }
        val afterGold=OnlinePreferenceTraining.score(model,gold)
        val afterBad=OnlinePreferenceTraining.score(model,bad)
        assertEquals(55,model.updates)
        assertTrue(afterGold>beforeGold)
        assertTrue(afterBad<beforeBad)
        assertTrue(afterGold>afterBad)
    }
    @Test fun representationIsStableAndBounded() {
        val item="Rose layered petals anatomy materials exactly three views"
        assertArrayEquals(DesignFeatureEncoder.vector(item),
            DesignFeatureEncoder.vector(item),0.0)
        assertEquals(1024,DesignFeatureEncoder.DIMENSIONS)
    }
}
