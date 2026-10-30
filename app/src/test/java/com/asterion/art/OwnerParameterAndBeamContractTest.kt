package com.asterion.art

import com.asterion.art.engine.ArtworkBeamPolicy
import com.asterion.art.engine.CreativeIntensity
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/** Regression tests for the owner's corrected, non-branching Artwork contract. */
class OwnerParameterAndBeamContractTest {
    @Test fun intensityIsContinuousAndNotCappedAtTen() {
        assertEquals("off",CreativeIntensity.label(BigDecimal.ZERO))
        assertEquals("prominent",CreativeIntensity.label(BigDecimal.ONE))
        assertEquals("above prominent; no intensity ceiling",
            CreativeIntensity.label(CreativeIntensity.parse("1000000000000000000")))
        CreativeIntensity.validate(mapOf(
            "B" to "1","E.user_defined" to "12345.67",
            "new_independent_axis" to "7.75"))
        assertEquals(listOf("B","E","T"),CreativeIntensity.axes.map{it.key})
        assertTrue(CreativeIntensity.description(mapOf("B" to "1"))
            .contains("no defined maximum"))
    }

    @Test fun onlySingleBestArtworkImageSurvivesEachPhase() {
        assertEquals(1,ArtworkBeamPolicy.survivors)
        assertEquals(3,ArtworkBeamPolicy.schedule(0,1,3))
        for(phase in 2..5)
            assertEquals(3,ArtworkBeamPolicy.schedule(1,phase,3))
        assertEquals(15,(1..5).sumOf{
            ArtworkBeamPolicy.schedule(if(it==1)0 else 1,it,3)
        })
        assertEquals(25,(1..5).sumOf{
            ArtworkBeamPolicy.schedule(if(it==1)0 else 1,it,5)
        })
        val scores=listOf(9.0,1.0,7.0,8.0,4.0,3.0)
        assertEquals(listOf(9.0),ArtworkBeamPolicy.top(scores){it})
    }
}
