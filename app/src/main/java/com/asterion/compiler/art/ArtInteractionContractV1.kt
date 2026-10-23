package com.asterion.compiler.art

/** Only these destinations may appear as user-selectable creation workflows. */
enum class ArtCreationMode {
    CHARACTER_SHEET, ART_STYLE_SHEET, THEME_SHEET, ELEMENT_SHEET,
    CHARACTER_CREATION, ART_STYLE_CORRECTION,
    OUTFIT_CREATION, WEAPON_CREATION, TRANSFORMATION_CREATION,
    MUTATION_CREATION, CREATURE_CREATION, ALTERNATE_UNIVERSE_CREATION,
}
enum class ArtRenderingMode { ANIME, REALISTIC, COSPLAY, ANIMATION }
enum class InternalArtPolicy {
    POSE_GEOMETRY, PROMPT_COMPILATION, REFERENCE_AUTHORITY,
    FIVE_PASS_IMAGE_GENERATION, SCORING, FREEZE_REVISION,
}
enum class ArtControlKind {
    CARD, CHIP, SELECTOR, SLIDER, IMAGE_PICKER, CROP_GESTURE,
    MASK_BRUSH, RATING, CORE_SEARCH,
}
object ArtInteractionContractV1 {
    val modes = ArtCreationMode.entries.toList()
    val policies = InternalArtPolicy.entries.toList()
    val supportedControls = ArtControlKind.entries.toSet()
    val permittedTextEntry = setOf(ArtControlKind.CORE_SEARCH)
    fun userCanType(control: ArtControlKind) = control in permittedTextEntry
}
