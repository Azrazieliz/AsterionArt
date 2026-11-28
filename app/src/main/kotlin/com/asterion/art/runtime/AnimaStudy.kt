package com.asterion.art.runtime

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import com.asterion.art.creation.ContentArtifact
import com.asterion.art.creation.ExecutionSignal
import com.asterion.art.creation.RefRole
import com.asterion.art.creation.Reference
import com.asterion.art.creation.RuntimeUnavailable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

@Serializable
internal data class AnimaStudyAsset(val name: String, val artifact: ContentArtifact) {
    init {
        require(name.isNotBlank() && name == File(name).name && name !in setOf(".", "..", KIT_FILE))
        require(artifact.mediaType == "application/octet-stream")
    }
}

@Serializable
internal data class AnimaStudyKit(
    val version: Int,
    val diffusion: AnimaStudyAsset,
    val textEncoder: AnimaStudyAsset,
    val vae: AnimaStudyAsset,
    val vision: AnimaStudyAsset,
    val referenceAuthor: AnimaStudyAsset,
    val trainingReceipt: AnimaStudyAsset,
    val originalDiffusionSha256: String = diffusion.artifact.sha256,
    val visionSourceSha256: String,
) {
    val all get() = listOf(diffusion, textEncoder, vae, vision, referenceAuthor, trainingReceipt)
    init {
        require(version == 1 && all.map { it.name }.distinct().size == all.size)
        require(listOf(originalDiffusionSha256, visionSourceSha256).all { it.matches(Regex("[a-f0-9]{64}")) })
    }
}

@Serializable
private data class AnimaTrainingReceipt(
    val domain: String,
    @SerialName("source_dataset_ids") val sourceDatasetIds: List<String>,
    @SerialName("vision_family") val visionFamily: String,
    @SerialName("author_sha256") val authorSha256: String,
    @SerialName("diffusion_sha256") val diffusionSha256: String,
    @SerialName("text_encoder_sha256") val textEncoderSha256: String,
    @SerialName("visual_encoder_sha256") val visionSourceSha256: String,
)

private const val KIT_FILE = "anima-study.json"

internal data class AnimaStudyResult(val image: File, val conditioning: AnimaConditioningUsage)

internal class AnimaStudy(context: Context, private val workspace: ArtWorkspace) {
    private val resolver = context.contentResolver
    private val directory = File(workspace.directory, "anima-study")
    private val stored = AssetFiles(File(directory, "weights"))
    private val active = File(directory, KIT_FILE)
    private val budget = DeviceBudget(context)
    private val codec = Json { ignoreUnknownKeys = true }

    fun isStaged(): Boolean = active.isFile

    fun stage(tree: Uri, signal: ExecutionSignal) {
        require(tree.authority == NEXUS_DOCUMENTS && DocumentsContract.isTreeUri(tree)) {
            "Select the Anima study folder from Nexus"
        }
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val files = checkNotNull(resolver.query(children, columns, null, null, null)) {
            "Nexus could not list the selected model folder"
        }.use { rows ->
            buildMap {
                while (rows.moveToNext()) {
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, rows.getString(0))
                    val name = rows.getString(1)
                    require(put(name, uri) == null) { "Nexus returned duplicate model names" }
                }
            }
        }
        fun open(name: String) = checkNotNull(resolver.openInputStream(checkNotNull(files[name]) { "Missing $name" })) {
            "Nexus could not open $name"
        }
        val bytes = open(KIT_FILE).use { it.readNBytes(65537) }
        require(bytes.size < 65537) { "The model manifest is invalid" }
        val kit = codec.decodeFromString<AnimaStudyKit>(bytes.decodeToString())
        for ((name, artifact) in kit.all) {
            signal.check()
            val cached = stored.file(artifact.sha256)
            if (!cached.isFile || cached.length() != artifact.size) {
                open(name).use { input ->
                    check(stored.put(input, artifact.mediaType, artifact.sha256, signal) == artifact)
                }
            } else {
                stored.verified(artifact)
            }
        }
        verifyTraining(kit)
        val staged = Files.createTempFile(directory.toPath(), ".kit", ".incoming")
        try {
            staged.toFile().writeBytes(bytes)
            Files.move(staged, active.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(staged)
        }
    }

    fun render(direction: String, references: List<Reference>, signal: ExecutionSignal): AnimaStudyResult {
        if (!isStaged()) throw RuntimeUnavailable("Stage the Anima study kit first")
        val kit = codec.decodeFromString<AnimaStudyKit>(active.readText())
        val training = verifyTraining(kit)
        val threads = budget.threads()
        val checkedSignal = ExecutionSignal {
            signal.check()
            budget.check()
        }
        val bindings = NativeGraph(stored.verified(kit.vision.artifact), ComputeBackend.CPU, threads).use { vision ->
            NativeGraph(stored.verified(kit.referenceAuthor.artifact), ComputeBackend.CPU, threads).use { author ->
                references.map { source ->
                    checkedSignal.check()
                    val pixels = readProjectionPixels(
                        workspace.references.path(source.asset),
                        imageNet = training.visionFamily == "dinov2",
                    )
                    val embedding = vision.execute(mapOf("pixel_values" to pixels), setOf("image_embeds"), checkedSignal)
                    val role = when (source.role) {
                        RefRole.ELEMENT -> "motif_tokens"
                        RefRole.OUTFIT -> "surface_tokens"
                        else -> throw IllegalArgumentException("Select a motif or an outfit material reference for this study")
                    }
                    val features = author.execute(embedding, setOf(role), checkedSignal).getValue(role)
                    AnimaReference(features, source.region, 0.16f)
                }
            }
        }
        val models = AnimaModels(
            stored.verified(kit.diffusion.artifact), stored.verified(kit.textEncoder.artifact), stored.verified(kit.vae.artifact),
        )
        val request = AnimaImageRequest(direction, 320, 416, 1457, 18, 4f, AnimaBackend.CPU)
        val output = NativeAnima(threads).use { it.render(models, request, bindings, checkedSignal) }
        try {
            val runFiles = workspace.outputFiles(UUID.randomUUID().toString())
            val temporary = Files.createTempFile(directory.toPath(), ".render", ".png")
            try {
                temporary.toFile().outputStream().use { stream ->
                    check(output.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                }
                val artifact = temporary.toFile().inputStream().use { runFiles.put(it, "image/png", signal = checkedSignal) }
                return AnimaStudyResult(runFiles.verified(artifact), output.conditioning)
            } finally {
                Files.deleteIfExists(temporary)
            }
        } finally {
            output.bitmap.recycle()
        }
    }

    private fun verifyTraining(kit: AnimaStudyKit): AnimaTrainingReceipt {
        val receipt = codec.decodeFromString<AnimaTrainingReceipt>(stored.verified(kit.trainingReceipt.artifact).readText())
        require(receipt.domain == "anime" && receipt.sourceDatasetIds.isNotEmpty() &&
            receipt.visionFamily in setOf("clip", "dinov2")) {
            "This study requires an Anime-trained reference author with recorded sources"
        }
        require(
            receipt.authorSha256 == kit.referenceAuthor.artifact.sha256 &&
                receipt.diffusionSha256 == kit.originalDiffusionSha256 &&
                receipt.textEncoderSha256 == kit.textEncoder.artifact.sha256 &&
                receipt.visionSourceSha256 == kit.visionSourceSha256,
        ) { "The reference author, text encoder and renderer do not match the training receipt" }
        return receipt
    }
}
