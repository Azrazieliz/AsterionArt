package com.asterion.compiler.art

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Exports video and an explicitly unapproved NoWorld asset manifest.
 * NoWorld has not validated these frames; never mark them production ready.
 */
object ArtExport {
    fun exportForNoWorld(context: Context, result: NightmareResult,
                         sheetIds: List<String>, mode: ArtMode): List<String> {
        require(Build.VERSION.SDK_INT >= 29) { "Scoped-storage export requires Android 10 or newer" }
        val digest = NightmareBridge.sha256(result.file)
        val name = "asterion_${System.currentTimeMillis()}"
        val path = Environment.DIRECTORY_DOWNLOADS + "/AsterionArt/NoWorld/"
        val manifest = JSONObject()
            .put("schema", "noworld.asset.preview.v1")
            .put("project", "NoWorld: False Horizon")
            .put("asset_id", name)
            .put("source_lane", "ASTERION_ART_2D")
            .put("source_format", "mp4")
            .put("source_file", "$name.mp4")
            .put("source_sha256", digest)
            .put("source_size_bytes", result.file.length())
            .put("art_acceptance", "NOT_APPROVED")
            .put("device_validation", "NOT_TESTED")
            .put("unreal_native_import", "NOT_TESTED")
            .put("source_approval_is_not_inferred", true)
            .put("reference_sheet_ids", JSONArray(sheetIds))
            .put("generation_mode", mode.name)
        val resultUris = mutableListOf<String>()
        resultUris += put(context, "$name.mp4", "video/mp4", path) { sink ->
            result.file.inputStream().use { it.copyTo(sink, bufferSize = 256 * 1024) }
        }
        resultUris += put(context, "$name.nwasset.json", "application/json", path) { sink ->
            sink.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
        }
        return resultUris
    }

    private fun put(context: Context, title: String, mime: String,
                    relative: String, write: (java.io.OutputStream) -> Unit): String {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, title)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(context.contentResolver.insert(collection, values)) {
            "Could not create NoWorld export"
        }
        try {
            context.contentResolver.openOutputStream(uri)?.use(write)
                ?: error("Could not write export")
            context.contentResolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw e
        }
        return uri.toString()
    }
}
