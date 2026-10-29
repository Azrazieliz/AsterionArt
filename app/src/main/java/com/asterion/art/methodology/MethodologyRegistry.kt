package com.asterion.art.methodology

import com.asterion.art.model.CreationMode
import com.asterion.art.model.SheetRole

data class DefaultLayout(
    val ratio: String, val totalDesigns: Int, val pageCount: Int,
    val columns: Int, val rows: Int, val whiteBackground: Boolean = true
)
data class CreationWorkflow(
    val mode: CreationMode, val name: String,
    val required: Set<SheetRole>, val minimumConcepts: Int,
    val requiresIllustration: Boolean, val protectIdentity: Boolean,
    val changeBodyAllowed: Boolean, val layout: DefaultLayout,
    val directive: String
)

/** Mode-specific constraints from user Methodology.zip; Character Creation v3 takes precedence. */
object MethodologyRegistry {
    private val five = DefaultLayout("16:9",5,1,5,1)
    private val ten = DefaultLayout("16:9",10,1,5,2)
    val modes: Map<CreationMode,CreationWorkflow> = listOf(
        CreationWorkflow(CreationMode.CHARACTER,"Character Creation",
            emptySet(),2,false,false,true,DefaultLayout("16:9",15,3,5,1),
            "Character Creation Phase 1 is an independent, model-authored "+
                "clinical anatomical female character population. The user selects "+
                "1–5 exact 16:9 sheets, five independent identities per sheet, "+
                "with FIFTEEN full-body depictions per sheet: five identities each with "+
                "full-body true-front, exact 90-degree side profile and direct rear "+
                "for every anatomy-frozen identity. All characters must be fully "+
                "authored, refined and independently differentiated before rendering. "+
                "Strong coherent complete concept fusion, skeletal and morphological "+
                "integrity, independently authored face/eyes/hair, flagship 2D "+
                "anime/manhwa style and actual unclothed anatomical presentation "+
                "are mandatory. No clothing, Design Direction, sub-concepts, "+
                "Outfit, Weapon or Assembly phase inside Character Creation."),
        CreationWorkflow(CreationMode.STYLE_CORRECTION,"Art Style Correction",
            setOf(SheetRole.ART_STYLE),0,true,true,false,DefaultLayout("SOURCE",1,1,1,1,false),
            "Exactly one target art-style sheet plus source illustration. Preserve identity, pose, " +
                "outfit, body, scene, camera, objects and framing. Completely replace the source render " +
                "style with the authoritative style; no half-transfer, source-style contamination or " +
                "invented content. No character/theme/element sheet is required."),
        CreationWorkflow(CreationMode.OUTFIT,"Outfit Creation",
            setOf(SheetRole.CHARACTER),0,false,true,false,ten,
            "Frozen character identity first. Exact 16:9 white neutral 2x5 catalogue containing ten " +
                "structurally different outfits, no weapons or environments. Dress the original " +
                "body without redesigning or normalizing its morphology."),
        CreationWorkflow(CreationMode.WEAPON,"Weapon Creation",
            setOf(SheetRole.CHARACTER),0,false,true,false,ten,
            "Frozen character, optional frozen outfit. Ten independent, clearly category-readable " +
                "fictional weapons, isolated in exact 16:9 white analytical 2x5 catalogue; no character " +
                "or scene. Simple conventional macro geometry, intricate distinctive micro construction."),
        CreationWorkflow(CreationMode.TRANSFORMATION,"Transformation Creation",
            setOf(SheetRole.CHARACTER),1,false,true,true,five,
            "Frozen character and form concept. Five different transformation routes, complete uncropped " +
                "full-body neutral stances on one 16:9 white sheet, preserving immutable identity anchors."),
        CreationWorkflow(CreationMode.MUTATION,"Mutation Creation",
            setOf(SheetRole.CHARACTER),1,false,false,true,five,
            "Source character and mutation concept. Five radically distinct neutral full-body mutations " +
                "in one exact 16:9 white sheet. Default retention is 70% source, 30% mutation."),
        CreationWorkflow(CreationMode.CREATURE,"Creature Creation",
            emptySet(),1,false,false,true,five,
            "Create five independent coherent creatures from selected concept inputs. Anatomical silhouette, " +
                "locomotion and sensory consistency; five uncropped creatures across exact 16:9 white sheet. " +
                "No narrative names, scenes, extra people, or unrelated props."),
        CreationWorkflow(CreationMode.ALTERNATE_UNIVERSE,"Alternate Universe",
            setOf(SheetRole.CHARACTER),1,false,true,true,five,
            "The SAME original character interpreted through five different alternate-universe design " +
                "logics. Exact 16:9 analytical white sheet, complete full bodies. Preserve identity."),
        CreationWorkflow(CreationMode.FEMINISATION,"Feminisation",
            emptySet(),0,true,true,true,DefaultLayout("9:16",1,1,1,1),
            "Complete female reconstruction preserving the source's conceptual identity; one full-body result.")
    ).associateBy { it.mode }

    // Source and validation are delegated to the exact workflow contract.
    // The legacy mode metadata remains for historical result compatibility.
    fun validate(mode: CreationMode, selected: Map<SheetRole,Int>,
                 conceptCount: Int = 0, sourceIllustration: Boolean = false): List<String> =
        DetailedMethodologies.validate(MethodologyRequest(
            mode = mode, sourceIllustrationPresent = sourceIllustration,
            sheetRoles = selected.filterValues { it > 0 }.keys,
            conceptIds = (0 until conceptCount).map { "selected-" + it }
        ))

    fun compileDirective(mode: CreationMode, selected: Map<SheetRole,Int>,
                         conceptCount: Int = 0, sourceIllustration: Boolean = false): String {
        val errors = validate(mode, selected, conceptCount, sourceIllustration)
        require(errors.isEmpty()) { errors.joinToString("; ") }
        return DetailedMethodologies.all.getValue(mode).rendererDirective
    }

    fun pageCaption(mode: CreationMode, index: Int): String {
        val layout=modes.getValue(mode).layout
        require(index in 0 until layout.pageCount)
        val start=index*layout.columns*layout.rows+1
        val end=(start+layout.columns*layout.rows-1).coerceAtMost(layout.totalDesigns)
        return "Designs "+start.toString().padStart(2,'0')+"–"+end.toString().padStart(2,'0')
    }
}
