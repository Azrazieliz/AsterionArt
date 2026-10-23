package com.asterion.compiler.art

/** Stable V1 architecture. No generation or UI is implied by this data contract. */
enum class ArtSheetRole { CHARACTER, ART_STYLE, THEME, ELEMENT }
enum class CharacterPanelKind {
    PRIMARY_IDENTITY, FACE, EYES, HAIR, BODY, OUTFIT, PALETTE, IDENTITY_INVARIANTS,
    SIGNATURE_WEAPON, EQUIPMENT, WINGS, HORNS, TAIL, MARKINGS, SPECIES_ANATOMY,
    MECHANICAL_STRUCTURE, HAIR_ARCHITECTURE, TRANSFORMATION_STATE, OTHER_IDENTITY_FEATURE,
    DIAGNOSTIC_CALLOUT
}
data class CanonicalCharacterForm(val characterId: String, val formId: String, val franchiseId: String) {
    init { require(characterId.isNotBlank() && formId.isNotBlank() && franchiseId.isNotBlank()) }
}
data class CharacterPanel(
    val id: String, val kind: CharacterPanelKind, val referenceIds: Set<String>,
    val priority: Int, val identitySignificant: Boolean = false,
    val requiredInRender: Boolean = false
) { init { require(id.isNotBlank() && priority in 0..100) } }
data class NormalizedCharacterSheet(
    val sheetId: String, val revision: Int, val identity: CanonicalCharacterForm,
    val references: Set<String>, val panels: List<CharacterPanel>,
    val exceptionalCalloutsAllowed: Boolean = false
) { init { require(sheetId.isNotBlank() && revision > 0) } }
object CharacterSheetArchitectureV1 {
    private val order = listOf(
        CharacterPanelKind.PRIMARY_IDENTITY, CharacterPanelKind.FACE, CharacterPanelKind.EYES,
        CharacterPanelKind.HAIR, CharacterPanelKind.BODY, CharacterPanelKind.OUTFIT,
        CharacterPanelKind.SIGNATURE_WEAPON, CharacterPanelKind.EQUIPMENT, CharacterPanelKind.WINGS,
        CharacterPanelKind.HORNS, CharacterPanelKind.TAIL, CharacterPanelKind.MARKINGS,
        CharacterPanelKind.SPECIES_ANATOMY, CharacterPanelKind.MECHANICAL_STRUCTURE,
        CharacterPanelKind.HAIR_ARCHITECTURE, CharacterPanelKind.TRANSFORMATION_STATE,
        CharacterPanelKind.OTHER_IDENTITY_FEATURE, CharacterPanelKind.PALETTE,
        CharacterPanelKind.IDENTITY_INVARIANTS, CharacterPanelKind.DIAGNOSTIC_CALLOUT
    )
    private val extensions = setOf(
        CharacterPanelKind.SIGNATURE_WEAPON, CharacterPanelKind.EQUIPMENT,
        CharacterPanelKind.WINGS, CharacterPanelKind.HORNS, CharacterPanelKind.TAIL,
        CharacterPanelKind.MARKINGS, CharacterPanelKind.SPECIES_ANATOMY,
        CharacterPanelKind.MECHANICAL_STRUCTURE, CharacterPanelKind.HAIR_ARCHITECTURE,
        CharacterPanelKind.TRANSFORMATION_STATE, CharacterPanelKind.OTHER_IDENTITY_FEATURE
    )
    fun validate(sheet: NormalizedCharacterSheet): List<String> {
        val issues = mutableListOf<String>()
        if (sheet.panels.none { it.kind == CharacterPanelKind.PRIMARY_IDENTITY })
            issues += "Missing primary identity view"
        if (sheet.panels.map { it.id }.distinct().size != sheet.panels.size)
            issues += "Duplicate panel ID"
        if (sheet.panels.any { !sheet.references.containsAll(it.referenceIds) })
            issues += "Unknown visual reference"
        val callouts = sheet.panels.count { it.kind == CharacterPanelKind.DIAGNOSTIC_CALLOUT }
        if (callouts > if (sheet.exceptionalCalloutsAllowed) 6 else 4)
            issues += "Detail callouts exceed authorized limit"
        if (sheet.panels.any { it.kind in extensions && (!it.identitySignificant || it.referenceIds.isEmpty()) })
            issues += "Character-specific panels must have identity significance and reference evidence"
        return issues
    }
    fun orderedPanels(sheet: NormalizedCharacterSheet): List<CharacterPanel> {
        val issues = validate(sheet)
        require(issues.isEmpty()) { issues.joinToString("; ") }
        return sheet.panels.sortedWith(compareBy<CharacterPanel> { order.indexOf(it.kind) }
            .thenByDescending { it.priority }.thenBy { it.id })
    }
}
