package com.asterion.art

import com.asterion.art.engine.ArtworkBeamPolicy
import com.asterion.art.engine.CreativeIntensity
import com.asterion.art.engine.CreativeTaxonomyRegistry
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class OwnerParameterAndBeamContractTest {
    @Test fun oneIsAlreadyProminentAndThereIsNoIntensityCeiling() {
        assertEquals("not requested",CreativeIntensity.label(BigDecimal.ZERO))
        assertEquals("prominent",CreativeIntensity.label(BigDecimal.ONE))
        assertEquals("extreme",CreativeIntensity.label(BigDecimal(4)))
        assertEquals("unbounded escalation above level 10",
            CreativeIntensity.label(CreativeIntensity.parse("1000000000000000000")))
        CreativeIntensity.validate(mapOf(
            "B" to "1","E.explicitness" to "12345.67",
            "morphology" to "7.75","custom.owner" to "900000000000"))
        assertTrue(CreativeIntensity.description(mapOf("B" to "1"))
            .contains("already prominent"))
    }
    @Test fun independentFrozenRouletteIsReproducibleAndDistinct() {
        val vocabulary=(0 until 90).map{"atom"+it}
        val one=CreativeTaxonomyRegistry.roulette(vocabulary,4123)
        val two=CreativeTaxonomyRegistry.roulette(vocabulary,4123)
        assertEquals(one,two)
        assertEquals(5,one.size)
        assertEquals(5,one.map{it.sorted().joinToString("|")}.distinct().size)
        assertTrue(one.all{it.size in 3..6 && it.distinct().size==it.size})
    }
    @Test fun eachArtworkPhaseKeepsThreeAndLaterPhasesHaveNineCandidates() {
        assertEquals(3,ArtworkBeamPolicy.schedule(0,1))
        for(phase in 2..5)assertEquals(9,
            ArtworkBeamPolicy.schedule(3,phase))
        assertEquals(39,3+4*9)
        val inputs=listOf(9.0,1.0,7.0,8.0,4.0,3.0,0.0,5.0,6.0)
        assertEquals(listOf(9.0,8.0,7.0),
            ArtworkBeamPolicy.top(inputs){it})
    }
}