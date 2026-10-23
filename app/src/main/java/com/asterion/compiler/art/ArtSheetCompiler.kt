package com.asterion.compiler.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class ArtPromptDraft(
    val positive: String,
    val warnings: List<String>,
)

/** First-stage, explicitly text-only extraction. Image conditioning comes later. */
class ArtSheetCompiler(private val context: Context) {
    suspend fun compile(project: ArtProject): ArtPromptDraft = withContext(Dispatchers.IO) {
        val missing = project.problems().filterNot { it.contains("keyframe") }
        require(missing.isEmpty()) { missing.joinToString("; ") }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val problems = mutableListOf<String>()
        try {
            val sections = project.references.map { sheet ->
                val bitmap = thumbnailForOcr(Uri.parse(sheet.uri))
                val recognized = try {
                    Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)),
                        20, TimeUnit.SECONDS)
                } finally { bitmap.recycle() }
                val extracted = recognized.text.trim()
                    .lines().map(String::trim).filter { it.length > 2 }.distinct()
                if (extracted.isEmpty()) {
                    problems += "Could not read text from ${sheet.title}; no attributes were inferred"
                }
                // Bound prompt authoring conservatively until a real model-specific
                // tokenize endpoint is integrated. An overflow is reported.
                val selected = extracted.joinToString(", ")
                val trimmed = selected.take(850)
                if (trimmed.length != selected.length) problems += "${sheet.title}: OCR description clipped at 850 characters"
                "${sheet.kind.name.replace('_', ' ')}: $trimmed"
            }
            check(problems.none { it.startsWith("Could not read") }) {
                problems.joinToString("; ")
            }
            val render = when (project.mode) {
                ArtMode.ANIME -> "Anime illustration, reference-consistent character design"
                ArtMode.REALISTIC -> "Photorealistic fantasy portrayal with plausible materials and lighting"
                ArtMode.COSPLAY -> "Photorealistic cosplay depiction, retain horns, wings, tails, markings and all canonical fantasy traits"
                ArtMode.ANIMATION -> "Animation keyframe preserving the selected visual references"
            }
            ArtPromptDraft(
                positive = (listOf(render) + sections + project.additionalDirection)
                    .filter(String::isNotBlank).joinToString(", "),
                warnings = problems + "Text-only OCR compilation: reference-image conditioning is not yet supported in this flow."
            )
        } finally { recognizer.close() }
    }

    private fun thumbnailForOcr(uri: Uri): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Unable to open reference" }
            BitmapFactory.decodeStream(stream, null, options)
        }
        require(options.outWidth > 0 && options.outHeight > 0) { "Invalid reference image" }
        val longest = maxOf(options.outWidth, options.outHeight)
        var sample = 1
        while (longest / sample > 2600) sample *= 2
        return context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Unable to reopen reference" }
            BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Failed to decode reference")
    }
}
