package com.asterion.art.render

import com.asterion.art.model.SheetRole

/**
 * Sheet-specific, source-anchored instructions. Character master sheet is
 * ONE whole-image reconstruction, never independently pasted crops.
 * Other sheet roles retain distinct documented visual evidence authorities.
 */
object SheetMasterPrompts {
    const val PREFIX="Professional 9:16 portrait reference master sheet, clean "+
        "precisely aligned analytical layout, white-blue paper, thin restrained "+
        "dividers, serif main title, legible small section labels, generous whitespace. "
    fun positive(role:SheetRole,evidence:String=""):String {
        val study=when(role) {
            SheetRole.CHARACTER ->
                "ONE finished character master sheet generated in ONE image. "+
                "The provided character is the SOLE canonical identity in EVERY panel. "+
                "Large full-body PRIMARY IDENTITY VIEW at left. "+
                "FACE IDENTITY front, three-quarter, exact left profile. "+
                "Detailed EYE IDENTITY and HAIR CONSTRUCTION front, profile, back. "+
                "Large CANONICAL ANATOMICAL PROFILE: clinically outfit-free "+
                "neutral full body at front, exact left profile, three-quarter and "+
                "direct rear. Retain the same identity, body morphology, species "+
                "features, hair, eyes and proportions across all views. "+
                "DEFAULT OUTFIT CONSTRUCTION: full-body outfit front, exact "+
                "side, rear with accurate fabric, layers, closures and accessories. "+
                "Compact accurate PHYSICAL and OUTFIT COLOR PALETTE with material "+
                "swatches. Optional crucial weapon/detail callouts only. "+
                "No repetition, filler, lore, unrelated characters or arbitrary "+
                "body normalization. Reconstruct unknown views, never merely "+
                "copy or crop existing pixels."
            SheetRole.ART_STYLE ->
                "Character-agnostic ART STYLE MASTER SHEET. "+
                "Show supplied unchanged illustration reference examples as "+
                "calibration images. Extract actual style properties only: "+
                "STYLE IDENTITY tendencies, defining STYLE FINGERPRINTS, "+
                "face/eye/hair/body/hands/feet MORPHOLOGICAL GRAMMAR, "+
                "linework/shadows/highlights/colors RENDERING GRAMMAR, "+
                "MATERIAL LIBRARY, DETAIL HIERARCHY, COMPOSITION BEHAVIOR, "+
                "VARIATION ENVELOPE, precise positive/negative descriptors, "+
                "confidence and failure cases. Never assume sampled characters "+
                "are canonical subjects. Never fabricate reference examples."
            SheetRole.THEME ->
                "THEME MASTER SHEET with a dominant canonical theme "+
                "implementation and precise design-system sections for "+
                "COMPOSITION, VISUAL HIERARCHY, POSE and BODY LANGUAGE, "+
                "EXPRESSION, MATERIAL and ELEMENT INTERACTION, "+
                "ENVIRONMENT, LIGHTING, CAMERA and ATMOSPHERE, "+
                "recurring relationships, invariants and allowed variation. "+
                "Aesthetic priorities and mechanism callouts must agree "+
                "with the input, not merely repeat its crop."
            SheetRole.ELEMENT ->
                "ELEMENT MASTER SHEET: large canonical element manifestation, "+
                "structural geometry, invariants, lawful variations, placement "+
                "and scaling, color and material behavior, illumination, "+
                "host-dependent adaptations, visual interactions, "+
                "compositional weight and clear prohibitions. "+
                "Do not graft a separate floating foreign object onto a character."
        }
        return PREFIX+study+(if(evidence.isBlank())"" else
            " VERIFIED VISIBLE REFERENCE OBSERVATIONS: "+evidence.take(1600))
    }
    fun negative(role:SheetRole):String {
        val base="poor image quality, low-detail placeholders, duplicated panels, "+
            "incoherent geometry, inconsistent identity, missing limbs, "+ 
            "illegible panel layout, unrelated decoration, generic filler, watermark"
        return base+if(role==SheetRole.CHARACTER)
            ", costume covering anatomy panel, anatomy mismatch, front views in place of profiles"
            else ", invented features without source support"
    }
}
