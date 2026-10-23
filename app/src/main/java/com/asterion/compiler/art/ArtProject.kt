package com.asterion.compiler.art

/**
 * The four references are different resources, not four prompts. A character
 * sheet is stored in Core; Art owns a project snapshot of the chosen references.
 */
enum class ArtMode { ANIME, REALISTIC, COSPLAY, ANIMATION }
enum class SheetKind { CHARACTER, ART_STYLE, THEME, ELEMENT }

data class ReferenceSheet(
    val id: String,
    val kind: SheetKind,
    val uri: String,
    val title: String,
    /** A future Core content:// URI resolves to the same contract. */
    val coreRecordId: String? = null,
)

data class StoryboardFrame(
    val id: String,
    val imageUri: String,
    val motionToNext: String,
    val segmentSeconds: Int = 2,
) {
    init {
        require(id.isNotBlank() && imageUri.isNotBlank())
        require(segmentSeconds in 1..5)
    }
}

data class ArtProject(
    val id: String,
    val mode: ArtMode = ArtMode.ANIME,
    val references: List<ReferenceSheet> = emptyList(),
    val frames: List<StoryboardFrame> = emptyList(),
    val additionalDirection: String = "",
) {
    init { require(id.isNotBlank()) }
    fun of(kind: SheetKind) = references.filter { it.kind == kind }
    fun problems(): List<String> = buildList {
        if (of(SheetKind.CHARACTER).isEmpty()) add("Select at least one character reference")
        if (of(SheetKind.ART_STYLE).isEmpty()) add("Select an art-style sheet")
        if (of(SheetKind.THEME).isEmpty()) add("Select a theme sheet")
        // Elements are optional by design: not every scene needs a special object.
        if (mode == ArtMode.ANIMATION && frames.isEmpty()) add("Choose at least one animation keyframe")
    }
}

enum class VideoConditioning { FIRST_FRAME_ONLY, FIRST_AND_LAST }

/** No inference is claimed when the selected backend cannot condition on both images. */
data class VideoCapabilities(
    val supportsFirstFrame: Boolean,
    val supportsFirstAndLast: Boolean,
    val maxSeconds: Int,
)

data class VideoShot(
    val start: StoryboardFrame,
    val end: StoryboardFrame?,
    val seconds: Int,
    val conditioning: VideoConditioning,
)

sealed interface VideoPlanResult {
    data class Ready(val shots: List<VideoShot>, val warning: String?) : VideoPlanResult
    data class Blocked(val reason: String) : VideoPlanResult
}

object VideoPlanner {
    /**
     * More than one reference image means "animate between these anchors".
     * Strict mode never pretends an I2V single-image endpoint obeys an end frame.
     * Approximate mode makes independent, image-anchored generative segments.
     */
    fun plan(
        frames: List<StoryboardFrame>,
        capabilities: VideoCapabilities,
        allowApproximateSequence: Boolean = false,
    ): VideoPlanResult {
        if (!capabilities.supportsFirstFrame) return VideoPlanResult.Blocked("The video backend cannot accept an image.")
        if (frames.isEmpty()) return VideoPlanResult.Blocked("No keyframes selected.")
        if (frames.any { it.segmentSeconds > capabilities.maxSeconds }) {
            return VideoPlanResult.Blocked("A requested segment exceeds the installed video's maximum duration.")
        }
        if (frames.size == 1) {
            return VideoPlanResult.Ready(listOf(VideoShot(frames[0], null, frames[0].segmentSeconds,
                VideoConditioning.FIRST_FRAME_ONLY)), null)
        }
        if (!capabilities.supportsFirstAndLast && !allowApproximateSequence) {
            return VideoPlanResult.Blocked(
                "This backend can condition only on the first frame, not both endpoints. " +
                    "Exact keyframe-to-keyframe inference needs a different video model."
            )
        }
        val shots = frames.zipWithNext { first, next ->
            VideoShot(first, next, first.segmentSeconds,
                if (capabilities.supportsFirstAndLast) VideoConditioning.FIRST_AND_LAST
                else VideoConditioning.FIRST_FRAME_ONLY)
        }
        val warning = if (capabilities.supportsFirstAndLast) null
            else "Approximate mode: each anchor guides a real I2V segment, but its next target frame " +
                "is NOT passed to the model. Continuity and endpoint matching are unverified."
        return VideoPlanResult.Ready(shots, warning)
    }
}
