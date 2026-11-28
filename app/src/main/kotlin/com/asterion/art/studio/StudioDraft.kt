package com.asterion.art.studio

import android.content.Context
import com.asterion.art.creation.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.random.Random

@Serializable
internal data class ShotDraft(
    val id: String,
    val scene: String = "",
    val seconds: String = "",
    val camera: String = "",
    val motion: String = "",
    val openingFrameId: String? = null,
    val endingFrameId: String? = null,
)

@Serializable
internal data class AudioDraft(val assetId: String, val seconds: String = "0", val dialogue: Boolean = false)

@Serializable
internal data class StudioDraft(
    val mode: Mode,
    val direction: String = "",
    val slots: String = "",
    val count: String = "",
    val references: List<Reference> = emptyList(),
    val family: Family = Family.ANIME,
    val quality: Quality = Quality.AUTOMATIC,
    val route: Route = Route.DIRECT,
    val theme: ThemeDesign? = null,
    val baseIdentityId: String? = null,
    val cast: ThemeCast? = null,
    val malePool: List<String> = emptyList(),
    val beauty: String = "",
    val eroticism: String = "",
    val tension: String = "",
    val creativeFreedom: String = "",
    val keywords: String = "",
    val constructionViews: Boolean = false,
    val progression: Boolean = false,
    val profile: String = "",
    val retention: Map<VisualAttribute, Float> = emptyMap(),
    val shots: List<ShotDraft> = emptyList(),
    val audio: List<AudioDraft> = emptyList(),
    val characterOperation: CharacterOperation = CharacterOperation.CREATE,
    val id: String = UUID.randomUUID().toString(),
) {
    fun request(): CreationRequest {
        fun count(default: Int) = count.ifBlank { default.toString() }.toInt()
        val input: DesignInput = when (mode) {
            Mode.ARTWORK -> ArtworkInput(direction, count(3), cast, malePool)
            Mode.CHARACTER -> CharacterInput(
                slots.split(Regex("[×+\n]")).map(String::trim).filter(String::isNotEmpty),
                direction, count(if (characterOperation == CharacterOperation.CREATE) 5 else 1), characterOperation,
            )
            Mode.THEME -> {
                val axes = if (listOf(beauty, eroticism, tension).any(String::isNotBlank)) {
                    ThemeAxes(beauty.ifBlank { "0" }.toFloat(), eroticism.ifBlank { "0" }.toFloat(), tension.ifBlank { "0" }.toFloat())
                } else null
                ThemeInput(direction, axes, keywords.split('\n').map(String::trim).filter(String::isNotEmpty))
            }
            Mode.CHARACTER_SHEET, Mode.THEME_SHEET, Mode.STYLE_SHEET -> SheetInput(
                mode, direction, theme, baseIdentityId,
            )
            Mode.CLIP, Mode.ANIMATION -> VideoInput(
                mode, direction,
                shots.mapIndexed { index, shot ->
                    val duration = shot.seconds.toDouble()
                    require(duration.isFinite() && duration > 0)
                    Shot(
                        shot.id, (duration * 1_000).toLong(), shot.scene, shot.camera, shot.motion,
                        openingFrameId = shot.openingFrameId,
                        endingFrameId = shot.endingFrameId,
                        previousShotId = shots.getOrNull(index - 1)?.id,
                    )
                },
                audio.map { cue ->
                    val seconds = cue.seconds.toDouble()
                    require(seconds.isFinite() && seconds >= 0)
                    AudioCue(cue.assetId, (seconds * 1_000).toLong(), cue.dialogue)
                },
            )
            else -> OriginalInput(
                mode, direction, count(if (mode == Mode.OUTFIT || mode == Mode.EQUIPMENT) 10 else 1),
                constructionViews = constructionViews, progression = progression,
                retention = retention, reconstructionProfile = profile.ifBlank { null },
            )
        }
        return CreationRequest(
            input, references, family, quality, route, Random.nextLong(),
            creativeFreedom.ifBlank { null }?.toFloat(),
        )
    }
}

internal class Drafts(context: Context) {
    private val settings = context.getSharedPreferences("art_authoring_v1", Context.MODE_PRIVATE)
    private val codec = Json { encodeDefaults = true }

    fun load(mode: Mode): StudioDraft {
        val active = settings.getString("active.${mode.name}", null)
        val encoded = active?.let { settings.getString("draft.$it", null) } ?: settings.getString(mode.name, null)
        return encoded?.let(codec::decodeFromString) ?: StudioDraft(mode)
    }

    fun revisions(): List<StudioDraft> = settings.all.entries.mapNotNull { (key, value) ->
        if (key.startsWith("draft.") && value is String) codec.decodeFromString<StudioDraft>(value) else null
    }.sortedBy { it.mode.title }

    fun write(draft: StudioDraft) {
        settings.edit()
            .putString("draft.${draft.id}", codec.encodeToString(draft))
            .putString("active.${draft.mode.name}", draft.id)
            .apply()
    }
}
