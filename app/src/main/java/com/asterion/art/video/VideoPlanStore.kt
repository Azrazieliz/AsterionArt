package com.asterion.art.video

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.engine.UniversalCreativePlanner
import com.asterion.art.engine.ConceptResolver
import com.asterion.art.engine.ConceptTaxonomy
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class MotionPreset(val title: String, val instruction: String) {
    CINEMATIC("Cinematic", "cinematic slow camera movement and natural secondary motion"),
    CAMERA_PAN("Camera pan","slow lateral camera pan preserving character identity"),
    CAMERA_PUSH("Camera push","slow forward camera movement with coherent perspective"),
    WIND("Wind and fabric","natural hair and fabric movement driven by a soft wind"),
    IDLE("Idle motion","subtle breathing and realistic restrained movement"),
    DYNAMIC("Dynamic action","dramatic coherent body movement and dynamic camera"),
    MANIFESTATION("World manifestation","the environment gradually transforms around the fixed characters"),
    ULTIMATE("Ultimate animation","progressive charging, magical effects, then a cinematic climax")
}

data class VideoPlan(
    val id: String,
    val imageUris: List<String>,
    val preset: MotionPreset,
    val secondsPerSegment: Int,
    val fps: Int,
    val seed: Long,
    val createdAt: Long,
    val direction: String = "",
    val positivePrompt: String = "",
    val negativePrompt: String = ""
)

class VideoPlanStore(context: Context) {
    private val root=File(context.applicationContext.filesDir,"video-plans").also { it.mkdirs() }
    fun create(images: List<String>, preset: MotionPreset, seconds: Int = 2,
               direction: String = ""): VideoPlan {
        require(images.size in 1..8) { "Choose between one and eight reference images" }
        require(images.distinct().size == images.size)
        require(seconds in 1..5)
        val prompts=compileMotion(preset,direction,ConceptTaxonomy(context))
        val plan=VideoPlan(UUID.randomUUID().toString(),images,preset,seconds,24,
            System.currentTimeMillis().and(0x7FFFFFFFL),System.currentTimeMillis(),
            direction.trim(),prompts.first,prompts.second)
        val data=JSONObject().apply {
            put("schema_version",1)
            put("id",plan.id)
            put("images",JSONArray(plan.imageUris))
            put("preset",plan.preset.name)
            put("seconds",plan.secondsPerSegment)
            put("fps",plan.fps)
            put("seed",plan.seed)
            put("created",plan.createdAt)
            put("direction",plan.direction)
            put("positive_prompt",plan.positivePrompt)
            put("negative_prompt",plan.negativePrompt)
        }
        GeneratedImagePublisher.atomicWrite(File(root,plan.id+".json"),
            data.toString(2).toByteArray(Charsets.UTF_8))
        return plan
    }
    companion object {
        fun compileMotion(preset: MotionPreset, direction: String = "",
                          resolver: ConceptResolver?=null): Pair<String,String> {
            val compiled=UniversalCreativePlanner.motion(preset.title,direction,resolver)
            val base=preset.instruction + if(direction.isBlank()) "" else
                ". Requested motion: " + direction.trim()
            return (base + ". " + compiled.positive) to compiled.negative
        }
    }
    fun load(id: String): VideoPlan {
        require(id.matches(Regex("[A-Za-z0-9_-]{8,90}")))
        val data=JSONObject(File(root,id+".json").readText())
        require(data.getInt("schema_version")==1)
        val list=data.getJSONArray("images")
        return VideoPlan(id,(0 until list.length()).map { list.getString(it) },
            MotionPreset.valueOf(data.getString("preset")),
            data.getInt("seconds"),data.getInt("fps"),
            data.getLong("seed"),data.getLong("created"),
            data.optString("direction",""),
            data.optString("positive_prompt","").ifBlank {
                compileMotion(MotionPreset.valueOf(data.getString("preset")),
                    data.optString("direction","")).first },
            data.optString("negative_prompt","").ifBlank {
                compileMotion(MotionPreset.valueOf(data.getString("preset")),
                    data.optString("direction","")).second })
    }
}
