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
    const val MAX_PAGE_CHARS = 6000
    private val jobIdPattern = Regex("[A-Za-z0-9_-]{8,90}")
    private val artifacts = setOf(
        "native-prompt-trace",
        "brain-plan",
        "native-stage-inputs",
        "last-failure",
        "model-load-ms",
        "character-capsules"
    )

    fun validJobId(value: String): Boolean = jobIdPattern.matches(value)

    fun validArtifact(value: String): Boolean = value in artifacts

    fun artifactFile(filesDir: File, jobId: String, artifact: String): File {
        require(validJobId(jobId)) { "Invalid generation job ID" }
        require(validArtifact(artifact)) { "Unsupported diagnostic artifact" }
        val plans = File(filesDir, "generation-plans").canonicalFile
        val checkpoints = File(filesDir, "generation-checkpoints").canonicalFile
        val file = when (artifact) {
            "brain-plan" -> File(plans, "$jobId-brain.json")
            "character-capsules" -> File(plans, "$jobId-capsules.json")
            "native-prompt-trace" -> File(checkpoints, "$jobId/native-prompt-trace.json")
            "native-stage-inputs" -> File(checkpoints, "$jobId/actual-native-stage-inputs.jsonl")
            "last-failure" -> File(checkpoints, "$jobId/last-failure.txt")
            "model-load-ms" -> File(checkpoints, "$jobId/model-load-ms.txt")
            else -> error("Unsupported diagnostic artifact")
        }.canonicalFile
        require(file.path.startsWith(plans.path + File.separator) ||
            file.path.startsWith(checkpoints.path + File.separator)) {
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