package com.asterion.art.render

import com.asterion.art.model.CreationMode
import com.asterion.art.model.SheetRole

/**
 * Deliberately separate visual construction directions. No repeated source
 * crops and no requirement for a second user photograph: every panel is a
 * NEW native diffusion call conditioned on the same one-image identity.
 */
data class NeuralSheetPanel(val key:String,val title:String,val instruction:String)

object NeuralSheetStoryboard {
    fun panels(role:SheetRole,original:CreationMode?):List<NeuralSheetPanel> {
        fun list(vararg entries:Triple<String,String,String>):List<NeuralSheetPanel> =
            entries.map { NeuralSheetPanel(it.first,it.second,it.third) }
        val base=when(original) {
            CreationMode.WEAPON ->list(
                Triple("hero","SIGNATURE WEAPON","one complete unique canonical weapon, hero isolated view"),
                Triple("front","FRONT CONSTRUCTION","same weapon, exact front elevation, functional weapon geometry"),
                Triple("side","SIDE PROFILE","same weapon, strict 90 degree edge or mechanism side"),
                Triple("rear","REAR CONSTRUCTION","same weapon, precise direct rear elevation"),
                Triple("grip","GRIP AND SCALE","same weapon hilt, handle and hand placement at realistic scale"),
                Triple("mechanics","JOINTS","same weapon fastenings, mechanical joints and assembly"),
                Triple("materials","MATERIALS","same weapon blade, surface, metal, texture, and ornament"),
                Triple("sections","CROSS SECTIONS","same weapon structural cross sections and connection points"),
                Triple("palette","COLOR AND DETAIL","same weapon accurate color and material swatches"))
            CreationMode.OUTFIT ->list(
                Triple("hero","SELECTED OUTFIT","same canonical host wearing one complete selected outfit"),
                Triple("front","FRONT GARMENTS","same exact outfit and body, clothing front elevation"),
                Triple("side","SIDE GARMENTS","same outfit and body, strict 90 degree side clothing"),
                Triple("rear","BACK CONSTRUCTION","same outfit and body, direct rear clothing and closures"),
                Triple("collar","NECK AND COLLAR","same outfit collar, fasteners, neckline and layers"),
                Triple("sleeves","SLEEVES","same outfit sleeves, seams, wrist details and proportions"),
                Triple("materials","FABRICS","same outfit textile weight, stitching and material close details"),
                Triple("layers","LAYER ASSEMBLY","same garments layered correctly with structural construction"),
                Triple("palette","COLOR SWATCHES","same outfit matching colors, accessories and fabric detail"))
            CreationMode.CREATURE ->list(
                Triple("hero","CREATURE HERO","same fully formed original organism, coherent full-body view"),
                Triple("front","FRONT ANATOMY","same species front elevation, limbs and sensory structures"),
                Triple("side","TRUE PROFILE","same species 90 degree side anatomy and locomotion"),
                Triple("rear","DIRECT REAR","same species rear anatomy and appendage connections"),
                Triple("head","HEAD DETAILS","same organism face, eyes, jaws and sensory architecture"),
                Triple("locomotion","MOVEMENT","same organism natural functional locomotion, skeletal loading"),
                Triple("limbs","JOINT STRUCTURE","same organism anatomically supported limbs and attachment roots"),
                Triple("materials","SURFACE","same organism skin, plating, fur and functional material details"),
                Triple("palette","BIOLOGICAL SYSTEMS","same organism coloration and functional adaptations"))
            else ->when(role) {
                SheetRole.CHARACTER ->list(
                    Triple("hero","CANONICAL CHARACTER","same source identity, exceptional full-body canonical female illustration"),
                    Triple("front","FRONT ANATOMY","same character anatomical front, standing neutral, full body"),
                    Triple("side","90 DEGREE PROFILE","same character exact 90 degree left profile, full body"),
                    Triple("rear","DIRECT REAR","same character strict rear anatomical elevation, full body"),
                    Triple("face","FACE IDENTITY","same face, front, three-quarter and profile identity construction"),
                    Triple("eyes","EYES AND HAIR","same character distinctive eyes, eyebrows, bangs and roots, enlarged"),
                    Triple("outfit","OUTFIT FRONT","same exact outfit front, complete clothing construction"),
                    Triple("outfitrear","OUTFIT REAR","same outfit side and rear, accurate seams and layers"),
                    Triple("materials","PALETTE AND ACCESSORIES","same identity precise hair, skin, textile, accessory material details"))
                SheetRole.ART_STYLE ->list(
                    Triple("hero","STYLE FINGERPRINT","new original example demonstrating only the supplied artistic style"),
                    Triple("lines","LINEWORK","same style stroke shapes, outlines, edges and controlled line variation"),
                    Triple("shading","SHADING","same style lighting transitions, shadows and tonal grammar"),
                    Triple("morphology","MORPHOLOGY","same style facial and anatomical proportion grammar, different neutral subject"),
                    Triple("faces","EYE AND HAIR GRAMMAR","same art style eyes, hair strands and painterly construction"),
                    Triple("materials","MATERIAL RENDERING","same art style textures, cloth, skin, metal and surface treatment"),
                    Triple("palette","COLOR RELATIONS","same style color hierarchy, saturation and value relationships"),
                    Triple("camera","COMPOSITION","same style camera, balance, framing and negative space"),
                    Triple("variations","VALID VARIATIONS","same style controlled variations without source-character copying"))
                SheetRole.THEME ->list(
                    Triple("hero","THEME HERO","new scene showing the full original theme with physical interactions"),
                    Triple("space","ENVIRONMENT","same theme environmental structure and precise spatial relationships"),
                    Triple("pose","POSE SYSTEM","same theme full-body gesture, physical pose and body language"),
                    Triple("expression","EXPRESSION","same theme expressive faces and emotionally consistent acting"),
                    Triple("lighting","LIGHTING","same theme key, fill, shadow and atmospheric light design"),
                    Triple("materials","MATERIAL RESPONSE","same theme materials responding to elements and atmosphere"),
                    Triple("camera","CAMERA","same theme distinct viewpoint, perspective and lens composition"),
                    Triple("hierarchy","VISUAL HIERARCHY","same theme subject dominance, focal planes and visual hierarchy"),
                    Triple("interaction","INTERACTIONS","same theme causal contact between characters, props and setting"))
                SheetRole.ELEMENT ->list(
                    Triple("hero","ELEMENT HERO","one canonical coherent manifestation of the source element"),
                    Triple("structure","STRUCTURE","same element physical structure, joints and geometry"),
                    Triple("side","SIDE VIEW","same element precise 90 degree side construction"),
                    Triple("rear","REAR VIEW","same element rear view with consistent structure"),
                    Triple("material","MATERIAL","same element genuine material surface and microstructure"),
                    Triple("placement","HOST INTEGRATION","same element anchored and integrated with compatible host anatomy"),
                    Triple("lighting","ILLUMINATION","same element interactions with light, shade and surrounding space"),
                    Triple("adaptation","ADAPTATIONS","same element lawful scale and form changes retaining its core identity"),
                    Triple("palette","VARIATIONS","same element accurate palette and bounded structural variations"))
            }
        }
        val prefix=when(original) {
            CreationMode.FEMINISATION -> "Same reconstructed female-first identity, "
            CreationMode.TRANSFORMATION -> "Same source identity with required physical transformation, "
            CreationMode.MUTATION -> "Same source identity with controlled functional mutation, "
            CreationMode.ALTERNATE_UNIVERSE -> "Same recognizable identity adapted to the target world's actual rules, "
            else -> ""
        }
        return base.map { panel ->
            panel.copy(instruction=(
                "Detailed high-quality reference illustration. Plain white analytical "+
                "background, no collage or pasted source pixels. "+prefix+panel.instruction+
                ". Preserve exact source identity and proportions where applicable.").trim())
        }
    }
}
