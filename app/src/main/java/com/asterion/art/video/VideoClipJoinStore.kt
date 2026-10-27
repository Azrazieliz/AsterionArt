package com.asterion.art.video

import android.content.Context
import java.net.URI
import com.asterion.art.data.GeneratedImagePublisher
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Persist chosen video clips outside WorkManager's input-data byte limit. */
data class VideoClipJoinPlan(val id:String,val clips:List<String>) {
    init {
        require(id.matches(Regex("join_[a-f0-9]{32}")))
        require(clips.size>=2&&clips.none{it.isBlank()})
        require(clips.distinct().size==clips.size)
        require(clips.all{raw->runCatching{
            val uri=URI.create(raw)
            uri.scheme=="content"&&!uri.authority.isNullOrBlank()
        }.getOrDefault(false)})
    }
    fun json():String=JSONObject().apply {
        put("schema","asterion.clip-join.v1")
        put("id",id)
        put("clips",JSONArray(clips))
    }.toString()
    companion object {
        fun fromJson(raw:String):VideoClipJoinPlan {
            val json=JSONObject(raw)
            require(json.getString("schema")=="asterion.clip-join.v1")
            val parts=json.getJSONArray("clips")
            return VideoClipJoinPlan(json.getString("id"),
                (0 until parts.length()).map{parts.getString(it)})
        }
    }
}
class VideoClipJoinStore(context:Context) {
    private val root=File(context.applicationContext.filesDir,"video-join-plans")
        .also{it.mkdirs()}
    fun create(clips:List<String>):VideoClipJoinPlan {
        val plan=VideoClipJoinPlan("join_"+UUID.randomUUID().toString().replace("-",""),clips)
        GeneratedImagePublisher.atomicWrite(File(root,plan.id+".json"),
            plan.json().toByteArray(Charsets.UTF_8))
        return plan
    }
    fun load(id:String):VideoClipJoinPlan {
        require(id.matches(Regex("join_[a-f0-9]{32}")))
        return VideoClipJoinPlan.fromJson(
            File(root,id+".json").readText(Charsets.UTF_8))
    }
}
