package com.asterion.art.studio

import com.asterion.art.creation.ArtworkInput
import com.asterion.art.creation.AssetRef
import com.asterion.art.creation.AssetSource
import com.asterion.art.creation.ContentArtifact
import com.asterion.art.creation.CreationRequest
import com.asterion.art.creation.CreationRun
import com.asterion.art.creation.DesignField
import com.asterion.art.creation.EvaluatedImage
import com.asterion.art.creation.Mode
import com.asterion.art.creation.QualityAssessment
import com.asterion.art.creation.RefRole
import com.asterion.art.creation.Reference
import com.asterion.art.creation.Region
import com.asterion.art.creation.RunStatus
import com.asterion.art.creation.SheetInput
import com.asterion.art.creation.ThemeCast
import com.asterion.art.creation.ThemeDesign
import com.asterion.art.creation.ThemeSubject
import com.asterion.art.creation.VisualFact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class StudioDraftTest {
    private fun ref(role: RefRole, id: String, checksum: Char) =
        Reference(role, AssetRef(id, AssetSource.DOCUMENT, "content://documents/$id", id, checksum.toString().repeat(64)))

    @Test
    fun reuseTheSameThemePreservesTheCastAndReplacingItClearsTheOldAuthority() {
        val theme = ref(RefRole.THEME, "library-theme", 'a')
        val cast = ThemeCast("original-theme", 1, listOf(ThemeSubject("lead", "lead-sheet", "Winter coat")))
        val initial = StudioDraft(Mode.ARTWORK).attach(theme, cast)
        assertEquals(cast.copy(authority = theme.asset.id), initial.cast)
        val reimported = initial.attach(ref(RefRole.THEME, "same-file-under-another-uri", 'a'))
        assertEquals(initial, reimported)

        val replacement = ref(RefRole.THEME, "new-theme", 'b')
        val replaced = initial.attach(replacement)
        assertEquals(listOf(replacement), replaced.references)
        assertNull(replaced.cast)
    }

    @Test
    fun projectThemeReusesItsOwnIdentityAuthoritiesAndOnlyClearsTheRemovedOne() {
        val lead = ref(RefRole.CHARACTER, "lead-sheet", 'a')
        val companion = ref(RefRole.CHARACTER, "companion-sheet", 'b')
        val unrelated = ref(RefRole.CHARACTER, "unused-sheet", 'c')
        val subjects = listOf(ThemeSubject("lead", lead.asset.id), ThemeSubject("companion", companion.asset.id))
        val cast = ThemeCast("prepared-theme", 2, subjects)
        val theme = ThemeDesign(
            "winter-market", "Winter market", "The lead and companion meet in the square.",
            mapOf(
                DesignField.COMPOSITION to VisualFact("Two figures framed by market stalls"),
                DesignField.ENVIRONMENT to VisualFact("Stone square in snow"),
                DesignField.POSE to VisualFact("Turned toward one another"),
                DesignField.MATERIALS to VisualFact("Heavy wool and lacquered wood"),
            ),
            emptyList(), emptyList(), cast,
        )
        val sheet = ref(RefRole.THEME, "finished-sheet", 'd')
        val draft = StudioDraft(Mode.ARTWORK).attachThemeSheet(
            sheet.asset, SheetInput(Mode.THEME_SHEET, theme = theme), listOf(lead, companion, unrelated),
        )
        assertEquals(listOf(sheet, lead, companion), draft.references)
        assertEquals(cast.copy(authority = sheet.asset.id), draft.cast)
        assertEquals(listOf(subjects[0].copy(characterAuthority = null), subjects[1]), draft.changeReference(1, null).cast?.subjects)
    }

    @Test
    fun differentRegionalAssignmentsCanReuseTheExactSameCharacter() {
        val character = ref(RefRole.CHARACTER, "character-sheet", 'c')
        val left = character.copy(subject = "lead", region = Region(0f, 0f, .4f, 1f))
        val right = character.copy(subject = "double", region = Region(.6f, 0f, 1f, 1f))
        val draft = StudioDraft(Mode.ARTWORK).attach(left).attach(right)
        assertEquals(listOf(left, right), draft.references)
        assertSame(draft, draft.attach(right))
    }

    @Test
    fun addingAudioCreatesAnEditableCueAndRemovalOnlyUnlinksItsOwnRole() {
        val audio = ref(RefRole.AUDIO, "dialogue", 'd')
        val frame = ref(RefRole.KEYFRAME, "opening", 'e')
        val initial = StudioDraft(
            Mode.ANIMATION, shots = listOf(ShotDraft("shot", openingFrameId = frame.asset.id)),
        ).attach(audio).attach(frame)
        assertEquals(listOf(AudioDraft(audio.asset.id)), initial.audio)
        assertSame(initial, initial.attach(audio))
        val removed = initial.changeReference(0, null)
        assertEquals(emptyList<AudioDraft>(), removed.audio)
        assertEquals(frame.asset.id, removed.shots.single().openingFrameId)
        assertNull(removed.changeReference(0, null).shots.single().openingFrameId)
    }

    @Test
    fun artworkReferencePickerOnlyOffersCompletedSheetsInTheMatchingRole() {
        val artifact = ContentArtifact("f".repeat(64), "image/png", 12)
        val image = EvaluatedImage(artifact, QualityAssessment(.8f, true, true))
        val style = CreationRun(
            "style", CreationRequest(SheetInput(Mode.STYLE_SHEET), seed = 1), 0, 0,
            status = RunStatus.COMPLETE, finalImage = image,
        )
        val character = style.copy(id = "character", request = CreationRequest(SheetInput(Mode.CHARACTER_SHEET), seed = 1))
        val artwork = style.copy(id = "artwork", request = CreationRequest(ArtworkInput(), seed = 1))
        val unfinished = style.copy(id = "unfinished", status = RunStatus.RUNNING)
        val all = listOf(style, character, artwork, unfinished)
        assertEquals(listOf(style to artifact), referenceOutputs(RefRole.ART_STYLE, all))
        assertEquals(listOf(character to artifact), referenceOutputs(RefRole.CHARACTER, all))
        assertEquals("${style.id}/${artifact.sha256}", outputReference(style, artifact).locator)
    }
}
