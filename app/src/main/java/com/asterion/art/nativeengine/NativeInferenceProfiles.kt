package com.asterion.art.nativeengine

import android.content.Context

/** Runs with the pinned native executable's existing supported flags only.
 * Performance mode is opt-in and never implies increased tokenizer context.
 */
enum class MemoryProfile(val label:String) {
    SAFE("Safe · sequential graphs"), AUTO("Auto · conservative"),
    PERFORMANCE("Performance · persistent graphs")
}

data class NativeMemoryDecision(
    val profile:MemoryProfile,
    val lowRam:Boolean,
    val sequentialDiT:Boolean,
    val reason:String
)

object NativeInferenceProfiles {
    private const val GIB=1024L*1024*1024
    fun decide(backend:String, profile:MemoryProfile,
               totalBytes:Long, availableBytes:Long):NativeMemoryDecision {
        if(backend !in setOf("anima","sdxl")) return NativeMemoryDecision(
            profile,false,false,"This backend uses its own QNN graph strategy")
        val enough=totalBytes>=11*GIB && availableBytes>=5*GIB
        val fast=profile==MemoryProfile.PERFORMANCE && enough
        return NativeMemoryDecision(
            profile,!fast,backend=="anima" && !fast,
            when {
                fast -> "Opt-in persistent graph mode; verify memory and thermal behavior"
                profile==MemoryProfile.PERFORMANCE -> "Insufficient free RAM; safe graph loading retained"
                else -> "Safe bounded-memory native inference"
            })
    }
    fun get(context:Context):MemoryProfile {
        val prefs=context.getSharedPreferences("asterion_native_execution_v1",Context.MODE_PRIVATE)
        return runCatching {
            MemoryProfile.valueOf(prefs.getString("memory_profile","AUTO") ?: "AUTO")
        }.getOrDefault(MemoryProfile.AUTO)
    }
    fun set(context:Context,profile:MemoryProfile) {
        check(context.getSharedPreferences("asterion_native_execution_v1",Context.MODE_PRIVATE)
            .edit().putString("memory_profile",profile.name).commit())
    }
}
