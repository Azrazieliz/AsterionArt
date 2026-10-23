package com.asterion.art.model

/**
 * The layout's semantic labels are user-assigned; they are never inferred
 * by looking at an image. All role-specific reference sections are optional
 * except PRIMARY. Additional panels appear only when the source supplies them.
 */
data class SheetSlot(val id: String, val label: String, val priority: Int)

object SheetLayouts {
    private val character = listOf(
        SheetSlot("PRIMARY", "Full identity", 0),
        SheetSlot("FACE", "Face views", 1),
        SheetSlot("EYES", "Eyes", 2),
        SheetSlot("HAIR", "Hair states", 3),
        SheetSlot("BODY", "Body reference", 4),
        SheetSlot("OUTFIT", "Outfit", 5),
        SheetSlot("DISTINCTIVE", "Weapon / wings / identity features", 6),
        SheetSlot("PALETTE", "Color palette", 7),
        SheetSlot("DIAGNOSTIC", "Detail callout", 8)
    )
    private val style = listOf(
        SheetSlot("PRIMARY", "Style overview", 0),
        SheetSlot("FINGERPRINT", "Linework / style", 1),
        SheetSlot("SHAPE", "Form language", 2),
        SheetSlot("MATERIAL", "Materials", 3),
        SheetSlot("LIGHT", "Lighting", 4),
        SheetSlot("COLOR", "Color strategy", 5),
        SheetSlot("COMPOSITION", "Composition", 6),
        SheetSlot("NEGATIVE", "Avoid examples", 7)
    )
    private val theme = listOf(
        SheetSlot("PRIMARY", "Scene overview", 0),
        SheetSlot("POSE", "Pose and interaction", 1),
        SheetSlot("ENVIRONMENT", "Environment", 2),
        SheetSlot("ELEMENT", "Physical elements", 3),
        SheetSlot("LIGHT", "Lighting", 4),
        SheetSlot("VIEW", "Viewpoint", 5),
        SheetSlot("NEGATIVE", "Avoid examples", 6)
    )
    private val element = listOf(
        SheetSlot("PRIMARY", "Core element", 0),
        SheetSlot("STRUCTURE", "Form geometry", 1),
        SheetSlot("VARIATIONS", "Variations", 2),
        SheetSlot("PLACEMENT", "Placement", 3),
        SheetSlot("MATERIAL", "Materials", 4),
        SheetSlot("COLOR", "Color states", 5),
        SheetSlot("NEGATIVE", "Avoid examples", 6)
    )
    fun forRole(role: SheetRole): List<SheetSlot> = when(role) {
        SheetRole.CHARACTER -> character
        SheetRole.ART_STYLE -> style
        SheetRole.THEME -> theme
        SheetRole.ELEMENT -> element
    }
    fun isValid(role: SheetRole, id: String) = forRole(role).any { it.id == id }
    fun label(role: SheetRole, id: String) =
        forRole(role).firstOrNull { it.id == id }?.label ?: "Unclassified reference"
}
