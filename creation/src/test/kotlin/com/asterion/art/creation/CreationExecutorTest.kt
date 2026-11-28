package com.asterion.art.creation

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CreationExecutorTest {
    @get:Rule val folder = TemporaryFolder()
    private val taxonomy = CharacterTaxonomy.load()

    @Test
    fun artworkResumesPartialPhaseAndPassesOnlyTheWinner() {
        val state = folder.newFolder()
        val workspace = DurableWorkspace(state)
        val theme = reference(RefRole.THEME)
        val style = reference(RefRole.ART_STYLE)
        val subject = reference(RefRole.CHARACTER)
        val request = CreationRequest(
            ArtworkInput(themeCast = ThemeCast(theme.asset.id, 1, listOf(ThemeSubject("heroine", subject.asset.id)))),
            listOf(theme, style, subject),
            seed = 88,
        )
        val run = workspace.create(request)
        val model = RecordingSession(interruptAfter = 4)
        val paused = CreationExecutor(workspace, taxonomy).execute(run.id, ExecutionSignal {}) { _, _ -> model }
        assertEquals(RunStatus.PAUSED, paused.status)
        assertEquals(4, paused.totalRenders)
        assertEquals(1, paused.pendingPhase.size)

        val reopened = DurableWorkspace(state)
        reopened.resume(run.id)
        val result = CreationExecutor(reopened, taxonomy).execute(run.id, ExecutionSignal {}) { _, _ -> model }
        assertEquals(RunStatus.COMPLETE, result.status)
        assertEquals(15, result.totalRenders)
        assertEquals(ArtworkPhase.entries, result.phases.map { it.phase })
        assertEquals(listOf(3, 3, 3, 3, 3), result.phases.map { it.generated })
        assertEquals(15, model.rendered.size)
        assertEquals(1, model.prepared)
        ArtworkPhase.entries.forEachIndexed { index, phase ->
            val expectedParent = result.phases.getOrNull(index - 1)?.winner?.artifact
            assertEquals(setOf(expectedParent), model.rendered.filter { it.phase == phase }.map { it.parent }.toSet())
        }
        assertEquals(10, model.removed.size)
        assertTrue(result.pendingPhase.isEmpty())
        assertNotNull(result.finalImage)
    }

    @Test
    fun characterPreparationFreezesFiveIndependentInputsBeforeRendering() {
        val workspace = DurableWorkspace(folder.newFolder())
        val request = CreationRequest(CharacterInput(listOf("Moth", "Orchid")), route = Route.DESIGN_FIRST, seed = 92)
        workspace.saveDraft(request)
        val run = workspace.create(request)
        val session = RecordingSession()
        val executor = CreationExecutor(workspace, taxonomy)
        val design = executor.execute(run.id, ExecutionSignal {}) { _, _ -> session }
        assertEquals(RunStatus.AWAITING_DESIGN, design.status)
        assertEquals(List(5) { listOf("SC019", "SC086") }, design.frozen!!.characterSlots.map { set -> set.map { it.id } })
        assertEquals(5, design.designs.size)
        assertTrue(session.rendered.isEmpty())
        workspace.approveDesign(run.id)
        val rendered = executor.execute(run.id, ExecutionSignal {}) { _, _ -> session }
        assertEquals(RunStatus.COMPLETE, rendered.status)
        assertEquals(5, rendered.independentImages.map { it.artifact.sha256 }.distinct().size)
        assertEquals(1, session.prepared)
        assertFalse(rendered.phases.isNotEmpty())
        assertEquals(request, workspace.draft(Mode.CHARACTER))
    }

    @Test
    fun unsupportedRuntimeKeepsInputAndDoesNotReportAnOutput() {
        val workspace = DurableWorkspace(Files.createTempDirectory(folder.root.toPath(), "workspace").toFile())
        val request = CreationRequest(ThemeInput(keywords = listOf("veil")), seed = 904)
        val job = workspace.create(request)
        val result = CreationExecutor(workspace, taxonomy).execute(job.id, ExecutionSignal {}) { _, _ ->
            throw RuntimeUnavailable("No trained Theme author is installed")
        }
        assertEquals(RunStatus.BLOCKED, result.status)
        assertEquals(request, result.request)
        assertTrue(result.themes.isEmpty())
    }

    @Test
    fun aRandomMaleIsOutsideTheThemeSpecifiedCount() {
        val theme = reference(RefRole.THEME)
        val character = reference(RefRole.CHARACTER)
        val male = reference(RefRole.CHARACTER, "alternate")
        val cast = ThemeCast(
            theme.asset.id, 1, listOf(ThemeSubject("lead", character.asset.id, "formal armor")),
            "second",
        )
        val chosen = cast.freeze(listOf(theme, character, male), listOf(male.asset), kotlin.random.Random(12))
        assertEquals(2, chosen.size)
        assertEquals(1, cast.specifiedCount)
        assertEquals(male.asset.id, chosen.last().characterAuthority)
        assertEquals(null, chosen.last().clothingOverride)
    }

    @Test
    fun animationUsesAndPersistsTheActualPreviousLastFrame() {
        val workspace = DurableWorkspace(folder.newFolder())
        val request = CreationRequest(
            VideoInput(Mode.ANIMATION, "A quiet scene followed by a reveal", emptyList()),
            seed = 2026,
        )
        val first = Shot("arrival", 3000, "An orchid-lined doorway")
        val second = Shot("reveal", 6000, "The heroine turns toward the stage", previousShotId = first.id)
        val session = RecordingSession(animationPlan = listOf(first, second))
        val queued = workspace.create(request)
        val completed = CreationExecutor(workspace, taxonomy).execute(queued.id, ExecutionSignal {}) { _, _ -> session }
        assertEquals(RunStatus.COMPLETE, completed.status)
        assertEquals(listOf(first, second), completed.plannedShots)
        assertEquals(listOf(null, completed.segments.first().lastFrame), session.continuations)
        assertEquals("video/mp4", completed.video?.mediaType)
    }

    private fun reference(role: RefRole, id: String = role.name) =
        Reference(role, AssetRef(id, AssetSource.DOCUMENT, "content://references/$id", "$id.png"))
}

private class RecordingSession(
    private var interruptAfter: Int? = null,
    private val animationPlan: List<Shot>? = null,
) : CreationSession {
    override val model = ModelIdentity("1".repeat(64), "2".repeat(64), "3".repeat(64))
    var prepared = 0
    val rendered = mutableListOf<ImageRenderRequest>()
    val removed = mutableSetOf<ContentArtifact>()
    val continuations = mutableListOf<ContentArtifact?>()
    override fun close() = Unit

    override fun prepare(request: PreparationRequest, signal: ExecutionSignal): List<PreparedDesign> {
        prepared++
        val tensor = ConditioningTensor(ContentArtifact("f".repeat(64), "application/octet-stream", 4), listOf(1), TensorType.FLOAT32)
        val bindings = request.creation.references.map { ConditionedReference(it.asset.id, it.role, it.subject, it.region, it.priority) }
        val packet = ConditioningPacket(model, mapOf("design_features" to tensor), bindings)
        return List(request.designCount) { index ->
            val fields = DesignField.entries.take(8).associateWith { VisualFact("Prepared feature ${it.name} $index") }
            val views = if (request.creation.mode == Mode.CHARACTER) CharacterView.entries else emptyList()
            PreparedDesign(DesignState(listOf(DesignSubject("design-$index", fields)), emptyMap(), views), packet)
        }
    }

    override fun projectEdits(request: CreationRequest, baseline: PreparedDesign, edited: DesignState, signal: ExecutionSignal) =
        baseline.copy(state = edited)

    override fun render(request: ImageRenderRequest, signal: ExecutionSignal): EvaluatedImage {
        if (rendered.size == interruptAfter) {
            interruptAfter = null
            throw InterruptedException()
        }
        rendered += request
        val artifact = ContentArtifact(rendered.size.toString(16).padStart(64, '0'), "image/png", 100)
        return EvaluatedImage(artifact, QualityAssessment(rendered.size / 100f, true, true))
    }

    override fun removeUnretained(artifact: ContentArtifact) { removed += artifact }
    override fun enhancementChains(creation: CreationRequest) = emptyList<String>()
    override fun enhance(creation: CreationRequest, source: EvaluatedImage, chain: String, signal: ExecutionSignal) = error("Unexpected enhancement")
    override fun correct(request: ImageRenderRequest, source: EvaluatedImage, defect: VisualDefect, signal: ExecutionSignal) = error("Unexpected correction")
    override fun themeUniverse() = error("Unexpected roulette")
    override fun fuseThemeAtoms(draw: AtomDraw, signal: ExecutionSignal) = error("Unexpected Theme atom fusion")
    override fun synthesizeThemes(
        input: ThemeInput, references: List<Reference>, draw: AtomDraw, heavyAtoms: List<HeavyAtom>, signal: ExecutionSignal,
    ) = error("Unexpected Theme synthesis")
    override fun renderShot(
        creation: CreationRequest, shot: Shot, continuation: ContentArtifact?, signal: ExecutionSignal,
    ): VideoSegment {
        checkNotNull(animationPlan)
        continuations += continuation
        val index = continuations.size.toString(16)
        return VideoSegment(
            ContentArtifact(index.padStart(64, 'a'), "video/mp4", 125),
            ContentArtifact(index.padStart(64, 'b'), "image/png", 100),
        )
    }
    override fun planShots(creation: CreationRequest, signal: ExecutionSignal) = checkNotNull(animationPlan)
    override fun finishVideo(
        timeline: VideoInput, segments: List<VideoSegment>, signal: ExecutionSignal,
    ): ContentArtifact {
        require(segments.size == timeline.shots.size)
        return ContentArtifact("c".repeat(64), "video/mp4", 250)
    }
}
