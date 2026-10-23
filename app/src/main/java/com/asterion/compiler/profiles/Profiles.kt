package com.asterion.compiler.profiles

import com.asterion.compiler.prompt.PromptPassType
import com.asterion.compiler.worksheet.WorksheetRole

data class GenerationParameters(
    val width: Int,
    val height: Int,
    val steps: Int,
    val cfgScale: Double,
    val sampler: String,
    val seed: Long? = null,
)

data class CheckpointProfile(
    val id: String,
    val displayName: String,
    val baseModelFamily: String,
    val defaultParameters: GenerationParameters,
    val promptOptimization: CheckpointPromptOptimization = CheckpointPromptOptimization(),
    /** Advisory only. Real NPU capacity must be read from Local Dream /tokenize. */
    val inferenceContextHint: Int? = null,
    val backend: String = "sdxl",
    val dynamicLoRaSupported: Boolean = false,
)

data class CheckpointPromptOptimization(
    val lexicalReplacements: Map<String, String> = emptyMap(),
    val negativeTerms: List<String> = emptyList(),
    val passNegativeTerms: Map<PromptPassType, List<String>> = emptyMap(),
)

data class GlobalPreferenceProfile(
    val offlineOnly: Boolean,
    val persistentHistoryEnabled: Boolean,
    val exportEnabled: Boolean,
    val promptLanguage: String,
)

data class CompilationStrategy(
    val id: String,
    val displayName: String,
    val checkpointProfile: CheckpointProfile,
    val passOrder: List<PromptPassType>,
    val rolePriority: List<WorksheetRole> = listOf(WorksheetRole.CHARACTER, WorksheetRole.THEME, WorksheetRole.ART_STYLE),
    /** Authoring budget in approximate word units, NOT actual model tokens. */
    val maximumTokens: Int = 2048,
    val compactDescriptors: Boolean = false,
)

object EmbeddedProfiles {
    private val prefectIllustriousV8 = supportedCheckpoint(
        id = "prefect_illustrious_v8",
        displayName = "prefect_illustrious_v8",
        sampler = "DPM++ 2M Karras",
    )
    private val illustriousV17 = supportedCheckpoint(
        id = "illustrious_v17",
        displayName = "illustrious_v17",
        sampler = "DPM++ 2M Karras",
    )
    private val rinFlanimeV1Turbo = supportedCheckpoint(
        id = "rin_flanime_v1_turbo",
        displayName = "rin_flanime_v1_turbo",
        sampler = "FlowMatch",
        family = "ANIMA",
    )
    private val waiAnimaV1Turbo = supportedCheckpoint(
        id = "wai_anima_v1_turbo",
        displayName = "wai_anima_v1_turbo",
        sampler = "FlowMatch",
        family = "ANIMA",
    )
    private val animaYumeV1Turbo = supportedCheckpoint(
        id = "animayume_v1_turbo",
        displayName = "animayume_v1_turbo",
        sampler = "FlowMatch",
        family = "ANIMA",
    )
    private val samAnimaRealistic = supportedCheckpoint(
        id = "sam_anima_realistic_v23_turbo",
        displayName = "sam_anima_realistic_v23_turbo",
        sampler = "FlowMatch",
        family = "ANIMA",
    )
    private val noobAiXl = supportedCheckpoint(
        id = "NoobAI-XL-v11", displayName = "NoobAI-XL-v11",
        sampler = "DPM++ 2M Karras",
    )
    private val realismSdxl = supportedCheckpoint(
        id = "realismSDXLByStable_v80DMD", displayName = "realismSDXLByStable_v80DMD",
        sampler = "DPM++ 2M Karras",
    )
    private val cyberRealistic = supportedCheckpoint(
        id = "cyberrealistic_v3_turbo", displayName = "cyberrealistic_v3_turbo",
        sampler = "DPM++ 2M Karras",
    )
    private val novaanimeV3Turbo = supportedCheckpoint(
        id = "novaanime_v3_turbo",
        displayName = "novaanime_v3_turbo",
        sampler = "DPM++ SDE Karras",
    )

    val checkpoints: List<CheckpointProfile> = listOf(
        prefectIllustriousV8,
        illustriousV17,
        rinFlanimeV1Turbo,
        waiAnimaV1Turbo,
        novaanimeV3Turbo,
        animaYumeV1Turbo,
        samAnimaRealistic,
        noobAiXl,
        realismSdxl,
        cyberRealistic,
    )

    val globalPreferences = GlobalPreferenceProfile(
        offlineOnly = true,
        persistentHistoryEnabled = false,
        exportEnabled = false,
        promptLanguage = "en",
    )

    val strategies: List<CompilationStrategy> = listOf(
        CompilationStrategy(
            id = "balanced",
            displayName = "Balanced",
            checkpointProfile = prefectIllustriousV8,
            passOrder = PromptPassType.entries,
        ),
        CompilationStrategy(
            id = "character-priority",
            displayName = "Character Priority",
            checkpointProfile = prefectIllustriousV8,
            passOrder = PromptPassType.entries,
            rolePriority = listOf(WorksheetRole.CHARACTER, WorksheetRole.THEME, WorksheetRole.ART_STYLE),
        ),
        CompilationStrategy(
            id = "theme-priority",
            displayName = "Theme Priority",
            checkpointProfile = prefectIllustriousV8,
            passOrder = PromptPassType.entries,
            rolePriority = listOf(WorksheetRole.THEME, WorksheetRole.CHARACTER, WorksheetRole.ART_STYLE),
        ),
        CompilationStrategy(
            id = "style-priority",
            displayName = "Style Priority",
            checkpointProfile = prefectIllustriousV8,
            passOrder = PromptPassType.entries,
            rolePriority = listOf(WorksheetRole.ART_STYLE, WorksheetRole.CHARACTER, WorksheetRole.THEME),
        ),
        CompilationStrategy(
            id = "compact-77",
            displayName = "Detailed (384 Authoring Words)",
            checkpointProfile = prefectIllustriousV8,
            passOrder = PromptPassType.entries,
            maximumTokens = 384,
            compactDescriptors = true,
        ),
        CompilationStrategy(
            id = "expanded-512",
            displayName = "Extended (2048 Authoring Words)",
            checkpointProfile = prefectIllustriousV8,
            passOrder = PromptPassType.entries,
            maximumTokens = 2048,
        ),
    )

    fun strategyById(id: String): CompilationStrategy? = strategies.firstOrNull { it.id == id }

    fun checkpointById(id: String): CheckpointProfile? = checkpoints.firstOrNull { it.id == id }

    fun defaultStrategyForCheckpoint(checkpointId: String): CompilationStrategy? = when (checkpointId) {
        "prefect_illustrious_v8", "illustrious_v17", "NoobAI-XL-v11", "realismSDXLByStable_v80DMD", "cyberrealistic_v3_turbo" -> strategies.first { it.id == "compact-77" }
        "rin_flanime_v1_turbo", "wai_anima_v1_turbo", "novaanime_v3_turbo", "animayume_v1_turbo", "sam_anima_realistic_v23_turbo" -> strategies.first { it.id == "expanded-512" }
        else -> null
    }

    private fun checkpointOptimization(
        lexicalReplacements: Map<String, String>,
        negativeTerms: List<String>,
    ): CheckpointPromptOptimization = CheckpointPromptOptimization(
        lexicalReplacements = lexicalReplacements,
        negativeTerms = negativeTerms,
        passNegativeTerms = mapOf(
            PromptPassType.FOUNDATION to listOf("distorted face", "extra limbs", "crossed eyes"),
            PromptPassType.MATERIAL to listOf("plastic skin", "incorrect fabric texture", "flat materials"),
            PromptPassType.SCENE to listOf("flat background", "floating objects", "inconsistent perspective"),
            PromptPassType.RENDERING to listOf("color banding", "over-sharpened edges", "muddy details"),
        ),
    )

    private fun supportedCheckpoint(id: String, displayName: String, sampler: String, family: String = "SDXL"): CheckpointProfile = CheckpointProfile(
        id = id,
        displayName = displayName,
        baseModelFamily = family,
        backend = if (family == "ANIMA") "anima" else "sdxl",
        inferenceContextHint = if (family == "ANIMA") 512 else 77,
        defaultParameters = GenerationParameters(1024, 1024, 30, if (family == "ANIMA") 1.0 else 6.5, sampler),
        promptOptimization = checkpointOptimization(
            lexicalReplacements = emptyMap(),
            negativeTerms = listOf("lowres", "blurry", "watermark", "signature"),
        ),
    )
}