package com.asterion.art.nativeengine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

data class OriginalCheckpoint(
    val id: String, val name: String, val format: String,
    val sizeBytes: Long, val sourceUri: String
)

/**
 * Original weights stay in the document provider. Only a persisted SAF reference
 * and metadata are stored; raw weights are NEVER misrepresented as QNN-ready.
 */
class OriginalCheckpointLibrary(private val context: Context) {
    private val prefs=context.applicationContext.getSharedPreferences(
        "asterion_original_checkpoint_refs_v1",Context.MODE_PRIVATE)

    fun list(): List<OriginalCheckpoint> = runCatching {
        val records=JSONArray(prefs.getString("records","[]"))
        (0 until records.length()).map { i ->
            records.getJSONObject(i).let { json ->
                OriginalCheckpoint(json.getString("id"),json.getString("name"),
                    json.getString("format"),json.getLong("bytes"),json.getString("uri"))
            }
        }
    }.getOrElse { emptyList() }

    /** Unregister a SAF pointer only. The original external file is untouched. */
    fun unregister(id: String): Boolean {
        val existing = list()
        if (existing.none { it.id == id }) return false
        val records = JSONArray()
        existing.filterNot { it.id == id }.forEach { item ->
            records.put(JSONObject().apply {
                put("id", item.id); put("name", item.name)
                put("format", item.format); put("bytes", item.sizeBytes)
                put("uri", item.sourceUri)
            })
        }
        check(prefs.edit().putString("records", records.toString()).commit())
        return true
    }

    /**
     * Register a Hugging Face original downloaded into this app's verified
     * private Hub directory. No second model copy and no QNN compatibility
     * claim. The durable catalogue points to the actual on-disk weights.
     */
    @Synchronized fun registerDownloaded(file:File,label:String):OriginalCheckpoint? {
        val canonical=file.canonicalFile
        val app=context.applicationContext
        val allowed=listOf(File(app.filesDir,"hf-library"))+
            app.getExternalFilesDirs(null).filterNotNull().map{File(it,"hf-library")}
        require(canonical.isFile && allowed.any { candidate ->
            canonical.path.startsWith(candidate.canonicalPath+File.separator)
        }){"Downloaded model is outside verified app-owned Hub storage"}
        val extension=canonical.extension.lowercase()
        if(extension !in setOf("safetensors","gguf","ckpt","pt","pth"))
            return null
        val bytes=canonical.length()
        require(bytes>32L){"Model file is empty"}
        val magic=canonical.inputStream().use{it.readNBytes(9)}
        val kind=when(extension) {
            "gguf"->{
                require(magic.size>=4 &&
                    String(magic,0,4,Charsets.US_ASCII)=="GGUF"){
                    "Downloaded GGUF has invalid model header"
                }
                "GGUF"
            }
            "safetensors"->{
                require(magic.size==9&&
                    ByteBuffer.wrap(magic,0,8).order(ByteOrder.LITTLE_ENDIAN).long
                        in 2L..100_000_000L&&magic[8]=='{'.code.toByte()){
                    "Downloaded safetensors file has invalid model header"
                }
                "SAFETENSORS"
            }
            else->"ORIGINAL_CHECKPOINT"
        }
        val uri=Uri.fromFile(canonical).toString()
        val id=MessageDigest.getInstance("SHA-256")
            .digest(uri.toByteArray(Charsets.UTF_8)).take(12)
            .joinToString(""){"%02x".format(it)}
        val record=OriginalCheckpoint(id,label.take(100).ifBlank{canonical.name},
            kind,bytes,uri)
        val json=JSONArray()
        (list().filterNot{it.id==id}+record).forEach{item->
            json.put(JSONObject().apply{
                put("id",item.id);put("name",item.name)
                put("format",item.format);put("bytes",item.sizeBytes)
                put("uri",item.sourceUri)
            })
        }
        check(prefs.edit().putString("records",json.toString()).commit())
        return record
    }

    /** Return null only when this isn't a recognized original-checkpoint file. */
    fun registerIfOriginal(uri: Uri, proposedLabel: String): OriginalCheckpoint? {
        val projection=arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE)
        var filename=""
        var length=-1L
        context.contentResolver.query(uri,projection,null,null,null)?.use { cursor ->
            if(cursor.moveToFirst()) {
                filename=cursor.getString(0).orEmpty()
                if(!cursor.isNull(1))length=cursor.getLong(1)
            }
        }
        val extension=filename.substringAfterLast('.', "").lowercase()
        if(extension !in setOf("safetensors","gguf","ckpt","pt","pth")) return null
        val magic=context.contentResolver.openInputStream(uri)?.use { it.readNBytes(9) }
            ?: error("Original checkpoint URI is no longer accessible")
        val format=when(extension) {
            "gguf" -> {
                require(magic.size>=4 && String(magic,0,4,Charsets.US_ASCII)=="GGUF") {
                    "File has .gguf extension but lacks the GGUF header"
                }
                "GGUF"
            }
            "safetensors" -> {
                require(magic.size>=9 &&
                    ByteBuffer.wrap(magic,0,8).order(ByteOrder.LITTLE_ENDIAN).long in
                        2L..100_000_000L && magic[8]=='{'.code.toByte()) {
                    "Invalid safetensors header; source is not a valid original checkpoint"
                }
                "SAFETENSORS"
            }
            else -> "ORIGINAL_CHECKPOINT"
        }
        val id=MessageDigest.getInstance("SHA-256")
            .digest(uri.toString().toByteArray())
            .take(12).joinToString("") { "%02x".format(it) }
        val record=OriginalCheckpoint(id, proposedLabel.ifBlank { filename }.take(100),
            format,length,uri.toString())
        val existing=list().filterNot { it.id==id }
        val json=JSONArray()
        (existing+record).forEach { item ->
            json.put(JSONObject().apply {
                put("id",item.id);put("name",item.name);put("format",item.format)
                put("bytes",item.sizeBytes);put("uri",item.sourceUri)
            })
        }
        check(prefs.edit().putString("records",json.toString()).commit())
        return record
    }
}