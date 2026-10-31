package com.asterion.art

import com.asterion.art.methodology.*
import org.junit.Assert.*
import org.junit.Test

class DeterministicThemeCompilerTest {
    @Test fun five_unique_valid_seeded_theme_systems() {
        val plan=ThemeTextPlan(id="fixed-plan",createdAt=123,
            badassness=2,eroticity=0,tension=3,
            keywords=listOf("alicorn","crystal"),extraParameters=emptyMap(),
            characterConfiguration="one identity-neutral figure",
            liquidDirection=null,seed=88203)
        val atoms=AtomicThemeRoulette.generate(plan.seed)
        assertEquals(5,atoms.size)
        val compiled=atoms.mapIndexed { index,set ->
            DeterministicThemeCompiler.compile(plan,set,index)
        }
        assertEquals(5,compiled.map{it.name}.distinct().size)
        assertEquals(5,compiled.map{it.visualSystem}.distinct().size)
        compiled.forEachIndexed{index,theme->
            assertEquals("T"+(index+1),theme.id)
            assertTrue(theme.visualSystem.length>=400)
            assertTrue(theme.atoms.all{theme.visualSystem.contains(it,true)})
            assertTrue(theme.visualSystem.contains("alicorn"))
        }
        assertEquals(compiled.first().visualSystem,
            DeterministicThemeCompiler.compile(plan,atoms.first(),0).visualSystem)
    }
}
