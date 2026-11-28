package com.asterion.art.creation

import java.io.IOException
import kotlin.random.Random
import kotlinx.serialization.Serializable

@Serializable
enum class ArtworkPhase {
    FOUNDATION, IDENTITY_GEOMETRY, MATERIAL_LIGHT, FINE_CONSTRUCTION, FINAL_COHERENCE,
}

@Serializable
enum class RunStatus {
    QUEUED, RUNNING, AWAITING_DESIGN, PAUSED, BLOCKED, FAILED, CANCELLED, COMPLETE,
}

@Serializable
enum class DefectScope { LOCAL, GLOBAL }

@Serializable
data class VisualDefect(val reason: String, val scope: DefectScope, val region: Region? = null) {
    init {
        require(reason.isNotBlank())
        require(scope != DefectScope.LOCAL || region != null)
    }
}

@Serializable
data class QualityAssessment(
    val aesthetics: Float,
    val requirementsSatisfied: Boolean,
    val protectedFeaturesPreserved: Boolean,
    val defects: List<VisualDefect> = emptyList(),
) {
    init {
        require(aesthetics in 0f..1f)
    }

    val valid get() = requirementsSatisfied && protectedFeaturesPreserved && defects.isEmpty()
}

@Serializable
data class EvaluatedImage(val artifact: ContentArtifact, val assessment: QualityAssessment) {
    init {
        require(artifact.mediaType.startsWith("image/"))
    }
}

@Serializable
data class PreparedDesign(val state: DesignState, val packet: ConditioningPacket)

@Serializable
data class PhaseProgress(
    val phase: ArtworkPhase,
    val winner: EvaluatedImage,
    val generated: Int,
)

@Serializable
data class VideoSegment(val video: ContentArtifact, val lastFrame: ContentArtifact) {
    init {
        require(video.mediaType.startsWith("video/") && lastFrame.mediaType.startsWith("image/"))
    }
}

@Serializable
data class CreationRun(
    val id: String,
    val request: CreationRequest,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 0,
    val status: RunStatus = RunStatus.QUEUED,
    val reason: String? = null,
    val model: ModelIdentity? = null,
    val frozen: FrozenPreparation? = null,
    val designs: List<PreparedDesign> = emptyList(),
    val designEdits: Map<Int, DesignState> = emptyMap(),
    val designApproved: Boolean = false,
    val phases: List<PhaseProgress> = emptyList(),
    val pendingPhase: List<EvaluatedImage> = emptyList(),
    val preservedCandidates: Set<String> = emptySet(),
    val savedImages: List<EvaluatedImage> = emptyList(),
    val pendingRemoval: List<ContentArtifact> = emptyList(),
    val independentImages: List<EvaluatedImage> = emptyList(),
    val finalImage: EvaluatedImage? = null,
    val themes: List<ThemeDesign> = emptyList(),
    val segments: List<VideoSegment> = emptyList(),
    val video: ContentArtifact? = null,
) {
    val totalRenders: Int get() = phases.sumOf { it.generated } + pendingPhase.size + independentImages.size
}

interface RunJournal {
    fun read(id: String): CreationRun
    fun update(id: String, update: (CreationRun) -> CreationRun): CreationRun
}

class RuntimeUnavailable(message: String) : IOException(message)
class ExecutionPaused(message: String) : IOException(message)

fun interface ExecutionSignal {
    fun check()
}

data class PreparationRequest(
    val creation: CreationRequest,
    val frozen: FrozenPreparation,
    val designCount: Int,
)

data class ImageRenderRequest(
    val creation: CreationRequest,
    val design: PreparedDesign,
    val seed: Long,
    val phase: ArtworkPhase? = null,
    val parent: ContentArtifact? = null,
)

data class ThemeUniverse(val pool: ThemeAtomPool, val drawCount: Int)

/**
 * Each method runs an installed learned operator or exposes a runtime failure.
 * The executor owns workflow transitions and keeps model-provided data unchanged.
 */
interface CreationSession : AutoCloseable {
    val model: ModelIdentity
    fun prepare(request: PreparationRequest, signal: ExecutionSignal): List<PreparedDesign>
    fun projectEdits(
        request: CreationRequest, baseline: PreparedDesign, edited: DesignState, signal: ExecutionSignal,
    ): PreparedDesign
    fun render(request: ImageRenderRequest, signal: ExecutionSignal): EvaluatedImage
    fun enhancementChains(creation: CreationRequest): List<String>
    fun enhance(
        creation: CreationRequest, source: EvaluatedImage, chain: String, signal: ExecutionSignal,
    ): EvaluatedImage
    fun correct(
        request: ImageRenderRequest, source: EvaluatedImage, defect: VisualDefect, signal: ExecutionSignal,
    ): EvaluatedImage
    fun themeUniverse(): ThemeUniverse
    fun fuseThemeAtoms(draw: AtomDraw, signal: ExecutionSignal): List<HeavyAtom>
    fun synthesizeThemes(
        input: ThemeInput, references: List<Reference>, draw: AtomDraw,
        heavyAtoms: List<HeavyAtom>, signal: ExecutionSignal,
    ): List<ThemeDesign>
    fun renderShot(
        creation: CreationRequest, shot: Shot, continuation: ContentArtifact?, signal: ExecutionSignal,
    ): VideoSegment
    fun finishVideo(
        timeline: VideoInput, segments: List<VideoSegment>, signal: ExecutionSignal,
    ): ContentArtifact
    fun removeUnretained(artifact: ContentArtifact)
}

class CreationExecutor(private val journal: RunJournal, private val taxonomy: CharacterTaxonomy) {
    fun execute(
        id: String,
        batteryAndThermals: ExecutionSignal,
        openSession: (CreationRun, ExecutionSignal) -> CreationSession,
    ): CreationRun {
        val started = journal.update(id) {
            require(it.status == RunStatus.QUEUED) { "Only an explicitly queued creation can run" }
            it.copy(status = RunStatus.RUNNING, reason = null)
        }
        val signal = ExecutionSignal {
            if (journal.read(id).status != RunStatus.RUNNING) throw InterruptedException("Creation stopped")
            batteryAndThermals.check()
        }
        return try {
            signal.check()
            openSession(started, signal).use { session ->
                val run = checkpoint(id) {
                    require(it.model == null || it.model == session.model) {
                        "Resume requires the original set of model weights"
                    }
                    it.copy(model = session.model)
                }
                val prepared = freezeInputs(run, session)
                when (prepared.request.input) {
                    is ThemeInput -> themes(prepared, session, signal)
                    is VideoInput -> video(prepared, session, signal)
                    else -> images(prepared, session, signal)
                }
            }
        } catch (error: InterruptedException) {
            stop(id, RunStatus.PAUSED, "Interrupted at the last checkpoint")
        } catch (error: ExecutionPaused) {
            stop(id, RunStatus.PAUSED, error.message.orEmpty())
        } catch (error: RuntimeUnavailable) {
            stop(id, RunStatus.BLOCKED, error.message.orEmpty())
        } catch (error: Exception) {
            stop(id, RunStatus.FAILED, error.message ?: "Creation could not complete")
        }
    }

    private fun freezeInputs(run: CreationRun, session: CreationSession): CreationRun {
        if (run.frozen != null) return run
        val request = run.request
        val resolved = when (val input = request.input) {
            is CharacterInput -> {
                val random = Random(request.seed)
                val slots = List(input.count) {
                    input.slots.map { slot ->
                        when (val selection = taxonomy.select(slot, random)) {
                            is ConceptSelection.Frozen -> selection.value
                            is ConceptSelection.Clarify -> throw RuntimeUnavailable("Refine the selector ${selection.query}")
                        }
                    }
                }
                FrozenPreparation(characterSlots = slots, taxonomyHash = taxonomy.sourceSha256)
            }
            is ArtworkInput -> {
                val pool = request.references.map { it.asset }.filter { it.id in input.randomMalePoolIds }
                val cast = checkNotNull(input.themeCast).freeze(request.references, pool, Random(request.seed))
                FrozenPreparation(cast = cast)
            }
            is ThemeInput -> {
                val universe = session.themeUniverse()
                FrozenPreparation(atomDraw = universe.pool.sample(universe.drawCount, request.seed))
            }
            else -> FrozenPreparation()
        }
        return checkpoint(run.id) { it.copy(frozen = resolved) }
    }

    private fun images(run: CreationRun, session: CreationSession, signal: ExecutionSignal): CreationRun {
        val request = run.request
        val count = when (val input = request.input) {
            is CharacterInput -> input.count
            is OriginalInput -> input.count
            else -> 1
        }
        var current = run
        current = clearDiscarded(current, session)
        if (current.designs.isEmpty()) {
            signal.check()
            val designs = session.prepare(PreparationRequest(request, checkNotNull(run.frozen), count), signal)
            require(designs.size == count) { "The artistic model returned an incomplete design population" }
            designs.forEach { checkDesign(it, request, session.model) }
            current = checkpoint(run.id) { it.copy(designs = designs) }
        }
        if (request.route == Route.DESIGN_FIRST && !current.designApproved) {
            return checkpoint(run.id) { it.copy(status = RunStatus.AWAITING_DESIGN) }
        }
        if (current.designEdits.isNotEmpty()) {
            val updated = current.designs.toMutableList()
            current.designEdits.forEach { (index, state) ->
                signal.check()
                val projected = session.projectEdits(request, updated[index], state, signal)
                require(projected.state == state) { "The projection changed the approved design" }
                checkDesign(projected, request, session.model)
                updated[index] = projected
            }
            current = checkpoint(run.id) { it.copy(designs = updated, designEdits = emptyMap()) }
        }
        if (request.input is ArtworkInput) return artwork(current, session, signal)
        val random = Random(request.seed)
        current.designs.forEachIndexed { index, design ->
            val seed = random.nextLong()
            if (index < current.independentImages.size) return@forEachIndexed
            signal.check()
            val command = ImageRenderRequest(request, design, seed)
            val raw = session.render(command, signal)
            val accepted = repairIfNeeded(command, raw, session, signal)
            require(accepted.assessment.valid) { "Design ${index + 1} has unresolved visual defects" }
            current = checkpoint(run.id) { it.copy(independentImages = it.independentImages + accepted) }
            if (raw.artifact != accepted.artifact) session.removeUnretained(raw.artifact)
        }
        return complete(run.id)
    }

    private fun artwork(run: CreationRun, session: CreationSession, signal: ExecutionSignal): CreationRun {
        val request = run.request
        val input = request.input as ArtworkInput
        val random = Random(request.seed)
        var current = run
        for (phase in ArtworkPhase.entries) {
            if (phase.ordinal < current.phases.size) {
                repeat(input.candidatesPerPhase) { random.nextLong() }
                continue
            }
            val parent = current.phases.lastOrNull()?.winner?.artifact
            repeat(input.candidatesPerPhase) { index ->
                val seed = random.nextLong()
                if (index >= current.pendingPhase.size) {
                    signal.check()
                    val command = ImageRenderRequest(request, current.designs.single(), seed, phase, parent)
                    val candidate = session.render(command, signal)
                    current = checkpoint(run.id) { it.copy(pendingPhase = it.pendingPhase + candidate) }
                }
            }
            val candidates = current.pendingPhase
            val winner = candidates.filter { it.assessment.valid }.maxByOrNull { it.assessment.aesthetics }
                ?: error("No valid candidate passed ${phase.name.lowercase().replace('_', ' ')}")
            current = checkpoint(run.id) { latest ->
                val winnerIds = latest.phases.map { it.winner.artifact.sha256 }.toSet() + winner.artifact.sha256
                latest.copy(
                    phases = latest.phases + PhaseProgress(phase, winner, candidates.size),
                    pendingPhase = emptyList(),
                    pendingRemoval = candidates.map { it.artifact }.distinct()
                        .filter { it.sha256 !in winnerIds && it.sha256 !in latest.preservedCandidates },
                )
            }
            current = clearDiscarded(current, session)
        }
        val raw = current.phases.last().winner
        var best = raw
        for (chain in session.enhancementChains(request).distinct()) {
            signal.check()
            val enhanced = session.enhance(request, raw, chain, signal)
            if (enhanced.assessment.valid && enhanced.assessment.aesthetics > best.assessment.aesthetics) {
                if (best.artifact != raw.artifact) session.removeUnretained(best.artifact)
                best = enhanced
            } else if (enhanced.artifact != best.artifact && enhanced.artifact != raw.artifact) {
                session.removeUnretained(enhanced.artifact)
            }
        }
        return checkpoint(run.id) { it.copy(status = RunStatus.COMPLETE, finalImage = best) }
    }

    private fun repairIfNeeded(
        request: ImageRenderRequest, source: EvaluatedImage, session: CreationSession, signal: ExecutionSignal,
    ): EvaluatedImage {
        if (source.assessment.valid) return source
        val defects = source.assessment.defects
        signal.check()
        val candidate = if (
            source.assessment.protectedFeaturesPreserved &&
            defects.size == 1 && defects.single().scope == DefectScope.LOCAL
        ) {
            session.correct(request, source, defects.single(), signal)
        } else {
            session.render(request.copy(seed = Random(request.seed).nextLong()), signal)
        }
        if (candidate.assessment.valid) return candidate
        if (candidate.artifact != source.artifact) session.removeUnretained(candidate.artifact)
        return source
    }

    private fun clearDiscarded(run: CreationRun, session: CreationSession): CreationRun {
        if (run.pendingRemoval.isEmpty()) return run
        run.pendingRemoval.forEach(session::removeUnretained)
        return checkpoint(run.id) { it.copy(pendingRemoval = emptyList()) }
    }

    private fun themes(run: CreationRun, session: CreationSession, signal: ExecutionSignal): CreationRun {
        signal.check()
        val input = run.request.input as ThemeInput
        val frozen = checkNotNull(run.frozen)
        val draw = checkNotNull(frozen.atomDraw)
        val frozenIds = draw.atoms.map { it.id }.toSet()
        val molecules = frozen.heavyAtoms ?: session.fuseThemeAtoms(draw, signal).also { heavyAtoms ->
            require(heavyAtoms.all { atom -> atom.components.all(frozenIds::contains) })
            checkpoint(run.id) { it.copy(frozen = frozen.copy(heavyAtoms = heavyAtoms)) }
        }
        signal.check()
        val themes = session.synthesizeThemes(input, run.request.references, draw, molecules, signal)
        require(themes.size == 5 && themes.map { it.id }.distinct().size == 5)
        require(themes.map { it.title.trim().lowercase() }.distinct().size == 5)
        require(themes.map { theme ->
            listOf(DesignField.COMPOSITION, DesignField.ENVIRONMENT, DesignField.POSE).map {
                theme.visualSystems.getValue(it).value.trim().lowercase()
            }
        }.distinct().size == 5) { "Theme synthesis did not produce five distinct visual situations" }
        require(themes.all { it.atomIds.all(frozenIds::contains) && it.keywords == input.keywords })
        return checkpoint(run.id) { it.copy(status = RunStatus.COMPLETE, themes = themes) }
    }

    private fun video(run: CreationRun, session: CreationSession, signal: ExecutionSignal): CreationRun {
        val timeline = run.request.input as VideoInput
        var current = run
        timeline.shots.drop(current.segments.size).forEach { shot ->
            signal.check()
            val segment = session.renderShot(run.request, shot, current.segments.lastOrNull()?.lastFrame, signal)
            current = checkpoint(run.id) { it.copy(segments = it.segments + segment) }
        }
        signal.check()
        val result = session.finishVideo(timeline, current.segments, signal)
        require(result.mediaType.startsWith("video/"))
        return checkpoint(run.id) { it.copy(status = RunStatus.COMPLETE, video = result) }
    }

    private fun checkDesign(design: PreparedDesign, request: CreationRequest, model: ModelIdentity) {
        design.state.checkReferences(request.references)
        design.packet.verify(model, request.references)
        if (request.mode == Mode.CHARACTER) design.state.checkCharacterBoard()
    }

    private fun complete(id: String): CreationRun = checkpoint(id) { it.copy(status = RunStatus.COMPLETE) }

    private fun checkpoint(id: String, change: (CreationRun) -> CreationRun): CreationRun =
        journal.update(id) {
            if (it.status != RunStatus.RUNNING) throw InterruptedException("Creation stopped")
            change(it)
        }

    private fun stop(id: String, status: RunStatus, reason: String): CreationRun = journal.update(id) {
        if (it.status == RunStatus.RUNNING) it.copy(status = status, reason = reason) else it
    }
}
