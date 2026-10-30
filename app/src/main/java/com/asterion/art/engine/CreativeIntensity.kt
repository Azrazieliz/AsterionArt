package com.asterion.art.engine

import org.json.JSONObject
import java.math.BigDecimal

/**
 * Asterion's independent creative intensity vocabulary (v2).
 *
 * Zero switches a requested effect off. ONE is already PROMINENT; unlike
 * ordinary zero-based strength sliders, 1 never means "barely perceptible".
 * Positive levels are open-ended decimal values. There is no artificial
 * creative cap or narrowing of intent at high intensity.
 *
 * This is a generation-language contract, NOT a claim that an installed
 * diffusion model can satisfy every possible requested value.
 */
object CreativeIntensity {
    const val VERSION=2
    val defaults=mapOf("B" to "1","E" to "1","T" to "1")
    val grades=linkedMapOf(
        0 to "not requested",
        1 to "prominent",
        2 to "strong",
        3 to "very strong",
        4 to "extreme",
        5 to "dominant",
        6 to "overwhelming",
        7 to "massive",
        8 to "pervasive",
        9 to "near maximal",
        10 to "absolute requested maximum"
    )
    fun parse(value:String):BigDecimal {
        val numeric=value.trim().toBigDecimal()
        require(numeric.signum()>=0){"Creative intensity cannot be negative"}
        return numeric.stripTrailingZeros()
    }
    fun label(value:BigDecimal):String {
        if(value.signum()==0)return grades.getValue(0)
        if(value>BigDecimal.TEN) return "unbounded escalation above level 10"
        val lower=value.toInt()
        val upper=if(value==BigDecimal(lower))lower else lower+1
        return if(lower==upper)grades.getValue(lower)
            else grades.getValue(lower)+" → "+grades.getValue(upper)
    }

    data class Axis(val key:String,val name:String,val function:String,
        val parent:String?=null)

    /** Original B/E/T remain the three primary axes; children refine them. */
    val axes=listOf(
        Axis("B","Badassness","Command, capacity, visual power, force and presence"),
        Axis("B.presence","Presence","Spatial and visual dominance of the subject","B"),
        Axis("B.power","Physical power","Visible capability and force","B"),
        Axis("B.kinetic","Kinetic energy","Motion, dynamic force and impact","B"),
        Axis("B.aura","Imposing quality","Composure, authority and visual command","B"),
        Axis("E","Eroticity","Erotic and sensual visual intent as requested"),
        Axis("E.sensuality","Sensuality","Sensory and material qualities","E"),
        Axis("E.expression","Body expression","Pose, gesture, expression and proximity","E"),
        Axis("E.explicitness","Explicitness","Directness of requested depiction","E"),
        Axis("E.interaction","Erotic interaction","Relevant spatial and subject interaction","E"),
        Axis("T","Contextual tension","Anticipation, unstable conditions and consequence"),
        Axis("T.anticipation","Anticipation","Imminence and suspense","T"),
        Axis("T.instability","Instability","Volatile geometry, environment and movement","T"),
        Axis("T.pressure","Pressure","Physical, environmental and dramatic constraint","T"),
        Axis("T.stakes","Consequences","Consequentiality of depicted action","T"),
        Axis("morphology","Morphological complexity","Structural anatomy, geometry and appendages"),
        Axis("transformation","Transformation magnitude","Degree of physical change where method permits"),
        Axis("composition","Composition complexity","Depth, visual hierarchy and staging"),
        Axis("environment","Environmental intensity","Atmosphere and environmental visual activity"),
        Axis("materials","Material richness","Surface differentiation and physical material behavior"),
        Axis("density","Detail density","Local complexity and visual informational richness"),
        Axis("interaction","Interaction complexity","Physical relationships among subjects and objects"),
        Axis("variation","Variation magnitude","Distance between independent alternatives")
    )
    private val catalog=axes.associateBy{it.key}
    fun description(parameters:Map<String,String>):String {
        if(parameters.isEmpty())return ""
        return buildString {
            append("ASTERION CREATIVE INTENSITY v2: 0=not requested; ")
            append("1=already prominent; 2=strong; 3=very strong; ")
            append("4=extreme; 5=dominant; 6=overwhelming; 7=massive; ")
            append("8=pervasive; 9=near maximal; 10=absolute requested maximum; ")
            append("values above 10 request further escalation without an ")
            append("artificial top-end clamp. Preserve exact requested values.\n")
            parameters.forEach{(key,value)->
                val axis=catalog[key]
                val normalized=parse(value)
                append(axis?.name?:key)
                axis?.parent?.let{append(" [").append(it).append(" subaxis]")}
                append(": ").append(normalized.toPlainString())
                append(" (").append(label(normalized)).append(")")
                axis?.let{append("; effect: ").append(it.function)}
                append("\n")
            }
        }
    }
    fun validate(values:Map<String,String>) {
        values.forEach{(key,value)->
            require(key.isNotBlank()&&key.length<=100)
            parse(value)
        }
    }
    fun json(values:Map<String,String>):JSONObject=JSONObject().apply {
        put("_version",VERSION)
        values.forEach{(key,value)->put(key,parse(value).toPlainString())}
    }
    fun fromJson(json:JSONObject?):Map<String,String> {
        if(json==null)return emptyMap()
        return json.keys().asSequence().filter{it!="_version"}
            .associateWith{parse(json.getString(it)).toPlainString()}
    }
}
