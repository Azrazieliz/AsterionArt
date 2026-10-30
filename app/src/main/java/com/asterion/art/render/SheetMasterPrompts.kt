package com.asterion.art.render

import com.asterion.art.model.SheetRole
import com.asterion.art.model.CreationMode

/**
 * Sheet-specific, source-anchored instructions. Character master sheet is
 * ONE whole-image reconstruction, never independently pasted crops.
 * Other sheet roles retain distinct documented visual evidence authorities.
 */
object SheetMasterPrompts {
    const val PREFIX="Professional 9:16 portrait reference master sheet, clean "+
        "precisely aligned analytical layout, white-blue paper, thin restrained "+
        "dividers, serif main title, legible small section labels, generous whitespace. "
    private fun specific(original:CreationMode):String=when(original) {
        CreationMode.CHARACTER ->
            "ORIGINAL CHARACTER identity reference. Reconstruct same frozen "+
            "full-body female concept anatomy across exact front, exact side "+
            "and direct rear with close face, eyes, hair, rooted appendages, "+
            "fused materials, palette and important unique anatomy. "+
            "NO invented outfit or weapon, NO costumes, NO fashion panels."
        CreationMode.OUTFIT ->
            "OUTFIT CONSTRUCTION MASTER SHEET. Preserve the frozen host "+
            "character and present only the selected costume faithfully: "+
            "dominant fully outfitted full-body view, outfit exact front, "+
            "90-degree side and rear, separate collar, sleeves, closures, "+
            "layer seams, textile materials, ornament and color swatches. "+
            "No new body design, face, haircut or unrelated outfit variants."
        CreationMode.WEAPON ->
            "SIGNATURE WEAPON MASTER SHEET. Hero isolated full weapon, "+
            "exact front/side/back structural projections, readable grip, "+
            "blade or mechanism, cross sections, joint connections, scaled "+
            "host-hand placement, material/edge/ornament detail and palette. "+
            "Reconstruct the SAME weapon, not multiple alternatives."
        CreationMode.TRANSFORMATION ->
            "TRANSFORMATION FORM MASTER SHEET. Retain original identity "+
            "through exact front/side/rear anatomy, transformed structures, "+
            "face/eye/hair anchors, rooted physical changes and coherent "+
            "new material systems. Show before/after feature relationships "+
            "and topology details only when grounded in the source."
        CreationMode.MUTATION ->
            "CONTROLLED MUTATION MASTER SHEET. Exact full-body front, "+
            "profile and rear of one mutated individual; highlight source "+
            "identity preserved and specific anatomical deviations, "+
            "functional appendage roots, skin/substrate interfaces and "+
            "the accurate mutated palette. No unrelated redesign."
        CreationMode.CREATURE ->
            "CREATURE MORPHOLOGY MASTER SHEET. One specific original "+
            "organism presented in exact front, 90-degree profile, direct "+
            "rear and functional movement view; skeletal attachment "+
            "points, limbs, locomotion, sensory structures, materials, "+
            "true scale and fused biological concepts. No clothing "+
            "or generic humanoid body normalization."
        CreationMode.ALTERNATE_UNIVERSE ->
            "ALTERNATE-UNIVERSE CHARACTER MASTER SHEET. Maintain one "+
            "recognizable source identity with full-body front, side and "+
            "rear, world-native outfit/technology/magic construction, "+
            "face/hair/identity invariants, detail callouts and palette. "+
            "Show lawful adaptations to the target world, not costume swapping."
        CreationMode.FEMINISATION ->
            "FEMINISATION RECONSTRUCTION MASTER SHEET accompanying the "+
            "separate final 9:16 art: consistent female-first identity "+
            "front, exact profile and rear with anatomical, face/hair "+
            "and reconstructed outfit details, palette and source identity "+
            "recognition anchors. Never revert to the old male silhouette."
        CreationMode.STYLE_CORRECTION ->
            "STYLE CORRECTION QUALITY REFERENCE: preserve source content "+
            "exactly and compare target linework, shading, material treatment "+
            "and colors; no character substitution or content redesign."
    }
    fun positive(role:SheetRole,evidence:String="",
        original:CreationMode?=null):String {
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
                (if(original==CreationMode.CHARACTER)
                    "UNCLAD ORIGINAL PHASE-1 FORM: do not invent an outfit, armor, "+
                    "weapon or costume panels. Use this space for accurate "+
                    "body morphology, structural concept-fusion, appendage roots "+
                    "and full front/profile/rear identity reference. "
                 else
                    "DEFAULT OUTFIT CONSTRUCTION: full-body outfit front, exact "+
                    "side, rear with accurate fabric, layers, closures and accessories. ")+
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
        // An Original is SOURCE authority for a new role-specific sheet.
        // It must augment the full sheet contract, never replace the
        // canonical Character/Theme/Art Style/Element requirements.
        val originalAuthority=if(original==null)"" else
            " ORIGINAL-SPECIFIC SOURCE CONSTRAINTS: "+specific(original)
        return PREFIX+study+originalAuthority+(if(evidence.isBlank())"" else
            " UNVERIFIED MACHINE OBSERVATIONS (CHECK AGAINST SOURCE PIXELS): "+evidence)
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
