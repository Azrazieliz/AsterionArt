package com.asterion.art.methodology

import com.asterion.art.model.CreationMode
import com.asterion.art.model.SheetRole

/** Registered constraints extracted from the user's authoritative method families. */
data class CreationWorkflow(
    val mode: CreationMode,
    val name: String,
    val required: Set<SheetRole>,
    val protectIdentity: Boolean,
    val changeBodyAllowed: Boolean,
    val targetIsExistingImage: Boolean,
    val directive: String
)

object MethodologyRegistry {
    val modes: Map<CreationMode,CreationWorkflow> = listOf(
        CreationWorkflow(
            CreationMode.CHARACTER,"Character Creation",
            setOf(SheetRole.ART_STYLE,SheetRole.THEME),
            false,true,false,
            "Author a new coherent premium character based only on the supplied concepts, style and source references. Resolve anatomy, face, outfit and silhouette before decoration."),
        CreationWorkflow(
            CreationMode.STYLE_CORRECTION,"Art Style Correction",
            setOf(SheetRole.ART_STYLE),true,false,true,
            "Redraw the actual source illustration in the authoritative target art style. Preserve identity, original pose, outfit, composition and all scene objects. Do not redesign content."),
        CreationWorkflow(
            CreationMode.OUTFIT,"Outfit Creation",
            setOf(SheetRole.CHARACTER),true,false,false,
            "Design an outfit fitting the frozen character, preserving face, eyes, hair, anatomy, species traits, horns, wings, markings and identity. The outfit must adapt to the body, not the reverse."),
        CreationWorkflow(
            CreationMode.WEAPON,"Weapon Creation",
            setOf(SheetRole.CHARACTER),true,false,false,
            "Design a distinctive fictional signature weapon appropriate to this frozen character. Preserve all established anatomical, outfit and identity constraints."),
        CreationWorkflow(
            CreationMode.TRANSFORMATION,"Transformation Creation",
            setOf(SheetRole.CHARACTER,SheetRole.ELEMENT),true,true,false,
            "Create an alternate physical form of the SAME character. Materialize the supplied transformation elements throughout the design while retaining enough immutable recognition anchors."),
        CreationWorkflow(
            CreationMode.MUTATION,"Mutation Creation",
            setOf(SheetRole.CHARACTER,SheetRole.ELEMENT),false,true,false,
            "Create a structurally coherent mutation or offshoot with explicit lineage and differential traits. Mutation need not preserve all transformation-level identity constraints."),
        CreationWorkflow(
            CreationMode.CREATURE,"Creature Creation",
            setOf(SheetRole.THEME),false,true,false,
            "Construct a complete non-generic creature from the supplied concepts, including coherent silhouette, locomotion, sensory systems and material/anatomical integration."),
        CreationWorkflow(
            CreationMode.ALTERNATE_UNIVERSE,"Alternate Universe",
            setOf(SheetRole.CHARACTER,SheetRole.THEME),true,true,false,
            "Re-author the SAME canonical individual as a native of the chosen world. Adapt society, materials, profession, outfit and equipment through world logic, while preserving distinct character identity.")
    ).associateBy { it.mode }

    fun validate(mode: CreationMode, selected: Map<SheetRole,Int>): List<String> {
        val spec=modes[mode] ?: error("Unknown creation workflow")
        val missing=spec.required.filter { selected.getOrDefault(it,0)<1 }
        return missing.map { it.display+" authority required for "+spec.name }
    }

    /** Compose semantic constraints, never textually hallucinate source details. */
    fun compileDirective(mode: CreationMode, selected: Map<SheetRole,Int>): String {
        val missing=validate(mode,selected)
        require(missing.isEmpty()) { missing.joinToString("; ") }
        return requireNotNull(modes[mode]).directive
    }
}
