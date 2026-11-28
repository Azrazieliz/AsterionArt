package com.asterion.art.creation

import java.util.Locale
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TaxonomyNode(
    val name: String,
    val description: String,
    val leaves: List<String>,
    val children: List<String>,
)

@Serializable
data class ConceptLeaf(val name: String, val path: List<String>)

@Serializable
data class TaxonomyView(val name: String, val targets: List<String>)

@Serializable
data class SelectorAlias(val terms: List<String>, val targets: List<String>, val kind: String)

@Serializable
data class SelectorAmbiguity(val terms: List<String>, val candidates: List<String>)

@Serializable
data class SelectorFacet(val terms: List<String>, val targets: List<String>)

@Serializable
enum class SelectionMethod { FIXED_CANONICAL, FIXED_LITERAL, UNIFORM_LEAF }

@Serializable
data class FrozenConcept(
    val slot: String,
    val method: SelectionMethod,
    val id: String?,
    val name: String,
    val eligiblePool: List<String> = emptyList(),
    val eligibleConstraint: List<String>? = null,
)

sealed interface ConceptSelection {
    data class Frozen(val value: FrozenConcept) : ConceptSelection
    data class Clarify(val query: String, val candidates: List<String> = emptyList()) : ConceptSelection
}

@Serializable
@ConsistentCopyVisibility
data class CharacterTaxonomy internal constructor(
    val version: String,
    val sourceSha256: String,
    val roots: List<String>,
    val nodes: Map<String, TaxonomyNode>,
    val leaves: Map<String, ConceptLeaf>,
    val aliases: List<SelectorAlias>,
    val domains: Map<String, TaxonomyView>,
    val retired: Map<String, TaxonomyView>,
    val ambiguities: List<SelectorAmbiguity>,
    val facets: List<SelectorFacet>,
    val joinWords: List<String>,
) {
    fun pool(targets: Iterable<String>): Set<String> = targets.flatMap(::descendants).toSet()

    fun descendants(target: String): List<String> {
        leaves[target]?.let { return listOf(target) }
        nodes[target]?.let { return it.leaves + it.children.flatMap(::descendants) }
        domains[target]?.let { return pool(it.targets).sorted() }
        retired[target]?.let { return pool(it.targets).sorted() }
        return emptyList()
    }

    fun searchLeaves(query: String): List<String> {
        val key = query.normalized()
        val direct = leaves.filter { (id, entry) -> key in id.normalized() || key in entry.name.normalized() }
        val synonyms = aliases.filter { alias ->
            alias.kind == "leaf" && alias.terms.any { key in it.normalized() }
        }
        return (direct.keys + synonyms.flatMap { it.targets }).distinct().sortedBy { leaves.getValue(it).name }
    }

    fun select(
        slot: String,
        random: Random,
        eligible: Set<String>? = null,
    ): ConceptSelection {
        val input = slot.trim()
        require(input.isNotEmpty()) { "Specify a concept or selector" }
        val key = input.normalized()
        if (key == "x?" || key == "show branches") return ConceptSelection.Clarify(input, roots)
        if (key == "x") return draw(input, leaves.keys, random, eligible)

        val constrained = WILDCARD.matchEntire(input)
        val query = constrained?.groupValues?.get(1)?.normalized() ?: key
        if (constrained == null && key.startsWith("x{")) return ConceptSelection.Clarify(input)
        val fixed = fixedMatches(query)
        if (constrained == null) {
            if (fixed.size == 1) {
                val id = fixed.single()
                return ConceptSelection.Frozen(
                    FrozenConcept(input, SelectionMethod.FIXED_CANONICAL, id, leaves.getValue(id).name),
                )
            }
            if (fixed.size > 1) return ConceptSelection.Clarify(input, fixed)
        }

        ambiguities.firstOrNull { query in it.terms.map(String::normalized) }?.let {
            return ConceptSelection.Clarify(input, it.candidates)
        }
        val exactNodes = nodes.filter { (id, node) -> query == id.normalized() || query == node.name.normalized() }
        if (exactNodes.isNotEmpty()) return selectPool(input, exactNodes.keys.map(::listOf), random, eligible)

        val equivalentNodes = nodes.filter { (_, node) -> pluralEquivalent(query, node.name.normalized()) }
        val views = (domains + retired).filter { (id, view) ->
            query == id.normalized() || query == view.name.normalized()
        }
        val aliasesForQuery = aliases.filter { alias ->
            alias.kind == "view" && alias.terms.any { term ->
                query == term.normalized() || pluralEquivalent(query, term.normalized())
            }
        }
        val branchMatches = views.values.map { it.targets } +
            aliasesForQuery.map { it.targets } + equivalentNodes.keys.map(::listOf)
        if (branchMatches.isNotEmpty()) return selectPool(input, branchMatches, random, eligible)

        composableFacetPool(query)?.let { return draw(input, it, random, eligible) }
        if (constrained != null) {
            if (fixed.size == 1) return draw(input, fixed.toSet(), random, eligible)
            return ConceptSelection.Clarify(input, fixed)
        }
        return ConceptSelection.Frozen(FrozenConcept(input, SelectionMethod.FIXED_LITERAL, null, input))
    }

    private fun fixedMatches(query: String): List<String> {
        val direct = leaves.filter { (id, leaf) -> query == id.normalized() || query == leaf.name.normalized() }
        if (direct.isNotEmpty()) return direct.keys.toList()
        val synonyms = aliases.filter { alias ->
            alias.kind == "leaf" && alias.terms.any { term ->
                query == term.normalized() || pluralEquivalent(query, term.normalized())
            }
        }.flatMap { it.targets }
        if (synonyms.isNotEmpty()) return synonyms.distinct()
        return leaves.filterValues { pluralEquivalent(query, it.name.normalized()) }.keys.toList()
    }

    private fun selectPool(
        slot: String,
        matches: List<List<String>>,
        random: Random,
        eligible: Set<String>?,
    ): ConceptSelection {
        val possibilities = matches.map(::pool).distinct()
        if (possibilities.size != 1) return ConceptSelection.Clarify(slot, matches.flatten().distinct())
        return draw(slot, possibilities.single(), random, eligible)
    }

    private fun draw(
        slot: String,
        pool: Set<String>,
        random: Random,
        eligible: Set<String>?,
    ): ConceptSelection {
        val valid = (eligible?.let(pool::intersect) ?: pool).sorted()
        if (valid.isEmpty()) return ConceptSelection.Clarify(slot)
        val selected = valid[random.nextInt(valid.size)]
        return ConceptSelection.Frozen(
            FrozenConcept(
                slot, SelectionMethod.UNIFORM_LEAF, selected, leaves.getValue(selected).name,
                valid, eligible?.sorted(),
            ),
        )
    }

    private fun composableFacetPool(query: String): Set<String>? {
        val words = query.split(' ')
        val tokens = facets.flatMap { facet ->
            facet.terms.map { it.normalized().split(' ') to facet.targets }
        }.sortedByDescending { it.first.size }
        var index = 0
        var accumulated: Set<String>? = null
        while (index < words.size) {
            val match = tokens.firstOrNull { (term, _) -> words.drop(index).take(term.size) == term }
            if (match != null) {
                val targets = pool(match.second)
                accumulated = accumulated?.intersect(targets) ?: targets
                index += match.first.size
            } else if (words[index] in joinWords) {
                index++
            } else {
                return null
            }
        }
        return accumulated
    }

    companion object {
        private val WILDCARD = Regex("x\\{([^{}]+)}", RegexOption.IGNORE_CASE)

        fun load(): CharacterTaxonomy = checkNotNull(
            CharacterTaxonomy::class.java.getResourceAsStream("/character-taxonomy.json"),
        ).bufferedReader().use { Json.decodeFromString(it.readText()) }
    }
}

private fun String.normalized(): String = trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

private fun pluralEquivalent(a: String, b: String): Boolean {
    fun plural(word: String): String = when {
        word.endsWith("y") && word.length > 1 && word[word.length - 2] !in "aeiou" ->
            word.dropLast(1) + "ies"
        word.endsWith("s") || word.endsWith("x") || word.endsWith("ch") || word.endsWith("sh") ->
            word + "es"
        else -> word + "s"
    }
    return plural(a) == b || plural(b) == a
}
