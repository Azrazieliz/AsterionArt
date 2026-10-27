package com.asterion.art

import com.asterion.art.vision.VisualObservationText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualObservationTextTest {
    @Test fun preservesLongObservationsWithoutPlannerDroppingOversizedSentences() {
        val text=(1..180).joinToString(" ") { "visible-detail-"+it }
        val parts=VisualObservationText.units(text)
        assertTrue(parts.size>2)
        assertTrue(parts.all{it.length in 3..280})
        assertEquals(text,parts.joinToString(" "))
    }
    @Test fun preservesNonAsciiAndLongRuns() {
        val text="physically visible: "+"é".repeat(600)+" distinct silhouette"
        val units=VisualObservationText.units(text)
        assertTrue(units.all{it.length<=260})
        assertEquals(text.replace(" ",""),units.joinToString("").replace(" ",""))
    }
    @Test fun blankObservationDoesNotProduceInventedEvidence() {
        assertTrue(VisualObservationText.units(" \n  ").isEmpty())
    }
}
