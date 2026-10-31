package com.asterion.art

import com.asterion.art.brain.CanonicalAnatomyCompiler
import com.asterion.art.engine.ConceptDefinition
import com.asterion.art.methodology.*
import org.junit.Assert.*
import org.junit.Test

class CanonicalAnatomyCompilerTest {
    private val available=(1..8).associate { i ->
        val id="SC"+i.toString().padStart(3,'0')
        id to ConceptDefinition(id,"Concept$i",
            "Original visible morphological system $i with characteristic silhouette and joint placement.",
            "Body-owned material growth $i along layered skeletal surfaces and axial anatomical supports.",
            "Mutual tissue-graft mechanism $i changes the support of other materials.",
            "","","Never use external floating symbols.", "Full canonical entry $i")
    }
    private fun selection(number:Int,count:Int)=CharacterSelection(number,
        (1..count).map { i ->
            PrimaryConcept("SC"+i.toString().padStart(3,'0'),
                "Concept$i","SC"+i.toString().padStart(3,'0'),"fixed_canonical")
        })

    @Test fun repeatable_seed_and_distinct_identities() {
        val first=CanonicalAnatomyCompiler.compile(selection(1,2),328L,available::get)
        val again=CanonicalAnatomyCompiler.compile(selection(1,2),328L,available::get)
        val next=CanonicalAnatomyCompiler.compile(selection(2,2),328L,available::get)
        assertEquals(first.signature(),again.signature())
        assertNotEquals(first.signature(),next.signature())
        assertTrue(first.renderDescription.length>=600)
        assertEquals(1,first.pairwise.size)
        assertTrue(first.higherOrder.isBlank())
        assertTrue(first.auditEvidence.contains("SC001"))
        assertTrue(first.renderDescription.contains("Concept1"))
        assertTrue(first.renderDescription.contains("Concept2"))
        first.validate();next.validate()
    }

    @Test fun all_pairwise_and_higher_order_for_eight_primaries() {
        val eight=CanonicalAnatomyCompiler.compile(selection(8,8),19288L,available::get)
        eight.validate()
        assertEquals(28,eight.pairwise.size)
        assertTrue(eight.higherOrder.length>=110)
        for(i in 1..8)assertTrue(eight.renderDescription.contains("Concept$i"))
    }

    @Test fun fixed_open_literals_are_never_silently_replaced() {
        val item=CharacterSelection(3,listOf(
            PrimaryConcept("Uncatalogued impulse","Uncatalogued impulse",null,"fixed_temporary_literal"),
            PrimaryConcept("SC001","Concept1","SC001","fixed_canonical")))
        val frozen=CanonicalAnatomyCompiler.compile(item,3L,available::get)
        assertEquals("Uncatalogued impulse",frozen.primary.first().name)
        assertEquals(null,frozen.primary.first().canonicalId)
        frozen.validate()
    }
}
