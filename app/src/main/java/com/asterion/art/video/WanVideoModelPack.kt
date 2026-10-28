package com.asterion.art.video

import android.content.Context
import android.net.Uri
import com.asterion.art.models.HuggingFaceHub
import com.asterion.art.nativeengine.OriginalCheckpointLibrary
import com.asterion.art.work.HuggingFaceDownloadWorker
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/**
 * Three exact Wan TI2V components, not generic GGUF files. All downloads use
 * Asterion's verified, commit-pinned, resumable Hugging Face worker.
 */
object WanVideoModelPack {
    data class Component(val repo:String,val file:String,val description:String)
    data class Ready(val diffusion:File,val vae:File,val encoder:File)
    val components=listOf(
        Component("off-grid-ai/Wan2.2-TI2V-5B-GGUF",
            "Wan2.2-TI2V-5B-Q4_K_S.gguf","Wan 2.2 TI2V diffusion"),
        Component("off-grid-ai/Wan2.2-TI2V-5B-GGUF",
            "wan2.2_vae.safetensors","Wan 2.2 frame decoder"),
        Component("off-grid-ai/Video-Companions-GGUF",
            "umt5-xxl-encoder-Q4_K_M.gguf","Wan text conditioning")
    )
    /** Other checkpoints, even .gguf, are not presumed compatible. */
    fun resolved(context:Context):Map<Component,File> {
        val registered=OriginalCheckpointLibrary(context).list()
        return components.mapNotNull { part ->
            val record=registered.firstOrNull { it.name==
                part.repo+"/"+part.file && it.sourceUri.startsWith("file:") }
                ?:return@mapNotNull null
            val file=runCatching{File(requireNotNull(Uri.parse(record.sourceUri).path))
                .canonicalFile}.getOrNull()?:return@mapNotNull null
            val source=runCatching {
                val manifest=JSONObject(File(file.path+".json").readText())
                manifest.getString("repo")==part.repo &&
                    manifest.getString("file")==part.file &&
                    manifest.getLong("size")==file.length() &&
                    manifest.getString("sha256").matches(Regex("[a-fA-F0-9]{64}")) &&
                    file.length()>1_000_000L
            }.getOrDefault(false)
            if(source)part to file else null
        }.toMap()
    }
    fun ready(context:Context):Ready? {
        val files=resolved(context)
        if(files.size!=components.size)return null
        return Ready(files.getValue(components[0]),files.getValue(components[1]),
            files.getValue(components[2]))
    }
    /**
     * Read and validate EVERY local byte before loading the multi-GB Wan
     * diffusion model. The fast 'ready' catalogue check deliberately does not
     * claim that image/video inference has verified the bytes.
     */
    fun verifiedForInference(context:Context,
        cancelled:()->Boolean={false}):Ready {
        val pack=ready(context)
            ?:error("Complete Wan 2.2 model pack not installed")
        val resolved=resolved(context)
        for((part,file) in resolved) {
            if(cancelled())throw CancellationException("Model inspection cancelled")
            val manifest=JSONObject(File(file.path+".json").readText())
            require(manifest.getString("repo")==part.repo &&
                manifest.getString("file")==part.file &&
                manifest.getLong("size")==file.length()){
                "Wan model manifest does not match its local file: "+part.file
            }
            val sha=manifest.getString("sha256")
            require(sha.matches(Regex("[a-fA-F0-9]{64}"))){
                "Wan model is missing its verified Hub checksum: "+part.file
            }
            WanVideoChecksumVerifier.check(file,sha,cancelled)
        }
        return pack
    }

    /**
     * Explicit user action. The API supplies exact commit SHAs and LFS SHA256s;
     * missing or unverified files cannot be downloaded as runnable weights.
     */
    fun enqueueMissing(context:Context):List<String> {
        val found=resolved(context)
        val missing=components.filter{it !in found}
        if(missing.isEmpty())return emptyList()
        val hub=HuggingFaceHub()
        return missing.groupBy{it.repo}.flatMap{(id,required)->
            val repo=hub.repository(id)
            require(!repo.gated){"Model repository requires authorization: "+id}
            val files=hub.files(repo).associateBy{it.path}
            required.map{part->
                val metadata=files[part.file]
                    ?:error("Required component missing from repository: "+part.file)
                require(metadata.bytes>1_000_000L &&
                    metadata.sha256?.matches(Regex("[a-fA-F0-9]{64}"))==true){
                    "Cannot verify the Hugging Face LFS checksum for "+part.file
                }
                HuggingFaceDownloadWorker.enqueue(context,id,repo.revision,part.file,
                    metadata.bytes,metadata.sha256,"auto")
            }
        }
    }
}

object WanVideoRules {
    /** Wan 2.2 VAE temporal compression requires exactly 4n+1 frames. */
    fun frameCount(seconds:Int):Int {
        require(seconds in 1..5)
        return seconds*24+1
    }
    fun outputFps(seconds:Int):Int {
        require(seconds in 1..5)
        return 24
    }

    /**
     * Official Wan2.2 TI2V 5B native training geometry is 1280x704, not
     * 512-square. Padding, not subject cropping, preserves other source ratios.
     */
    fun canvas(sourceWidth:Int,sourceHeight:Int):Pair<Int,Int> {
        require(sourceWidth>0 && sourceHeight>0)
        return if(sourceHeight>sourceWidth)704 to 1280 else 1280 to 704
    }
}

/**
 * Cached only for this app process and exact path, length, mtime and checksum.
 * Avoid re-reading 8+ GiB for every one-second continuation segment.
 */
internal object WanVideoChecksumVerifier {
    private data class Proof(val length:Long,val modified:Long,val sha:String)
    private val proofs=mutableMapOf<String,Proof>()

    @Synchronized fun check(file:File,expectedSha:String,
        cancelled:()->Boolean={false}) {
        val canonical=file.canonicalFile
        require(expectedSha.matches(Regex("[a-fA-F0-9]{64}")))
        require(canonical.isFile&&canonical.length()>0L)
        val sha=expectedSha.lowercase()
        val key=canonical.absolutePath
        val length=canonical.length()
        val modified=canonical.lastModified()
        if(proofs[key]==Proof(length,modified,sha))return
        val digest=MessageDigest.getInstance("SHA-256")
        canonical.inputStream().buffered(1024*1024).use {input->
            val buffer=ByteArray(1024*1024)
            while(true) {
                if(cancelled())throw CancellationException("Model validation cancelled")
                val bytes=input.read(buffer)
                if(bytes<0)break
                if(bytes>0)digest.update(buffer,0,bytes)
            }
        }
        require(canonical.length()==length &&
            canonical.lastModified()==modified){
            "Wan video model changed while it was being verified"
        }
        val actual=digest.digest().joinToString(""){"%02x".format(it)}
        require(actual==sha) {
            "Wan video model failed full SHA-256 verification: "+canonical.name
        }
        proofs[key]=Proof(length,modified,sha)
    }
}
