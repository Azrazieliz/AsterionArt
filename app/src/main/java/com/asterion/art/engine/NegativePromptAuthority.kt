package com.asterion.art.engine

/**
 * Preserves independent negative-prompt authorities without rephrasing,
 * silently dropping a source, or normalising user-authored semantics.
 * The native encoder applies its measured capacity to the FULL result.
 */
object NegativePromptAuthority {
    fun merge(vararg inputs:String?):String {
        val ordered=LinkedHashSet<String>()
        inputs.forEach { source ->
            source?.trim()?.takeIf { it.isNotEmpty() }?.let(ordered::add)
        }
        return ordered.joinToString(", ")
    }

    fun merge(inputs:Iterable<String?>):String =
        merge(*inputs.toList().toTypedArray())
}
