package com.asterion.art.brain

import android.content.Context
import com.asterion.art.engine.ConceptDefinition
import com.asterion.art.engine.ConceptTaxonomy
import com.asterion.art.engine.CanonicalSelectorGraph
import com.asterion.art.methodology.*
import java.util.Random

/**
 * CPU-only, seeded, canonical-first creative specification compiler.
 * This is not a substitute diffusion image model or a claim of visual quality:
 * it creates a complete mechanically checkable native conditioning contract
 * WITHOUT spending minutes decoding eight-field Qwen JSON per character.
 */
object CanonicalAnatomyCompiler {
    private data class Evidence(val primary:PrimaryConcept,
        val core:String,val body:String,val fusion:String,val boundary:String)
    private fun clean(raw:String,max:Int):String {
        val prose=raw.replace(Regex("(?m)^#{1,6}\\s+[^\\n]+")," ")
            .replace(Regex("(?m)^[-*]\\s+")," ")
            .replace(Regex("[*_`\\r\\n]+")," ")
            .replace(Regex("\\s+")," ").trim()
        return prose.take(max).trim().ifBlank{"No extra canonical physical detail is specified"}
    }
    private fun source(concept:PrimaryConcept,lookup:(String)->ConceptDefinition?):Evidence {
        val record=concept.canonicalId?.let(lookup)?:lookup(concept.name)
        if(record==null) {
            val wording=concept.name.trim()
            return Evidence(concept,
                "Owner-defined literal "+wording+"; interpret only its supplied meaning",
                "Materialize "+wording+" as the body's own structural matter, " +
                    "never as an external icon or attached decoration",
                "Require "+wording+" to reciprocally change every companion " +
                    "concept's load paths and tissue boundaries",
                "Literal owner wording is authoritative; do not assign an invented canonical ID")
        }
        return Evidence(concept,clean(record.core,430),
            clean(record.materialization.ifBlank{record.core},520),
            clean(record.symbiosis.ifBlank{record.materialization},420),
            clean(record.interpretationBoundary,250))
    }
    private fun proportion(random:Random,low:Int,high:Int):Int =
        low+random.nextInt(high-low+1)

    /**
     * Explicit concept-grounded anatomy and interaction graph, not a chosen
     * mannequin, body-template, decorative prompt or LLM-generated filler.
     * Geometry parameterization is deterministic per seed and identity.
     * The image model remains responsible for actually synthesizing the art.
     */
    fun compile(selection:CharacterSelection,seed:Long,
        lookup:(String)->ConceptDefinition?):FrozenCharacter {
        require(selection.concepts.size>=2)
        val evidence=selection.concepts.map{source(it,lookup)}
        val mixed=seed xor (selection.number.toLong()*0x4F1BBCDCBFA54001L) xor
            selection.names.joinToString("|").hashCode().toLong()
        val random=Random(mixed)
        val height=proportion(random,151,194)
        val shoulders=proportion(random,76,108)
        val hips=proportion(random,85,123)
        val leg=proportion(random,44,58)
        val faceWidth=proportion(random,74,111)
        val eyeTilt=proportion(random,-13,18)
        val hairMass=proportion(random,16,48)
        val axis=proportion(random,-12,12)
        val bodyPlaces=listOf(
            "skull and cranial vault", "cervical and shoulder support",
            "sternum and anterior ribcage", "spinal and dorsal load path",
            "pelvis and hip girdle", "forearms and articulated hands",
            "thighs, shins and weight-bearing feet",
            "face, ocular sockets and hair-root topology")
        val order=bodyPlaces.toMutableList().apply {
            java.util.Collections.shuffle(this,random)
        }
        val anchors=evidence.mapIndexed { index,e ->
            val first=order[index%order.size]
            val second=order[(index+3)%order.size]
            e.primary.name+" owns "+first+" and modifies "+second+
                " through the following canonical physical vocabulary: "+e.body+
                ". Keep source meaning: "+e.core+"."
        }
        val pairwise=evidence.flatMapIndexed{index,a->
            evidence.drop(index+1).map{b->
                val load=order[(index+evidence.indexOf(b)+1)%order.size]
                PairwisePhysicalFusion(a.primary.name,b.primary.name,
                    "At the shared "+load+", "+a.primary.name+" changes how "+
                    b.primary.name+" is supported, rooted and distributed through "+
                    "the same continuous tissue or structural substrate; "+
                    b.primary.name+" reciprocally redirects the "+
                    a.primary.name+" growth boundaries and mechanical load "+
                    "transfer. First canonical interaction: "+a.fusion+
                    ". Second canonical interaction: "+b.fusion+
                    ". This joint anatomy is visible from front, side and back.")
            }
        }
        val higher=if(evidence.size<3)"" else
            "All "+selection.names.joinToString(", ")+" are required simultaneously "+
            "to construct one shared continuous "+order.last()+"-to-"+order.first()+
            " anatomical load route. Each source changes the material transition "+
            "and branching of the others, not merely its colour or a separate ornament. "+
            "The structure must collapse or change shape if any one concept is removed. "+
            evidence.joinToString(" "){it.primary.name+": "+it.fusion.take(160)}
        val names=selection.names.joinToString(" × ")
        val anatomy="Original female-readable anatomical identity "+selection.id+
            " physically embodies "+names+". Structural targets: "+
            "standing height "+height+" cm design scale; shoulder-to-neutral "+
            "reference "+shoulders+"%, hip-to-reference "+hips+
            "%, relative leg contribution "+leg+"%, major silhouette axis "+
            axis+" degrees. These are design proportions, not copied templates. "+
            anchors.joinToString(" ")
        val face="Independently construct the face of "+selection.id+
            " from "+names+": cranial width "+faceWidth+
            "% of local style baseline, eye outer-corner tilt "+eyeTilt+
            " degrees, directional hair-volume displacement "+hairMass+
            "% of head width; continuously integrate concept-derived materials "+
            "through bone, brows, eye sockets, iris architecture, cheek planes, "+
            "hair roots and hair masses. "+
            evidence.joinToString(" "){it.primary.name+" facial influence: "+it.core.take(160)}
        val topology="Body-owned rooted topology for "+names+
            ", with load paths from axial skeleton to appendage attachment, "+
            "hands, feet and rear silhouette. "+anchors.joinToString(" ")+
            " Preserve three-view anatomical continuity; no floating decorations."
        val fusion="Reciprocal physical integration rather than adjacent labels: "+
            pairwise.joinToString(" "){it.mechanism}+
            (if(higher.isBlank())"" else " Shared multi-concept architecture: "+higher)
        val materials="Physically differentiated anatomical substrates, not clothing: "+
            evidence.joinToString(" "){e ->
                e.primary.name+" governs material boundaries and surface rendering: "+
                e.body+". Boundary guard: "+e.boundary+"."
            }+" Preserve readable mass and restrained macro-to-micro hierarchy."
        val differentiation="Unique frozen design "+selection.id+"; seed "+mixed+
            " fixes its own silhouette measurements, concept attachment graph, "+
            "facial planes, substrate transitions and higher-order structures. "+
            "Do not reuse another identity's appendage roots, iris design, "+
            "hair profile or same body-matter distribution. Priority concepts: "+names+
            ". Shape anchors: "+order.joinToString(" -> ")+"."
        val render="Render exactly one original, full-length female-readable "+
            "hand-illustrated 2D anime/manhwa anatomical character "+
            selection.id+" built from "+names+
            ". Use the approved true-front, exact profile and direct-rear "+
            "neutral orthographic views of the SAME frozen design on white. "+
            "Do not use outfits, accessory icons, props, labels, scene elements, "+
            "3D CGI or photoreal simulation. "+
            "PERSON-SPECIFIC BODY: "+anatomy+
            " FACE, EYES AND HAIR: "+face+
            " ROOTED THREE-VIEW TOPOLOGY: "+topology+
            " PHYSICAL RECIPROCAL FUSION: "+fusion+
            " INTRINSIC BODY MATERIALS: "+materials+
            " DIFFERENTIATION: "+differentiation+
            " DRAWN FINISH: "+CharacterPhaseOne.DRAWING
        val audit="Canonical selection "+selection.id+": "+
            evidence.joinToString("; "){it.primary.name+" ("+
                (it.primary.canonicalId?:"owner literal")+")"}+
            ". Validated pairwise mechanisms: "+pairwise.size+
            ". Frozen geometry seed: "+mixed+
            ". This contract is an unrendered deterministic specification; "+
            "visual originality and reference-sheet accuracy require inspection."
        return FrozenCharacter(selection.number,selection.concepts,anatomy,face,
            topology,fusion,materials,differentiation,render,audit,pairwise,higher)
            .also{it.validate()}
    }
}

class DeterministicCharacterResolver(context:Context) {
    private val app=context.applicationContext
    private val taxonomy=ConceptTaxonomy(app)
    private val store=CharacterPlanStore(app)
    fun resolve(id:String,progress:(Int,Int,String)->Unit):CharacterPhaseOnePlan {
        val plan=store.load(id)
        require(plan.status=="DESIGNING")
        val selections=CharacterPrimaryResolver.resolve(
            plan.slots,plan.sheets,taxonomy.list(),Random(plan.seed),
            CanonicalSelectorGraph.load(app))
        val frozen=plan.frozen.toMutableList()
        require(frozen.size<=selections.size)
        frozen.forEachIndexed{i,c->
            require(c.number==selections[i].number &&
                c.primary==selections[i].concepts){
                "Saved character selection changed; preserve original plan data"
            }
            c.validate()
        }
        for(selection in selections.drop(frozen.size)) {
            progress(frozen.size,plan.total,
                "Compiling canonical anatomical design "+selection.id)
            val built=CanonicalAnatomyCompiler.compile(selection,plan.seed,taxonomy::find)
            require(frozen.none{it.signature()==built.signature()}){
                "Deterministic geometry collision at "+selection.id
            }
            frozen+=built
            store.progress(id,selections,frozen)
        }
        progress(frozen.size,plan.total,"Checking all original character specifications")
        require(CharacterSheetCompiler.audit(frozen,plan.sheets).isEmpty())
        return store.certify(id,"deterministic-canonical-v1",selections,frozen)
    }
}
