package com.asterion.art

import com.asterion.art.brain.CanonicalAnatomyCompiler
import com.asterion.art.engine.ConceptDefinition
import com.asterion.art.engine.TokenCapacity
import com.asterion.art.methodology.*
import org.junit.Assert.*
import org.junit.Test

class CharacterNativeConditioningTest {
    private val records=(1..8).associate { i ->
        val id="SC"+i.toString().padStart(3,'0')
        id to ConceptDefinition(id,"Concept$i",
            "Distinct actual canonical cranial and limb material for element $i.",
            "**Body-system possibilities.** A continuous $i material grows "+
                "from the ribcage to the shoulder joints and changes the limb profile. "+
                "**Materialization modes.** Different $i surface.",
            "Shared body changes for element $i.","","",
            "Never substitute external labels.", "Original source")
    }
    private fun design(count:Int):FrozenCharacter {
        val selection=CharacterSelection(1,(1..count).map { i ->
            val id="SC"+i.toString().padStart(3,'0')
            PrimaryConcept(id,"Concept$i",id,"fixed_canonical")
        })
        return CanonicalAnatomyCompiler.compile(selection,713L,records::get)
    }
    private fun limit(max:Int):(String)->TokenCapacity = {prompt->
        val count=prompt.split(Regex("\\s+")).size+2
        TokenCapacity(count,max,if(count>max)count-max else -1)
    }
    @Test fun preserves_all_primary_names_and_three_views_with_finite_context() {
        val original=design(2)
        val best=CharacterNativeConditioning.select(original,records::get,limit(77))
        assertTrue(best.counted.fits)
        assertTrue(best.prompt.contains("Concept1"))
        assertTrue(best.prompt.contains("Concept2"))
        assertTrue(best.prompt.contains("front",true))
        assertTrue(best.prompt.contains("side",true))
        assertTrue(best.prompt.contains("rear",true)||
            best.prompt.contains("back",true))
        assertFalse(best.prompt.contains("standing height"))
    }
    @Test fun refuses_to_drop_primaries_when_context_is_impossible() {
        val original=design(8)
        assertTrue(runCatching{
            CharacterNativeConditioning.select(original,records::get,limit(3))
        }.isFailure)
    }
    @Test fun detailed_variant_preferred_when_real_tokenizer_accepts_it() {
        val original=design(3)
        val best=CharacterNativeConditioning.select(original,records::get,limit(10000))
        assertEquals("canonical-detailed",best.variant)
        assertTrue(best.prompt.contains("Concept3"))
    }
}
