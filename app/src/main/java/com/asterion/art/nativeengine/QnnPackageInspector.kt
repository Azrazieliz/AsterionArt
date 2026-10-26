package com.asterion.art.nativeengine

/**
 * Inspects archive member names without ever reading weights into memory.
 *
 * Family markers are path components, not substrings in arbitrary filenames:
 * a ZIP containing a checkpoint called "sdxl_finetune.safetensors" must NOT
 * be classified as a converted SDXL package.
 */
object QnnPackageInspector {
    data class Member(val path: String, val bytes: Long, val directory: Boolean = false)
    data class Report(
        val detectedFamilies: Set<String>,
        val compiledBins: List<String>,
        val originalWeights: List<String>,
        val markerParent: String?,
        val files: Int
    )

    private val familyMarkers = mapOf(
        "anima" to "anima", "npucustom" to "sd15npu", "sdxl" to "sdxl",
        "zimage" to "zimage", "flux2_klein" to "klein",
        "qwen_image_2_1" to "qwen21", "krea_2_turbo" to "krea2turbo"
    )

    fun inspect(members: Iterable<Member>): Report {
        val marked = mutableListOf<Pair<String, String>>()
        val compiled = mutableListOf<String>()
        val originals = mutableListOf<String>()
        var files = 0
        members.forEach { member ->
            val name = member.path.replace('\\', '/').trim('/')
            val parts = name.split('/').filter { it.isNotEmpty() }
            for (i in parts.indices) {
                // A marker is a directory or a standalone marker file,
                // not part of another filename or a filename extension.
                val family = familyMarkers[parts[i].lowercase()] ?: continue
                if (i < parts.lastIndex || member.directory || member.bytes <= 4096L) {
                    marked += family to parts.take(i).joinToString("/")
                }
            }
            if (member.directory) return@forEach
            files++
            val extension = parts.lastOrNull()?.substringAfterLast('.', "")?.lowercase().orEmpty()
            if (extension == "bin" && member.bytes > 1024L * 1024L) compiled += name
            if (extension in setOf("safetensors", "gguf", "ckpt", "pt", "pth")) originals += name
        }
        val parent = marked.minByOrNull { it.second.count { c -> c == '/' } }?.second
        return Report(marked.map { it.first }.toSet(), compiled, originals, parent, files)
    }

    fun validate(report: Report, selectedFamily: String?): String {
        require(report.detectedFamilies.size <= 1) {
            "Conflicting QNN model markers: " + report.detectedFamilies.sorted().joinToString()
        }
        require(report.compiledBins.isNotEmpty()) {
            if (report.originalWeights.isNotEmpty())
                "Original checkpoints found (" + report.originalWeights.take(3).joinToString() +
                    ") but no compiled QNN .bin tensor files. Conversion is required."
            else
                "Missing compiled QNN .bin tensor files. A standard ZIP is not a runnable QNN model."
        }
        val detected = report.detectedFamilies.firstOrNull()
        if (detected != null && selectedFamily != null) require(detected == selectedFamily) {
            "Selected QNN family $selectedFamily conflicts with detected $detected"
        }
        val family = detected ?: selectedFamily
            ?: error("Compiled .bin data exists, but the QNN model family is unknown. " +
                "Select the correct converted family (ANIMA/SDXL/etc.); selecting it does not convert weights.")
        require(report.files >= 3) {
            "Incomplete QNN archive: only ${report.files} files; model dependencies are missing."
        }
        return family
    }
}
