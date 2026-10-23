package com.asterion.art.video

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class VideoResult(
    val jobId: String,
    val outputUri: String,
    val sourceImages: List<String>,
    val segmentCount: Int,
    val engine: String,
    val generatedAt: Long
)

class VideoResultStore(context: Context) {
    private val root=File(context.applicationContext.filesDir,"video-plans").also { it.mkdirs() }

    fun record(result: VideoResult) {
        val json=JSONObject().apply {
            put("schema_version",1)
            put("id",result.jobId)
            put("video",result.outputUri)
            put("references",JSONArray(result.sourceImages))
            put("segments",result.segmentCount)
            put("engine",result.engine)
            put("created",result.generatedAt)
        }
        GeneratedImagePublisher.atomicWrite(File(root,result.jobId+"-result.json"),
            json.toString(2).toByteArray())
    }
    fun list(): List<VideoResult> = root.listFiles()?.filter {
        it.name.endsWith("-result.json")
    }?.mapNotNull { file ->
        runCatching {
            val obj=JSONObject(file.readText())
            val raw=obj.getJSONArray("references")
            VideoResult(obj.getString("id"),obj.getString("video"),
                (0 until raw.length()).map { raw.getString(it) },
                obj.getInt("segments"),obj.optString("engine","Wan video"),
                obj.getLong("created"))
        }.getOrNull()
    }?.sortedByDescending { it.generatedAt }.orEmpty()
}
