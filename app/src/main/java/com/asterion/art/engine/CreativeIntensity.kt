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
    // 0 = absent. 1 = already prominent. Positive decimals and arbitrarily
    // large values are accepted without a fixed count of levels or a maximum.
    fun parse(value:String):BigDecimal {
        val numeric=value.trim().toBigDecimal()
        require(numeric.signum()>=0){"Creative intensity cannot be negative"}
        return numeric.stripTrailingZeros()
    }
    fun label(value:BigDecimal):String = when {
        value.signum()==0 -> "off"
        value<BigDecimal.ONE -> "below prominent (non-default)"
        value==BigDecimal.ONE -> "prominent"
        else -> "above prominent; no intensity ceiling"
    }

    data class Axis(val key:String,val name:String,val function:String)
    // ONLY the user's original axes have defined meanings. Other primary
    // axes and B/E/T subaxes are owner-authored, not predeclared here.
    val axes=listOf(
        Axis("B","Badassness","Overall badassness and visual force"),
        Axis("E","Eroticity","Overall erotic visual intensity"),
        Axis("T","Contextual tension","Overall contextual tension")
    )
    private val catalog=axes.associateBy{it.key}
    fun description(parameters:Map<String,String>):String {
        if(parameters.isEmpty())return ""
        return buildString {
            append("ASTERION CREATIVE INTENSITY v2: continuous decimal ")
            append("scale; 0=off, 1=already prominent, every greater value ")
            append("increases intensity with no defined maximum. ")
            append("Every owner-defined parameter keeps its exact number ")
            append("and declared scope without guessed semantics.\\n")
            parameters.forEach{(key,value)->
                val axis=catalog[key]
                val normalized=parse(value)
                append(axis?.name?:key)
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
