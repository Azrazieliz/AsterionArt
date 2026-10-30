package com.asterion.art

import com.asterion.art.engine.CanonicalSelectorGraph
import com.asterion.art.engine.ConceptTaxonomy
import com.asterion.art.methodology.CharacterPrimaryResolver
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Random

/** The owner-supplied Concepts.md, not a newly invented classification. */
class CanonicalSelectorGraphTest {
    private fun source():String=listOf(
        "src/main/assets/methodology/concepts_full_v3.md",
        "app/src/main/assets/methodology/concepts_full_v3.md")
        .map(::File).first{it.isFile}.readText()
    private fun graph()=CanonicalSelectorGraph.parse(source())

    @Test fun everySelectableLeafHasExactlyOneCanonicalPath() {
        val graph=graph()
        val definitions=ConceptTaxonomy.parse(source())
        assertEquals(18,graph.roots.size)
        assertEquals(92,graph.nodes.size)
        assertEquals(455,graph.canonicalPaths.size)
        assertEquals(definitions.keys,graph.canonicalPaths.keys)
        assertEquals(definitions.keys,graph.fullPool().leafIds.toSet())
        assertTrue(graph.canonicalPaths.values.all{path->
            path.first() in graph.roots && path.last().startsWith("SC")
        })
    }

    @Test fun branchFirstPrecedenceExcludesLiteralOrMechanismFallback() {
        val graph=graph()
        val definitions=ConceptTaxonomy.parse(source())
        val dragon=graph.resolve("Draconic",definitions)
        assertEquals(7,dragon.leafIds.size)
        assertEquals(graph.branch("SF001").leafIds,dragon.leafIds)
        val angel=graph.resolve("angel",definitions)
        assertEquals(7,angel.leafIds.size)
        assertEquals(graph.branch("SF002").leafIds,angel.leafIds)
        val fixed=definitions.values.first{it.name=="Angel"}
        assertEquals("SC002",fixed.id)
        assertTrue(angel.leafIds.contains(fixed.id))
        assertEquals("exact_leaf",
            graph.resolve("SC002",definitions).route)
    }

    @Test fun constrainedSelectorNeverPerformsSubstringFuzzyDraw() {
        val graph=graph()
        val definitions=ConceptTaxonomy.parse(source())
        try {
            graph.resolve("CompletelyNotACanonicalBranch",definitions)
            fail("An unknown constrained query must ask for clarification")
        } catch(expected:IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("Unknown"))
        }
    }

    @Test fun characterResolverUsesIndependentActualCanonicalLeafPools() {
        val graph=graph()
        val definitions=ConceptTaxonomy.parse(source())
        val choices=CharacterPrimaryResolver.resolve(
            listOf("X{Draconic}","X{Mammals}"),1,
            definitions.values.toList(),Random(31L),graph)
        assertEquals(5,choices.size)
        val dragons=graph.branch("SF001").leafIds.toSet()
        val mammals=graph.resolve("Mammals",definitions).leafIds.toSet()
        assertTrue(choices.all{it.concepts[0].canonicalId in dragons})
        assertTrue(choices.all{it.concepts[1].canonicalId in mammals})
        assertTrue(choices.all{it.concepts.all{c->c.canonicalId!=null}})
    }
}
