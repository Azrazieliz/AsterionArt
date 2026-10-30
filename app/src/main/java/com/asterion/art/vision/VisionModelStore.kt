package com.asterion.art.vision

import android.content.Context
import android.os.Environment
import com.asterion.art.data.GeneratedImagePublisher
import java.io.File
import java.security.MessageDigest

/** SHA-pinned Gemma 3 and restored Qwen3-VL GGUF pairs. */
data class VisionWeight(
    val name:String,val bytesMax:Long,val sha256:String,
    val repository:String="bartowski/google_gemma-3-4b-it-GGUF"
) {
    val url:String get()="https://huggingface.co/"+repository+
        "/resolve/main/"+name+"?download=true"
}
object VisionCatalog {
    // The SAME Gemma 3 family is used for design reasoning and pixel vision.
    // Separately validated GGUF + matching F16 projector, never substituted
    // with an unrelated architecture or the old Gemma vision pair.
    val language=VisionWeight(
        "google_gemma-3-4b-it-Q4_K_M.gguf",2_489_758_112L,
        "4996030242583a40aa151ff93f49ed787ac8c25e4120c3ae4588b2e2a7d1ae94")
    val projector=VisionWeight(
        "mmproj-google_gemma-3-4b-it-f16.gguf",851_251_104L,
        "8c0fb064b019a6972856aaae2c7e4792858af3ca4561be2dbf649123ba6c40cb")
    // Previously approved Qwen3-VL model family: retain existing user's
    // SHA-verified files and projector; never replace with Gemma silently.
    val qwenLanguage=VisionWeight(
        "Qwen3VL-2B-Instruct-Q4_K_M.gguf",1_107_409_952L,
        "089d75c52f4b7ffc56ba998ffc50aae89fcafc755f9e7208aacca281dca6c2ae",
        "Qwen/Qwen3-VL-2B-Instruct-GGUF")
    val qwenProjector=VisionWeight(
        "mmproj-Qwen3VL-2B-Instruct-Q8_0.gguf",445_053_216L,
        "f9a68fabba69c3b81e153367b2c7521030b0fa8bb0de400c9599c8e6725f9c82",
        "Qwen/Qwen3-VL-2B-Instruct-GGUF")
    // Keep the existing Gemma download workflow unchanged; registration of
    // restored Qwen does not trigger an unrequested multi-GB download.
    val all=listOf(language,projector)
}
data class VisionInstalledPair(val language:File,val projector:File)
class VisionModelStore(context:Context) {
    private val app=context.applicationContext
    private fun directories(name:String)=
        (app.getExternalFilesDirs(null).filterNotNull()
            .filter{Environment.getExternalStorageState(it)==Environment.MEDIA_MOUNTED}
            .map{File(it,name)}+File(app.filesDir,name))
    private val gemmaRoots=directories("asterion-vision-gemma3")
    private val qwenRoots=directories("asterion-vision-qwen3vl")
    private fun rootsFor(weight:VisionWeight)=
        if(weight.repository.startsWith("Qwen/"))qwenRoots else gemmaRoots
    fun target(weight:VisionWeight):File {
        val roots=rootsFor(weight)
        val existing=roots.map{File(it,weight.name)}
            .firstOrNull{it.isFile || File(it.path+".partial").exists()}
        if(existing!=null)return existing
        val root=roots.firstOrNull{
            (it.parentFile?.usableSpace?:0L)>weight.bytesMax+256L*1024*1024
        } ?:error("Not enough app-owned storage to install the selected Gemma 3 weights")
        root.mkdirs()
        return File(root,weight.name)
    }
    private fun marker(file:File)=File(file.path+".verified")
    fun path(weight:VisionWeight):File? {
        // Reuse the exact same SHA-verified Gemma 3 language GGUF when
        // already installed as the design brain, avoiding a redundant 2.49GB
        // download and retaining owner-provisioned model storage.
        if(weight.name==VisionCatalog.language.name) {
            val shared=com.asterion.art.brain.BrainCatalog.models
                .firstOrNull{it.filename==weight.name&&it.sha256==weight.sha256}
                ?.let{com.asterion.art.brain.BrainModelStore(app).location(it)}
            if(shared!=null)return shared
        }
        return rootsFor(weight).map{File(it,weight.name)}.firstOrNull{
            it.isFile&&it.length() in 100_000_000L..weight.bytesMax &&
                marker(it).isFile &&
                marker(it).readText().trim().equals(weight.sha256,true)
        }
    }
    fun installed():VisionInstalledPair? {
        // Prefer already installed approved Qwen3-VL when present. Gemma
        // remains available without deleting either model directory.
        val qwen=path(VisionCatalog.qwenLanguage)
        val qwenMm=path(VisionCatalog.qwenProjector)
        if(qwen!=null && qwenMm!=null)return VisionInstalledPair(qwen,qwenMm)
        val gemma=path(VisionCatalog.language)?:return null
        val gemmaMm=path(VisionCatalog.projector)?:return null
        return VisionInstalledPair(gemma,gemmaMm)
    }
    @Synchronized fun verifyAndCommit(weight:VisionWeight,partial:File) {
        require(partial.isFile&&partial.length() in 100_000_000L..weight.bytesMax)
        require(partial.inputStream().use {input->
            val magic=ByteArray(4)
            input.read(magic)==4 && magic.contentEquals(
                byteArrayOf(0x47,0x47,0x55,0x46))
        }){"Not a GGUF model"}
        val sha=MessageDigest.getInstance("SHA-256")
        partial.inputStream().use{stream->
            val buf=ByteArray(1024*1024)
            while(true) {
                val count=stream.read(buf)
                if(count<0)break
                sha.update(buf,0,count)
            }
        }
        val actual=sha.digest().joinToString(""){"%02x".format(it)}
        require(actual.equals(weight.sha256,true)) {
            "Downloaded vision GGUF does not match its official SHA-256"
        }
        val output=target(weight)
        if(output.exists())check(output.delete())
        check(partial.renameTo(output)){"Could not commit verified vision model"}
        GeneratedImagePublisher.atomicWrite(marker(output),
            weight.sha256.toByteArray(Charsets.UTF_8))
    }
}
