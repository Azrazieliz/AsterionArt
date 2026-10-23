package com.asterion.compiler.profiles

import java.util.Locale

/**
 * LoRA routing is deliberately fail-closed. A QNN binary cannot accept an arbitrary
 * safetensors LoRA at generation time; a premerged/converted NPU variant is required.
 * The catalog contains metadata only: never load every LoRA merely because it exists.
 */
data class LoRaAsset(
    val id: String,
    val displayName: String,
    val uri: String,
    val sha256: String,
    val modelFamily: String,
    val triggerWords: List<String>,
    val defaultWeight: Float = 1.0f,
    val allowedCheckpointIds: Set<String> = emptySet(),
    val enabled: Boolean = true,
) {
    init {
        require(id.isNotBlank() && displayName.isNotBlank() && sha256.isNotBlank())
        require(defaultWeight.isFinite() && defaultWeight in -2f..2f)
        require(triggerWords.none { it.isBlank() })
    }
}

data class ConvertedModelVariant(
    val modelId: String,
    val checkpointId: String,
    /** Exact installed conversion fingerprint: LoRA ID to baked-in weight. */
    val loraWeights: Map<String, Float>,
    val ready: Boolean,
)

data class LoRaResolution(
    val triggered: List<LoRaAsset>,
    val selectedModelId: String?,
    val blockers: List<String>,
) {
    val ready: Boolean get() = blockers.isEmpty() && selectedModelId != null
}

object LoRaKeywordRouter {
    // Asterion stores human-readable trigger words; no active LoRA is assumed.
    fun resolve(
        prompt: String,
        checkpoint: CheckpointProfile,
        library: List<LoRaAsset>,
        availableVariants: List<ConvertedModelVariant>,
    ): LoRaResolution {
        val normalizedPrompt = prompt.lowercase(Locale.ROOT)
        val candidates = library
            .filter { it.enabled && it.triggerWords.any { trigger -> hasTrigger(normalizedPrompt, trigger) } }
            .distinctBy { it.id }
            .sortedBy { it.id }
        val incompatible = candidates.filter { lora ->
            !lora.modelFamily.equals(checkpoint.baseModelFamily, ignoreCase = true) ||
                (lora.allowedCheckpointIds.isNotEmpty() && checkpoint.id !in lora.allowedCheckpointIds)
        }
        if (incompatible.isNotEmpty()) {
            return LoRaResolution(
                candidates,
                null,
                incompatible.map { "LoRA '${it.id}' is incompatible with checkpoint '${checkpoint.id}'." },
            )
        }
        if (candidates.isEmpty()) {
            return LoRaResolution(emptyList(), checkpoint.id, emptyList())
        }

        // Exact combinations only: do not silently run the base model or a variant
        // with extra/missing LoRAs. A premerged variant is not runtime LoRA loading.
        val requestedWeights = candidates.associate { it.id to it.defaultWeight }
        val matching = availableVariants.firstOrNull { variant ->
            variant.checkpointId == checkpoint.id &&
                variant.ready &&
                variant.loraWeights.keys == requestedWeights.keys &&
                requestedWeights.all { (id, weight) ->
                    variant.loraWeights[id]?.let { kotlin.math.abs(it - weight) < 0.0001f } == true
                }
        }
        return if (matching == null) {
            LoRaResolution(candidates, null, listOf(
                "No verified premerged NPU model variant for checkpoint '${checkpoint.id}' " +
                    "and LoRAs ${requestedWeights.keys.joinToString()}. Conversion is required.",
            ))
        } else {
            LoRaResolution(candidates, matching.modelId, emptyList())
        }
    }

    private fun hasTrigger(normalizedPrompt: String, rawTrigger: String): Boolean {
        val trigger = rawTrigger.trim().lowercase(Locale.ROOT)
        if (trigger.isEmpty()) return false
        // Literal substring within tag boundaries, rather than a regex that can
        // accidentally activate \"art\" from \"cartoon\" or interpret punctuation.
        var index = normalizedPrompt.indexOf(trigger)
        while (index >= 0) {
            val before = if (index == 0) null else normalizedPrompt[index - 1]
            val end = index + trigger.length
            val after = if (end == normalizedPrompt.length) null else normalizedPrompt[end]
            if ((before == null || !before.isLetterOrDigit() && before != '_') &&
                (after == null || !after.isLetterOrDigit() && after != '_')
            ) return true
            index = normalizedPrompt.indexOf(trigger, index + 1)
        }
        return false
    }
}
