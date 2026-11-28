package com.asterion.art.creation

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DurableWorkspace(
    private val directory: File,
    private val clock: () -> Long = System::currentTimeMillis,
) : RunJournal {
    private val runs = File(directory, "runs")
    private val drafts = File(directory, "drafts")
    private val codec = Json { encodeDefaults = true }

    init {
        check(runs.mkdirs() || runs.isDirectory)
        check(drafts.mkdirs() || drafts.isDirectory)
    }

    @Synchronized
    fun create(request: CreationRequest): CreationRun {
        request.validateForStart()
        val id = UUID.randomUUID().toString()
        return CreationRun(id, request, clock(), clock()).also(::save)
    }

    @Synchronized
    override fun read(id: String): CreationRun = codec.decodeFromString(runFile(id).readText())

    @Synchronized
    fun list(): List<CreationRun> = runs.listFiles().orEmpty()
        .filter { it.extension == "json" }
        .map { codec.decodeFromString<CreationRun>(it.readText()) }
        .sortedByDescending { it.createdAt }

    @Synchronized
    override fun update(id: String, update: (CreationRun) -> CreationRun): CreationRun {
        val previous = read(id)
        val next = update(previous)
        require(previous.id == next.id && previous.request == next.request && previous.createdAt == next.createdAt)
        if (previous == next) return previous
        return next.copy(revision = previous.revision + 1, updatedAt = clock()).also(::save)
    }

    @Synchronized
    fun next(): CreationRun? = list().lastOrNull { it.status == RunStatus.QUEUED }

    @Synchronized
    fun recoverInterrupted() {
        list().filter { it.status == RunStatus.RUNNING }.forEach { run ->
            update(run.id) { it.copy(status = RunStatus.QUEUED, reason = "Recovered from the last checkpoint") }
        }
    }

    fun resume(id: String) = update(id) {
        require(it.status in setOf(RunStatus.BLOCKED, RunStatus.PAUSED, RunStatus.FAILED))
        it.copy(status = RunStatus.QUEUED, reason = null)
    }

    fun cancel(id: String) = update(id) {
        if (it.status == RunStatus.COMPLETE) it else it.copy(status = RunStatus.CANCELLED)
    }

    fun retainCandidate(id: String, sha256: String) = update(id) {
        val image = it.pendingPhase.firstOrNull { candidate -> candidate.artifact.sha256 == sha256 }
        require(image != null) { "This candidate has already been removed" }
        if (sha256 in it.preservedCandidates) it else it.copy(
            preservedCandidates = it.preservedCandidates + sha256, savedImages = it.savedImages + image,
        )
    }

    fun editDesign(id: String, index: Int, design: DesignState) = update(id) {
        require(it.status == RunStatus.AWAITING_DESIGN && index in it.designs.indices)
        design.checkReferences(it.request.references)
        if (it.request.mode == Mode.CHARACTER) design.checkCharacterBoard()
        it.copy(designEdits = it.designEdits + (index to design))
    }

    fun approveDesign(id: String) = update(id) {
        require(it.status == RunStatus.AWAITING_DESIGN)
        it.copy(status = RunStatus.QUEUED, designApproved = true)
    }

    @Synchronized
    fun saveDraft(request: CreationRequest) {
        writeAtomically(File(drafts, "${request.mode.name}.json"), codec.encodeToString(request))
    }

    @Synchronized
    fun draft(mode: Mode): CreationRequest? = File(drafts, "${mode.name}.json")
        .takeIf(File::isFile)?.readText()?.let(codec::decodeFromString)

    private fun runFile(id: String): File {
        require(UUID.fromString(id).toString() == id)
        return File(runs, "$id.json")
    }

    private fun save(run: CreationRun) = writeAtomically(runFile(run.id), codec.encodeToString(run))

    private fun writeAtomically(file: File, contents: String) {
        val temporary = Files.createTempFile(file.parentFile.toPath(), ".", ".tmp")
        try {
            FileOutputStream(temporary.toFile()).use {
                it.write(contents.toByteArray(Charsets.UTF_8))
                it.fd.sync()
            }
            Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
