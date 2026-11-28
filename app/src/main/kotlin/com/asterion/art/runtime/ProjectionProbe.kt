package com.asterion.art.runtime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import com.asterion.art.creation.ExecutionSignal
import java.io.File
import kotlin.system.measureTimeMillis

internal class ProjectionProbe(context: Context, private val workspace: ArtWorkspace) {
    private val app = context.applicationContext
    private val modelFiles = AssetFiles(File(workspace.directory, "projection-study"))
    private val budget = DeviceBudget(app)

    enum class Part(val label: String, val checksum: String, val output: String = "", val shape: List<Int> = emptyList()) {
        ENCODER("Compact visual encoder", "583fd1110a514667812fee7d684952aaf82a99b959760c8d7dca7e0ab9839299"),
        IP_ADAPTER("SD 1.5 reference projection", "700af9c61bd0e116a561284e0b30ac79da6817b2114762243e68dd40894c9b29",
            "ip_adapter_image_embeds", listOf(1, 1024)),
        RIN_ANIMA("Rin Anima reference projection", "790bf54a0a3bcd28b34aff02730d3a148ce4e642597feb8c1afc12fbf8d805b2",
            "anima_reference_tokens", listOf(1, 1, 1024)),
    }

    fun stage(part: Part, asset: Uri) {
        require(asset.scheme == "content" && asset.authority in setOf(NEXUS_DOCUMENTS, NEXUS_LIBRARY)) {
            "Select the staged model from Nexus"
        }
        val stream = checkNotNull(app.contentResolver.openInputStream(asset)) { "Nexus could not open the model" }
        stream.use { modelFiles.put(it, "application/octet-stream", part.checksum) }
    }

    fun stagedParts(): Set<Part> = Part.entries.filter { modelFiles.file(it.checksum).isFile }.toSet()

    fun canCheck(parts: Set<Part>): Boolean = Part.ENCODER in parts && parts.any { it.output.isNotEmpty() }

    fun check(referenceUri: Uri): String {
        val staged = stagedParts()
        require(canCheck(staged)) { "Stage the visual encoder and at least one trained projection" }
        val reference = workspace.references.path(workspace.references.import(referenceUri))
        val input = readProjectionPixels(reference)
        val signal = ExecutionSignal { budget.check() }
        val threads = budget.threads()
        return NativeGraph(modelFiles.file(Part.ENCODER.checksum), ComputeBackend.CPU, threads).use { encoder ->
            staged.filter { it.output.isNotEmpty() }.joinToString("\n") { part ->
                var shape: List<Int> = emptyList()
                val elapsed = measureTimeMillis {
                    NativeGraph(modelFiles.file(part.checksum), ComputeBackend.CPU, threads).use { projector ->
                        shape = ReferenceProjection(encoder, projector, part.output).project(input, signal).dimensions
                    }
                }
                check(shape == part.shape) { "The result does not match the expected reference tensor" }
                "${part.label} · CPU · ${shape.joinToString(" × ")} features · $elapsed ms"
            }
        }
    }

}

@Suppress("DEPRECATION")
internal fun readProjectionPixels(file: File, imageNet: Boolean = false): FloatTensor {
        val target = 224
        val decoder = checkNotNull(BitmapRegionDecoder.newInstance(file.absolutePath, false)) {
            "The reference must be a decodable image"
        }
        val cropped = try {
            val shortest = minOf(decoder.width, decoder.height)
            val left = (decoder.width - shortest) / 2
            val top = (decoder.height - shortest) / 2
            var sample = 1
            while (shortest / sample >= target * 2) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            checkNotNull(decoder.decodeRegion(Rect(left, top, left + shortest, top + shortest), options))
        } finally {
            decoder.recycle()
        }
        val sized = Bitmap.createScaledBitmap(cropped, target, target, true)
        try {
            val pixels = IntArray(target * target)
            sized.getPixels(pixels, 0, target, 0, 0, target, target)
            val values = FloatArray(pixels.size * 3)
            val offsets = intArrayOf(16, 8, 0)
            val means = if (imageNet) floatArrayOf(0.485f, 0.456f, 0.406f)
                else floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
            val deviations = if (imageNet) floatArrayOf(0.229f, 0.224f, 0.225f)
                else floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)
            for (channel in 0..2) {
                for (index in pixels.indices) {
                    val value = ((pixels[index] shr offsets[channel]) and 255) / 255f
                    values[channel * pixels.size + index] = (value - means[channel]) / deviations[channel]
                }
            }
            return FloatTensor(listOf(1, 3, target, target), values)
        } finally {
            if (sized !== cropped) sized.recycle()
            cropped.recycle()
        }
}
