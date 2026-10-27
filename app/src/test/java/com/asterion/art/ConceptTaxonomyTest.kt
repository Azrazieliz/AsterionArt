package com.asterion.art

import com.asterion.art.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ConceptTaxonomyTest {
    @Test fun fullCanonicalConceptsAreEmbeddedAndParseable() {
        val source=listOf(
            "src/main/assets/methodology/concepts_full_v3.md",
            "app/src/main/assets/methodology/concepts_full_v3.md")
            .map(::File).firstOrNull { it.isFile }
            ?: error("Full canonical taxonomy missing from APK source assets")
        val records=ConceptTaxonomy.parse(source.readText())
        assertEquals(455,records.size)
        val rose=records.values.first { it.name.equals("Rose",true) }
        assertTrue(rose.core.contains("spiral",true) || rose.core.contains("petal",true))
        assertTrue(rose.materialization.contains("body",true))
        assertTrue(rose.symbiosis.isNotBlank())
        assertTrue(rose.outfit.isNotBlank())
        assertTrue(rose.weapon.isNotBlank())
        assertTrue(rose.interpretationBoundary.isNotBlank())
        assertTrue(rose.fullRecord.length>1000)
        assertTrue(rose.physicalInstruction(CreativeDomain.WEAPON).contains("Rose"))
        assertTrue(rose.physicalInstruction(CreativeDomain.OUTFIT).contains("Rose"))
    }
    @Test fun taxonomyOverridesGenericGuessingForEveryDomain() {
        val definition=ConceptDefinition("SC001","Concept Q",
            "Distinctive central body geometry.",
            "Anatomical membrane panels.",
            "Partner concept changes membrane topology.",
            "Wearable layered construction.",
            "Coherent category-recognizable grip construction.",
            "A printed icon is not intrinsic.",
            "Full source")
        val resolver=object:ConceptResolver {
            override fun find(name:String) =
                if(name=="Concept Q") definition else null
        }
        val weapon=UniversalCreativePlanner.interpret(DesignIntent(
            CreativeDomain.WEAPON,listOf("Concept Q")),resolver)
        assertTrue(weapon.positive.contains("category-recognizable grip"))
        val outfit=UniversalCreativePlanner.interpret(DesignIntent(
            CreativeDomain.OUTFIT,listOf("Concept Q")),resolver)
        assertTrue(outfit.positive.contains("Wearable layered"))
        val creature=UniversalCreativePlanner.interpret(DesignIntent(
            CreativeDomain.CREATURE,listOf("Concept Q")),resolver)
        assertTrue(creature.positive.contains("Anatomical membrane"))
    }
}
