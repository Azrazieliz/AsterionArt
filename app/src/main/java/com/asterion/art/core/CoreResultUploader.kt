package com.asterion.art.core

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.model.RenderMode

/**
 * Streams finished PNGs into Core's signed provider. Core verifies SHA and
 * indexes the full image in its real local SQLite library.
 */
class CoreResultUploader(private val context:Context) {
    companion object {
        const val AUTHORITY="com.ailm.android.asterionart.results"
        val inbox:Uri=Uri.Builder().scheme("content").authority(AUTHORITY)
            .appendPath("upload").build()
    }
    fun upload(uri:Uri,jobId:String,mode:RenderMode,sha256:String):Uri {
        require(jobId.matches(Regex("[A-Za-z0-9_-]{8,90}")))
        require(sha256.matches(Regex("[a-fA-F0-9]{64}")))
        val resolver=context.contentResolver
        val values=ContentValues().apply {
            put("job_id",jobId)
            put("render_mode",mode.name)
            put("sha256",sha256)
        }
        val destination=try {
            resolver.insert(inbox,values)
                ?: error("Core Art result service unavailable")
        } catch(e:SecurityException) {
            error("Core/Art signing certificates do not match; cannot save to Core.")
        }
        requireNotNull(resolver.openInputStream(uri)).use { input ->
            requireNotNull(resolver.openOutputStream(destination,"w")).use { output ->
                input.copyTo(output,512*1024)
            }
        }
        require(resolver.update(destination,ContentValues().apply {
            put("commit",1)
        },null,null)==1) { "Core rejected artwork validation" }
        val id=destination.lastPathSegment ?: error("Missing uploaded asset id")
        return Uri.Builder().scheme("content").authority(AUTHORITY)
            .appendPath("asset").appendPath(id).build()
    }
}
