package com.asterion.art.data

import android.content.Context
import com.asterion.art.model.GeneratorSettings
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class GenerationJob(
    val id: String,
    val mode: RenderMode,
    val sheetIds: List<String>,
    val settings: GeneratorSettings,
    val createdAt: Long,
    val creationMode: CreationMode? = null,
    val sourceImageUri: String? = null,
    val concepts: List<String> = emptyList(),
    val designPrompt: String? = null,
    val designNegative: String? = null,
    val parentDesignIds: List<String> = emptyList(),
    val characterPlanId: String? = null
)

class GenerationJobStore(context: Context) {
    private val folder = File(context.applicationContext.filesDir, "generation-plans")
        .also { it.mkdirs() }

    fun create(mode: RenderMode, sheets: List<String>, settings: GeneratorSettings,
               creationMode: CreationMode? = null, sourceImageUri: String? = null,
               concepts: List<String> = emptyList(),
               designPrompt: String? = null,
               designNegative: String? = null,
               parentDesignIds: List<String> = emptyList(),
               characterPlanId: String? = null): GenerationJob {
        val job = GenerationJob(UUID.randomUUID().toString(), mode, sheets.distinct(),
            settings, System.currentTimeMillis(),creationMode,sourceImageUri,
            concepts.distinct(),designPrompt,designNegative,parentDesignIds.distinct(),
            characterPlanId)
        val s = JSONObject().apply {
            put("ratio", settings.ratio)
            put("steps", settings.steps)
            put("cfg", settings.cfg.toDouble())
            put("batch", settings.batch)
            put("denoise", settings.denoise.toDouble())
            put("scheduler", settings.scheduler)
            put("karras", settings.karras)
            put("automatic", settings.automatic)
            put("seed", settings.seed ?: JSONObject.NULL)
        }
        val root = JSONObject().apply {
            put("version", 1)
            put("id", job.id)
            put("mode", mode.name)
            put("sheets", JSONArray(job.sheetIds))
            put("created", job.createdAt)
            put("creation_mode", creationMode?.name ?: JSONObject.NULL)
            put("source_image",sourceImageUri ?: JSONObject.NULL)
            put("concepts",JSONArray(job.concepts))
            put("design_prompt",job.designPrompt ?: JSONObject.NULL)
            put("design_negative",job.designNegative ?: JSONObject.NULL)
            put("design_parents",JSONArray(job.parentDesignIds))
            put("character_phase1_plan",job.characterPlanId ?: JSONObject.NULL)
            put("settings", s)
        }
        val tmp = File(folder, job.id + ".tmp")
        tmp.writeText(root.toString())
        require(tmp.renameTo(File(folder, job.id + ".json"))) { "Cannot persist job plan" }
        return job
    }

    fun load(id: String): GenerationJob {
        require(id.matches(Regex("[A-Za-z0-9_-]{8,90}")))
        val root = JSONObject(File(folder, id + ".json").readText())
        require(root.getInt("version") == 1)
        val s = root.getJSONObject("settings")
        val names = root.getJSONArray("sheets")
        return GenerationJob(
            id, RenderMode.valueOf(root.getString("mode")),
            (0 until names.length()).map { names.getString(it) },
            GeneratorSettings(
                ratio = s.getString("ratio"),
                steps = s.getInt("steps"),
                cfg = s.getDouble("cfg").toFloat(),
                batch = s.getInt("batch"),
                denoise = s.getDouble("denoise").toFloat(),
                scheduler = s.getString("scheduler"),
                karras = s.getBoolean("karras"),
                seed = if (s.isNull("seed")) null else s.getLong("seed"),
                automatic = s.getBoolean("automatic")
            ),
            root.getLong("created"),
            if (root.isNull("creation_mode") || !root.has("creation_mode")) null
                else CreationMode.valueOf(root.getString("creation_mode")),
            if (root.isNull("source_image") || !root.has("source_image")) null
                else root.getString("source_image"),
            root.optJSONArray("concepts")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }.orEmpty(),
            if(root.isNull("design_prompt")) null
                else root.optString("design_prompt","").takeIf { it.isNotBlank() },
            if(root.isNull("design_negative")) null
                else root.optString("design_negative","").takeIf { it.isNotBlank() },
            root.optJSONArray("design_parents")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }.orEmpty(),
            root.optString("character_phase1_plan","").takeIf { it.isNotBlank() && it!="null" }
        )
    }
}
