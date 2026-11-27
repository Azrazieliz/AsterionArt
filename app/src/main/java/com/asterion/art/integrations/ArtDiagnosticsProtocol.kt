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