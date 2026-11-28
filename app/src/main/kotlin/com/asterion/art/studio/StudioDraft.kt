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
    val presentation: OriginalPresentation? = null,
    val retention: Map<VisualAttribute, Float> = emptyMap(),
    val shots: List<ShotDraft> = emptyList(),
    val audio: List<AudioDraft> = emptyList(),
    val characterOperation: CharacterOperation = CharacterOperation.CREATE,
    val id: String = UUID.randomUUID().toString(),
) {
    fun attach(reference: Reference, sheetCast: ThemeCast? = null): StudioDraft {
        require(sheetCast == null || reference.role == RefRole.THEME)
        if (mode == Mode.ARTWORK && reference.role == RefRole.THEME) {
            val existing = references.firstOrNull { it.role == RefRole.THEME && it.asset.sameContent(reference.asset) }
            val selected = existing ?: reference
            val keptCast = cast?.takeIf { it.authority == selected.asset.id }
            return copy(
                references = references.filterNot { it.role == RefRole.THEME } + selected,
                cast = keptCast ?: sheetCast?.copy(authority = selected.asset.id),
            )
        }
        val duplicate = references.any {
            it.role == reference.role && it.subject == reference.subject && it.region == reference.region &&
                it.priority == reference.priority && it.asset.sameContent(reference.asset)
        }
        if (duplicate) return this
        val cues = if (mode in setOf(Mode.CLIP, Mode.ANIMATION) && reference.role == RefRole.AUDIO) {
            audio + AudioDraft(reference.asset.id)
        } else audio
        return copy(references = references + reference, audio = cues)
    }

    fun changeReference(index: Int, replacement: Reference?): StudioDraft {
        val updated = references.toMutableList().apply {
            if (replacement == null) removeAt(index) else set(index, replacement)
        }
        val frames = updated.filter { it.role == RefRole.KEYFRAME }.map { it.asset.id }.toSet()
        val sounds = updated.filter { it.role == RefRole.AUDIO }.map { it.asset.id }.toSet()
        val characters = updated.filter { it.role == RefRole.CHARACTER }.map { it.asset.id }.toSet()
        val removedCharacter = references[index].takeIf { it.role == RefRole.CHARACTER && it.asset.id !in characters }?.asset?.id
        val updatedCast = cast?.takeIf { authority ->
            updated.any { it.role == RefRole.THEME && it.asset.id == authority.authority }
        }?.let { authority ->
            authority.copy(subjects = authority.subjects.map { subject ->
                if (removedCharacter == subject.characterAuthority) subject.copy(characterAuthority = null) else subject
            })
        }
        return copy(
            references = updated,
            shots = shots.map {
                it.copy(
                    openingFrameId = it.openingFrameId?.takeIf(frames::contains),
                    endingFrameId = it.endingFrameId?.takeIf(frames::contains),
                )
            },
            audio = audio.filter { it.assetId in sounds },
            cast = updatedCast,
            malePool = malePool.filter(characters::contains),
        )
    }

    fun attachThemeSheet(asset: AssetRef, source: SheetInput?, sourceReferences: List<Reference>): StudioDraft {
        val next = attach(Reference(RefRole.THEME, asset), source?.theme?.cast)
        val required = next.cast?.subjects?.mapNotNull { it.characterAuthority }.orEmpty().toSet()
        val known = next.references.filter { it.role == RefRole.CHARACTER }.map { it.asset.id }.toSet()
        val linked = sourceReferences.filter { it.role == RefRole.CHARACTER && it.asset.id in required && it.asset.id !in known }
        return next.copy(references = next.references + linked)
    }

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
                    ThemeAxes(
                        ThemeIntensity(beauty.ifBlank { "0" }),
                        ThemeIntensity(eroticism.ifBlank { "0" }),
                        ThemeIntensity(tension.ifBlank { "0" }),
                    )
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
                retention = retention, reconstructionProfile = profile.ifBlank { null }, presentation = presentation,
            )
        }
        return CreationRequest(
            input, references, family, quality, route, Random.nextLong(),
            creativeFreedom.ifBlank { null }?.toFloat(),
        )
    }
}

private fun AssetRef.sameContent(other: AssetRef) = id == other.id || sha256 != null && sha256 == other.sha256

internal class Drafts(context: Context) {
    private val settings = context.getSharedPreferences("art_authoring_v1", Context.MODE_PRIVATE)
    private val codec = Json { encodeDefaults = true }

    fun load(mode: Mode): StudioDraft {
        val active = settings.getString("active.${mode.name}", null)
        val encoded = active?.let { settings.getString("draft.$it", null) } ?: settings.getString(mode.name, null)
        return encoded?.let(codec::decodeFromString) ?: StudioDraft(mode)
    }

    fun read(id: String): StudioDraft? = settings.getString("draft.$id", null)?.let(codec::decodeFromString)

    fun revisions(): List<StudioDraft> = settings.all.entries.mapNotNull { (key, value) ->
        if (key.startsWith("draft.") && value is String) codec.decodeFromString<StudioDraft>(value) else null
    }.sortedBy { it.mode.title }

    fun write(draft: StudioDraft, activate: Boolean = true) {
        settings.edit()
            .putString("draft.${draft.id}", codec.encodeToString(draft))
            .apply { if (activate) putString("active.${draft.mode.name}", draft.id) }
            .apply()
    }
}
