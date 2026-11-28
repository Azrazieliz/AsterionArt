package com.asterion.art.runtime

import com.asterion.art.creation.ExecutionSignal
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GeometryReferencesTest {
    @get:Rule val temporary = TemporaryFolder()
    private val active = ExecutionSignal {}

    @Test
    fun rawAndPackagedGlbRetainTheExactSameGeometryBytes() {
        val source = glb()
        val cache = temporary.newFolder("cache")
        val files = AssetFiles(temporary.newFolder("references"))
        val raw = stageGeometry(source.inputStream(), cache, files, digest(source), active)
        val archive = foundryPackage(source)
        val packaged = stageGeometry(archive.inputStream(), cache, files, digest(archive), active)

        assertEquals(raw, packaged)
        assertEquals("model/gltf-binary", packaged.mediaType)
        assertArrayEquals(source, files.verified(packaged).readBytes())
        assertEquals(emptyList<File>(), cache.listFiles()!!.toList())
    }

    @Test
    fun rejectsIncorrectHashesAndTruncatedGeometryWithoutPublishingAnAsset() {
        val source = glb()
        val cache = temporary.newFolder("cache")
        val directory = temporary.newFolder("references")
        val files = AssetFiles(directory)
        val archive = foundryPackage(source, "0".repeat(64))
        assertThrows(IllegalArgumentException::class.java) {
            stageGeometry(archive.inputStream(), cache, files, null, active)
        }
        assertThrows(IllegalArgumentException::class.java) {
            stageGeometry(source.copyOf(source.size - 1).inputStream(), cache, files, null, active)
        }
        assertEquals(emptyList<File>(), cache.listFiles()!!.toList())
        assertEquals(emptyList<File>(), directory.listFiles()!!.toList())
    }

    @Test
    fun cancellationCleansBothTemporaryAndDurableDirectories() {
        val cache = temporary.newFolder("cache")
        val references = temporary.newFolder("references")
        assertThrows(CancellationException::class.java) {
            stageGeometry(foundryPackage(glb()).inputStream(), cache, AssetFiles(references), null) {
                throw CancellationException()
            }
        }
        assertEquals(emptyList<File>(), cache.listFiles()!!.toList())
        assertEquals(emptyList<File>(), references.listFiles()!!.toList())
    }

    private fun glb(): ByteArray {
        val json = """{"asset":{"version":"2.0"}}""".encodeToByteArray()
        val chunkLength = (json.size + 3) / 4 * 4
        return ByteBuffer.allocate(20 + chunkLength).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(0x46546c67).putInt(2).putInt(capacity())
            putInt(chunkLength).putInt(0x4e4f534a).put(json)
            repeat(chunkLength - json.size) { put(32.toByte()) }
        }.array()
    }

    private fun foundryPackage(asset: ByteArray, sha256: String = digest(asset)): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("asset.glb"))
            zip.write(asset)
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("import-manifest.json"))
            zip.write("""{"schema":"asterion.foundry.intermediate.v1","asset":"asset.glb","sha256":"$sha256"}""".encodeToByteArray())
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
