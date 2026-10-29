package com.asterion.art.vision

import android.content.Context
import android.os.Environment
import com.asterion.art.data.GeneratedImagePublisher
import java.io.File
import java.security.MessageDigest

/** Pinned public Apache-2.0 official Gemma 3-2B GGUF pair. */
data class VisionWeight(
    val name:String,val bytesMax:Long,val sha256:String
) {
    val url:String get()=
        "https://huggingface.co/bartowski/google_gemma-3-4b-it-GGUF/resolve/main/"+
            name+"?download=true"
}
object VisionCatalog {
    // The SAME Gemma 3 family is used for design reasoning and pixel vision.
    // Separately validated GGUF + matching F16 projector, never substituted
    // with an unrelated architecture or the old Gemma vision pair.
    val language=VisionWeight(
        "google_gemma-3-4b-it-Q4_K_M.gguf",2_800_000_000L,
        "4996030242583a40aa151ff93f49ed787ac8c25e4120c3ae4588b2e2a7d1ae94")
    val projector=VisionWeight(
        "mmproj-google_gemma-3-4b-it-f16.gguf",1_000_000_000L,
        "8c0fb064b019a6972856aaae2c7e4792858af3ca4561be2dbf649123ba6c40cb")
    val all=listOf(language,projector)
}
data class VisionInstalledPair(val language:File,val projector:File)
class VisionModelStore(context:Context) {
    private val app=context.applicationContext
    private val roots=(app.getExternalFilesDirs(null).filterNotNull()
        .filter{Environment.getExternalStorageState(it)==Environment.MEDIA_MOUNTED}
        .map{File(it,"asterion-vision-gemma3")}+
        File(app.filesDir,"asterion-vision-gemma3"))
    fun target(weight:VisionWeight):File {
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
        return roots.map{File(it,weight.name)}.firstOrNull{
            it.isFile&&it.length() in 100_000_000L..weight.bytesMax &&
                marker(it).isFile &&
                marker(it).readText().trim().equals(weight.sha256,true)
        }
    }
    fun installed():VisionInstalledPair? {
        val first=path(VisionCatalog.language)?:return null
        val second=path(VisionCatalog.projector)?:return null
        return VisionInstalledPair(first,second)
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
