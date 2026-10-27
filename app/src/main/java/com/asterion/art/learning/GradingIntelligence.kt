package com.asterion.art.learning

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.min

/** One separately trained pixel-feature head per ORIGINAL/creation mode and grade dimension. */
data class LearnedVisualHead(
    val mode:String,val dimension:String,
    val samples:Int,val bias:Double,val weights:List<Double>
) {
    init {require(weights.size==VisualGradeFeatures.SIZE)}
    fun predict(features:List<Double>):Double {
        require(features.size==VisualGradeFeatures.SIZE)
        val centered=weights.indices.sumOf{index->
            weights[index]*(features[index]-0.5)
        }
        return (1.0+4.0*(bias+centered).coerceIn(0.0,1.0)).coerceIn(1.0,5.0)
    }
}

object GradingVisualTraining {
    /** Genuine bounded supervised learning from *user-assessed actual image pixels*. */
    fun train(grades:List<DetailedGrade>):List<LearnedVisualHead> {
        val records=grades.filter {
            it.visualFeatures.size==VisualGradeFeatures.SIZE &&
                it.visualFeatures.all {v->v.isFinite()&&v in 0.0..1.0}
        }.sortedBy{it.id}.takeLast(3000)
        data class Event(val mode:String,val axis:String,
                         val pixels:List<Double>,val target:Double)
        val events=records.flatMap{grade->
            val mode=grade.creationMode?.name ?:
                grade.renderMode?.name ?: "UNSCOPED"
            val axes=grade.ratings.map{it.key.name to it.value}+
                listOfNotNull(grade.overall?.let{"OVERALL" to it})
            axes.map{(dimension,score)->
                Event(mode,dimension,grade.visualFeatures,(score-1)/4.0)
            }
        }
        return events.groupBy{it.mode to it.axis}.map{(key,training)->
            val weights=DoubleArray(VisualGradeFeatures.SIZE)
            var bias=0.5
            // Deterministic model rebuild: correcting/deleting a grade undoes
            // its effect rather than accumulating stale and duplicate updates.
            repeat(15) {
                for(example in training) {
                    val features=example.pixels
                    val prediction=(bias+weights.indices.sumOf{index->
                        weights[index]*(features[index]-0.5)
                    }).coerceIn(0.0,1.0)
                    val error=example.target-prediction
                    bias=(bias+0.055*error).coerceIn(0.0,1.0)
                    for(i in weights.indices)
                        weights[i]=(weights[i]*0.9995)+
                            0.0025*error*(features[i]-0.5)
                }
            }
            LearnedVisualHead(key.first,key.second,training.size,
                bias,weights.toList())
        }
    }
}

/** Local, bounded, inspectable trained state; NOT a fine-tuned Qwen/VLM/DiT. */
class GradingLearningModel(context:Context) {
    private val file=File(context.applicationContext.filesDir,
        "grading/learned-visual-heads.v1.json")
    @Synchronized fun rebuild(grades:List<DetailedGrade>):List<LearnedVisualHead> {
        val heads=GradingVisualTraining.train(grades)
        val json=JSONObject().apply {
            put("schema","asterion.visual-grade-linear.v1")
            put("training_images",grades.count{it.visualFeatures.size==
                VisualGradeFeatures.SIZE})
            put("architecture","per-mode per-dimension bounded pixel regression")
            put("not_visual_semantic_understanding",true)
            put("does_not_modify_qwen_or_diffusion",true)
            put("heads",JSONArray(heads.map{head->
                JSONObject().put("mode",head.mode)
                    .put("axis",head.dimension)
                    .put("samples",head.samples)
                    .put("bias",head.bias)
                    .put("weights",JSONArray(head.weights))
            }))
        }
        GeneratedImagePublisher.atomicWrite(file,
            json.toString(2).toByteArray(Charsets.UTF_8))
        return heads
    }
    fun heads():List<LearnedVisualHead> {
        if(!file.exists())return emptyList()
        return runCatching {
            val json=JSONObject(file.readText(Charsets.UTF_8))
            require(json.getString("schema")=="asterion.visual-grade-linear.v1")
            val array=json.getJSONArray("heads")
            (0 until array.length()).map{index->
                val value=array.getJSONObject(index)
                val w=value.getJSONArray("weights")
                LearnedVisualHead(value.getString("mode"),value.getString("axis"),
                    value.getInt("samples"),value.getDouble("bias"),
                    (0 until w.length()).map{w.getDouble(it)})
            }
        }.getOrDefault(emptyList())
    }
    /** Below five examples per axis: do not issue a model prediction. */
    fun estimate(mode:String,axis:String,features:List<Double>):Double? =
        heads().firstOrNull{it.mode==mode&&it.dimension==axis&&it.samples>=5}
            ?.predict(features)
}

/**
 * Only USER-ENTERED correction/preservation text influences future prompts.
 * Pixel-derived numerical scores cannot invent anatomy or methodology rules.
 */
object GradingGuidance {
    private fun normalize(value:String)=value.trim().lowercase()
    fun applicable(grades:List<DetailedGrade>,mode:CreationMode?,
                   concepts:List<String>,designId:String?=null,
                   limit:Int=4,renderMode:RenderMode?=null):List<DetailedGrade> {
        require(limit in 1..30)
        val conceptSet=concepts.map(::normalize).filter{it.isNotBlank()}.toSet()
        return grades.asSequence().filter { grade ->
            if(mode!=null)grade.creationMode==mode
            else renderMode!=null && grade.creationMode==null &&
                grade.renderMode==renderMode
        }
            .filter{it.preserve.isNotBlank()||it.correct.isNotBlank()}
            .filter { grade ->
                if(grade.targetDesignId!=null)grade.targetDesignId==designId
                else {
                    val sources=grade.concepts.map(::normalize).toSet()
                    sources.isNotEmpty()&&conceptSet.isNotEmpty()&&
                        sources.intersect(conceptSet).isNotEmpty()
                }
            }
            .sortedByDescending{it.updatedAt}.take(limit).toList()
    }
    fun evidence(grades:List<DetailedGrade>):String=buildString {
        if(grades.isEmpty())return@buildString
        append("USER-AUTHORED, MODE-MATCHED GRADING EVIDENCE. ")
        append("Do not extrapolate corrections to unmentioned features; ")
        append("never override canonical taxonomy or approved methodology.\n")
        for(grade in grades) {
            append("\nGRADE RECORD ").append(grade.id).append('\n')
            grade.targetDesignId?.let{append("TARGET DESIGN: ").append(it).append('\n')}
            if(grade.feature.isNotBlank())append("SPECIFIC FEATURE: ")
                .append(grade.feature).append('\n')
            grade.ratings.forEach{(axis,value)->
                append("USER SCORE ").append(axis.name)
                    .append(": ").append(value).append("/5\n")
            }
            append("USER REGION: normalized x=").append(grade.region.x)
                .append(" y=").append(grade.region.y)
                .append(" w=").append(grade.region.width)
                .append(" h=").append(grade.region.height).append('\n')
            if(grade.preserve.isNotBlank())
                append("USER EXPLICITLY WANTS TO PRESERVE: ")
                    .append(grade.preserve).append('\n')
            if(grade.correct.isNotBlank())
                append("USER EXPLICITLY WANTS TO CORRECT: ")
                    .append(grade.correct).append('\n')
            // The text-only Qwen3 cannot see these pixels. Their real private
            // patch references are available to the training infrastructure.
            grade.evidenceSha256?.let{
                append("REAL PIXEL EVIDENCE SHA-256: ").append(it).append('\n')
            }
        }
    }
}
