package com.asterion.art.learning

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * User-controlled assessment dimensions. An empty score means NOT ASSESSED,
 * not a zero, failure, preference, or global aesthetic verdict.
 */
enum class GradeDimension(val title:String) {
    ANATOMY("Anatomy and physical coherence"),
    CONCEPT_INTEGRATION("Concept integration"),
    IDENTITY("Identity and view consistency"),
    DIFFERENTIATION("Design differentiation"),
    AESTHETICS("Visual design and aesthetics"),
    TWO_D_ILLUSTRATION("2D illustration grammar"),
    MATERIALS("Materials and construction"),
    COMPOSITION("Composition and presentation"),
    TECHNICAL("Rendering and artifacts"),
    TEMPORAL("Motion and temporal consistency")
}

data class GradeRegion(val x:Float,val y:Float,val width:Float,val height:Float) {
    init {
        require(listOf(x,y,width,height).all{it.isFinite()})
        require(x in 0f..1f && y in 0f..1f)
        require(width>0f && height>0f)
        require(x+width<=1.00001f && y+height<=1.00001f)
    }
    companion object { val FULL=GradeRegion(0f,0f,1f,1f) }
}

data class DetailedGrade(
    val id:String=UUID.randomUUID().toString(),
    val assetUri:String,
    val targetDesignId:String?=null,
    val jobId:String?=null,
    val creationMode:CreationMode?=null,
    val renderMode:RenderMode?=null,
    val concepts:List<String> = emptyList(),
    val ratings:Map<GradeDimension,Int> = emptyMap(),
    val overall:Int?=null,
    val feature:String="",
    val preserve:String="",
    val correct:String="",
    val region:GradeRegion=GradeRegion.FULL,
    val patchPath:String?=null,
    val evidenceSha256:String?=null,
    val visualFeatures:List<Double> = emptyList(),
    val updatedAt:Long=System.currentTimeMillis()
) {
    fun validate() {
        require(id.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        require(assetUri.isNotBlank())
        require(targetDesignId==null||targetDesignId.isNotBlank())
        require(overall==null||overall in 1..5)
        require(ratings.all{it.value in 1..5})
        require(feature.length<=300&&preserve.length<=5000&&correct.length<=5000)
        require(ratings.isNotEmpty()||overall!=null||
            preserve.isNotBlank()||correct.isNotBlank()) {
            "Add a score or an explicit correction before saving grading"
        }
        require(concepts.size<=32 && concepts.all{it.length<=250})
        require(visualFeatures.isEmpty() ||
            (visualFeatures.size==VisualGradeFeatures.SIZE &&
                visualFeatures.all{it.isFinite() && it in 0.0..1.0}))
        require((patchPath==null)==(evidenceSha256==null))
    }
}

/** App-private, editable, reversible grading records. No network export. */
class DetailedGradeStore(context:Context) {
    private val app=context.applicationContext
    private val folder=File(app.filesDir,"grading").also{it.mkdirs()}
    private val file=File(folder,"feedback.v2.json")
    private fun obj(grade:DetailedGrade):JSONObject=JSONObject().apply {
        put("id",grade.id)
        put("asset",grade.assetUri)
        put("design_id",grade.targetDesignId?:JSONObject.NULL)
        put("job_id",grade.jobId?:JSONObject.NULL)
        put("creation_mode",grade.creationMode?.name?:JSONObject.NULL)
        put("render_mode",grade.renderMode?.name?:JSONObject.NULL)
        put("concepts",JSONArray(grade.concepts))
        put("scores",JSONObject().apply {
            grade.ratings.forEach{(axis,score)->put(axis.name,score)}
        })
        put("overall",grade.overall?:JSONObject.NULL)
        put("feature",grade.feature)
        put("preserve",grade.preserve)
        put("correct",grade.correct)
        put("region",JSONObject().put("x",grade.region.x.toDouble())
            .put("y",grade.region.y.toDouble())
            .put("width",grade.region.width.toDouble())
            .put("height",grade.region.height.toDouble()))
        put("patch_path",grade.patchPath?:JSONObject.NULL)
        put("evidence_sha256",grade.evidenceSha256?:JSONObject.NULL)
        put("visual_features",JSONArray(grade.visualFeatures))
        put("updated_at",grade.updatedAt)
    }
    private fun JSONObject.optNonblank(key:String):String? =
        optString(key,"").takeUnless{it.isBlank()||it=="null"}
    private fun deserialize(v:JSONObject):DetailedGrade {
        val scoreObject=v.optJSONObject("scores")?:JSONObject()
        val scores=GradeDimension.entries.mapNotNull{axis->
            val score=scoreObject.optInt(axis.name,0)
            if(score in 1..5)axis to score else null
        }.toMap()
        val bounds=v.getJSONObject("region")
        val features=v.optJSONArray("visual_features")?:JSONArray()
        val concepts=v.optJSONArray("concepts")?:JSONArray()
        return DetailedGrade(
            v.getString("id"),v.getString("asset"),
            v.optNonblank("design_id"),v.optNonblank("job_id"),
            v.optNonblank("creation_mode")?.let{CreationMode.valueOf(it)},
            v.optNonblank("render_mode")?.let{RenderMode.valueOf(it)},
            (0 until concepts.length()).map{concepts.getString(it)},
            scores,v.optInt("overall",0).takeIf{it in 1..5},
            v.optString("feature",""),v.optString("preserve",""),
            v.optString("correct",""),
            GradeRegion(bounds.getDouble("x").toFloat(),
                bounds.getDouble("y").toFloat(),bounds.getDouble("width").toFloat(),
                bounds.getDouble("height").toFloat()),
            v.optNonblank("patch_path"),v.optNonblank("evidence_sha256"),
            (0 until features.length()).map{features.getDouble(it)},
            v.optLong("updated_at",0)
        ).also{it.validate()}
    }
    @Synchronized fun list():List<DetailedGrade> {
        if(!file.isFile)return emptyList()
        val root=JSONObject(file.readText(Charsets.UTF_8))
        require(root.optString("schema")=="asterion.detailed-grades.v2"){
            "Unsupported grading schema; existing feedback has not been overwritten"
        }
        val a=root.getJSONArray("grades")
        return (0 until a.length()).map{deserialize(a.getJSONObject(it))}
    }
    @Synchronized fun forAsset(uri:String):List<DetailedGrade> =
        list().filter{it.assetUri==uri}.sortedByDescending{it.updatedAt}
    @Synchronized fun save(record:DetailedGrade) {
        record.validate()
        val existing=list()
        val updated=existing.filterNot{it.id==record.id}+record
        write(updated)
        GradingLearningModel(app).rebuild(updated)
    }
    @Synchronized fun delete(id:String) {
        val existing=list()
        require(existing.any{it.id==id}){"Grading record not found"}
        val remaining=existing.filterNot{it.id==id}
        write(remaining)
        GradingLearningModel(app).rebuild(remaining)
        // The image evidence remains private and is not deleted without
        // separately accounting for files referenced by other records.
    }
    private fun write(records:List<DetailedGrade>) {
        val root=JSONObject()
            .put("schema","asterion.detailed-grades.v2")
            .put("grades",JSONArray(records.map(::obj)))
        GeneratedImagePublisher.atomicWrite(file,
            root.toString(2).toByteArray(Charsets.UTF_8))
    }
}
