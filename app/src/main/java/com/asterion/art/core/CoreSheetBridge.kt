package com.asterion.art.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CoreCharacterSheet(
    val sheetId: String,
    val characterId: String,
    val characterName: String,
    val seriesCode: String,
    val sha256: String,
    val uri: Uri
)

class CoreSheetBridge(private val context: Context) {
    companion object {
        const val AUTHORITY = "com.ailm.android.asterionart.sheets"
        val listUri: Uri = Uri.Builder().scheme("content")
            .authority(AUTHORITY).appendPath("characters").build()
    }

    suspend fun search(query: String, limit: Int = 48): List<CoreCharacterSheet> =
        withContext(Dispatchers.IO) {
            val uri = listUri.buildUpon()
                .appendQueryParameter("search", query.take(150))
                .appendQueryParameter("limit", limit.coerceIn(1, 100).toString())
                .build()
            try {
                val result = mutableListOf<CoreCharacterSheet>()
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                    ?: throw IllegalStateException("Asterion Core sheet provider not available")
                cursor.use { rows ->
                    val id = rows.getColumnIndexOrThrow("sheet_asset_id")
                    val character = rows.getColumnIndexOrThrow("character_id")
                    val name = rows.getColumnIndexOrThrow("character_name")
                    val series = rows.getColumnIndexOrThrow("series_code")
                    val hash = rows.getColumnIndexOrThrow("sha256")
                    val media = rows.getColumnIndexOrThrow("reference_uri")
                    while (rows.moveToNext()) {
                        result += CoreCharacterSheet(
                            rows.getString(id), rows.getString(character),
                            rows.getString(name), rows.getString(series),
                            rows.getString(hash), Uri.parse(rows.getString(media))
                        )
                    }
                }
                result
            } catch (e: SecurityException) {
                throw IllegalStateException(
                    "Asterion Core reference permission denied. " +
                    "Install matching signed Core and Art releases.", e)
            } catch (e: IllegalArgumentException) {
                throw IllegalStateException("Asterion Core sheet bridge unavailable or outdated.", e)
            }
        }
}
