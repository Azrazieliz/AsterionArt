package com.asterion.art

import com.asterion.art.engine.TokenCapacity
import com.asterion.art.render.OneShotSheetConditioning
import org.junit.Assert.*
import org.junit.Test

class OneShotSheetConditioningTest {
    private fun capacity(n:Int):(String)->TokenCapacity={value->
        TokenCapacity(value.length,n,if(value.length>n)n else -1)
    }
    @Test fun keepsWholeCanonicalSheetWhenNeuralPlanWouldOverflow() {
        assertEquals("front and rear",OneShotSheetConditioning.select(
            "front and rear","very long optional analysis","negative",capacity(25)))
    }
    @Test fun addsNeuralPlanOnlyWhenItsCombinedPromptFits() {
        val value=OneShotSheetConditioning.select(
            "front and rear","hair identity","negative",capacity(120))
        assertTrue(value.contains("front and rear"))
        assertTrue(value.contains("hair identity"))
    }
    @Test fun rejectsCanonicalTruncationInsteadOfStitchingPanels() {
        assertTrue(runCatching{OneShotSheetConditioning.select(
            "front rear side","", "",capacity(5))}.isFailure)
    }
    @Test fun requiresNegativePromptCapacityToo() {
        assertTrue(runCatching{OneShotSheetConditioning.select(
            "front","", "mandatory negative",capacity(7))}.isFailure)
    }
}
