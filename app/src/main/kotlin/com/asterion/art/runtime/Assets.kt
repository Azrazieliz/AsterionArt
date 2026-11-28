package com.asterion.art.runtime

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.asterion.art.creation.AssetRef
import com.asterion.art.creation.AssetSource
import com.asterion.art.creation.ContentArtifact
import com.asterion.art.creation.ExecutionSignal
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

internal const val NEXUS_LIBRARY = "com.asterion.nexus.library"
internal const val NEXUS_DOCUMENTS = "com.asterion.nexus.terabox.documents"

internal class AssetFiles(private val directory: File) {
    init {
        check(directory.mkdirs() || directory.isDirectory)
    }

    fun put(
        input: InputStream,
        mediaType: String,
        expectedSha256: String? = null,
        signal: ExecutionSignal = ExecutionSignal {},
    ): ContentArtifact {
        val temporary = Files.createTempFile(directory.toPath(), ".", ".incoming")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var bytes = 0L
            FileOutputStream(temporary.toFile()).use { output ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    signal.check()
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                    bytes += count
                }
                output.fd.sync()
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            require(expectedSha256 == null || expectedSha256 == actual) { "The selected asset changed in transit" }
            val receipt = ContentArtifact(actual, mediaType, bytes)
            Files.move(temporary, file(actual).toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            return receipt
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    fun file(sha256: String): File {
        require(sha256.matches(Regex("[a-f0-9]{64}")))
        return File(directory, sha256)
    }

    fun verified(asset: ContentArtifact): File = file(asset.sha256).also {
        check(it.length() == asset.size && it.sha256() == asset.sha256) { "An asset checkpoint is incomplete" }
    }
}

private fun File.sha256(): String = inputStream().use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(1024 * 1024)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        digest.update(buffer, 0, count)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

internal class ReferenceFiles(private val context: Context, private val directory: File) {
    val staged = AssetFiles(File(directory, "references"))

    fun import(uri: Uri): AssetRef {
        require(uri.scheme == "content")
        val resolver = context.contentResolver
        val type = resolver.getType(uri) ?: "application/octet-stream"
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: "Reference"
        val isNexus = uri.authority in setOf(NEXUS_LIBRARY, NEXUS_DOCUMENTS)
        val declaredHash = uri.lastPathSegment?.takeIf {
            uri.authority == NEXUS_LIBRARY && uri.pathSegments.firstOrNull() == "original" &&
                it.matches(Regex("[a-f0-9]{64}"))
        }
        val asset = checkNotNull(resolver.openInputStream(uri)) { "The selected reference is unavailable" }
            .use { staged.put(it, type, declaredHash) }
        return AssetRef(
            id = declaredHash ?: UUID.randomUUID().toString(),
            source = if (isNexus) AssetSource.NEXUS else AssetSource.DOCUMENT,
            locator = uri.toString(),
            name = name,
            sha256 = asset.sha256,
        )
    }

    fun path(reference: AssetRef): File {
        val sha256 = checkNotNull(reference.sha256) { "The reference needs to be staged" }
        if (reference.source != AssetSource.ART_OUTPUT) return staged.file(sha256)
        val runId = reference.locator.substringBefore('/')
        require(UUID.fromString(runId).toString() == runId && reference.locator == "$runId/$sha256")
        return AssetFiles(File(directory, "outputs/$runId")).file(sha256)
    }
}
