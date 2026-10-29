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
            // Source authority must not vanish merely because a user-supplied
            // field exceeds a guessed short prompt-fragment length.
            // Native capacity is validated AFTER complete compilation.
            .filter { it.length >= 3 && alphaNum.matches(it) }
            .distinct()

    private fun join(words: List<String>) = clean(words).joinToString(", ")

    private fun enforceRoles(sources: List<SourceEvidence>, creation: CreationMode?,
                             concepts: List<String>, sourceIllustration: Boolean) {
        val selected = sources.groupingBy { it.role }.eachCount()
        val errors = if (creation==null) {
            // Artwork takes Character, Theme and Art Style sheet authorities.
            // A single reference image belongs to Sheet Creation / editing,
            // not an implicit replacement for the owner's three sheet roles.
            listOf(SheetRole.CHARACTER,SheetRole.THEME,SheetRole.ART_STYLE)
                .filter{selected.getOrDefault(it,0)==0}
                .map{"Artwork requires a "+it.display+" sheet"}
        } else MethodologyRegistry.validate(
            creation,selected,concepts.size,sourceIllustration)
        require(errors.isEmpty()) { errors.joinToString("; ") }
    }
    private fun directive(creation: CreationMode?, sources: List<SourceEvidence>,
                          concepts: List<String>, sourceIllustration: Boolean): String =
        if (creation == null) "" else MethodologyRegistry.compileDirective(
            creation,sources.groupingBy { it.role }.eachCount(),
            concepts.size,sourceIllustration)

    fun compile(
        sources: List<SourceEvidence>,
        mode: RenderMode,
        settings: GeneratorSettings,
        creation: CreationMode? = null,
        concepts: List<String> = emptyList(),
        sourceIllustration: Boolean = false,
        resolver:ConceptResolver?=null
    ): GenerationRecipe {
        enforceRoles(sources,creation,concepts,sourceIllustration)
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
        val core = listOf(directive(creation,sources,concepts,sourceIllustration),
            concepts.joinToString(", "), character, outfitAndPose, theme,
            environmentAndElements, appearance)
            .filter { it.isNotBlank() }.joinToString("\n\n")
        require(core.isNotBlank()) { "No interpretable prompt content in selected reference images." }

        val positiveDetails = listOf(
            pieces(characters, "FACE", "EYES", "HAIR", "BODY", "OUTFIT") +
                pieces(themes, "POSE", "VIEW", "INTERACTION", "CONTACT") +
                pieces(elements, "PLACEMENT", "STRUCTURE"),
            pieces(styles, "MATERIAL", "LIGHT", "COLOR") +
                pieces(themes, "LIGHT", "ENVIRONMENT"),
            pieces(characters, "FACE", "EYES", "HAIR", "DISTINCTIVE", "DIAGNOSTIC") +
                pieces(styles, "LINEWORK", "DETAIL", "TEXTURE") +
                pieces(elements, "STRUCTURE", "VARIATIONS", "MATERIAL", "COLOR"),
            pieces(styles, "FINGERPRINT", "COMPOSITION") +
                pieces(themes, "VIEW")
        )
        val negative = join(
            pieces(styles, "NEGATIVE") +
                pieces(themes, "NEGATIVE") +
                pieces(elements, "NEGATIVE")
        )

        val denoise = listOf(1f, 0.48f, 0.32f, 0.22f, 0.13f)
        val passes = if (creation == null) (1..5).map { n ->
            // The owner's five quality phases belong to Artwork Creation ONLY.
            val extra = if (n == 1) emptyList() else positiveDetails[n - 2]
            val full = if (extra.isEmpty()) core else core + "\n\n" + join(extra)
            GenerationPass(n, full, negative, denoise[n - 1])
        } else listOf(GenerationPass(1, core, negative,
            if (creation == CreationMode.STYLE_CORRECTION) settings.denoise else 1f))
        if(creation==null)ArtworkFivePhaseContract.validate(passes)
        return UniversalCreativePlanner.enrich(
            GenerationRecipe(mode, passes, sources.map { it.sheetId }, settings),
            creation,concepts,sources,resolver)
    }

    /**
     * One-source artwork: the actual image remains the neural img2img input.
     * This is not a prompt-only substitute and requires source pixels in the
     * corresponding GenerationWorker invocation.
     */
    fun compileFromSource(mode:RenderMode,settings:GeneratorSettings):GenerationRecipe {
        val base=when(mode) {
            RenderMode.ANIME -> "Create a coherent high-quality original anime illustration"
            RenderMode.REALISTIC -> "Create a coherent photorealistic illustration"
            RenderMode.COSPLAY -> "Create a cohesive realistic cosplay interpretation"
            RenderMode.OTHER -> "Create a coherent illustration for the selected visual category"
            RenderMode.ANIMATION -> error("Use the video engine for animation")
        }+" conditioned on the PROVIDED SOURCE IMAGE PIXELS. Preserve exact "+
            "recognizable identity, face and hair architecture, character anatomy, "+
            "established clothing and materials, source objects and composition "+
            "unless the user explicitly requests a localized change. Do not invent "+
            "alternate characters or erase the provided source authority."
        val corrections=listOf(
            "Reconstruct source-consistent geometry and material appearance.",
            "Refine face, hair, anatomy and preserved primary identity.",
            "Improve local shading, physical material behavior and perspective.",
            "Clarify small clothing and object construction without changing identity.",
            "Finalize line quality and visual coherence without scene drift."
        )
        val strength=listOf(settings.denoise,
            minOf(settings.denoise,0.45f),0.30f,0.20f,0.12f)
        return GenerationRecipe(mode,corrections.mapIndexed{i,detail->
            GenerationPass(i+1,base+" "+detail,"",strength[i])
        },emptyList(),settings)
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
        concepts: List<String> = emptyList(),
        sourceIllustration: Boolean = false,
        resolver:ConceptResolver?=null
    ): GenerationRecipe {
        enforceRoles(sources,creation,concepts,sourceIllustration)
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
            RenderMode.OTHER ->
                "Create a faithful image for the selected custom category; use provided sources as authority."
        }
        val base=look+" "+directive(creation,sources,concepts,sourceIllustration)+" "+concepts.joinToString(", ")+
            " Use ONLY actual reference images for character identity, style, scene and mandatory details. "+
            numbered+(if(creation==null) " Render one coherent artwork, not a labeled reference-sheet infographic. "+
                "Never invent a missing canonical trait or omit an established one."
             else " Follow the selected methodology's EXACT output layout, view count and " +
                "design count. Catalogue sheets and analytical labels are permitted ONLY when " +
                "required by that methodology; keep source identity constraints intact.")
        val modifiers=listOf(
            "",
            "Preserve established character identity, facial features, outfit geometry and body interactions.",
            "Refine material response, lighting, surface reflections and environmental coherence.",
            "Refine existing fine details, ornaments, hair, accessories and element microstructure.",
            "Final rendering coherence without altering the original composition or established subjects."
        )
        val denoise=listOf(if (creation==CreationMode.STYLE_CORRECTION) .5f else 1f,
            .45f,.32f,.22f,.13f)
        val passes=if(creation==null)(1..5).map { index ->
            GenerationPass(index,base+" "+modifiers[index-1],"",denoise[index-1])
        } else listOf(GenerationPass(1,base,"",denoise.first()))
        if(creation==null)ArtworkFivePhaseContract.validate(passes)
        return UniversalCreativePlanner.enrich(
            GenerationRecipe(mode,passes,sources.map { it.sheetId },settings),
            creation,concepts,sources,resolver)
    }


    /**
     * Character Creation v3 and original Creature Creation start without sheets.
     * The selected, frozen user concepts are the authoritative task input.
     */
    fun compileConcepts(concepts: List<String>, creation: CreationMode,
                        mode: RenderMode, settings: GeneratorSettings,
                        resolver:ConceptResolver?=null): GenerationRecipe {
        require(creation==CreationMode.CHARACTER || creation==CreationMode.CREATURE)
        enforceRoles(emptyList(),creation,concepts,false)
        val specification=MethodologyRegistry.modes.getValue(creation)
        val frozen=concepts.distinct().joinToString(" × ")
        val base=specification.directive+"\nPrimary concepts: "+frozen+
            "\nProduce a faithful 2D visual output. Follow exact framing and candidate count."+
            " No unrelated environmental scenery or textual labels except the specified candidate numbers."
        val details=listOf("Establish every major structure and all characters in this pass.",
            "Stabilize body architecture, silhouette, face, head and visual identity.",
            "Resolve anatomical and material interaction with consistent lighting.",
            "Detail intrinsic morphology, hair, surfaces and smaller structure without redesign.",
            "Finalize faithful visual coherence and clean neutral presentation.")
        // Creature/Character are Originals and must not inherit Artwork's
        // five sequential image refinements. Their methodology owns the output.
        val passes=listOf(GenerationPass(1,base+"\n"+details.joinToString("\n"),"",1f))
        return UniversalCreativePlanner.enrich(
            GenerationRecipe(mode,passes,emptyList(),settings),
            creation,concepts,emptyList(),resolver)
    }

    fun selectModelId(models: List<Pair<String, String>>, mode: RenderMode): String? {
        // One automatic route shown to the user; the ranking operates internally.
        // This is NOT a claim of checkpoint weight fusion.
        val rank = when (mode) {
            RenderMode.ANIME, RenderMode.ANIMATION ->
                listOf("rin_flanime", "miaomiao", "hassaku", "noobai")
            RenderMode.REALISTIC, RenderMode.COSPLAY ->
                listOf("cyberrealistic", "realism", "sam_anima_realistic", "rin_flanime")
            RenderMode.OTHER -> emptyList()
        }
        // Imported QNN IDs are randomized. Match their actual labels even
        // when upstream uses spaces, hyphens or underscores for one family.
        // This preserves the owner's existing model-quality preference order.
        fun normalized(value:String):String =
            value.lowercase().replace(Regex("[^a-z0-9]+"),"_").trim('_')
        return rank.firstNotNullOfOrNull { needle ->
            models.firstOrNull { (id, name) ->
                normalized(id).contains(needle) ||
                    normalized(name).contains(needle)
            }?.first
        }
    }
}
