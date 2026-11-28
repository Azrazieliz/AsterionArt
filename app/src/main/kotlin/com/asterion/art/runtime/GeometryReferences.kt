package com.asterion.art.runtime

import com.asterion.art.creation.ContentArtifact
import com.asterion.art.creation.ExecutionSignal
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val GLB_MAGIC = 0x46546c67
private const val ZIP_MAGIC = 0x04034b50

internal fun stageGeometry(
    input: InputStream,
    scratch: File,
    destination: AssetFiles,
    sha256: String?,
    signal: ExecutionSignal,
): ContentArtifact {
    val stream = BufferedInputStream(input)
    return when (stream.header(4).int) {
        GLB_MAGIC -> stageGlb(stream, destination, sha256, signal)
        ZIP_MAGIC -> {
            val temporary = Files.createTempFile(scratch.toPath(), ".foundry-", ".zip")
            try {
                Files.newOutputStream(temporary).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        signal.check()
                        val count = stream.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
                val archive = temporary.toFile()
                require(sha256 == null || archive.sha256(signal) == sha256) { "The selected package changed in transit" }
                stageFoundryPackage(archive, destination, signal)
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
        else -> throw IllegalArgumentException("Choose a GLB or a Foundry editable package")
    }
}

private fun stageFoundryPackage(archive: File, destination: AssetFiles, signal: ExecutionSignal): ContentArtifact =
    ZipFile(archive).use { zip ->
        val files = zip.entries().asSequence()
            .filter { it.name == "import-manifest.json" || it.name == "asset.glb" }
            .groupBy { it.name }
        val manifest = requireNotNull(files["import-manifest.json"]?.singleOrNull()) { "The Foundry import manifest is missing or duplicated" }
        require(!manifest.isDirectory && manifest.size in 1L..65_536L) { "The Foundry manifest size is invalid" }
        val bytes = ByteArray(manifest.size.toInt())
        DataInputStream(zip.getInputStream(manifest)).use { source ->
            source.readFully(bytes)
            require(source.read() == -1) { "The Foundry manifest length is invalid" }
        }
        val fields = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
        require(fields["schema"]?.jsonPrimitive?.contentOrNull == "asterion.foundry.intermediate.v1" &&
            fields["asset"]?.jsonPrimitive?.contentOrNull == "asset.glb") {
            "This package does not contain a supported Foundry geometry export"
        }
        val expected = fields["sha256"]?.jsonPrimitive?.contentOrNull
        require(expected != null && expected.matches(Regex("[a-f0-9]{64}"))) { "The Foundry geometry checksum is missing" }
        val mesh = requireNotNull(files["asset.glb"]?.singleOrNull()) { "The Foundry geometry is missing or duplicated" }
        require(!mesh.isDirectory)
        zip.getInputStream(mesh).buffered().use { stageGlb(it, destination, expected, signal) }
    }

private fun stageGlb(
    stream: BufferedInputStream,
    destination: AssetFiles,
    sha256: String?,
    signal: ExecutionSignal,
): ContentArtifact {
    val header = stream.header(12)
    require(header.int == GLB_MAGIC && header.int == 2) { "Choose a glTF 2.0 binary geometry file" }
    val length = Integer.toUnsignedLong(header.int)
    require(length >= 20 && length % 4 == 0L) { "The GLB container length is invalid" }
    return destination.put(stream, "model/gltf-binary", sha256, signal, expectedSize = length)
}

private fun BufferedInputStream.header(length: Int): ByteBuffer {
    mark(length)
    val bytes = ByteArray(length)
    DataInputStream(this).readFully(bytes)
    reset()
    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
}
