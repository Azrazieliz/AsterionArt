package com.asterion.art.vision

import android.content.Context
import android.os.Environment
import com.asterion.art.data.GeneratedImagePublisher
import java.io.File
import java.security.MessageDigest

/** Pinned public Apache-2.0 official Qwen3-VL-2B GGUF pair. */
data class VisionWeight(
    val name:String,val bytesMax:Long,val sha256:String
) {
    val url:String get()=
        "https://huggingface.co/Qwen/Qwen3-VL-2B-Instruct-GGUF/resolve/main/"+
            name+"?download=true"
}
object VisionCatalog {
    val language=VisionWeight(
        "Qwen3VL-2B-Instruct-Q4_K_M.gguf",1_400_000_000L,
        "089d75c52f4b7ffc56ba998ffc50aae89fcafc755f9e7208aacca281dca6c2ae")
    val projector=VisionWeight(
        "mmproj-Qwen3VL-2B-Instruct-Q8_0.gguf",600_000_000L,
        "f9a68fabba69c3b81e153367b2c7521030b0fa8bb0de400c9599c8e6725f9c82")
    val all=listOf(language,projector)
}
data class VisionInstalledPair(val language:File,val projector:File)
class VisionModelStore(context:Context) {
    private val app=context.applicationContext
    private val roots=(app.getExternalFilesDirs(null).filterNotNull()
        .filter{Environment.getExternalStorageState(it)==Environment.MEDIA_MOUNTED}
        .map{File(it,"asterion-vision-qwen3vl")}+
        File(app.filesDir,"asterion-vision-qwen3vl"))
    fun target(weight:VisionWeight):File {
        val existing=roots.map{File(it,weight.name)}
            .firstOrNull{it.isFile || File(it.path+".partial").exists()}
        if(existing!=null)return existing
        val root=roots.firstOrNull{
            (it.parentFile?.usableSpace?:0L)>2_500_000_000L
        }
            ?:error("At least 2.5 GB free app storage required for local vision models")
        root.mkdirs()
        return File(root,weight.name)
    }
    private fun marker(file:File)=File(file.path+".verified")
    fun path(weight:VisionWeight):File?=
        roots.map{File(it,weight.name)}.firstOrNull{
            it.isFile&&it.length() in 100_000_000L..weight.bytesMax &&
                marker(it).isFile &&
                marker(it).readText().trim().equals(weight.sha256,true)
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
