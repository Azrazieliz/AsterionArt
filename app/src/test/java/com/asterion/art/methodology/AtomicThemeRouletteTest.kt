package com.asterion.art.methodology

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtomicThemeRouletteTest {
    @Test fun deterministicDistinctCompleteSets() {
        listOf(0L, 1L, 71L, 20261030L, Long.MAX_VALUE).forEach { seed ->
            val first=AtomicThemeRoulette.generate(seed)
            assertEquals(first,AtomicThemeRoulette.generate(seed))
            assertEquals(5,first.size)
            assertEquals(5,first.distinct().size)
            val all=first.flatten()
            assertEquals(all.size,all.distinct().size)
            assertTrue(first.all { it.size in 3..6 })
            assertTrue(all.all { it.matches(Regex("[a-z]+")) })
        }
    }

    @Test fun differentSeedsProduceDifferentRoulettes() {
        assertNotEquals(AtomicThemeRoulette.generate(17L),
            AtomicThemeRoulette.generate(18L))
    }
}
