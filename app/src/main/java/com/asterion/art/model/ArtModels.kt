package com.asterion.art.model

import java.util.UUID

enum class SheetRole(val display: String) {
    CHARACTER("Character"), ART_STYLE("Art Style"), THEME("Theme"), ELEMENT("Element")
}
enum class CreationMode(val display: String) {
    CHARACTER("Character Creation"), STYLE_CORRECTION("Art Style Correction"),
    OUTFIT("Outfit Creation"), WEAPON("Weapon Creation"),
    TRANSFORMATION("Transformation"), MUTATION("Mutation"),
    CREATURE("Creature Creation"), ALTERNATE_UNIVERSE("Alternate Universe")
}
enum class RenderMode(val display: String) {
    ANIME("Anime"), REALISTIC("Realistic"), COSPLAY("Cosplay"), ANIMATION("Animation")
}
enum class TaskStatus { QUEUED, RUNNING, COMPLETED, FAILED }
enum class TaskKind { SHEET_RENDER, STILL_IMAGE, VIDEO }
enum class DetailKind {
    SIGNATURE_WEAPON, WINGS, HORNS, TAIL, MARKING, SPECIES_FEATURE, UNIQUE_EQUIPMENT, OTHER
}
data class ReferenceAsset(val uri: String, val ordinal: Int)
data class SheetRecord(
    val id: String = UUID.randomUUID().toString(),
    val role: SheetRole,
    val label: String,
    val sourceUris: List<String>,
    val referenceSlots: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val outputUri: String? = null,
    val characterId: String? = null,
    val formId: String? = null,
    val structuredDataVersion: Int = 1
)
data class TaskRecord(
    val id: String = UUID.randomUUID().toString(),
    val targetId: String,
    val kind: TaskKind,
    val createdAt: Long = System.currentTimeMillis(),
    val status: TaskStatus = TaskStatus.QUEUED,
    val error: String? = null
)
data class GradedAsset(
    val uri: String,
    val stars: Int = 0,
    val sourceSheetId: String? = null,
    val autoScore: Double? = null,
    val sourceJobId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
data class GeneratorSettings(
    val ratio: String = "2:3",
    val steps: Int = 20,
    val cfg: Float = 4f,
    val batch: Int = 3,
    val denoise: Float = 0.45f,
    val scheduler: String = "DPM++ 2M",
    val karras: Boolean = false,
    val seed: Long? = null,
    val automatic: Boolean = true
)

object ArtPolicies {
    val creationModes = CreationMode.entries.toList()
    val renderingModes = RenderMode.entries.toList()
    val sheetRoles = SheetRole.entries.toList()
    val selectableRatios = listOf("1:1", "3:4", "4:3", "9:16", "2:3", "3:2")
    val schedulers = listOf("DPM++ 2M", "DPM++ 2M SDE", "Euler", "Euler a")
    val modelRanking = listOf("Rin Flanime", "MiaoMiao", "Illustrious Hassaku XL", "NoobAI XL v11")
    fun grade(stars: Int): Int = stars.coerceIn(0, 5)
    fun isCharacterSpecific(kind: DetailKind, hasReferences: Boolean): Boolean = hasReferences
    fun eligibleForAutomation(selected: Map<SheetRole, Int>): Boolean =
        selected.getOrDefault(SheetRole.CHARACTER, 0) > 0 &&
        selected.getOrDefault(SheetRole.ART_STYLE, 0) > 0 &&
        selected.getOrDefault(SheetRole.THEME, 0) > 0
}
