package com.asterion.art.nativeengine

import com.asterion.art.engine.DreamModel
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The majority of imported QNN ZIPs have no advertised resolution manifest.
 * Honor the chosen aspect ratio instead of silently forcing 1024x1024.
 */
object NativeResolutionPolicy {
    private val ratios=mapOf(
        "1:1" to 1.0, "3:4" to 3.0/4.0, "4:3" to 4.0/3.0,
        "9:16" to 9.0/16.0, "2:3" to 2.0/3.0,
        "3:2" to 3.0/2.0,"16:9" to 16.0/9.0,
        "16:45" to 16.0/45.0
    )
    fun choose(model:DreamModel,requested:String):Pair<Int,Int> {
        val ratio=ratios[requested] ?: error("Unsupported image aspect ratio: "+requested)
        val published=model.resolutions.filter{(w,h)->
            w in 256..2048 && h in 256..2048
        }
        if(published.isNotEmpty()) {
            return published.minWith(
                compareBy<Pair<Int,Int>> {
                    abs(it.first.toDouble()/it.second-ratio)
                }.thenByDescending{it.first.toLong()*it.second}
            )
        }
        val edge=model.generationSize.coerceIn(512,1536)
        fun snap(n:Double)=((n/32.0).roundToInt()*32).coerceIn(256,2048)
        return if(ratio>=1.0)edge to snap(edge/ratio)
            else snap(edge*ratio) to edge
    }
}
