package com.asterion.art.methodology

import com.asterion.art.model.CreationMode
import com.asterion.art.model.SheetRole

/**
 * User Methodology.zip, versioned contracts. Never apply the generic
 * artwork sheet requirements to a methodology-specific default.
 *
 * Character Creation v3 is authoritative over v2 and v1.5. Concepts.md
 * and Design_Direction.md are subordinate knowledge authorities.
 */
enum class AuthorityKind { CHARACTER, ART_STYLE, THEME, ELEMENT, SOURCE_IMAGE, CONCEPTS, DESIGN_DIRECTION }
enum class StageAction { CATALOGUE, SINGLE_PREVIEW, CONSTRUCTION, BODY, OUTFIT, WEAPON, ASSEMBLY }
enum class SourcePolicy { NONE, OPTIONAL, REQUIRED, CHARACTER_OR_IMAGE }
data class OutputGeometry(
    val ratio: String,
    val designsPerImage: Int,
    val imageCount: Int = 1,
    val columns: Int = designsPerImage,
    val rows: Int = 1,
    val frontAndRearViews: Boolean = false,
    val whiteNeutralField: Boolean = true,
    val preserveOriginalFraming: Boolean = false
) {
    init { require(designsPerImage > 0 && imageCount > 0 && columns > 0 && rows > 0) }
}
data class MethodologyContract(
    val mode: CreationMode,
    val name: String,
    val referenceSource: String,
    val requiredSheets: Set<SheetRole>,
    val sourcePolicy: SourcePolicy,
    val minimumConceptSlots: Int,
    val catalogue: OutputGeometry,
    val preview: OutputGeometry,
    val protectedSystems: Set<String>,
    val changeableSystems: Set<String>,
    val rendererDirective: String,
    val commands: Set<StageAction>,
    val mustFreezeBeforeNextPhase: Boolean = false
)
data class MethodologyRequest(
    val mode: CreationMode,
    val sourceIllustrationPresent: Boolean = false,
    val sheetRoles: Set<SheetRole> = emptySet(),
    val conceptIds: List<String> = emptyList(),
    val designDirectionId: String? = null,
    val action: StageAction = StageAction.CATALOGUE,
    val frozenBodyRevision: String? = null,
    val frozenOutfitRevision: String? = null,
    val frozenWeaponRevision: String? = null
)
object DetailedMethodologies {
    private val portrait = OutputGeometry("9:16",1,1,1,1)
    private val ten = OutputGeometry("16:9",10,1,5,2)
    private val five = OutputGeometry("16:9",5,1,5,1)
    private val source = OutputGeometry("SOURCE",1,1,1,1,whiteNeutralField=false,preserveOriginalFraming=true)
    private val identity = setOf("Face","Eyes","Hair","Body","Proportions","Canon traits","Character silhouette")
    private val freeze = setOf("Face","Hair","Body","Proportions","Species anatomy","Tail/Wings/Horns","Markings")
    val all: Map<CreationMode,MethodologyContract> = listOf(
        MethodologyContract(
            CreationMode.CHARACTER,"Character Creation v3",
            "Character Creation v3/characters_creation.md + Concepts.md + Design_Direction.md",
            emptySet(),SourcePolicy.NONE,2,
            OutputGeometry("16:9",5,3,5,1,frontAndRearViews=true),
            OutputGeometry("9:16",1,1,1,1,frontAndRearViews=true),
            emptySet(),setOf("Original body architecture","Face","Hair","Concept fusion"),
            "Phase 1: one population of exactly fifteen independent authored identities. Three separate 16:9 analytical sheets, each containing exactly five distinct characters. Each identity has an uncropped true-front neutral view plus smaller exact 90-degree profile and exact rear of the SAME body. White field. Concepts must materialize intrinsically as premium 2D female characters, not as surrounding icons, props or decorations. Fifteen designs must be authored and certified before any first-sheet rendering. No outfit, weapon, environment, pose substitution, 3/4 view, collage or cropped morphology. Phase 2 outfit, Phase 3 weapon and Phase 4 assembly require explicit user approval and frozen previous phase.",
            setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW),
            mustFreezeBeforeNextPhase=true
        ),
        MethodologyContract(
            CreationMode.STYLE_CORRECTION,"Art Style Correction",
            "Art Style Correction/art style sanitisation.md",
            setOf(SheetRole.ART_STYLE),SourcePolicy.REQUIRED,0,source,source,
            identity + setOf("Pose","Composition","Scene","Outfit","Props","Camera","Source colors as content anchors"),
            setOf("Drawing grammar","Linework","Shading","Render materials","Rendering finish"),
            "Read target Art Style Sheet as style-only authority, and the original illustration as content-only authority. Preserve subjects, identity, camera, crop, pose, clothing, scene, color identities and geometry. Replace entire source rendering treatment with the target style; do not hybridize old/new styles or import characters depicted on the style sheet. One final re-render per original, same aspect ratio and framing.",
            setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
        ),
        MethodologyContract(
            CreationMode.OUTFIT,"Outfit Creation",
            "Outfit Creation/Outfit Creation.md",
            emptySet(),SourcePolicy.OPTIONAL,0,ten,portrait,
            freeze,setOf("Garment construction","Coverage","Materials","Outfit colors","Ornament"),
            "Freeze source character body, face, hair, proportions and concept anatomy. Default exactly ten structurally and materially different outfits on one 16:9 white 2x5 catalogue. Use analytical headless forms matching frozen morphology when needed. Do not reauthor body, insert weapons or scenes. Selected preview: exact 9:16 white full-body original character wearing chosen outfit.",
            setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW,StageAction.CONSTRUCTION)
        ),
        MethodologyContract(
            CreationMode.WEAPON,"Weapon Creation",
            "Weapon Creation/Weapon Creation.md",
            emptySet(),SourcePolicy.OPTIONAL,0,ten,portrait,
            freeze + setOf("Selected frozen outfit"),setOf("Category","Weapon geometry","Grip","Materials","Ornament"),
            "Freeze character and selected outfit. Default exactly ten distinct, complete, ISOLATED readable weapons on a single white 16:9 two-row by five-column catalogue. No character, hand, scene or decorative effects. Simple category-recognizable macro structure with premium character-specific fine construction. Preview may show original full-body character holding selected weapon only after user selection.",
            setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW,StageAction.CONSTRUCTION)
        ),
        MethodologyContract(
            CreationMode.TRANSFORMATION,"Transformation Creation",
            "Transformation Creation/Transformation Creation.md",
            emptySet(),SourcePolicy.CHARACTER_OR_IMAGE,0,five,portrait,
            identity,setOf("Body state","Materialization","Appendage state","Outfit adaptation"),
            "Same frozen individual in coherent alternate forms. Default five distinct uncropped, full-body neutral characters across one 16:9 analytical white sheet. No new identity, scenery or unrelated props. Single selected preview is 9:16.",
            setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
        ),
        MethodologyContract(
            CreationMode.MUTATION,"Mutation Creation",
            "Mutation/Mutation Creation.md",
            emptySet(),SourcePolicy.CHARACTER_OR_IMAGE,0,five,portrait,
            setOf("Source continuity","Selected protected anchors"),
            setOf("Morphology","Asymmetry","Material","Appendages","Silhouette"),
            "Mutation is controlled deviation, not a power-state transformation. Default 70 percent source identity retention and 30 percent mutation pressure; five radically different uncropped full-body mutants on one 16:9 white analytical sheet. Mutation must affect structure; do not use generic costume swaps.",
            setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
        ),
        MethodologyContract(
            CreationMode.CREATURE,"Creature Creation",
            "Creature/Creature Creation.md",
            emptySet(),SourcePolicy.OPTIONAL,1,five,portrait,
            emptySet(),setOf("Body plan","Locomotion","Sensory anatomy","Material fusion"),
            "Create original creatures with coherent full body plans, locomotion, sensory systems and structural concept symbiosis. Five different complete, uncropped creatures in one continuous 16:9 white analytical catalogue; no characters, props or scenes. The source owner is optional and relational, not a source to redraw.",
            setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
        ),
        MethodologyContract(
            CreationMode.ALTERNATE_UNIVERSE,"Alternate Universe",
            "Alternate Version/Alternate Universe Creation.md",
            emptySet(),SourcePolicy.CHARACTER_OR_IMAGE,1,five,portrait,
            identity,setOf("World logic","Equipment","Technology","Clothing","Role"),
            "The SAME source character must naturally belong to five systematically different alternate universes on a 16:9 white analytical five-design catalogue. Preserve distinctive identity while reauthoring outfit, weapons and material logic for each world. No generic cosplay costume swaps or environment; single AU preview is a neutral uncropped 9:16 portrait.",
            setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
        ),
        MethodologyContract(
            CreationMode.FEMINISATION,"Feminisation",
            "Feminisation/feminisation_reconstructed_current.md",
            emptySet(),SourcePolicy.REQUIRED,0,portrait,portrait,
            setOf("Recognizable identity anchors","Source concept","Canonical non-gender identity"),
            setOf("Female anatomy","Head and face","Hair","Outfit","Body proportions"),
            "Create one complete, uncropped 9:16 full-body female character on a plain white field. " +
                "Reconstruct head shape, face, eye architecture, hair design, anatomical proportions, " +
                "silhouette, clothing and materials as an independently authored female design. " +
                "Preserve recognizable source concept anchors without copying the original masculine " +
                "body template. Rebuild garments around the new body, never merely open the male outfit. " +
                "Level 4.5 means comprehensive coordinated female redesign across every body system, " +
                "not a numeric instruction for the diffusion model. One figure only, no extra panels.",
            setOf(StageAction.CATALOGUE,StageAction.SINGLE_PREVIEW)
        )
    ).associateBy { it.mode }

    fun geometry(request:MethodologyRequest):OutputGeometry {
        val contract=all.getValue(request.mode)
        return if(request.action==StageAction.SINGLE_PREVIEW) contract.preview
        else if(request.mode==CreationMode.CHARACTER && request.action==StageAction.ASSEMBLY)
            contract.preview
        else contract.catalogue
    }

    fun validate(request:MethodologyRequest):List<String> {
        val contract=all.getValue(request.mode)
        return buildList {
            for (role in contract.requiredSheets) if(role !in request.sheetRoles)
                add(role.display+" Sheet required")
            when(contract.sourcePolicy) {
                SourcePolicy.REQUIRED -> if(!request.sourceIllustrationPresent) add("Source illustration required")
                SourcePolicy.CHARACTER_OR_IMAGE -> if(!request.sourceIllustrationPresent &&
                    SheetRole.CHARACTER !in request.sheetRoles) add("Source character illustration or Character Sheet required")
                SourcePolicy.NONE,SourcePolicy.OPTIONAL -> Unit
            }
            if(request.conceptIds.distinct().size<contract.minimumConceptSlots)
                add("Select at least "+contract.minimumConceptSlots+" distinct canonical concept slots")
            if(request.action !in contract.commands)
                add("This methodology does not support the selected operation")
            if (false && request.mode==CreationMode.CHARACTER) {
                when(request.action) {
                    StageAction.OUTFIT -> if(request.frozenBodyRevision.isNullOrBlank())
                        add("Freeze the selected body before Phase 2")
                    StageAction.WEAPON -> {
                        if(request.frozenBodyRevision.isNullOrBlank()) add("Frozen body required for Phase 3")
                        if(request.frozenOutfitRevision.isNullOrBlank()) add("Frozen outfit required for Phase 3")
                    }
                    StageAction.ASSEMBLY -> {
                        if(request.frozenBodyRevision.isNullOrBlank()) add("Frozen body required for Phase 4")
                        if(request.frozenOutfitRevision.isNullOrBlank()) add("Frozen outfit required for Phase 4")
                        if(request.frozenWeaponRevision.isNullOrBlank()) add("Frozen weapon required for Phase 4")
                    }
                    else -> Unit
                }
            }
        }
    }
}
