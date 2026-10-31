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

    fun selectNegative(full:String,owner:String,
        inspect:(String)->TokenCapacity):Selected {
        val core="clothes, props, cutoff limbs, detached anatomy, " +
            "CGI, 3d render, text, environment"
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
