package com.asterion.art.brain

import android.content.Context
import android.os.Environment
import java.io.File
import java.security.MessageDigest

data class BrainWeights(
    val id:String, val label:String, val repository:String,
    val filename:String, val bytes:Long, val sha256:String
) {
    val downloadUrl:String get()="https://huggingface.co/"+repository+
        "/resolve/main/"+filename+"?download=true"
}

object BrainCatalog {
    val models=listOf(
        BrainWeights("qwen3_06_q5","Qwen3 0.6B · Q5_K_M",
            "bartowski/Qwen_Qwen3-0.6B-GGUF","Qwen_Qwen3-0.6B-Q5_K_M.gguf",
            551378336L,"2DF7894C99EE7716F0ED3C7D454C2386A7AE976C4B0AD0E716D55399C1360648"),
        BrainWeights("qwen3_17_q4","Qwen3 1.7B · Q4_K_M",
            "unsloth/Qwen3-1.7B-GGUF","Qwen3-1.7B-Q4_K_M.gguf",
            1107409472L,"B139949C5BD74937AD8ED8C8CF3D9FFB1E99C866C823204DC42C0D91FA181897")
    )
    fun get(id:String)=models.firstOrNull{it.id==id}
}

/** Keeps pre-trained GGUF weights outside the APK, preferably on removable storage. */
class BrainModelStore(context:Context) {
    private val app=context.applicationContext
    private val prefs=app.getSharedPreferences("asterion_brain_v1",Context.MODE_PRIVATE)
    private val privateRoot=File(app.filesDir,"asterion-brain-models")
    private val roots=listOf(privateRoot)+app.getExternalFilesDirs(null).filterNotNull()
        .filter{Environment.getExternalStorageState(it)==Environment.MEDIA_MOUNTED}
        .map{File(it,"asterion-brain-models")}
    fun target(model:BrainWeights):File {
        // A staged .partial model must finish in the SAME app-owned directory
        // even after copying consumed the free-space reserve.
        val staged=roots.map{File(it,model.filename)}.firstOrNull{
            File(it.path+".partial").isFile
        }
        if(staged!=null)return staged
        val root=roots.drop(1).lastOrNull {
            (it.parentFile?.usableSpace?:0L)>model.bytes+512L*1024*1024
        }?:privateRoot
        root.mkdirs()
        require(root.usableSpace>model.bytes+256L*1024*1024){
            "Not enough free app storage to install the selected Qwen3 model"
        }
        return File(root,model.filename)
    }
    fun location(model:BrainWeights):File?=roots.map{File(it,model.filename)}
        .firstOrNull{f-> f.isFile&&f.length()==model.bytes&&
            File(f.path+".verified").takeIf{it.isFile}?.readText()?.trim()
                ?.equals(model.sha256,true)==true}
    fun installed(): List<BrainWeights> = BrainCatalog.models.filter{location(it)!=null}
    fun selected():BrainWeights?=BrainCatalog.get(prefs.getString("selected","").orEmpty())
        ?.takeIf{location(it)!=null}?:installed().lastOrNull()
    fun select(id:String) {
        require(BrainCatalog.get(id)!=null)
        check(prefs.edit().putString("selected",id).commit())
    }
    fun contextTokens():Int=prefs.getInt("context",16384)
        .takeIf{it in listOf(4096,8192,16384,32768)}?:16384
    fun setContextTokens(n:Int) {
        require(n in listOf(4096,8192,16384,32768))
        check(prefs.edit().putInt("context",n).commit())
    }
    fun verifyAndCommit(model:BrainWeights,candidate:File):File {
        require(candidate.isFile&&candidate.length()==model.bytes) {
            "GGUF file length does not match the pinned model"
        }
        val digest=MessageDigest.getInstance("SHA-256")
        val buffer=ByteArray(1024*1024)
        candidate.inputStream().use{input->
            while(true){
                val n=input.read(buffer)
                if(n<0)break
                digest.update(buffer,0,n)
            }
        }
        val hash=digest.digest().joinToString(""){"%02X".format(it)}
        require(hash.equals(model.sha256,true)){"GGUF SHA-256 mismatch"}
        val dst=target(model)
        if(candidate.canonicalFile!=dst.canonicalFile){
            require(!dst.exists()||location(model)==null){"Already installed"}
            if(dst.exists())check(dst.delete())
            if(candidate.parentFile?.canonicalFile==dst.parentFile?.canonicalFile) {
                check(candidate.renameTo(dst)){"GGUF rename failed"}
            } else {
                val temp=File(dst.path+".installing")
                try {
                    candidate.inputStream().use{src->
                        temp.outputStream().use{out->src.copyTo(out,1024*1024)}
                    }
                    check(temp.length()==model.bytes)
                    check(temp.renameTo(dst))
                } finally {temp.delete()}
            }
        }
        File(dst.path+".verified").writeText(model.sha256+"\n")
        return dst
    }
}
