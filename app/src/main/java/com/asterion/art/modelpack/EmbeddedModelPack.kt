package com.asterion.art.modelpack

import android.content.Context
import com.asterion.art.brain.BrainCatalog
import com.asterion.art.brain.BrainModelStore
import com.asterion.art.vision.VisionCatalog
import com.asterion.art.vision.VisionModelStore
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Self-contained offline model pack supplied as APK assets during packaging.
 *
 * No downloading, hidden network fallback, or user-supplied model-file step.
 * Extract models only when needed: llama.cpp requires an actual filesystem path.
 * Prefers app-owned removable storage when available.
 */
class EmbeddedModelPack(private val context:Context) {
    private val app=context.applicationContext
    companion object {
        const val ASSET_ROOT="asterion_bundle"
        private val ID_PATTERN=Regex("^[a-z0-9_]{4,80}$")
        private val CHUNK_PATTERN=Regex("^([a-z0-9_]{4,80})/part-([0-9]{4}).chunk$")
        private const val MAX_CHUNK=64L*1024L*1024L
        private val EXPECTED:Map<String,Triple<String,Long,String>> =mapOf(
            "qwen3_17_q4" to Triple(
                "Qwen3-1.7B-Q4_K_M.gguf",1_107_409_472L,
                "b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897"),
            "qwen3_vl_2b_q4" to Triple(
                "Qwen3VL-2B-Instruct-Q4_K_M.gguf",1_107_409_952L,
                "089d75c52f4b7ffc56ba998ffc50aae89fcafc755f9e7208aacca281dca6c2ae"),
            "qwen3_vl_2b_mmproj_q8" to Triple(
                "mmproj-Qwen3VL-2B-Instruct-Q8_0.gguf",445_053_216L,
                "f9a68fabba69c3b81e153367b2c7521030b0fa8bb0de400c9599c8e6725f9c82"))
    }

    fun packaged():Boolean=runCatching {
        app.assets.open("$ASSET_ROOT/manifest.json").use{it.read()!=-1}
    }.getOrDefault(false)

    data class Part(val name:String,val bytes:Long,val sha256:String)
    data class Model(val id:String,val filename:String,val bytes:Long,
                     val sha256:String,val parts:List<Part>)

    fun readManifest():List<Model> {
        val json=app.assets.open("$ASSET_ROOT/manifest.json").use {input->
            require(input.available()<64*1024){"Oversized model manifest"}
            JSONObject(input.reader(Charsets.UTF_8).readText())
        }
        require(json.getString("schema")=="asterion.embedded-model-pack.v1")
        require(json.getLong("chunk_bytes")==MAX_CHUNK)
        require(!json.optBoolean("image_diffusion_checkpoint_included",false)) {
            "Image model inclusion must be declared through a separate verified source"
        }
        val models=json.getJSONArray("models")
        require(models.length()==EXPECTED.size)
        val out=(0 until models.length()).map{index->
            val data=models.getJSONObject(index)
            val id=data.getString("id")
            require(ID_PATTERN.matches(id))
            val expected=EXPECTED[id]?:error("Unknown embedded model "+id)
            val filename=data.getString("filename")
            val bytes=data.getLong("bytes")
            val sha=data.getString("sha256")
            require(filename==expected.first&&bytes==expected.second&&
                sha.equals(expected.third,true)){
                "Embedded model metadata differs from signed app code"
            }
            val parts=data.getJSONArray("parts")
            require(parts.length() in 1..40)
            val sequence=(0 until parts.length()).map{piece->
                val item=parts.getJSONObject(piece)
                val path=item.getString("path")
                val match=CHUNK_PATTERN.matchEntire(path)
                    ?:error("Invalid embedded asset path")
                require(match.groupValues[1]==id &&
                    match.groupValues[2].toInt()==piece)
                val size=item.getLong("bytes")
                val partHash=item.getString("sha256").lowercase()
                require(size in 1L..MAX_CHUNK &&
                    partHash.matches(Regex("[0-9a-f]{64}")))
                Part(path,size,partHash)
            }
            require(sequence.sumOf{it.bytes}==bytes)
            Model(id,filename,bytes,sha.lowercase(),sequence)
        }
        require(out.map{it.id}.toSet()==EXPECTED.keys)
        require(json.getLong("total_bytes")==out.sumOf{it.bytes})
        return out
    }

    private fun destination(model:Model):File {
        if(model.id=="qwen3_17_q4") {
            val weights=requireNotNull(BrainCatalog.get(model.id))
            return BrainModelStore(app).target(weights)
        }
        val spec=VisionCatalog.all.singleOrNull{
            it.name==model.filename
        }?:error("Vision model is not in the pinned catalogue")
        return VisionModelStore(app).target(spec)
    }
    fun installed(model:Model):Boolean =
        if(model.id=="qwen3_17_q4"){
            val weights=requireNotNull(BrainCatalog.get(model.id))
            BrainModelStore(app).location(weights)!=null
        }else {
            val spec=VisionCatalog.all.single{it.name==model.filename}
            VisionModelStore(app).path(spec)!=null
        }

    private fun sha(data:ByteArray):String =
        MessageDigest.getInstance("SHA-256")
            .digest(data).joinToString(""){"%02x".format(it)}

    /**
     * Restart an incomplete extraction from the APK (not the network).
     * No model is reported installed until the complete final SHA-256 passes.
     */
    fun install(model:Model,onProgress:(Long,Long)->Unit,
                cancelled:()->Boolean) {
        if(installed(model))return
        val target=destination(model)
        target.parentFile?.mkdirs()
        val partial=File(target.path+".partial")
        if(partial.exists())require(partial.delete()){
            "Cannot safely reset interrupted embedded model extraction"
        }
        val full=MessageDigest.getInstance("SHA-256")
        var total=0L
        try {
            FileOutputStream(partial,false).use {output->
                val buffer=ByteArray(1024*1024)
                model.parts.forEach {part ->
                    check(!cancelled()){"Offline model extraction interrupted"}
                    val digest=MessageDigest.getInstance("SHA-256")
                    var read=0L
                    app.assets.open("$ASSET_ROOT/"+part.name).use{input->
                        while(true) {
                            check(!cancelled()){"Offline model extraction interrupted"}
                            val count=input.read(buffer)
                            if(count<0)break
                            read+=count
                            total+=count
                            check(read<=part.bytes&&total<=model.bytes){
                                "Embedded model chunk exceeds verified manifest size"
                            }
                            digest.update(buffer,0,count)
                            full.update(buffer,0,count)
                            output.write(buffer,0,count)
                        }
                    }
                    check(read==part.bytes){
                        "Incomplete embedded asset "+part.name
                    }
                    val actual=digest.digest().joinToString(""){"%02x".format(it)}
                    check(actual==part.sha256){
                        "Embedded asset checksum mismatch: "+part.name
                    }
                    onProgress(total,model.bytes)
                }
                output.fd.sync()
            }
            require(total==model.bytes&&
                full.digest().joinToString(""){"%02x".format(it)}==model.sha256){
                "Embedded full-model checksum mismatch"
            }
            if(model.id=="qwen3_17_q4") {
                BrainModelStore(app).verifyAndCommit(
                    requireNotNull(BrainCatalog.get(model.id)),partial)
            }else {
                val spec=VisionCatalog.all.single{it.name==model.filename}
                VisionModelStore(app).verifyAndCommit(spec,partial)
            }
            require(installed(model)){"Installed model not locatable after verification"}
        } finally {
            // Never leave an incomplete file masquerading as a valid model.
            // Models already committed are not deleted.
            if(partial.exists())partial.delete()
        }
    }

    fun allReady():Boolean =
        runCatching{readManifest().all(::installed)}.getOrDefault(false)
}
