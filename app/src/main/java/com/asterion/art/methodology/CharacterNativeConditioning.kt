package com.asterion.art.methodology

import com.asterion.art.engine.ConceptDefinition
import com.asterion.art.engine.TokenCapacity

/**
 * Native image text encoders have a fixed *measured* token capacity.
 * Preserve the full canonical Phase-1 plan in storage; produce a separate,
 * finite model-facing input, chosen ONLY after the real native tokenizer
 * verifies every required identity and concept. No silent truncation.
 */
object CharacterNativeConditioning {
    data class Selected(val prompt:String,val counted:TokenCapacity,
        val variant:String)
    private fun plain(text:String,limit:Int):String =
        text.replace(Regex("(?m)^#{1,6}\\s+[^\\n]+")," ")
            .replace(Regex("(?m)^[-*]\\s+")," ")
            .replace(Regex("\\*{1,2}|\\r|\\n")," ")
            .replace(Regex("\\s+")," ").trim().take(limit)
            .trimEnd(' ', ',', ';', ':', '.')
    private fun firstSentence(text:String,limit:Int):String {
        val s=plain(text,limit*3)
        return (s.substringBefore(". ").take(limit)).trimEnd(' ','.')
    }
    private fun conceptFragments(design:FrozenCharacter,
        find:(String)->ConceptDefinition?):List<String> =
        design.primary.map { primary ->
            val concept=primary.canonicalId?.let(find)?:find(primary.name)
            val material=concept?.materialization.orEmpty()
            val core=concept?.core.orEmpty()
            val physical=material.substringAfter("**Body-system possibilities.**",
                material).substringBefore("**Materialization modes.**")
            val materialHint=firstSentence(physical,200).ifBlank {
                firstSentence(core,160)
            }
            primary.name+ if(materialHint.isBlank())"" else ": "+materialHint
        }

    fun variants(design:FrozenCharacter,
        find:(String)->ConceptDefinition?):List<Pair<String,String>> {
        design.validate()
        val fragments=conceptFragments(design,find)
        val names=design.primary.joinToString(", "){it.name}
        val identity="Original adult female anime/manhwa character: "+names+". "
        val views="One full-body front, strict side profile and identical rear view, white background. "
        val safeguards="Body-owned fused anatomy, rooted appendages, distinct face and hair, no outfit or props."
        val contextual="Physical concept construction: "+
            fragments.joinToString("; ")+". "+
            "Each changes the other through shared skeletal supports and "+
            "skin/material transitions. Cohesive silhouette, drawn 2D finish."
        val elaborate=identity+views+contextual+" "+
            firstSentence(design.fusion,260)+". "+
            "Material architecture: "+firstSentence(design.materials,220)+". "+
            safeguards
        val grounded=identity+views+contextual+" "+safeguards
        val short="Adult female 2D anime reference: "+names+". "+
            "Full-body front, 90-degree side, rear; same anatomy. "+
            "Mutually fused body materials, concept-built silhouette, "+
            "distinct face, hair and rooted limbs. White. No clothes or props."
        val minimum="Anime anatomical character, "+names+
            "; female full body, front, side, back, same form; "+
            "visible fused anatomy; white background, no clothes."
        return listOf("canonical-detailed" to elaborate,
            "canonical-grounded" to grounded,
            "native-concise" to short,
            "native-minimum" to minimum)
    }

    /**
     * One diffusion request = ONE anatomical figure in ONE orthographic view.
     * The prior three-view text prompt made native diffusion draw duplicates
     * and overlapping profile figures in a narrow 16:45 portrait.
     * The reference image for later views is supplied by the renderer.
     */
    fun selectView(design:FrozenCharacter,
        view:CharacterViewComposer.View,
        find:(String)->ConceptDefinition?,
        inspect:(String)->TokenCapacity):Selected {
        design.validate()
        val primaries=design.primary
        val names=primaries.joinToString(", "){it.name}
        val physical=primaries.map { primary ->
            val concept=primary.canonicalId?.let(find)?:find(primary.name)
            val material=concept?.materialization.orEmpty()
            val span=material.substringAfter("**Body-system possibilities.**",
                material).substringBefore("**Materialization modes.**")
            primary.name+": "+firstSentence(span,100).ifBlank{
                firstSentence(concept?.core.orEmpty(),80)
            }
        }
        val angle=when(view) {
            CharacterViewComposer.View.FRONT ->
                "exact front orthographic, both eyes and shoulders facing viewer"
            CharacterViewComposer.View.PROFILE ->
                "exact LEFT 90-degree profile, one visible eye, face looking left"
            CharacterViewComposer.View.REAR ->
                "direct back orthographic, back of head, spine, heels, no face"
        }
        val identity=if(view==CharacterViewComposer.View.FRONT)
            "Invent one singular original character"
        else "The same woman as the supplied front reference: identical hair, "+
            "skin, anatomy, material boundaries and body proportions"
        val required="ONE solitary full-length adult female anime anatomical study; "+
            angle+", standing neutral, feet visible, white backdrop. "
        val full=identity+". "+required+
            "Intrinsic fused body concepts "+names+". "+
            physical.joinToString("; ")+
            ". Anatomical material transitions rather than costume. "+
            "Hand-drawn 2D contours, purpose-built original silhouette. "+
            "No other people, no duplicated heads, no extra figures."
        val moderate=required+" Same unique individual; "+
            names+" as physically fused body anatomy. "+
            physical.joinToString("; ")+
            ". 2D anime lineart. ONE figure only."
        val groundedCues=primaries.mapIndexed { index,primary ->
            val record=primary.canonicalId?.let(find)?:find(primary.name)
            val modes=record?.materialization.orEmpty()
                .substringAfter("**Materialization modes.**","")
                .substringBefore("**Body-regime range.**")
                .split(';').map{it.replace(Regex("\\s+")," ").trim()
                    .trimEnd('.',',')}
                .filter{it.length in 6..85}
            val cue=modes.getOrNull((design.number+index*2)%modes.size.coerceAtLeast(1))
            if(cue==null)primary.name else primary.name+": "+cue
        }
        val concise=required+" "+
            groundedCues.joinToString("; ")+
            ". Physical fused body matter, one individual, hand-drawn anime."
        val short=required+" "+names+
            " fused into original skin, body and limbs. "+
            "One character only, no duplicate body, drawn anime."
        val minimum="solo full-body adult woman, "+angle+
            ", anime lineart, "+names+
            ", fused anatomical materials, neutral pose, white, one figure."
        val reports=listOf("source-grounded-full" to full,
            "source-grounded" to moderate,
            "source-specific-compact" to concise,
            "native-compact" to short,
            "native-minimum" to minimum).map {(variant,prompt)->
            Triple(variant,prompt,inspect(prompt))
        }
        val chosen=reports.firstOrNull{it.third.fits}
            ?:throw IllegalArgumentException(
                "Cannot encode one character in "+view.label+
                " view with this image checkpoint: "+
                reports.joinToString("; "){
                    it.first+"="+it.third.count+"/"+it.third.limit
                })
        return Selected(chosen.second,chosen.third,chosen.first)
    }

    fun selectNegative(full:String,owner:String,
        inspect:(String)->TokenCapacity):Selected {
        val core="two people, duplicate body, duplicated head, face-to-face, " +
            "mirrored figures, cropped feet, detached anatomy, clothes, " +
            "props, CGI, text, background scene"
        val required=owner.trim()
        val candidates=listOf(
            "full-negative" to full.trim(),
            "essential-negative" to (core+
                if(required.isBlank())"" else ", "+required)
        ).distinctBy{it.second}
        val reports=candidates.map{(variant,prompt)->
            Triple(variant,prompt,inspect(prompt))
        }
        val selected=reports.firstOrNull{it.third.fits}
            ?:throw IllegalArgumentException(
                "Native negative conditioning cannot fit owner exclusions. "+
                reports.joinToString("; "){
                    it.first+"="+it.third.count+"/"+it.third.limit
                })
        return Selected(selected.second,selected.third,selected.first)
    }

    fun select(design:FrozenCharacter,
        find:(String)->ConceptDefinition?,
        inspect:(String)->TokenCapacity):Selected {
        val candidates=variants(design,find)
        val measured=candidates.map{(name,prompt)->
            Triple(name,prompt,inspect(prompt))
        }
        val chosen=measured.firstOrNull{it.third.fits}
        if(chosen!=null) return Selected(chosen.second,chosen.third,chosen.first)
        val details=measured.joinToString("; "){
            it.first+"="+it.third.count+"/"+it.third.limit
        }
        throw IllegalArgumentException(
            "The selected native image tokenizer cannot encode the required "+
            "character concepts plus front/side/rear views. "+
            "Measured capacities: "+details+
            ". Nothing was silently removed from the frozen plan. "+
            "Use a compatible longer-context image model.")
    }
}
