package com.asterion.art.learning

import android.content.Context
import android.net.Uri
import com.asterion.art.data.GeneratedImagePublisher
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

    /** Explicit SAF export only. Exports exact verified visual crops + labels. */
    @Synchronized fun exportZip(target:Uri):Int {
        val entries=DetailedGradeStore(app).list()
            .filter{it.patchPath!=null&&it.evidenceSha256!=null}
        require(entries.isNotEmpty()){"No graded image evidence available to export"}
        val trusted=File(app.filesDir,"grading/evidence").canonicalFile
        val result=entries.map { grade->
            val file=File(requireNotNull(grade.patchPath)).canonicalFile
            require(file.parentFile==trusted){
                "Grading evidence path is outside the private image store"
            }
            require(file.isFile && file.length() in 64L..25_000_000L){
                "Missing or invalid grading evidence: "+grade.id
            }
            val digest=MessageDigest.getInstance("SHA-256")
            file.inputStream().use{input->
                val buf=ByteArray(65536)
                while(true) {
                    val count=input.read(buf)
                    if(count<0)break
                    digest.update(buf,0,count)
                }
            }
            val hash=digest.digest().joinToString(""){"%02x".format(it)}
            require(hash.equals(grade.evidenceSha256,true)) {
                "Grading evidence does not match its captured pixels"
            }
            grade to file
        }
        requireNotNull(app.contentResolver.openOutputStream(target,"w")).use{sink->
            ZipOutputStream(sink).use{zip->
                val manifest=result.joinToString("\n") { (grade,_)->
                    val row=GradingCorpus.trainingRecords(listOf(grade)).single()
                    row.put("private_patch_path","images/"+grade.id+".png")
                    row.toString()
                }+"\n"
                zip.putNextEntry(ZipEntry("explicit-visual-grades.jsonl"))
                zip.write(manifest.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                result.forEach{(grade,file)->
                    zip.putNextEntry(ZipEntry("images/"+grade.id+".png"))
                    file.inputStream().use{it.copyTo(zip)}
                    zip.closeEntry()
                }
            }
        }
        return result.size
    }
}
