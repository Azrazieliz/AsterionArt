package com.asterion.art.methodology

import kotlin.random.Random

/**
 * Five independent atom sets from the owner-approved theme roulette.
 * No inference, user keywords, intensities, character identities or stories
 * influence atom selection. The persisted seed makes an interrupted plan
 * reproducible; the frozen result is never rerolled on retry.
 */
object AtomicThemeRoulette {
    private val categories = listOf(
        listOf("gravity", "plasma", "magnetism", "resonance", "prism",
            "vacuum", "entropy", "vortex", "fracture", "momentum",
            "inertia", "flux", "radiation", "spectrum", "orbit", "polarity"),
        listOf("coral", "fern", "glacier", "kelp", "desert", "thunder",
            "blossom", "moss", "dune", "eclipse", "aurora", "monsoon",
            "reef", "sequoia", "tide", "fossil"),
        listOf("arch", "vault", "spire", "lattice", "citadel", "atrium",
            "buttress", "bridge", "bastion", "column", "corridor", "dome",
            "stair", "terrace", "portal", "scaffold"),
        listOf("turbine", "circuit", "antenna", "gyroscope", "relay",
            "servo", "piston", "sensor", "diode", "reactor", "valve",
            "rotor", "filament", "engine", "coil", "capacitor"),
        listOf("armor", "lantern", "compass", "tapestry", "heraldry",
            "chalice", "obelisk", "monolith", "scroll", "crown", "banner",
            "mosaic", "sundial", "amphora", "relic", "fortress"),
        listOf("mirage", "labyrinth", "phantom", "echo", "paradox",
            "rune", "nebula", "dream", "chimera", "veil", "halo",
            "sigil", "rift", "apparition", "illusion", "void"),
        listOf("quartz", "bronze", "basalt", "porcelain", "mercury",
            "obsidian", "silver", "glass", "amber", "ceramic", "brass",
            "marble", "iron", "jade", "titanium", "enamel")
    )

    fun generate(seed: Long): List<List<String>> {
        val random = Random(seed)
        val used = mutableSetOf<String>()
        val sets = (0 until 5).map {
            val count = 3 + random.nextInt(4)
            val chosenCategories = categories.indices.shuffled(random).take(count)
            chosenCategories.map { category ->
                val eligible = categories[category].filterNot { word -> word in used }
                require(eligible.isNotEmpty()) {
                    "Theme roulette category exhausted; frozen atoms unchanged"
                }
                eligible.random(random).also { used.add(it) }
            }.shuffled(random)
        }
        require(sets.size == 5 &&
            sets.map { it.joinToString("|") }.distinct().size == 5 &&
            sets.all { atoms -> atoms.size in 3..6 &&
                atoms.all { it.matches(Regex("[a-z]+")) } }) {
            "The five seeded theme atom sets violate the owner roulette contract"
        }
        return sets
    }
}
