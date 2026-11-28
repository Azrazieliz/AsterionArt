package com.asterion.art.creation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

@Serializable
enum class Family { ANIME, COSPLAY, REALISTIC }

@Serializable
enum class Quality { AUTOMATIC, PREVIEW, HIGH }

@Serializable
enum class Route { DIRECT, DESIGN_FIRST }

@Serializable
enum class Mode(val title: String) {
    ARTWORK("Artwork"),
    CHARACTER("Character Creation"),
    OUTFIT("Outfit Creation"),
    EQUIPMENT("Fictional Equipment"),
    TRANSFORMATION("Transformation"),
    MUTATION("Mutation"),
    CREATURE("Creature Creation"),
    ALTERNATE_UNIVERSE("Alternate Universe"),
    FEMINISATION("Feminisation"),
    STYLE_CORRECTION("Art Style Correction"),
    THEME("Theme Creation"),
    CHARACTER_SHEET("Character Sheet"),
    THEME_SHEET("Theme Sheet"),
    STYLE_SHEET("Art Style Sheet"),
    CLIP("Video Clip"),
    ANIMATION("Animation Project"),
}

@Serializable
enum class RefRole {
    CHARACTER, THEME, ART_STYLE, ELEMENT, POSE, SOURCE, OUTFIT, KEYFRAME, AUDIO, GEOMETRY,
}

@Serializable
enum class AssetSource { DOCUMENT, NEXUS, ART_OUTPUT }

@Serializable
data class AssetRef(
    val id: String,
    val source: AssetSource,
    val locator: String,
    val name: String,
    val sha256: String? = null,
) {
    init {
        require(id.isNotBlank() && locator.isNotBlank() && name.isNotBlank())
        require(sha256 == null || sha256.matches(Regex("[a-f0-9]{64}")))
        require(source != AssetSource.NEXUS || locator.startsWith("content://"))
    }
}

@Serializable
data class Region(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(left in 0f..1f && right in 0f..1f && top in 0f..1f && bottom in 0f..1f)
        require(left < right && top < bottom)
    }
}

@Serializable
data class Reference(
    val role: RefRole,
    val asset: AssetRef,
    val priority: Int = 0,
    val subject: String? = null,
    val region: Region? = null,
) {
    init {
        require(priority >= 0)
        require(subject == null || subject.isNotBlank())
    }
}

@Serializable
data class ThemeSubject(
    val id: String,
    val characterAuthority: String?,
    val clothingOverride: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(characterAuthority == null || characterAuthority.isNotBlank())
        require(clothingOverride == null || clothingOverride.isNotBlank())
    }
}

@Serializable
data class ThemeCast(
    val authority: String,
    val specifiedCount: Int,
    val subjects: List<ThemeSubject>,
    val randomMaleSubject: String? = null,
) {
    init {
        require(authority.isNotBlank())
        require(specifiedCount >= 0 && specifiedCount == subjects.size)
        require(subjects.map { it.id }.distinct().size == subjects.size)
        require(randomMaleSubject == null || randomMaleSubject.isNotBlank() && subjects.none { it.id == randomMaleSubject })
    }

    fun freeze(references: List<Reference>, malePool: List<AssetRef>, random: Random): List<ThemeSubject> {
        require(references.any { it.role == RefRole.THEME && it.asset.id == authority }) {
            "Theme cast must use the selected Theme Sheet authority"
        }
        val identities = references.filter { it.role == RefRole.CHARACTER }.map { it.asset.id }.toSet()
        require(subjects.all { it.characterAuthority in identities }) {
            "Assign a Character Sheet to each of the Theme's configured subjects"
        }
        if (randomMaleSubject == null) return subjects
        val available = malePool.distinctBy { it.id }.sortedBy { it.id }
        require(available.isNotEmpty() && available.all { it.id in identities }) {
            "Supply eligible Character Sheets for the Theme's random male subject"
        }
        return subjects + ThemeSubject(randomMaleSubject, available[random.nextInt(available.size)].id)
    }
}

@Serializable
sealed interface DesignInput {
    val mode: Mode
    val direction: String
}

@Serializable
@SerialName("artwork")
data class ArtworkInput(
    override val direction: String = "",
    val candidatesPerPhase: Int = 3,
    val themeCast: ThemeCast? = null,
    val randomMalePoolIds: List<String> = emptyList(),
) : DesignInput {
    override val mode get() = Mode.ARTWORK

    init {
        require(candidatesPerPhase in 1..(Int.MAX_VALUE / ArtworkPhase.entries.size))
        require(randomMalePoolIds.distinct().size == randomMalePoolIds.size)
    }
}

@Serializable
enum class CharacterOperation { CREATE, ALTER, REFINE, FUSE }

@Serializable
@SerialName("character")
data class CharacterInput(
    val slots: List<String>,
    override val direction: String = "",
    val count: Int = 5,
    val operation: CharacterOperation = CharacterOperation.CREATE,
) : DesignInput {
    override val mode get() = Mode.CHARACTER

    init {
        require(count > 0)
        require(slots.all(String::isNotBlank))
        require(operation != CharacterOperation.CREATE || slots.size >= 2)
        require(operation != CharacterOperation.FUSE || count == 1)
    }
}

@Serializable
enum class VisualAttribute { FACE, HAIR, PROPORTIONS, SILHOUETTE, PALETTE, MATERIALS, CLOTHING, EQUIPMENT }

@Serializable
enum class OriginalOperation { CREATE, ALTER, REFINE, FUSE, IMPROVE, DEVELOP, FINALISE }

@Serializable
enum class OriginalPresentation { CATALOGUE, SINGLE_DESIGN }

@Serializable
@SerialName("original")
data class OriginalInput(
    override val mode: Mode,
    override val direction: String = "",
    val count: Int = if (mode in setOf(Mode.OUTFIT, Mode.EQUIPMENT)) 10 else 1,
    val operation: OriginalOperation = OriginalOperation.CREATE,
    val constructionViews: Boolean = false,
    val progression: Boolean = false,
    val retention: Map<VisualAttribute, Float> = emptyMap(),
    val linkedProject: String? = null,
    val reconstructionProfile: String? = null,
    val presentation: OriginalPresentation? = null,
) : DesignInput {
    init {
        require(mode in ORIGINALS)
        require(count > 0)
        require(!constructionViews || mode == Mode.OUTFIT)
        require(!progression || mode == Mode.TRANSFORMATION)
        require(retention.isEmpty() || mode == Mode.MUTATION)
        require(retention.values.all { it in 0f..1f })
        require(reconstructionProfile == null || mode == Mode.FEMINISATION && reconstructionProfile.isNotBlank())
        require(presentation == null || mode in FIVE_DESIGN_CATALOGUES)
    }

    val defaultCatalogueSize: Int? get() = if (mode in FIVE_DESIGN_CATALOGUES) 5 else null
    val feminisationProfile: String? get() = if (mode == Mode.FEMINISATION) reconstructionProfile ?: "Level 4.5" else null

    companion object {
        val ORIGINALS = setOf(
            Mode.OUTFIT, Mode.EQUIPMENT, Mode.TRANSFORMATION, Mode.MUTATION, Mode.CREATURE,
            Mode.ALTERNATE_UNIVERSE, Mode.FEMINISATION, Mode.STYLE_CORRECTION,
        )
        val FIVE_DESIGN_CATALOGUES = setOf(Mode.TRANSFORMATION, Mode.MUTATION, Mode.CREATURE, Mode.ALTERNATE_UNIVERSE)
    }
}

@Serializable(with = ThemeIntensitySerializer::class)
data class ThemeIntensity(val exact: String) {
    init {
        require(exact.toBigDecimalOrNull()?.signum()?.let { it >= 0 } == true) {
            "Enter a nonnegative decimal intensity"
        }
    }
}

object ThemeIntensitySerializer : KSerializer<ThemeIntensity> {
    override val descriptor = PrimitiveSerialDescriptor("ThemeIntensity", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ThemeIntensity) = encoder.encodeString(value.exact)

    override fun deserialize(decoder: Decoder): ThemeIntensity = ThemeIntensity(
        if (decoder is JsonDecoder) decoder.decodeJsonElement().jsonPrimitive.content else decoder.decodeString(),
    )
}

@Serializable
data class ThemeAxes(val beauty: ThemeIntensity, val eroticism: ThemeIntensity, val tension: ThemeIntensity)

@Serializable
@SerialName("theme")
data class ThemeInput(
    override val direction: String = "",
    val axes: ThemeAxes? = null,
    val keywords: List<String> = emptyList(),
    val frozenParents: List<ThemeDesign> = emptyList(),
    val operation: OriginalOperation = OriginalOperation.CREATE,
) : DesignInput {
    override val mode get() = Mode.THEME
}

@Serializable
@SerialName("sheet")
data class SheetInput(
    override val mode: Mode,
    override val direction: String = "",
    val theme: ThemeDesign? = null,
    val baseIdentityId: String? = null,
) : DesignInput {
    init {
        require(mode in setOf(Mode.CHARACTER_SHEET, Mode.THEME_SHEET, Mode.STYLE_SHEET))
        require(theme == null || mode == Mode.THEME_SHEET)
        require(baseIdentityId == null || mode == Mode.CHARACTER_SHEET)
    }
}

@Serializable
data class Shot(
    val id: String,
    val durationMillis: Long,
    val scene: String,
    val camera: String = "",
    val motion: String = "",
    val openingFrameId: String? = null,
    val endingFrameId: String? = null,
    val previousShotId: String? = null,
) {
    init {
        require(id.isNotBlank() && durationMillis > 0 && scene.isNotBlank())
    }
}

@Serializable
data class AudioCue(val assetId: String, val offsetMillis: Long, val dialogue: Boolean) {
    init {
        require(assetId.isNotBlank() && offsetMillis >= 0)
    }
}

@Serializable
@SerialName("video")
data class VideoInput(
    override val mode: Mode,
    override val direction: String = "",
    val shots: List<Shot>,
    val audio: List<AudioCue> = emptyList(),
) : DesignInput {
    init {
        require(mode in setOf(Mode.CLIP, Mode.ANIMATION))
        require(mode != Mode.CLIP || shots.size == 1)
        require(shots.isNotEmpty() || direction.isNotBlank()) { "Describe the scene or add a shot" }
        require(shots.map { it.id }.distinct().size == shots.size)
        require(shots.firstOrNull()?.previousShotId == null)
        require(shots.zipWithNext().all { (first, second) -> second.previousShotId == first.id })
    }
}

@Serializable
data class CreationRequest(
    val input: DesignInput,
    val references: List<Reference> = emptyList(),
    val family: Family = Family.ANIME,
    val quality: Quality = Quality.AUTOMATIC,
    val route: Route = Route.DIRECT,
    val seed: Long,
    val creativeFreedom: Float? = null,
) {
    val mode get() = input.mode

    init {
        require(creativeFreedom == null || creativeFreedom.isFinite())
        require(route == Route.DIRECT || mode !in setOf(Mode.THEME, Mode.CLIP, Mode.ANIMATION))
    }

    fun validateForStart() {
        fun has(role: RefRole) = references.any { it.role == role }
        fun source() = has(RefRole.CHARACTER) || has(RefRole.SOURCE)
        when (val design = input) {
            is ArtworkInput -> {
                require(has(RefRole.THEME) && has(RefRole.ART_STYLE)) {
                    "Select a Theme Sheet and an Art Style Sheet"
                }
                require(design.themeCast != null) { "The Theme Sheet needs a character configuration" }
                val pool = references.filter {
                    it.role == RefRole.CHARACTER && it.asset.id in design.randomMalePoolIds
                }.map { it.asset }
                design.themeCast.freeze(references, pool, Random(seed))
            }
            is CharacterInput -> {
                require(design.operation == CharacterOperation.CREATE || source()) {
                    "Choose the exact character design this operation starts from"
                }
                if (design.operation == CharacterOperation.FUSE) {
                    require(references.filter { it.role in setOf(RefRole.CHARACTER, RefRole.SOURCE) }
                        .map { it.asset.id }.distinct().size >= 2) { "Select the designs to fuse" }
                }
            }
            is OriginalInput -> when (mode) {
                Mode.OUTFIT, Mode.EQUIPMENT, Mode.TRANSFORMATION, Mode.MUTATION,
                Mode.ALTERNATE_UNIVERSE, Mode.FEMINISATION -> require(source()) { "Choose a source character" }
                Mode.STYLE_CORRECTION -> require(has(RefRole.SOURCE) && has(RefRole.ART_STYLE)) {
                    "Choose the source artwork and target Art Style Sheet"
                }
                else -> require(design.direction.isNotBlank() || references.isNotEmpty()) { "Describe the creature" }
            }
            is SheetInput -> require(design.theme != null || references.isNotEmpty()) {
                "Choose a source for the sheet"
            }
            is VideoInput -> {
                val frames = references.filter { it.role == RefRole.KEYFRAME }.map { it.asset.id }.toSet()
                val audio = references.filter { it.role == RefRole.AUDIO }.map { it.asset.id }.toSet()
                require(design.shots.all { shot ->
                    listOfNotNull(shot.openingFrameId, shot.endingFrameId).all { it in frames }
                })
                require(design.audio.all { it.assetId in audio })
            }
            is ThemeInput -> Unit
        }
    }
}
