package com.asterion.art.engine

import com.asterion.art.model.GeneratorSettings
import com.asterion.art.model.CreationMode
import com.asterion.art.methodology.MethodologyRegistry
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

    private fun enforceRoles(sources: List<SourceEvidence>, creation: CreationMode?) {
        val selected = sources.groupingBy { it.role }.eachCount()
        val roles = if (creation == null)
            setOf(SheetRole.CHARACTER,SheetRole.ART_STYLE,SheetRole.THEME)
        else MethodologyRegistry.modes.getValue(creation).required
        val missing = roles.filter { selected.getOrDefault(it,0)<1 }
        require(missing.isEmpty()) {
            "Missing required reference authorities: "+missing.joinToString { it.display }
        }
    }
    private fun directive(creation: CreationMode?, sources: List<SourceEvidence>): String =
        if (creation == null) "" else MethodologyRegistry.compileDirective(
            creation,sources.groupingBy { it.role }.eachCount())

    fun compile(
        sources: List<SourceEvidence>,
        mode: RenderMode,
        settings: GeneratorSettings,
        creation: CreationMode? = null,
    ): GenerationRecipe {
        enforceRoles(sources,creation)
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
        val core = listOf(directive(creation,sources), character, outfitAndPose, theme,
            environmentAndElements, appearance)
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

    /**
     * Real multimodal alternative for Qwen/FLUX reference-editing engines.
     * Blank OCR does not imply blank reference images. No characteristics are
     * invented by this prompt: the native model must inspect source pixels.
     */
    fun compileVisual(
        sources: List<SourceEvidence>,
        mode: RenderMode,
        settings: GeneratorSettings,
        creation: CreationMode? = null,
    ): GenerationRecipe {
        enforceRoles(sources,creation)
        val groups=sources.groupBy { it.role }
        val roleLabels=listOf(
            SheetRole.CHARACTER to "character identity and physical form",
            SheetRole.ART_STYLE to "visual rendering style",
            SheetRole.THEME to "scene arrangement, pose and interaction",
            SheetRole.ELEMENT to "visual elements and their placement"
        ).filter { (role,_) -> groups[role].orEmpty().isNotEmpty() }
        val numbered=roleLabels.mapIndexed { i,(_,label) ->
            "Reference group "+(i+1)+" establishes "+label+"."
        }.joinToString(" ")
        val look=when(mode) {
            RenderMode.ANIME,RenderMode.ANIMATION ->
                "Create an anime illustration."
            RenderMode.REALISTIC ->
                "Create a photographically realistic fantasy illustration; preserve any nonhuman traits."
            RenderMode.COSPLAY ->
                "Create a photoreal cosplay interpretation; preserve the canonical wings, horns, ears, tails and marks."
        }
        val base=look+" "+directive(creation,sources)+" Use ONLY actual reference images for character identity, style, scene and mandatory details. "+
            numbered+" Render one coherent artwork, not a labeled reference-sheet infographic. "+
            "Never invent a missing canonical trait or omit an established one."
        val modifiers=listOf(
            "",
            "Preserve established character identity, facial features, outfit geometry and body interactions.",
            "Refine material response, lighting, surface reflections and environmental coherence.",
            "Refine existing fine details, ornaments, hair, accessories and element microstructure.",
            "Final rendering coherence without altering the original composition or established subjects."
        )
        val denoise=listOf(if (creation==CreationMode.STYLE_CORRECTION) .5f else 1f,
            .45f,.32f,.22f,.13f)
        val passes=(1..5).map { index ->
            GenerationPass(index,base+" "+modifiers[index-1],"",denoise[index-1])
        }
        return GenerationRecipe(mode,passes,sources.map { it.sheetId },settings)
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
