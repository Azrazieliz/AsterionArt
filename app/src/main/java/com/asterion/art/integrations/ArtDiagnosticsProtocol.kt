package com.asterion.art.integrations

import java.io.File

data class ArtDiagnosticPage(
    val text: String,
    val offset: Int,
    val totalChars: Int,
    val nextOffset: Int?
)

/** Pure allowlist and paging rules shared by the exported partner provider and JVM tests. */
object ArtDiagnosticsProtocol {
    const val MAX_PAGE_CHARS = 3000
    private val jobIdPattern = Regex("[A-Za-z0-9_-]{8,90}")
    private val artifacts = setOf(
        "job-plan",
        "native-prompt-trace",
        "brain-plan",
        "native-stage-inputs",
        "last-failure",
        "model-load-ms",
        "character-capsules",
        "character-plan",
        "character-telemetry",
        "character-visual-audit"
    )

    fun validJobId(value: String): Boolean = jobIdPattern.matches(value)

    fun validArtifact(value: String): Boolean = value in artifacts

    fun artifactFile(filesDir: File, jobId: String, artifact: String, characterPlanId: String? = null): File {
        require(validJobId(jobId)) { "Invalid generation job ID" }
        require(validArtifact(artifact)) { "Unsupported diagnostic artifact" }
        val plans = File(filesDir, "generation-plans").canonicalFile
        val checkpoints = File(filesDir, "generation-checkpoints").canonicalFile
        val characterPlans = File(filesDir, "character-phase1/plans").canonicalFile
        val characterTelemetry = File(filesDir, "character-phase1/telemetry").canonicalFile
        val visualAssessments = File(filesDir, "vision-assessments").canonicalFile
        val file = when (artifact) {
            "job-plan" -> File(plans, "$jobId.json")
            "brain-plan" -> File(plans, "$jobId-brain.json")
            "character-capsules" -> File(plans, "$jobId-capsules.json")
            "character-plan" -> {
                require(validJobId(characterPlanId.orEmpty())) { "Invalid character plan ID" }
                File(characterPlans, "$characterPlanId.json")
            }
            "character-telemetry" -> {
                require(validJobId(characterPlanId.orEmpty())) { "Invalid character plan ID" }
                File(characterTelemetry, "$characterPlanId.json")
            }
            "character-visual-audit" -> File(visualAssessments, "$jobId.json")
            "native-prompt-trace" -> File(checkpoints, "$jobId/native-prompt-trace.json")
            "native-stage-inputs" -> File(checkpoints, "$jobId/actual-native-stage-inputs.jsonl")
            "last-failure" -> File(checkpoints, "$jobId/last-failure.txt")
            "model-load-ms" -> File(checkpoints, "$jobId/model-load-ms.txt")
            else -> error("Unsupported diagnostic artifact")
        }.canonicalFile
        val allowedRoots = listOf(plans, checkpoints, characterPlans, characterTelemetry, visualAssessments)
        require(allowedRoots.any { file.path.startsWith(it.path + File.separator) }) {
            "Diagnostic path escaped the app-private data directory"
        }
        return file
    }

    private const val MAX_BRIDGE_EVENT_LINES = 128
    private const val MAX_BRIDGE_EVENT_BYTES = 262_144L
    private val bridgeEventLock = Any()

    private fun bridgeEventsFile(filesDir: File): File {
        val root = File(filesDir, "asterion-control-bridge").canonicalFile
        val file = File(root, "events.jsonl").canonicalFile
        require(file.path.startsWith(root.path + File.separator)) {
            "Bridge event journal escaped its private directory"
        }
        return file
    }

    /** Store one metadata-only provider-call record; never store request payloads or prompt text. */
    fun appendBridgeEvent(filesDir: File, serializedEvent: String) = synchronized(bridgeEventLock) {
        require(serializedEvent.isNotBlank() && serializedEvent.length <= 2048 &&
            serializedEvent.none { it == '\n' || it == '\r' }) {
            "Invalid bridge event record"
        }
        val file = bridgeEventsFile(filesDir)
        file.parentFile?.mkdirs()
        file.appendText(serializedEvent + "\n", Charsets.UTF_8)
        if (file.length() > MAX_BRIDGE_EVENT_BYTES) {
            val recent = file.readLines(Charsets.UTF_8).takeLast(MAX_BRIDGE_EVENT_LINES)
            val temporary = File(file.parentFile, "events.jsonl.tmp")
            temporary.writeText(
                if (recent.isEmpty()) "" else recent.joinToString("\n", postfix = "\n"),
                Charsets.UTF_8
            )
            try {
                try {
                    java.nio.file.Files.move(
                        temporary.toPath(), file.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (_: Exception) {
                    java.nio.file.Files.move(
                        temporary.toPath(), file.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                }
            } finally {
                temporary.delete()
            }
        }
    }

    /** Return only the newest bounded metadata records for owner-approved diagnostics. */
    fun readBridgeEvents(filesDir: File, limit: Int = MAX_BRIDGE_EVENT_LINES): String =
        synchronized(bridgeEventLock) {
            require(limit in 1..MAX_BRIDGE_EVENT_LINES) { "Invalid bridge event count" }
            val file = bridgeEventsFile(filesDir)
            if (!file.isFile) return@synchronized ""
            file.readLines(Charsets.UTF_8).takeLast(limit).joinToString("\n")
        }

    fun page(text: String, offset: Int, limit: Int): ArtDiagnosticPage {
        require(offset >= 0 && offset <= text.length) { "Invalid diagnostic offset" }
        require(limit in 1..MAX_PAGE_CHARS) { "Invalid diagnostic page size" }
        val end = (offset + limit).coerceAtMost(text.length)
        return ArtDiagnosticPage(
            text = text.substring(offset, end),
            offset = offset,
            totalChars = text.length,
            nextOffset = end.takeIf { it < text.length }
        )
    }
}