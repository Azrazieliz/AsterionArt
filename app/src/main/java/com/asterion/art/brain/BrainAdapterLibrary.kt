package com.asterion.art.brain

import android.content.Context
import android.net.Uri
import com.asterion.art.data.GeneratedImagePublisher
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

data class InstalledBrainAdapter(
    val id:String,
    val baseId:String,
    val file:File,
    val sha256:String,
    val sizeBytes:Long,
    val enabled:Boolean
)

/**
 * Verified model-adapter storage. A GGUF LoRA modifies inference weights
 * through the pinned llama.cpp adapter interface, without rewriting the
 * immutable base checkpoint. A downloaded LoRA is not a trained-by-Asterion
 * adapter unless its provenance explicitly establishes that fact.
 */
class BrainAdapterLibrary(context:Context) {
    private val app=context.applicationContext
    private val root=File(app.filesDir,"brain-adapters").also{it.mkdirs()}
    private fun metadata(file:File)=File(file.path+".json")
    private fun sha256(file:File):String {
        val md=MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input->
            val b=ByteArray(256*1024)
            while(true) {
                val n=input.read(b)
                if(n<0)break
                md.update(b,0,n)
            }
        }
        return md.digest().joinToString(""){"%02x".format(it)}
    }
    fun all():List<InstalledBrainAdapter> =
        root.listFiles().orEmpty().filter{it.isFile&&it.extension=="gguf"}
            .mapNotNull { file->
                runCatching {
                    val v=JSONObject(metadata(file).readText())
                    val base=v.getString("base_id")
                    require(BrainCatalog.get(base)!=null)
                    InstalledBrainAdapter(v.getString("id"),base,file,
                        v.getString("sha256"),v.getLong("bytes"),
                        v.optBoolean("enabled",false))
                }.getOrNull()
            }

    /**
     * Imports from a read-only SAF reference, without deleting the source.
     * Requires an explicit compatible base identifier; incompatibility at
     * llama.cpp loading is an error rather than an ignored adapter.
     */
    @Synchronized fun import(uri:Uri,baseId:String):InstalledBrainAdapter {
        require(BrainCatalog.get(baseId)!=null)
        val id="adapter_"+UUID.randomUUID().toString().replace("-","")
        val destination=File(root,id+".gguf")
        val partial=File(root,id+".partial")
        try {
            val src=app.contentResolver.openInputStream(uri)
                ?: error("Could not open GGUF adapter")
            var bytes=0L
            src.use { input->
                partial.outputStream().buffered().use { output->
                    val buffer=ByteArray(256*1024)
                    while(true) {
                        val n=input.read(buffer)
                        if(n<0)break
                        bytes+=n
                        require(bytes<=512L*1024*1024){"Adapter exceeds 512 MiB limit"}
                        output.write(buffer,0,n)
                    }
                }
            }
            require(bytes in 1024..(512L*1024*1024)){
                "Expected a GGUF LoRA adapter, not an empty file"
            }
            partial.inputStream().buffered().use { input->
                val magic=ByteArray(4)
                require(input.read(magic)==4 &&
                    magic.contentEquals(byteArrayOf(71,71,85,70))) {
                    "Adapter is not a GGUF file"
                }
            }
            val digest=sha256(partial)
            check(partial.renameTo(destination))
            val info=InstalledBrainAdapter(id,baseId,destination,digest,bytes,false)
            persist(info)
            return info
        } finally{partial.delete()}
    }
    private fun persist(info:InstalledBrainAdapter) {
        val data=JSONObject().apply {
            put("schema","asterion.brain-lora.v1")
            put("id",info.id);put("base_id",info.baseId)
            put("sha256",info.sha256);put("bytes",info.sizeBytes)
            put("enabled",info.enabled)
            put("base_weights_rewritten",false)
            put("trained_by_asterion",false)
        }
        GeneratedImagePublisher.atomicWrite(metadata(info.file),
            data.toString(2).toByteArray(Charsets.UTF_8))
    }
    @Synchronized fun enable(id:String):InstalledBrainAdapter {
        val chosen=all().firstOrNull{it.id==id}?:error("Adapter does not exist")
        require(chosen.file.length()==chosen.sizeBytes &&
            sha256(chosen.file).equals(chosen.sha256,true)) {
            "Adapter was modified after import"
        }
        all().forEach { persist(it.copy(enabled=it.id==id)) }
        return chosen.copy(enabled=true)
    }
    fun activeFor(baseId:String):File? =
        all().firstOrNull { it.baseId==baseId&&it.enabled&&
            it.file.length()==it.sizeBytes&&
            sha256(it.file).equals(it.sha256,true) }?.file
    @Synchronized fun disableAll() {
        all().filter{it.enabled}.forEach{persist(it.copy(enabled=false))}
    }
}
