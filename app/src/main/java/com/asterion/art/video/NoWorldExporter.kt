package com.asterion.art.video

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A neutral pending NoWorld package. Existing NoWorld art gates still decide
 * import/approval; an MP4 is not declared a finished engine asset.
 */
object NoWorldExporter {
    fun buildZip(context: Context, result: VideoResult): ByteArray {
        val source=context.contentResolver.openInputStream(Uri.parse(result.outputUri))
            ?.use { it.readBytes() } ?: error("Video output not accessible")
        val hash=MessageDigest.getInstance("SHA-256").digest(source)
            .joinToString("") { "%02x".format(it) }
        val manifest=JSONObject().apply {
            put("schema","noworld.asset.pending.v1")
            put("project","NoWorld: False Horizon")
            put("asset_id","ART-"+result.jobId)
            put("source_lane","ASTERION_ART")
            put("source_format","mp4")
            put("source_sha256",hash)
            put("source_file","animation.mp4")
            put("source_size_bytes",source.size)
            put("art_acceptance","NOT_APPROVED")
            put("unreal_native_import","NOT_TESTED")
            put("device_validation","NOT_TESTED")
            put("source_review","PENDING")
            put("source_approval_is_not_inferred",true)
            put("media_kind","TWO_DIMENSIONAL_ANIMATION")
            put("generation_backend",result.engine)
            put("segment_count",result.segmentCount)
            put("source_reference_uris",JSONArray(result.sourceImages))
        }
        val output=ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.nwasset.json"))
            zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("animation.mp4"))
            zip.write(source)
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    fun export(context: Context, result: VideoResult): Uri {
        val zip=buildZip(context,result)
        val values=ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME,
                "Asterion_" + result.jobId.take(12) + "_NoWorld.zip")
            put(MediaStore.Downloads.MIME_TYPE,"application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH,"Download/AsterionArt/NoWorld")
            put(MediaStore.Downloads.IS_PENDING,1)
        }
        val resolver=context.contentResolver
        val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values)
            ?: error("Cannot reserve NoWorld export")
        try {
            requireNotNull(resolver.openOutputStream(uri)).use { it.write(zip) }
            resolver.update(uri,ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING,0)
            },null,null)
            return uri
        } catch(e:Throwable) { resolver.delete(uri,null,null); throw e }
    }
}
