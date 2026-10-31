package com.asterion.art.methodology

import com.asterion.art.engine.ConceptTaxonomy
import kotlin.random.Random

/**
 * Mode-specific structural conditioning compiler. Source photographs and
 * reference sheets are interpreted separately by the real-pixel vision engine.
 * It never fabricates observations from file paths and never invokes Qwen.
 */
object DeterministicModeTextCompiler {
    private val structure=listOf(
        "axial vertical hierarchy with independent asymmetric secondary masses",
        "diagonal tension and counterweight through physically joined masses",
        "radial organization around a deliberately open central void",
        "interlocking folded volume with continuous connected load paths",
        "branching organic structure with one dominant quiet silhouette"
    )
    fun compile(plan:ModeTextPlan,observations:List<String>,
        taxonomy:ConceptTaxonomy):List<ModeTextDesign> {
        val schema=plan.schema
        val inputs=plan.concepts.map { name ->
            val record=taxonomy.find(name)
            val body=record?.materialization?.replace(Regex("\\s+")," ")?.take(450)
            val core=record?.core?.replace(Regex("\\s+")," ")?.take(200)
            buildString {
                append(name)
                if(!core.isNullOrBlank())append(": ").append(core)
                if(!body.isNullOrBlank())append("; body/material: ").append(body)
            }
        }
        val concepts=inputs.joinToString("; ").ifBlank {
            "No owner-supplied selectable concept; do not invent one"
        }
        val direction=plan.userDirection.ifBlank {
            "No extra owner direction; preserve mode methodology"
        }
        val limits=plan.exclusions.ifBlank{"No additional owner exclusions"}
        val evidence=observations.joinToString(" ") { it.trim() }
        val visibility=if(observations.isEmpty())
            "No reference pixels were supplied; do not assert a specific "+
                "reference identity, unseen body features or unsupported colours."
        else "Actual-pixel observations, not filename inferences: "+
            evidence.take(2300)+
            ". All recognizable source identities, proportions and layouts "+
            "must be preserved in any source-dependent mode."
        val random=Random(plan.id.hashCode()+plan.created)
        val assignments=(0 until schema.designsPerImage).map { ordinal ->
            val pattern=structure[ordinal%structure.size]
            val displaced=random.nextInt(-18,19)
            val emphasis=(ordinal+1)*17
            val identity="Design "+(ordinal+1)+" · "+schema.title+
                " · "+pattern.substringBefore(" with ")
            val shared="Mode "+schema.key+". Individual design "+(ordinal+1)+
                " of "+schema.designsPerImage+". Governing structural axis: "+
                pattern+"; relative local asymmetry "+displaced+
                " degrees and hierarchy contrast "+emphasis+
                " units. Binding owner concepts: "+concepts+
                ". Direction: "+direction+". Exclusions: "+limits+
                ". "+visibility
            val fields=schema.fields.associate { field ->
                field.key to buildString {
                    append(field.title).append(". ")
                    append(shared).append(". ")
                    when(field.key.lowercase()) {
                        "body","anatomy","geometry","silhouette","mutation",
                        "state","face","locomotion","fusion" ->
                            append("Physically construct the ")
                                .append(field.title.lowercase())
                                .append(" through continuous connected load paths, ")
                                .append("specific material attachment roots, ")
                                .append("visible structural transitions and ")
                                .append("coherent front/side/rear silhouettes. ")
                        "materials","coverage","clothing","outfit","detail",
                        "grip","line","shading","style" ->
                            append("Specify construction boundaries, layered ")
                                .append("edge treatment, attachment and ")
                                .append("surface response. Never substitute ")
                                .append("an accessory icon for required structural ")
                                .append("integration. ")
                        "render","details","scene","identity","content","universe",
                        "difference","senses","category","theme" ->
                            append("One visually independent final result, ")
                                .append("with correct anatomical or material ")
                                .append("continuity, readable scene hierarchy ")
                                .append("where this mode permits a scene, ")
                                .append("and no unobserved source invention. ")
                        else -> append("Preserve the exact stated mode-specific ")
                            .append("design field and its structural effect. ")
                    }
                    append("Apply the existing ").append(schema.title)
                    append(" field methodology and approved ")
                    append(schema.imageRatio).append(" output layout.")
                }
            }
            ModeTextDesign(ordinal+1,identity,fields)
        }
        schema.validate(assignments)
        return assignments
    }
}
