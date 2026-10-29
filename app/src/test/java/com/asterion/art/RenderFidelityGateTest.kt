package com.asterion.art

import com.asterion.art.engine.RenderFidelityGate
import com.asterion.art.engine.TechnicalScore
import org.junit.Assert.*
import org.junit.Test

class RenderFidelityGateTest {
    @Test fun rejectsUniformBlurryFields() {
        assertTrue(RenderFidelityGate.isDegenerate(
            TechnicalScore(0.3,0.010,0.002,0.0)))
    }
    @Test fun keepsTexturedIllustrations() {
        assertFalse(RenderFidelityGate.isDegenerate(
            TechnicalScore(50.0,0.42,0.33,0.0)))
    }
    @Test fun neverCertifiesNonfiniteScores() {
        assertTrue(RenderFidelityGate.isDegenerate(
            TechnicalScore(Double.NaN,0.5,0.5,0.0)))
    }
}
