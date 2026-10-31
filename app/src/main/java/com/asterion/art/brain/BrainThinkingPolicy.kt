package com.asterion.art.brain

/** Qwen3 has a thinking/nonthinking hybrid chat template.
 * For production design records, request direct output rather than spending
 * the token budget on hidden reasoning before the first usable JSON field.
 * This is a Qwen3 instruction only, not model fine-tuning or a context hack.
 */
object BrainThinkingPolicy {
    fun system(modelId:String,source:String):String =
        if(modelId.startsWith("qwen3_")) source+"\n/no_think" else source
}
