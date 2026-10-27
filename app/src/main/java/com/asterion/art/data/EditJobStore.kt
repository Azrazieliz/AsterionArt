package com.asterion.art.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class EditJob(val id: String, val sourceUri: String, val sourceJobId: String,
                   val maskPath: String, val strength: Float,
                   val correction:String="",val preserve:String="",
                   val sourceGradeId:String?=null)

class EditJobStore(context: Context) {
    private val root = File(context.applicationContext.filesDir, "edit-plans").also { it.mkdirs() }

    fun create(sourceUri: String, sourceJobId: String, mask: ByteArray, strength: Float,
               correction:String="",preserve:String="",gradeId:String?=null): EditJob {
        require(correction.length<=5000&&preserve.length<=5000)
        require(gradeId==null||gradeId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        require(strength in 0.01f..1.0f)
        val id = UUID.randomUUID().toString()
        val maskFile = File(root,id + "-mask.png")
        GeneratedImagePublisher.atomicWrite(maskFile, mask)
        val job = EditJob(id,sourceUri,sourceJobId,maskFile.absolutePath,
            strength,correction,preserve,gradeId)
        val file = File(root,id + ".json")
        GeneratedImagePublisher.atomicWrite(file,JSONObject().apply {
            put("id",id);put("uri",sourceUri);put("source",sourceJobId)
            put("mask",maskFile.absolutePath);put("strength",strength.toDouble())
            put("explicit_correction",correction);put("preserve_exact",preserve)
            put("origin_grade_id",gradeId?:JSONObject.NULL)
        }.toString().toByteArray())
        return job
    }

    fun load(id: String): EditJob {
        require(id.matches(Regex("[A-Za-z0-9_-]{8,90}")))
        val data = JSONObject(File(root,id + ".json").readText())
        val maskFile = File(root,id + "-mask.png").canonicalFile
        require(data.getString("mask") == maskFile.absolutePath && maskFile.isFile)
        return EditJob(id,data.getString("uri"),data.getString("source"),
            maskFile.absolutePath,data.getDouble("strength").toFloat(),
            data.optString("explicit_correction",""),
            data.optString("preserve_exact",""),
            data.optString("origin_grade_id","").takeIf{
                it.isNotBlank()&&it!="null"
            })
    }
}
