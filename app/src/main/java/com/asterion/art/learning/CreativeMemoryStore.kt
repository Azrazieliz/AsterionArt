package com.asterion.art.learning

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Candidate selection and revision history; never an implicit training or grading event. */
data class SavedDesign(
    val id: String,
    val jobId: String,
    val mode: RenderMode,
    val creationMode: CreationMode?,
    val ordinal: Int,
    val sheetNumber: Int,
    val prompt: String,
    val negative: String,
    val sheetUri: String?,
    val status: String = "CANDIDATE",
    val parents: List<String> = emptyList(),
    val sourceSheetIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val previewUri: String? = null
)

object DesignFusionRules {
    fun fuse(sources:List<String>, intent:String):String {
        require(sources.size>=2) { "Select at least two designs, including across sheets" }
        require(sources.all { it.isNotBlank() })
        require(intent.length<=4000)
        return buildString {
            append("AUTHOR A COHERENT NEW DESIGN. Freeze the defining recognition anchors ")
            append("of the source candidates below. Reconcile incompatible body/structural ")
            append("systems by creating physically supported interfaces rather than ")
            append("juxtaposing detached components. Distinctive features must interact ")
            append("reciprocally in anatomy, functional material and silhouette. ")
            append("Produce one complete, consistent new design, not a collage.\n")
            sources.forEachIndexed { index,prompt ->
                append("\nSOURCE ").append(index+1).append(":\n")
                append(prompt.take(16000)).append('\n')
            }
            if(intent.isNotBlank()) append("\nFUSION INTENT:\n").append(intent).append('\n')
        }
    }
    fun alter(source:String, change:String):String {
        require(source.isNotBlank())
        require(change.isNotBlank() && change.length<=4000)
        return "Preserve the selected candidate's identity, defining silhouette, " +
            "face, anatomy and material system except where explicitly changed. " +
            "Author and render ONE coherent full-body revision. " +
            "SOURCE DESIGN:\n" + source + "\nREQUESTED ALTERATION:\n" + change.trim()
    }
}

/** JSON file with atomic replace; references are not deleted by record changes. */
class CreativeMemoryStore(context:Context) {
    private val appContext=context.applicationContext
    private val file=File(appContext.filesDir,"creative-memory/designs.json")
    private fun arr(root:JSONObject):JSONArray = root.optJSONArray("designs") ?: JSONArray()
    private fun loadJson():JSONObject=runCatching {
        JSONObject(file.readText())
    }.getOrDefault(JSONObject().put("schema","asterion.creative-memory.v1")
        .put("designs",JSONArray()))

    fun list():List<SavedDesign> = synchronized(this) {
        val items=arr(loadJson())
        (0 until items.length()).mapNotNull { index ->
            runCatching {
                val v=items.getJSONObject(index)
                val p=v.optJSONArray("parents") ?: JSONArray()
                val references=v.optJSONArray("source_sheets") ?: JSONArray()
                SavedDesign(v.getString("id"),v.getString("job"),
                    RenderMode.valueOf(v.getString("mode")),
                    v.optString("creation_mode","").takeIf{it.isNotBlank()}?.let{
                        CreationMode.valueOf(it) },
                    v.optInt("ordinal",1),v.optInt("sheet",1),
                    v.getString("positive"),v.optString("negative",""),
                    v.optString("sheet_uri","").takeIf{it.isNotBlank()},
                    v.optString("status","CANDIDATE").let {
                        if(it=="KEPT"||it=="REJECTED")"CANDIDATE" else it
                    },
                    (0 until p.length()).map { p.getString(it) },
                    (0 until references.length()).map { references.getString(it) },
                    v.optLong("created",0),
                    v.optString("preview_uri","").takeIf { it.isNotBlank() })
            }.getOrNull()
        }
    }

    private fun serialize(x:SavedDesign)=JSONObject().apply {
        put("id",x.id);put("job",x.jobId);put("mode",x.mode.name)
        put("creation_mode",x.creationMode?.name ?: "")
        put("ordinal",x.ordinal);put("sheet",x.sheetNumber)
        put("positive",x.prompt);put("negative",x.negative)
        put("sheet_uri",x.sheetUri ?: "");put("status",x.status)
        put("parents",JSONArray(x.parents))
        put("source_sheets",JSONArray(x.sourceSheetIds))
        put("created",x.createdAt)
        put("preview_uri",x.previewUri ?: "")
    }
    private fun save(all:List<SavedDesign>) {
        file.parentFile?.mkdirs()
        val data=JSONObject().put("schema","asterion.creative-memory.v1")
            .put("designs",JSONArray().apply { all.forEach { put(serialize(it)) } })
        GeneratedImagePublisher.atomicWrite(file,data.toString(2).toByteArray(Charsets.UTF_8))
    }

    @Synchronized fun upsert(record:SavedDesign) {
        require(record.id.isNotBlank() && record.prompt.isNotBlank())
        val all=list().filterNot { it.id==record.id }
        save(all+record)
    }

    @Synchronized fun alter(id:String,changes:String):SavedDesign {
        val parent=list().first { it.id==id }
        val record=parent.copy(id=UUID.randomUUID().toString(),
            prompt=DesignFusionRules.alter(parent.prompt,changes),
            status="REVISION",parents=listOf(id),createdAt=System.currentTimeMillis())
        upsert(record)
        return record
    }
    @Synchronized fun fuse(ids:List<String>,instructions:String=""):SavedDesign {
        val unique=ids.distinct()
        require(unique.size>=2)
        val all=list()
        val sources=unique.map { id -> all.first { it.id==id } }
        require(sources.map { it.mode }.distinct().size==1) {
            "Fusion requires a compatible output medium. Different sheets within that medium are supported."
        }
        val merged=SavedDesign(
            UUID.randomUUID().toString(),sources.first().jobId,sources.first().mode,
            sources.first().creationMode,1,1,
            DesignFusionRules.fuse(sources.map { it.prompt },instructions),
            sources.map { it.negative }.filter { it.isNotBlank() }.distinct().joinToString(", "),
            null,"FUSION",unique,
            sources.flatMap { it.sourceSheetIds }.distinct())
        upsert(merged)
        return merged
    }
}
