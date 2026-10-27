package com.asterion.art.methodology

import com.asterion.art.engine.ConceptDefinition
import java.util.Random

/** Only Phase 1. Every sheet contains five independent clinical anatomical designs. */
object CharacterPhaseOne {
    const val PER_SHEET=5
    const val DEFAULT_SHEETS=3
    const val MAX_SHEETS=30
    const val MAX_SLOTS=8
    const val NEGATIVE=
        "clothes, garment, lingerie, underwear, uniform, fabric underlayer, " +
        "armor, decorative props, environment, fashion poses, three-quarter view, " +
        "cropped limbs, detached appendages, duplicate body templates, " +
        "redesigned rear view, CGI, 3d rendering, auxiliary infographic panels"
    const val DRAWING=
        "Premium native hand-illustrated two-dimensional anime/manhwa style; " +
        "exact silhouette contours, grouped hard and soft illustration shadows, " +
        "purpose-designed facial planes and eyes, distinctive sculpted hair masses, " +
        "material-specific drawn highlight and edge treatment, independently " +
        "developed palettes and controlled value hierarchies. " +
        "Full concept integration must remain at major or strong meso scale. " +
        "Simple strong macro silhouette precedes intricate subordinated microdetail, " +
        "with quiet regions, coherent form, no photographic or 3D simulation."
    const val PRESENTATION=
        "A single exact 16:9 continuous white analytical anatomical reference sheet " +
        "with five non-overlapping left-to-right character regions. Every identity " +
        "has exactly three complete uncropped full-body views: one large true-front " +
        "neutral standing, one smaller exactly 90-degree side profile, one smaller " +
        "direct-rear view. All three show exactly the SAME frozen character, anatomy, " +
        "materials, proportions and rooted appendages. No accessory panels or captions. " +
        "Clinical unclothed female anatomical studies, with deliberate body proportions, " +
        "skeletal, joint and appendage support, fully visible body substrates; " +
        "no clothing, fashion layers, concealment garments or artificial coverage. " +
        "Intrinsically developed scales, fur, skin, minerals, membranes or other " +
        "concept-derived body matter remain anatomy rather than clothing. " +
        "No erotic posing or scene. White neutral field, strictly balanced standing."
    const val AUTHORING=
        "Design one completely original premium female anime/manhwa character from " +
        "the strongest coherent physical embodiment of every resolved primary concept. " +
        "No generic body plus decorative elements, label, glow, costume or skin recolor. " +
        "All primary concepts construct and mutually transform body substrate, " +
        "topology, silhouette, skeleton, proportion, face, eyes, hair, materials, " +
        "cavities or appendages. Every pair produces a consequential shared physical " +
        "structure; when three or more concepts are present, the complete set has " +
        "a consequential higher-order shared fusion structure. Preserve complete " +
        "recognizable concept vocabulary, clearly embodied at major or strong " +
        "meso scale, without obligatory saturation of every body region. " +
        "Independently author anatomy, meaningful asymmetry, face, hair, color, " +
        "materials and flagship identity; resolve appendage roots and joint load " +
        "paths. Preserve feminine human-character readability even through radical " +
        "concept anatomy. Finalize aesthetic hierarchy, taste and refinements before " +
        "image generation. Clinical unclothed reference views, no Outfit/Weapon " +
        "phase, no Design Direction, no supplementary sub-concepts."
    fun count(sheets:Int):Int {
        require(sheets in 1..MAX_SHEETS){"Sheets must be 1.."+MAX_SHEETS}
        return sheets*PER_SHEET
    }
    fun label(number:Int):String {
        require(number>=1)
        return "D"+number.toString().padStart(3,'0')
    }
    fun validateSlots(slots:List<String>) {
        require(slots.size in 2..MAX_SLOTS){"Choose 2–"+MAX_SLOTS+" primary slots"}
        require(slots.none{it.trim().isBlank()}){"Empty concept slot"}
        val fixed=slots.filter{raw->
            val slot=raw.trim()
            !slot.equals("X",true) &&
                !Regex("^X\\{(.{1,140})\\}$",RegexOption.IGNORE_CASE).matches(slot)
        }
        require(fixed.map{it.trim().lowercase()}.distinct().size==fixed.size) {
            "Duplicate fixed primary concept"
        }
    }
}
data class PrimaryConcept(
    val originalSlot:String,val name:String,
    val canonicalId:String?,val provenance:String
)
data class CharacterSelection(val number:Int,val concepts:List<PrimaryConcept>) {
    val id:String get()=CharacterPhaseOne.label(number)
    val names:List<String> get()=concepts.map{it.name}
}
object CharacterPrimaryResolver {
    private val wildcard=Regex("^X\\{(.{1,140})\\}$",RegexOption.IGNORE_CASE)
    fun resolve(slots:List<String>,sheets:Int,
                candidates:List<ConceptDefinition>,random:Random):List<CharacterSelection> {
        CharacterPhaseOne.validateSlots(slots)
        val total=CharacterPhaseOne.count(sheets)
        val selectable=candidates.filter{
            it.id.matches(Regex("SC\\d{3}"))&&it.name.isNotBlank()
        }.distinctBy{it.id}.sortedBy{it.id}
        require(selectable.isNotEmpty()){"Canonical SC selector leaves unavailable"}
        val byName=selectable.associateBy{it.name.trim().lowercase()}
        val byId=selectable.associateBy{it.id.uppercase()}
        val reserved=slots.mapNotNull{raw->
            val slot=raw.trim()
            if(slot.equals("X",true)||wildcard.matchEntire(slot)!=null)null
            else (byId[slot.uppercase()]?:byName[slot.lowercase()])
                ?.name?:slot
        }.map{it.lowercase()}.toSet()
        return (1..total).map{number->
            val resolved=mutableListOf<PrimaryConcept>()
            for(raw in slots) {
                val slot=raw.trim()
                val query=wildcard.matchEntire(slot)?.groupValues?.get(1)?.trim()
                if(slot.equals("X",true)||query!=null) {
                    val pool=selectable.filter{ entry ->
                        val matches=query==null||entry.id.contains(query,true)||
                            entry.name.contains(query,true)||
                            entry.core.contains(query,true)||
                            entry.materialization.contains(query,true)||
                            entry.fullRecord.contains(query,true)
                        matches && entry.name.lowercase() !in reserved &&
                            resolved.none{it.name.equals(entry.name,true)}
                    }
                    require(pool.isNotEmpty()){
                        "No unused canonical leaf satisfies wildcard "+slot
                    }
                    val chosen=pool[random.nextInt(pool.size)]
                    resolved+=PrimaryConcept(slot,chosen.name,chosen.id,
                        if(query==null)"uniform_X" else "uniform_constrained_X")
                }else {
                    val known=byId[slot.uppercase()]?:byName[slot.lowercase()]
                    require(!slot.contains('{')&&!slot.contains('}')) {
                        "Invalid wildcard or literal: "+slot
                    }
                    val selected=if(known==null)
                        PrimaryConcept(slot,slot,null,"fixed_temporary_literal")
                    else PrimaryConcept(slot,known.name,known.id,"fixed_canonical")
                    require(resolved.none{it.name.equals(selected.name,true)}) {
                        "Duplicate resolved primary for "+CharacterPhaseOne.label(number)
                    }
                    resolved+=selected
                }
            }
            CharacterSelection(number,resolved)
        }
    }
}
data class PairwisePhysicalFusion(val first:String,val second:String,
    val mechanism:String)

data class FrozenCharacter(
    val number:Int,
    val primary:List<PrimaryConcept>,
    val anatomy:String,
    val faceEyesHair:String,
    val topology:String,
    val fusion:String,
    val materials:String,
    val differentiation:String,
    val renderDescription:String,
    val auditEvidence:String,
    val pairwise:List<PairwisePhysicalFusion> = emptyList(),
    val higherOrder:String = ""
) {
    val id:String get()=CharacterPhaseOne.label(number)
    fun validate() {
        require(number>=1&&primary.size>=2)
        val fields=mapOf("anatomy" to anatomy,"face" to faceEyesHair,
            "topology" to topology,"fusion" to fusion,"materials" to materials,
            "differentiation" to differentiation,"render" to renderDescription,
            "audit" to auditEvidence)
        fields.forEach{(key,value)->
            require(value.trim().length>=60){id+": unresolved "+key}
        }
        require(renderDescription.length>=600){
            id+": insufficient anatomically complete renderer description"
        }
        val requiredPairs=primary.flatMapIndexed{index,first->
            primary.drop(index+1).map{second->first.name to second.name}
        }
        require(pairwise.size==requiredPairs.size){
            id+": missing full pairwise concept-interaction matrix"
        }
        requiredPairs.forEach{(first,second)->
            require(pairwise.any{pair->
                ((pair.first.equals(first,true)&&pair.second.equals(second,true))||
                 (pair.first.equals(second,true)&&pair.second.equals(first,true)))&&
                    pair.mechanism.trim().length>=65
            }){id+": unresolved shared physical mechanism for "+first+" and "+second}
        }
        if(primary.size>=3)require(higherOrder.trim().length>=110){
            id+": missing consequential complete-set higher-order fusion mechanism"
        }
        val evidence=(anatomy+" "+faceEyesHair+" "+topology+" "+fusion+" "+
            materials+" "+renderDescription).lowercase()
        require(primary.all{evidence.contains(it.name.lowercase())}) {
            id+": missing visible-concept trace"
        }
    }
    fun signature():String=(
        anatomy+" "+faceEyesHair+" "+topology+" "+fusion+" "+materials)
        .lowercase().replace(Regex("\\s+")," ").trim()
}
object CharacterSheetCompiler {
    /** Complete character-specific visual state for controlled Alter/Fuse revisions. */
    fun individual(design:FrozenCharacter):String {
        design.validate()
        return buildString {
            append("One self-contained 9:16 clinical anatomical reference of a ")
            append("complete unclothed original female anime/manhwa character; ")
            append("dominant neutral full-body exact true-front and smaller exact ")
            append("90-degree profile and direct rear of identical frozen anatomy. ")
            append("Pure white field, no clothing or reference underlayer.\n")
            append(CharacterPhaseOne.DRAWING).append("\n")
            append("FROZEN CHARACTER ").append(design.id).append("\n")
            append("ANATOMY: ").append(design.anatomy).append("\n")
            append("FACE / EYES / HAIR: ").append(design.faceEyesHair).append("\n")
            append("TOPOLOGY AND APPENDAGES: ").append(design.topology).append("\n")
            append("CONCEPT FUSION: ").append(design.fusion).append("\n")
            design.pairwise.forEach{physical->
                append("JOINT PHYSICAL STRUCTURE: ").append(physical.mechanism)
                    .append("\n")
            }
            if(design.higherOrder.isNotBlank())
                append("HIGHER-ORDER COMPLETE-SET STRUCTURE: ")
                    .append(design.higherOrder).append("\n")
            append("MATERIAL AND PALETTE: ").append(design.materials).append("\n")
            append("FINAL VISUAL CHARACTER: ").append(design.renderDescription)
        }
    }
    fun audit(roster:List<FrozenCharacter>,sheets:Int):List<String> {
        require(roster.size==CharacterPhaseOne.count(sheets))
        val errors=mutableListOf<String>()
        val known=mutableMapOf<String,String>()
        roster.forEachIndexed{index,identity->
            identity.validate()
            if(identity.number!=index+1)errors+="Missing/duplicate character index "+(index+1)
            val key=identity.signature()
            val previous=known[key]
            if(previous!=null)errors+="Duplicated body/fusion architecture "+previous+"/"+identity.id
            known[key]=identity.id
        }
        return errors
    }
    fun sheet(designs:List<FrozenCharacter>,sheet:Int):String {
        require(sheet>=1&&designs.size==CharacterPhaseOne.PER_SHEET)
        designs.forEachIndexed{index,identity->
            require(identity.number==(sheet-1)*CharacterPhaseOne.PER_SHEET+index+1)
            identity.validate()
        }
        return buildString {
            append(CharacterPhaseOne.PRESENTATION).append("\n")
            append(CharacterPhaseOne.DRAWING).append("\n")
            append("FROZEN SELF-CONTAINED ANATOMICAL CHARACTER PAYLOADS:\n")
            designs.forEach{design->
                append("\nIDENTITY ").append(design.id).append("\n")
                append("ANATOMY: ").append(design.anatomy).append("\n")
                append("FACE, EYES AND HAIR: ").append(design.faceEyesHair).append("\n")
                append("ROOTED TOPOLOGY: ").append(design.topology).append("\n")
                append("FUSED BODY CONSTRUCTION: ").append(design.fusion).append("\n")
                design.pairwise.forEach{physical->
                    append("SHARED STRUCTURE OF ").append(physical.first)
                        .append(" AND ").append(physical.second).append(": ")
                        .append(physical.mechanism).append("\n")
                }
                if(design.higherOrder.isNotBlank())
                    append("COMPLETE-SET PHYSICAL FUSION: ")
                        .append(design.higherOrder).append("\n")
                append("ILLUSTRATED MATERIAL ARCHITECTURE: ")
                    .append(design.materials).append("\n")
                append("FINAL SELF-CONTAINED CHARACTER: ")
                    .append(design.renderDescription.trim()).append("\n")
            }
            append("COMPLETE SAME-IDENTITY VIEW LOCK: independently preserve " +
                "every body, attachment root, anatomy, material and physical " +
                "fusion construction across exact front/profile/rear. The five " +
                "dominant full bodies and ten subordinate views remain clinically " +
                "unclothed, analytically neutral, equally high-quality, uncropped.")
        }
    }
}
