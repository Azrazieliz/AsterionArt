package com.asterion.art.learning

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Evidence-preserving training manifest from EXPLICIT detailed grades only.
 *
 * Numeric grades, user-authored feature corrections, real image URI and private
 * cropped PNG travel together. It is NOT an invented binary Keep/Reject pair
 * and is NOT uploaded automatically to any provider.
 */
object GradingCorpus {
    fun trainingRecords(grades:List<DetailedGrade>):List<JSONObject> =
        grades.filter{it.patchPath!=null&&it.evidenceSha256!=null}
            .sortedBy{it.id}
            .map{grade->
                JSONObject().apply{
                    put("schema","asterion.grading-evidence.v1")
                    put("grade_id",grade.id)
                    put("source","explicit_user_visual_grading")
                    put("source_image_uri",grade.assetUri)
                    put("source_job_id",grade.jobId?:JSONObject.NULL)
                    put("design_id",grade.targetDesignId?:JSONObject.NULL)
                    put("creation_mode",grade.creationMode?.name?:JSONObject.NULL)
                    put("render_mode",grade.renderMode?.name?:JSONObject.NULL)
                    put("concepts",JSONArray(grade.concepts))
                    put("overall",grade.overall?:JSONObject.NULL)
                    put("dimensions",JSONObject().apply{
                        grade.ratings.forEach{(dimension,value)->
                            put(dimension.name,value)
                        }
                    })
                    put("target_feature",grade.feature)
                    put("preserve_exact",grade.preserve)
                    put("correct_exact",grade.correct)
                    put("region",JSONObject()
                        .put("x",grade.region.x.toDouble())
                        .put("y",grade.region.y.toDouble())
                        .put("width",grade.region.width.toDouble())
                        .put("height",grade.region.height.toDouble()))
                    put("private_patch_path",grade.patchPath)
                    put("patch_sha256",grade.evidenceSha256)
                    put("measured_pixel_features",JSONArray(grade.visualFeatures))
                    put("no_automatic_upload",true)
                    put("does_not_imply_global_preference",true)
                }
            }
}

/**
 * The app-private corpus is usable by a later authorized LoRA pipeline. This
 * export alone does not train Qwen, a vision encoder or diffusion weights.
 */
class GradingCorpusStore(context:Context) {
    private val app=context.applicationContext
    private val output=File(app.filesDir,
        "grading/training/explicit-visual-grades.jsonl")
    @Synchronized fun export():File {
        val records=GradingCorpus.trainingRecords(
            DetailedGradeStore(app).list())
        val data=records.joinToString("\n"){it.toString()}+
            if(records.isEmpty())"" else "\n"
        GeneratedImagePublisher.atomicWrite(output,
            data.toByteArray(Charsets.UTF_8))
        return output
    }
}
