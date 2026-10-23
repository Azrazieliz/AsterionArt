package com.asterion.art.engine

import com.asterion.art.model.GeneratorSettings
import com.asterion.art.model.RenderMode
import com.asterion.art.model.SheetRole

/** Source text is supplied by the exact selected sheets, never invented by this compiler. */
data class SourceEvidence(
    val sheetId: String,
    val role: SheetRole,
    val byPanel: Map<String, List<String>>,
) {
    init { require(sheetId.isNotBlank()) }
    fun select(vararg panels: String): List<String> =
        panels.flatMap { panel -> byPanel[panel].orEmpty() }
    fun all(): List<String> = byPanel.values.flatten()
}
data class GenerationPass(
    val number: Int,
    val positive: String,
    val negative: String,
    val denoise: Float,
)
data class GenerationRecipe(
    val mode: RenderMode,
    val passes: List<GenerationPass>,
    val evidenceIds: List<String>,
    val settings: GeneratorSettings,
)

object GenerationPlanner {
    private val alphaNum = Regex(".*[\\p{L}\\p{N}].*")
    private fun clean(words: List<String>): List<String> =
        words.map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.length in 3..280 && alphaNum.matches(it) }
            .distinct()

    private fun join(words: List<String>) = clean(words).joinToString(", ")

    fun compile(
        sources: List<SourceEvidence>,
        mode: RenderMode,
        settings: GeneratorSettings,
    ): GenerationRecipe {
        require(sources.any { it.role == SheetRole.CHARACTER }) { "Character sheet required" }
        require(sources.any { it.role == SheetRole.ART_STYLE }) { "Art style sheet required" }
        require(sources.any { it.role == SheetRole.THEME }) { "Theme sheet required" }
        require(sources.all { it.all().isNotEmpty() }) {
            "At least one selected sheet has no machine-readable visual specification. " +
            "Do not invent missing details."
        }
        val characters = sources.filter { it.role == SheetRole.CHARACTER }
        val themes = sources.filter { it.role == SheetRole.THEME }
        val styles = sources.filter { it.role == SheetRole.ART_STYLE }
        val elements = sources.filter { it.role == SheetRole.ELEMENT }

        fun pieces(items: List<SourceEvidence>, vararg slots: String): List<String> =
            items.flatMap { it.select(*slots) }
        fun everything(items: List<SourceEvidence>): List<String> = items.flatMap { it.all() }

        // Visual metadata in the authoritative order:
        // identity, outfit+pose, theme, environment+elements, style+render+camera+lighting.
        val character = join(pieces(characters, "PRIMARY", "FACE", "EYES", "HAIR"))
        val outfitAndPose = join(
            pieces(characters, "OUTFIT", "BODY", "DISTINCTIVE") +
                pieces(themes, "POSE")
        )
        val theme = join(pieces(themes, "PRIMARY", "VIEW"))
        val environmentAndElements = join(
            pieces(themes, "ENVIRONMENT", "ELEMENT") +
                everything(elements)
        )
        val appearance = join(
            pieces(styles, "PRIMARY", "FINGERPRINT", "SHAPE", "COMPOSITION", "LIGHT", "COLOR")
        )
        val core = listOf(character, outfitAndPose, theme, environmentAndElements, appearance)
            .filter { it.isNotBlank() }.joinToString("\n\n")
        require(core.isNotBlank()) { "No interpretable prompt content in selected reference images." }

        val positiveDetails = listOf(
            pieces(characters, "FACE", "EYES", "HAIR", "BODY", "OUTFIT"),
            pieces(styles, "MATERIAL", "LIGHT", "COLOR") +
                pieces(themes, "LIGHT", "ENVIRONMENT"),
            pieces(characters, "DISTINCTIVE", "DIAGNOSTIC") +
                pieces(elements, "STRUCTURE", "PLACEMENT", "VARIATIONS", "MATERIAL", "COLOR"),
            pieces(styles, "FINGERPRINT", "COMPOSITION") +
                pieces(themes, "VIEW")
        )
        val negative = join(
            pieces(styles, "NEGATIVE") +
                pieces(themes, "NEGATIVE") +
                pieces(elements, "NEGATIVE")
        )

        val denoise = listOf(1f, 0.48f, 0.32f, 0.22f, 0.13f)
        val passes = (1..5).map { n ->
            val extra = if (n == 1) emptyList() else positiveDetails[n - 2]
            val full = if (extra.isEmpty()) core else core + "\n\n" + join(extra)
            GenerationPass(n, full, negative, denoise[n - 1])
        }
        return GenerationRecipe(mode, passes, sources.map { it.sheetId }, settings)
    }

    fun selectModelId(models: List<Pair<String, String>>, mode: RenderMode): String? {
        // One automatic route shown to the user; the ranking operates internally.
        // This is NOT a claim of checkpoint weight fusion.
        val rank = when (mode) {
            RenderMode.ANIME, RenderMode.ANIMATION ->
                listOf("rin_flanime", "miaomiao", "hassaku", "noobai")
            RenderMode.REALISTIC, RenderMode.COSPLAY ->
                listOf("cyberrealistic", "realism", "sam_anima_realistic", "rin_flanime")
        }
        return rank.firstNotNullOfOrNull { needle ->
            models.firstOrNull { (id, name) ->
                id.contains(needle, true) || name.contains(needle, true)
            }?.first
        }
    }
}
