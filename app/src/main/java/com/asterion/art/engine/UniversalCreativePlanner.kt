package com.asterion.art.engine

import com.asterion.art.model.CreationMode
import com.asterion.art.model.SheetRole

/**
 * Shared pre-inference reasoning and prompt composition for every generation route.
 * Stage 1: interpret the requested systems and authorities.
 * Stage 2: synthesize distinct, physically meaningful design choices.
 * Stage 3: compile model-readable visual instructions and failure exclusions.
 *
 * Not a pretrained LLM. The full authority records remain separate from the
 * text encoder's actual token window; this compiler never claims to enlarge it.
 */
enum class CreativeDomain {
    ARTWORK, CHARACTER, OUTFIT, WEAPON, TRANSFORMATION, MUTATION,
    CREATURE, ALTERNATE_UNIVERSE, FEMINISATION, STYLE_CORRECTION,
    CHARACTER_SHEET, ART_STYLE_SHEET, THEME_SHEET, ELEMENT_SHEET,
    IMAGE_EDIT, VIDEO
}

data class DesignIntent(
    val domain: CreativeDomain,
    val concepts: List<String> = emptyList(),
    val referenceRoles: List<SheetRole> = emptyList(),
    val version: Int = 1
)

data class CompiledIntent(
    val intent: DesignIntent,
    val synthesis: String,
    val positive: String,
    val negative: String,
    val requiredEvidence: List<String>,
    val sourceAuthority: List<String>,
    val contextWordCount: Int
)

object UniversalCreativePlanner {
    const val DESIGN_CONTEXT_TARGET = 32768 // planning-layer target, NOT model encoder limit
    fun domain(mode: CreationMode?): CreativeDomain = when(mode) {
        null -> CreativeDomain.ARTWORK
        CreationMode.CHARACTER -> CreativeDomain.CHARACTER
        CreationMode.OUTFIT -> CreativeDomain.OUTFIT
        CreationMode.WEAPON -> CreativeDomain.WEAPON
        CreationMode.TRANSFORMATION -> CreativeDomain.TRANSFORMATION
        CreationMode.MUTATION -> CreativeDomain.MUTATION
        CreationMode.CREATURE -> CreativeDomain.CREATURE
        CreationMode.ALTERNATE_UNIVERSE -> CreativeDomain.ALTERNATE_UNIVERSE
        CreationMode.FEMINISATION -> CreativeDomain.FEMINISATION
        CreationMode.STYLE_CORRECTION -> CreativeDomain.STYLE_CORRECTION
    }
    fun sheetDomain(role:SheetRole): CreativeDomain = when(role) {
        SheetRole.CHARACTER -> CreativeDomain.CHARACTER_SHEET
        SheetRole.ART_STYLE -> CreativeDomain.ART_STYLE_SHEET
        SheetRole.THEME -> CreativeDomain.THEME_SHEET
        SheetRole.ELEMENT -> CreativeDomain.ELEMENT_SHEET
    }

    private fun normalize(input:List<String>): List<String> = input
        .map { it.trim().replace(Regex("\\s+"), " ") }
        .filter { it.isNotEmpty() }.distinct()

    private fun semantics(subject:String): String = when {
        subject.contains("antimatter",true) -> "matter-boundary geometry, containment topology and opposing-density interfaces"
        subject.contains("cloud",true) || subject.contains("mist",true) ->
            "layered suspended condensate, directional flows and soft-to-hard material transitions"
        subject.contains("oni",true) -> "rooted horns, reinforced cranial supports and powerful bone architecture"
        subject.contains("dragon",true) -> "rooted wings, scales, articulated tail and distinct cranial horns"
        subject.contains("galaxy",true) || subject.contains("cosmos",true) ->
            "structured stellar particulates, spatial gradients and bounded luminous voids"
        subject.contains("crystal",true) -> "rigid faceted geometry, optical translucency and load-bearing mineral interfaces"
        subject.contains("rose",true) -> "concentric petal layering, rooted cane structures and graduated soft/hard edges"
        subject.contains("cyber",true) -> "embedded cybernetic architecture, power routing and mechanical joint construction"
        subject.contains("gothic",true) -> "vertical tracery, pointed silhouettes and contrasting heavy/light ornament"
        subject.contains("armor",true) -> "wearable load paths, plates and protected joint articulation"
        subject.contains("sword",true) -> "readable blade, grip, guard, tang and balanced weapon scale"
        subject.contains("spear",true) -> "clear haft, tip connection and mechanically coherent thrusting geometry"
        else -> "recognizable physical geometry, material response and interactions of " + subject
    }

    private fun specification(domain: CreativeDomain): Pair<String,String> = when(domain) {
        CreativeDomain.CHARACTER ->
            "Construct distinct body-owned anatomies: head, eyes, hair, joints, material boundaries and complete silhouettes; preserve frozen identity across views." to
            "duplicate identity, generic mannequin, cropping, misplaced body halves, costume substituted for anatomy"
        CreativeDomain.OUTFIT ->
            "Engineer garment construction around the chosen body: coverage, seams, fasteners, layers, movement, comfort, texture, silhouette and materials." to
            "unwearable construction, disconnected straps, source-body redesign, garment discontinuity"
        CreativeDomain.WEAPON ->
            "Design a category-recognizable isolated weapon: macro geometry, load-bearing structure, grip, articulation, material response and balanced ornament." to
            "unreadable weapon class, missing grip, detached parts, impossible intersections"
        CreativeDomain.TRANSFORMATION ->
            "Preserve the same identifiable character while changing form, topology, material systems and connected apparel in a coherent alternate state." to
            "new unrelated identity, inconsistent source markings, random costume substitution"
        CreativeDomain.MUTATION ->
            "Preserve designated source anchors while authoring controlled structural changes of anatomy, asymmetry and material organization." to
            "only changed color, merely swapped costume, unsupported source identity loss"
        CreativeDomain.CREATURE ->
            "Construct biomechanically coherent novel organisms with explicit locomotion, sensory anatomy, joints, skeleton and materialized concepts." to
            "disconnected legs, ambiguous organism, unusable locomotion, ordinary animal recolor"
        CreativeDomain.ALTERNATE_UNIVERSE ->
            "Interpret the same identity in a specific complete world system; rebuild roles, technology, architecture, garments and equipment through that system." to
            "same outfit in different colors, unrelated identity, generic genre emblem"
        CreativeDomain.FEMINISATION ->
            "Comprehensively reconstruct the person as a distinct female design: original concept recognition retained, new head, face, hair, body proportions and clothing built for the new anatomy." to
            "superficial facial edit, original masculine body unchanged, unrelated character"
        CreativeDomain.STYLE_CORRECTION ->
            "Preserve source pixels' depicted subjects, geometry, expressions, pose, colors and framing as content; alter only the linework, shading, materials and render language according to target style." to
            "new character, pose change, subject loss, imported style-sheet identity"
        CreativeDomain.CHARACTER_SHEET ->
            "Describe one canonical character with coherent main portrait, true side/rear anatomical views, face, eyes, hair, materials, palette and adaptive distinctive detail slots." to
            "inconsistent canonical markings, unlabeled character, cropped feet, unrelated weapon"
        CreativeDomain.ART_STYLE_SHEET ->
            "Document visible rendering grammar only: contour, shading, palette behavior, materials, strokes and diagnostic visual comparisons." to
            "invented identity, scene requirements, unsupported art-style claims"
        CreativeDomain.THEME_SHEET ->
            "Document scene authority: camera, environment, poses, spatial relationships, lighting sources and compositional organization." to
            "style-sheet ownership confusion, arbitrary new characters, unsupported setting"
        CreativeDomain.ELEMENT_SHEET ->
            "Illustrate materials, physical manifestation, controlled variations, interactions, placement and recognizable structural identity." to
            "generic colored glow, mislabeled material, unsupported placement"
        CreativeDomain.IMAGE_EDIT ->
            "Modify only the selected target region with preserved edges and matched lighting, anatomy and materials at the boundary." to
            "unselected region changes, torn edges, identity drift, mismatched lighting"
        CreativeDomain.VIDEO ->
            "Maintain source identity, temporal continuity, coherent causal motion, consistent materials, environmental physics and stable camera grammar." to
            "identity morphing, flicker, sudden body duplication, discontinuous limbs, inconsistent motion"
        CreativeDomain.ARTWORK ->
            "Integrate character identity, outfit and pose, theme and scene geometry, optional elements and style according to their distinct authorities." to
            "character identity drift, inconsistent perspective, unsupported props, missing source features"
    }

    fun interpret(intent: DesignIntent, resolver:ConceptResolver?=null): CompiledIntent {
        val selected=normalize(intent.concepts)
        val (policy,rejections)=specification(intent.domain)
        val authority=intent.referenceRoles.distinct().map { role ->
            when(role) {
                SheetRole.CHARACTER -> "Character Sheet owns stable identity, morphology, face, hair and permanent markings."
                SheetRole.ART_STYLE -> "Art Style Sheet owns render grammar, not depicted identities or scene."
                SheetRole.THEME -> "Theme Sheet owns scene, environment, pose, framing and light sources."
                SheetRole.ELEMENT -> "Element Sheet owns element behavior, physical form and placements."
            }
        }
        val interpretations=selected.mapIndexed { i,name ->
            val domain=when(intent.domain) {
                CreativeDomain.WEAPON -> "weapon's geometry and material construction"
                CreativeDomain.OUTFIT -> "garments' seams, silhouette and material layers"
                CreativeDomain.VIDEO -> "temporal motion and evolving scene"
                CreativeDomain.THEME_SHEET -> "environment, camera and spatial relationships"
                CreativeDomain.ART_STYLE_SHEET -> "line, shadow, color and rendering grammar"
                else -> "anatomy, construction and visual materials"
            }
            val fact=resolver?.find(name)
            "Concept " + (i+1) + " (" + name + ") contributes to " + domain +
                " through " + (fact?.physicalInstruction(intent.domain) ?: semantics(name)) + "."
        }
        val fusion=if(selected.size>1)
            "Fuse all selected systems reciprocally: each modifies another system's geometry, " +
                "materials, boundaries or function. Do not create isolated visual stickers."
            else if(selected.size==1)
                "Make the chosen system intrinsic to the requested output, not an unrelated decoration."
            else "Use only supplied references and explicitly selected generation parameters."
        val physical=(listOf(policy) + interpretations + fusion).joinToString("\n")
        val positive=physical + if(authority.isEmpty()) "" else "\n" + authority.joinToString("\n")
        val negative=listOf(rejections,"unreadable structure","disconnected anatomy or objects")
            .distinct().joinToString(", ")
        return CompiledIntent(intent,physical,positive,negative,selected,authority,
            (physical + positive + negative + authority.joinToString(" ")).split(Regex("\\s+")).size)
    }

    /** Preserve upstream methodology and provenance; augment rather than replace. */
    fun enrich(recipe: GenerationRecipe, creation: CreationMode?, concepts: List<String>,
               references: List<SourceEvidence>,resolver:ConceptResolver?=null): GenerationRecipe {
        val plan=interpret(DesignIntent(domain(creation),concepts,references.map { it.role }),
            resolver)
        val passes=recipe.passes.map { pass ->
            val positive=pass.positive + "\n\n" + plan.positive
            val negative=listOf(pass.negative,plan.negative).filter { it.isNotBlank() }
                .distinct().joinToString(", ")
            pass.copy(positive=positive,negative=negative)
        }
        return recipe.copy(passes=passes)
    }

    /** Individual design from a multi-object catalogue; available to any studio. */
    fun individual(domain:CreativeDomain, concepts:List<String>, index:Int,
                   base:String):String {
        require(index>=0)
        val lenses=listOf(
            "dominant structural silhouette and anatomy",
            "layered materials and internal construction",
            "asymmetric but mechanically balanced topology",
            "face, head and distinctive recognizable identity",
            "appendages, joints and surface boundaries",
            "color and value architecture following material function",
            "rhythmic geometry and negative-space organisation",
            "support roots, attachment mechanisms and body integration",
            "environmental, functional and physical interactions",
            "refined, compositionally clean and recognizable details"
        )
        val primary=lenses[index%lenses.size]
        val secondary=lenses[(index*3+2)%lenses.size]
        return "Produce ONE complete independent " + domain.name.lowercase().replace('_',' ') +
            " design. Draw from this frozen source specification: " + base +
            "\nThis selected candidate uniquely emphasizes " + primary +
            " and " + secondary + ". Distinguish it from sibling variants through actual " +
            "shape and construction changes rather than superficial recoloring. " +
            if(concepts.isEmpty()) "" else
                "\nPreserve all selected systems: " + concepts.joinToString(" × ")
    }

    fun motion(preset:String, userDirection:String="",
               resolver:ConceptResolver?=null): CompiledIntent =
        interpret(DesignIntent(CreativeDomain.VIDEO,
            listOf(preset,userDirection).filter { it.isNotBlank() }),resolver)
}
