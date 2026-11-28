package com.asterion.art.creation

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterTaxonomyTest {
    private val taxonomy = CharacterTaxonomy.load()

    @Test
    fun canonicalRosterKeepsOnePathPerLeaf() {
        val active = taxonomy.roots.flatMap(taxonomy::descendants)
        assertEquals(taxonomy.leaves.keys, active.toSet())
        assertEquals(active.size, active.distinct().size)
        assertEquals("Orchid", taxonomy.leaves.getValue("SC086").name)
        assertEquals("Moth", taxonomy.leaves.getValue("SC019").name)
        assertTrue(taxonomy.leaves.getValue("SC086").path.first().startsWith("BC"))
        assertTrue(taxonomy.leaves.getValue("SC019").path.last().startsWith("SF"))
    }

    @Test
    fun constrainedAngelUsesBranchAndOrdinaryAngelUsesExactLeaf() {
        val ordinary = resolve("Angel")
        val constrained = resolve("X{angel}")
        assertEquals("SC002", ordinary.id)
        assertEquals(SelectionMethod.FIXED_CANONICAL, ordinary.method)
        assertEquals(SelectionMethod.UNIFORM_LEAF, constrained.method)
        assertEquals(taxonomy.descendants("SF002").toSet(), constrained.eligiblePool.toSet())
    }

    @Test
    fun randomSelectionKeepsEligiblePoolAndDoesNotReplaceOpenLiterals() {
        val eligible = setOf("SC019", "SC086")
        assertEquals(eligible, resolve("x", eligible).eligiblePool.toSet())
        assertEquals(SelectionMethod.FIXED_LITERAL, resolve("Quilled shimmer glass").method)
        assertTrue(taxonomy.select("x{unmatched constraint}", Random(22)) is ConceptSelection.Clarify)
        assertTrue(taxonomy.select("X{orchid}", Random(22), setOf("SC019")) is ConceptSelection.Clarify)
    }

    @Test
    fun retiredAliasesPreserveFormerScope() {
        val retired = taxonomy.retired.entries.first { it.value.targets.isNotEmpty() }
        assertEquals(retired.value.targets.toSet(), taxonomy.descendants(retired.key).toSet())
        val selection = resolve("X{${retired.key}}")
        assertEquals(retired.value.targets.toSet(), selection.eligiblePool.toSet())
    }

    private fun resolve(slot: String, eligible: Set<String>? = null) =
        (taxonomy.select(slot, Random(14), eligible) as ConceptSelection.Frozen).value
}
