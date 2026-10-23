package com.asterion.art.video

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Bounded-memory, mobile-first pending NoWorld artwork/animation exporter.
 * NoWorld remains authoritative for asset approval, rigging and import status.
 */
object NoWorldExporter {
    private fun sourceUri(result: VideoResult) = Uri.parse(result.outputUri)

    private fun sourceDigest(context: Context, result: VideoResult): Pair<String,Long> {
        val digest=MessageDigest.getInstance("SHA-256")
        var total=0L
        requireNotNull(context.contentResolver.openInputStream(sourceUri(result))).use { input ->
            val buffer=ByteArray(512*1024)
            while (true) {
                val n=input.read(buffer)
                if(n<0)break
                if(n>0) { digest.update(buffer,0,n); total+=n }
            }
        }
        require(total>1024) { "Video source is empty" }
        return digest.digest().joinToString("") { "%02x".format(it) } to total
    }

    private fun manifest(result: VideoResult, digest: String, size: Long): JSONObject =
        JSONObject().apply {
            put("schema","noworld.asset.pending.v1")
            put("project","NoWorld: False Horizon")
            put("asset_id","ART-"+result.jobId)
            put("source_lane","ASTERION_ART")
            put("source_format","mp4")
            put("source_sha256",digest)
            put("source_file","animation.mp4")
            put("source_size_bytes",size)
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

    fun export(context: Context, result: VideoResult): Uri {
        val (hash,size)=sourceDigest(context,result)
        val spec=manifest(result,hash,size)
        val values=ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME,"Asterion_"+result.jobId.take(12)+"_NoWorld.zip")
            put(MediaStore.Downloads.MIME_TYPE,"application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH,"Download/AsterionArt/NoWorld")
            put(MediaStore.Downloads.IS_PENDING,1)
        }
        val resolver=context.contentResolver
        val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values)
            ?: error("Could not reserve NoWorld ZIP")
        try {
            ZipOutputStream(requireNotNull(resolver.openOutputStream(uri,"w"))).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.nwasset.json"))
                zip.write(spec.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("animation.mp4"))
                requireNotNull(resolver.openInputStream(sourceUri(result))).use { input ->
                    input.copyTo(zip,512*1024)
                }
                zip.closeEntry()
            }
            require(resolver.update(uri,ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING,0)
            },null,null)>0)
            return uri
        } catch(e:Throwable) {
            resolver.delete(uri,null,null)
            throw e
        }
    }
}
