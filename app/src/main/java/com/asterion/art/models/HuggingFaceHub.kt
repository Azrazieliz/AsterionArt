package com.asterion.art.models

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Model selection uses the public Hub API; inference remains entirely on-device. */
data class HubRepository(val id:String,val revision:String,val downloads:Long,
    val gated:Boolean)
data class HubModelFile(val path:String,val bytes:Long,val sha256:String?) {
    val kind:String get()=when {
        path.lowercase().endsWith(".gguf")->"GGUF reasoning/vision weights"
        path.lowercase().endsWith(".zip")->"Model package (QNN ZIP when compatible)"
        path.lowercase().endsWith(".safetensors")->"Original checkpoint / adapter"
        path.lowercase().endsWith(".ckpt")->"Original checkpoint"
        path.lowercase().endsWith(".bin")->"Model weights"
        else->"Supporting model file"
    }
}

object HubPath {
    private val component=Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,100}")
    private val sha=Regex("[a-fA-F0-9]{40}")
    private val fileSegment=Regex("[A-Za-z0-9_+().-][A-Za-z0-9_+(). -]*")
    fun repository(id:String):String {
        val parts=id.split('/')
        require(parts.size==2&&parts.all{component.matches(it)}&&
            parts.none{it=="."||it==".."}){"Invalid Hugging Face repository"}
        return id
    }
    fun revision(value:String):String {
        require(sha.matches(value)){"A pinned Hugging Face revision is required"}
        return value
    }
    fun file(path:String):String {
        require(path.isNotBlank()&&path.length<=750&&
            path.split('/').all{fileSegment.matches(it)&&it!="."&&it!=".."}) {
            "Unsafe model file path"
        }
        return path
    }
    fun downloadUrl(id:String,revision:String,path:String):String =
        "https://huggingface.co/"+repository(id)+"/resolve/"+
            revision(revision)+"/"+file(path).split('/')
                .joinToString("/"){java.net.URLEncoder.encode(it,"UTF-8").replace("+","%20")}+
            "?download=true"
}

class HuggingFaceHub {
    private fun get(url:String):String {
        require(url.startsWith("https://huggingface.co/api/models")) {
            "Only the official Hugging Face model catalogue is supported"
        }
        val connection=(URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod="GET"
            connectTimeout=18_000
            readTimeout=25_000
            setRequestProperty("Accept","application/json")
            setRequestProperty("User-Agent","AsterionArt-Android/1.0")
        }
        try {
            check(connection.responseCode==200){
                "Hugging Face catalogue HTTP "+connection.responseCode+
                    if(connection.responseCode==401||connection.responseCode==403)
                        " (repository may require authentication)" else ""
            }
            val limit=3_000_000
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use{
                val text=it.readText()
                require(text.length<=limit){"Hub catalogue response exceeds size limit"}
                text
            }
        }finally{connection.disconnect()}
    }
    fun search(query:String):List<HubRepository> {
        val term=query.trim().take(100)
        require(term.length>=2){"Enter at least two characters"}
        val url="https://huggingface.co/api/models?search="+
            java.net.URLEncoder.encode(term,"UTF-8")+"&limit=30&full=true"
        val data=JSONArray(get(url))
        return (0 until data.length()).mapNotNull {i->
            val item=data.optJSONObject(i)?:return@mapNotNull null
            val id=item.optString("id")
            val revision=item.optString("sha")
            if(runCatching{HubPath.repository(id);HubPath.revision(revision)}.isFailure)
                return@mapNotNull null
            HubRepository(id,revision,item.optLong("downloads"),item.optBoolean("gated")||
                item.optString("gated")=="manual"||item.optString("gated")=="auto")
        }
    }
    fun files(repo:HubRepository):List<HubModelFile> {
        val id=HubPath.repository(repo.id)
        val revision=HubPath.revision(repo.revision)
        val raw=get("https://huggingface.co/api/models/"+id+"/tree/"+revision+
            "?recursive=true&expand=false")
        val tree=JSONArray(raw)
        require(tree.length()<=5000){"Repository contains too many files to browse"}
        return (0 until tree.length()).mapNotNull{index->
            val info=tree.optJSONObject(index)?:return@mapNotNull null
            if(info.optString("type")!="file")return@mapNotNull null
            val path=info.optString("path")
            if(runCatching{HubPath.file(path)}.isFailure)return@mapNotNull null
            val lower=path.lowercase()
            if(!listOf(".zip",".gguf",".safetensors",".ckpt",".bin",".onnx")
                    .any{lower.endsWith(it)})return@mapNotNull null
            val lfs=info.optJSONObject("lfs")
            val size=lfs?.optLong("size",0L)?.takeIf{it>0}
                ?:info.optLong("size",0L)
            val hash=lfs?.optString("oid","")?.removePrefix("sha256:")
                ?.takeIf{it.matches(Regex("[a-fA-F0-9]{64}"))}
            HubModelFile(path,size,hash)
        }.sortedWith(compareBy({it.path.substringAfterLast('.')},{it.path}))
    }
}
