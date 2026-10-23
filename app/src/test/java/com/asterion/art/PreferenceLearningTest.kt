package com.asterion.art

import com.asterion.art.learning.FeedbackExample
import com.asterion.art.learning.PreferenceLearning
import com.asterion.art.model.GeneratorSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceLearningTest {
    @Test fun sparseFeedbackDoesNotPretendLearning() {
        assertNull(PreferenceLearning.learn(GeneratorSettings(),listOf(
            FeedbackExample(5,4f,20,.4f))))
    }
    @Test fun userFeedbackMovesParametersWithinSafetyBounds() {
        val cases = listOf(
            FeedbackExample(5,6f,35,.5f),
            FeedbackExample(4,6f,35,.5f),
            FeedbackExample(5,5.8f,32,.48f),
            FeedbackExample(4,5.9f,34,.49f),
            FeedbackExample(1,3.5f,14,.2f),
            FeedbackExample(2,3.7f,15,.2f),
        )
        val base = GeneratorSettings(cfg=4f,steps=20,denoise=.4f)
        val result = requireNotNull(PreferenceLearning.learn(base,cases))
        assertEquals(6,result.evidenceCount)
        assertTrue(result.settings.cfg in 4f..4.5f)
        assertTrue(result.settings.steps in 20..24)
        assertTrue(result.settings.denoise >= .4f && result.settings.denoise <= .451f)
    }
    @Test fun parameterVariationIsDeterministicAndBounded() {
        val a = GeneratorSettings(cfg=2f,steps=10,denoise=.5f)
        val x = PreferenceLearning.candidate(a,0)
        assertEquals(x,PreferenceLearning.candidate(a,0))
        assertTrue(x.steps >= 5)
        assertEquals(a.copy(automatic=false),PreferenceLearning.candidate(a.copy(automatic=false),1))
    }
}
