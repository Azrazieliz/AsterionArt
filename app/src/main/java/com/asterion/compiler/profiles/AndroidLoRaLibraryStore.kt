package com.asterion.compiler.profiles

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Persistent, streaming SAF-backed LoRA catalog. Files stay at their original
 * locations; the compiler stores persisted read permissions, hashes and metadata.
 * This avoids duplicating multi-GB assets on the phone.
 */
class AndroidLoRaLibraryStore(context: Context) {
    private val app = context.applicationContext
    private val catalog = File(app.filesDir, "asterion_lora_catalog_v1.json")

    fun list(): List<LoRaAsset> = runCatching {
        if (!catalog.exists()) return@runCatching emptyList()
        val items = JSONObject(catalog.readText()).getJSONArray("assets")
        (0 until items.length()).map { index ->
            val obj = items.getJSONObject(index)
            val keywords = obj.getJSONArray("triggers")
            val checkpoints = obj.optJSONArray("checkpoints") ?: JSONArray()
            LoRaAsset(
                id = obj.getString("id"),
                displayName = obj.getString("name"),
                uri = obj.getString("uri"),
                sha256 = obj.getString("sha256"),
                modelFamily = obj.getString("family"),
                triggerWords = (0 until keywords.length()).map(keywords::getString),
                defaultWeight = obj.optDouble("weight", 1.0).toFloat(),
                allowedCheckpointIds = (0 until checkpoints.length()).map(checkpoints::getString).toSet(),
                enabled = obj.optBoolean("enabled", true),
            )
        }
    }.getOrElse { throw IllegalStateException("LoRA catalog is damaged; existing entries were not changed.", it) }

    suspend fun import(
        uri: Uri,
        name: String,
        triggerWords: List<String>,
        checkpoint: CheckpointProfile,
        weight: Float,
    ): LoRaAsset = withContext(Dispatchers.IO) {
        require(name.isNotBlank()) { "LoRA name is required" }
        require(triggerWords.any { it.isNotBlank() }) { "At least one trigger word is required" }
        require(weight.isFinite() && weight in -2f..2f) { "Invalid LoRA weight" }
        app.contentResolver.takePersistableUriPermission(
            uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = ByteArray(128 * 1024)
        val stream = requireNotNull(app.contentResolver.openInputStream(uri)) { "Cannot open LoRA file" }
        stream.use { input ->
            while (true) {
                val size = input.read(bytes)
                if (size < 0) break
                if (size > 0) digest.update(bytes, 0, size)
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        val item = LoRaAsset(
            id = "lora_${hash.take(20)}",
            displayName = name.trim(),
            uri = uri.toString(),
            sha256 = hash,
            modelFamily = checkpoint.baseModelFamily,
            triggerWords = triggerWords.map(String::trim).filter(String::isNotBlank).distinct(),
            defaultWeight = weight,
            allowedCheckpointIds = setOf(checkpoint.id),
        )
        val previous = list()
        // Exact-content re-import changes metadata without adding duplicates.
        persist(previous.filterNot { it.sha256 == hash } + item)
        item
    }

    fun remove(id: String) {
        persist(list().filterNot { it.id == id })
    }

    private fun persist(items: List<LoRaAsset>) {
        val array = JSONArray()
        items.forEach { entry ->
            array.put(JSONObject().apply {
                put("id", entry.id)
                put("name", entry.displayName)
                put("uri", entry.uri)
                put("sha256", entry.sha256)
                put("family", entry.modelFamily)
                put("triggers", JSONArray(entry.triggerWords))
                put("checkpoints", JSONArray(entry.allowedCheckpointIds.sorted()))
                put("weight", entry.defaultWeight.toDouble())
                put("enabled", entry.enabled)
            })
        }
        val target = JSONObject().put("version", 1).put("assets", array).toString(2)
        val temp = File(catalog.parentFile, catalog.name + ".tmp")
        temp.writeText(target)
        if (!temp.renameTo(catalog)) {
            temp.delete()
            error("Unable to replace the LoRA catalog without losing its previous version.")
        }
    }
}
