package com.asterion.art.nativeengine

import android.content.Context
import android.net.Uri
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
        "SDXL" to "sdxl",
        "Z_IMAGE" to "zimage",
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
                InstalledQnnModel(dir.name, data.getString("label"), backend, dir,
                    data.getLong("bytes"), data.getString("content_sha256"),
                    data.getLong("installed_at"))
            }.getOrNull()
        }.toList()

    fun importZip(uri: Uri, label: String, progress: (Long)->Unit,
                  stopped: ()->Boolean): InstalledQnnModel {
        val safeLabel = label.trim().ifBlank { "Imported QNN checkpoint" }.take(100)
        val id = "qnn_" + UUID.randomUUID().toString().replace("-", "").take(24)
        val stage = File(root,id+".partial").also { it.mkdirs() }
        var total = 0L
        var files = 0
        val contentDigest = MessageDigest.getInstance("SHA-256")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Model archive is inaccessible")
            input.use {
                ZipInputStream(it).use { zip ->
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
                                if (total % (32L*1024*1024) < n) progress(total)
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
            }
            // Many official ZIPs contain one top-level directory. Unwrap
            // it without copying model tensors or creating a second 7 GB tree.
            if (markers.keys.none { File(stage,it).exists() }) {
                val contents=stage.listFiles().orEmpty()
                if (contents.size==1 && contents[0].isDirectory) {
                    val wrapper=contents[0]
                    require(wrapper.listFiles().orEmpty().all { nested ->
                        File(stage,nested.name).canonicalPath.startsWith(
                            stage.canonicalPath + File.separator)
                    })
                    for (nested in wrapper.listFiles().orEmpty()) {
                        require(nested.renameTo(File(stage,nested.name))) {
                            "Could not unwrap model package root"
                        }
                    }
                    require(wrapper.delete()) { "Could not remove empty wrapper" }
                }
            }
            val backend=markers.entries.firstOrNull { File(stage,it.key).exists() }
                ?: error("Missing NPU model type marker (ANIMA/SDXL/etc.)")
            require(files>=3 && stage.walkTopDown().any {
                it.isFile && it.extension in listOf("bin","mnn","safetensors","gguf")
            }) { "Incomplete native model files" }
            val ready=File(root,id)
            require(stage.renameTo(ready)) { "Could not atomically finalize model" }
            val record=InstalledQnnModel(id,safeLabel,backend.value,ready,total,
                contentDigest.digest().joinToString("") { "%02x".format(it) },
                System.currentTimeMillis())
            File(ready,"asterion_model.json").writeText(JSONObject().apply {
                put("schema",1)
                put("label",record.label)
                put("backend",record.backend)
                put("bytes",record.sizeBytes)
                put("content_sha256",record.contentSha256)
                put("installed_at",record.installedAt)
            }.toString(2))
            return record
        } catch (e: Throwable) {
            stage.deleteRecursively()
            throw e
        }
    }
}
