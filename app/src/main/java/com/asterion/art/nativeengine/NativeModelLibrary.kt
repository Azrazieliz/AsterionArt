package com.asterion.art.nativeengine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.FilterInputStream
import java.io.InputStream
import com.asterion.art.engine.DreamModel
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

data class InstalledQnnModel(
    val id: String, val label: String, val backend: String,
    val root: File, val sizeBytes: Long, val contentSha256: String,
    val installedAt: Long,
) {
    fun toDreamModel(): DreamModel = DreamModel(
        id = id, name = label,
        generationSize = if (backend in setOf("anima", "sdxl", "sdxlmnn")) 1024 else 512,
        isAnima = backend == "anima",
        isSdxl = backend.startsWith("sdxl"),
        isCustom = true,
        ditKind = if (backend in listOf("zimage","klein","qwen21","krea2turbo")) backend else "",
        resolutions = emptyList()
    )
}

/** Persist 6+ GB model ZIP directly from SAF into one final app-owned location. */
class NativeModelLibrary(private val context: Context) {
    private val root = File(context.filesDir,"asterion-qnn-models").also { it.mkdirs() }
    private val markers = linkedMapOf(
        "ANIMA" to "anima",
        "npucustom" to "sd15npu",
        "SDXL" to "sdxl",
        "ZIMAGE" to "zimage",
        "FLUX2_KLEIN" to "klein",
        "QWEN_IMAGE_2_1" to "qwen21",
        "KREA_2_TURBO" to "krea2turbo"
    )

    fun list(): List<InstalledQnnModel> = root.listFiles().orEmpty()
        .asSequence().filter { it.isDirectory && !it.name.endsWith(".partial") }
        .mapNotNull { dir ->
            runCatching {
                val data = JSONObject(File(dir,"asterion_model.json").readText())
                val backend = data.getString("backend")
                require(backend in markers.values)
                val relative=data.optString("model_subdir", "")
                val modelRoot=File(dir,relative).canonicalFile
                require(modelRoot.path==dir.canonicalPath ||
                    modelRoot.path.startsWith(dir.canonicalPath+File.separator))
                require(modelRoot.isDirectory)
                InstalledQnnModel(dir.name,data.getString("label"),backend,modelRoot,
                    data.getLong("bytes"),data.getString("content_sha256"),
                    data.getLong("installed_at"))
            }.getOrNull()
        }.toList()

    fun importZip(uri: Uri, label: String, progress: (Long, Long)->Unit,
                  stopped: ()->Boolean, family: String? = null): InstalledQnnModel {
        val chosen = family?.lowercase()?.takeUnless { it=="auto" }
        require(chosen==null || chosen in markers.values) {
            "Unknown model architecture choice"
        }
        val name = context.contentResolver.query(uri,
            arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { rows ->
            if (rows.moveToFirst()) rows.getString(0) else ""
        }.orEmpty()
        if (name.endsWith(".safetensors", true) || name.endsWith(".ckpt", true) ||
            name.endsWith(".gguf", true)) {
            error("This is an original checkpoint, not a runnable QNN package. " +
                "Use a converted QNN model ZIP (ANIMA/SDXL marker and tensor files).")
        }
        val magic=context.contentResolver.openInputStream(uri)?.use { it.readNBytes(16) }
            ?: error("Document provider cannot open this model file")
        if(magic.size>=4 && String(magic,0,4,Charsets.US_ASCII)=="GGUF")
            error("GGUF detected: this file is not supported by the embedded QNN diffusion engine.")
        require(magic.size>=4 && magic[0]==0x50.toByte() && magic[1]==0x4B.toByte()) {
            "Not a converted QNN ZIP. First bytes: "+
                magic.take(4).joinToString(" ") { "%02X".format(it.toInt() and 255) }
        }
        val safeLabel = label.trim().ifBlank { "Imported QNN checkpoint" }.take(100)
        val id = "qnn_" + UUID.randomUUID().toString().replace("-", "").take(24)
        val stage = File(root,id+".partial").also { it.mkdirs() }
        var total = 0L
        var files = 0
        var compressedRead=0L
        val contentDigest = MessageDigest.getInstance("SHA-256")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Model archive is inaccessible")
            input.use {
                val counted = object: FilterInputStream(it) {
                    var bytesRead: Long = 0L
                    override fun read(): Int = super.read().also { if (it >= 0) bytesRead++ }
                    override fun read(buffer: ByteArray, off: Int, len: Int): Int {
                        val n = super.read(buffer, off, len)
                        if (n > 0) bytesRead += n
                        return n
                    }
                }
                ZipInputStream(counted).use { zip ->
                    while (true) {
                        if (stopped()) throw java.util.concurrent.CancellationException()
                        val entry=zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val path=entry.name.replace('\\','/')
                        require(path.isNotBlank() && !path.startsWith("/") &&
                            path.split('/').none { part -> part==".." || part=="." }) {
                            "Unsafe model archive path"
                        }
                        require(path.split('/').size<=8)
                        var name=path
                        val bits=name.split('/')
                        if (bits.size>1 && bits[0] in listOf("model","models","package","checkpoint"))
                            name=bits.drop(1).joinToString("/")
                        val out=File(stage,name).canonicalFile
                        require(out.path.startsWith(stage.canonicalPath+File.separator)) {
                            "Model archive tried to escape its destination"
                        }
                        out.parentFile?.mkdirs()
                        var bytes=0L
                        val digest=MessageDigest.getInstance("SHA-256")
                        FileOutputStream(out).use { target ->
                            val buffer=ByteArray(256*1024)
                            while (true) {
                                if (stopped()) throw java.util.concurrent.CancellationException()
                                val n=zip.read(buffer)
                                if (n<0) break
                                total+=n
                                bytes+=n
                                require(total<18_000_000_000L && bytes<12_000_000_000L) {
                                    "Unsafe model ZIP expansion size"
                                }
                                target.write(buffer,0,n)
                                digest.update(buffer,0,n)
                                if (total % (8L*1024*1024) < n) progress(counted.bytesRead, total)
                            }
                            target.fd.sync()
                        }
                        contentDigest.update(name.toByteArray(Charsets.UTF_8))
                        contentDigest.update(digest.digest())
                        files++
                        require(files<=350) { "Too many model files" }
                        zip.closeEntry()
                    }
                }
                compressedRead=counted.bytesRead
            }
            progress(compressedRead,total)
            // Nested ZIP wrappers are preserved; the validated model root
            // is stored as a relative subdirectory, without moving tensors.
            val found=stage.walkTopDown().filter { it.name in markers.keys }.toList()
            val families=found.mapNotNull { markers[it.name] }.distinct()
            require(families.size<=1) {
                "Conflicting QNN model markers: "+families.joinToString()
            }
            val detected=families.firstOrNull()
            if(detected!=null && chosen!=null && detected!=chosen)
                error("Selected "+chosen+" conflicts with detected QNN family "+detected)
            val compiledData=stage.walkTopDown().any { it.isFile &&
                it.extension.equals("bin",true) && it.length()>1024*1024 }
            if(!compiledData) {
                val raw=stage.walkTopDown().filter { it.isFile &&
                    it.extension.lowercase() in setOf("safetensors","ckpt","gguf","pt") }
                    .take(4).map { it.name }.toList()
                error(if(raw.isNotEmpty()) "Original weights ("+raw.joinToString()+
                    ") found but no compiled QNN .bin tensors. Conversion is required."
                else "Missing compiled QNN .bin tensor files in the selected ZIP.")
            }
            val backend=detected ?: chosen ?: error(
                "Missing QNN architecture marker (ANIMA/SDXL/etc.). Compiled .bin data exists, "+
                "but the architecture is unknown. Choose the actual QNN family; this does not convert it.")
            require(files>=3) { "Incomplete QNN archive: only "+files+" files" }
            val modelFolder=found.firstOrNull()?.parentFile ?: stage
            val relative=stage.toPath().relativize(modelFolder.toPath()).toString()
            val sha=contentDigest.digest().joinToString("") { "%02x".format(it) }
            val existing=list().firstOrNull { it.contentSha256==sha }
            if(existing!=null) {
                stage.deleteRecursively()
                return existing
            }
            // The manifest is committed before the atomic move; no unlisted
            // half-installed model can become visible.
            File(stage,"asterion_model.json").writeText(JSONObject().apply {
                put("schema",2);put("label",safeLabel);put("backend",backend)
                put("bytes",total);put("content_sha256",sha)
                put("installed_at",System.currentTimeMillis())
                put("model_subdir",relative)
            }.toString(2))
            val ready=File(root,id)
            require(stage.renameTo(ready)) { "Could not atomically finalize model" }
            return list().first { it.id==id }
        } catch (e: Throwable) {
            stage.deleteRecursively()
            throw e
        }
    }
}
