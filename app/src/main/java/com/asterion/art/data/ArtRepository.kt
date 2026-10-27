package com.asterion.art.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.asterion.art.model.GeneratorSettings
import com.asterion.art.model.GradedAsset
import com.asterion.art.model.SheetRecord
import com.asterion.art.model.SheetRole
import com.asterion.art.model.CreationMode
import com.asterion.art.model.TaskKind
import com.asterion.art.model.TaskRecord
import com.asterion.art.model.TaskStatus
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Durable local metadata. Content stays in the system media provider, never duplicated in app prefs. */
class ArtRepository(context: Context) {
    // Multiple WorkManager workers construct separate repository instances.
    // @Synchronized on an instance did not protect their shared preference
    // snapshots, causing finished sheets, jobs and assets to be overwritten.
    companion object { private val CATALOG_LOCK = Any() }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("asterion_art_catalog_v1", Context.MODE_PRIVATE)

    var sheets by mutableStateOf<List<SheetRecord>>(emptyList())
        private set
    var tasks by mutableStateOf<List<TaskRecord>>(emptyList())
        private set
    var assets by mutableStateOf<List<GradedAsset>>(emptyList())
        private set
    var settings by mutableStateOf(GeneratorSettings())
        private set

    init { reload() }

    @Synchronized fun reload() {
        synchronized(CATALOG_LOCK) {
        sheets = parseList("sheets") { obj ->
            SheetRecord(
                id = obj.getString("id"),
                role = SheetRole.valueOf(obj.getString("role")),
                label = obj.getString("label"),
                sourceUris = obj.getJSONArray("sources").toStringList(),
                referenceSlots = obj.optJSONArray("slots")?.toStringList().orEmpty(),
                createdAt = obj.getLong("createdAt"),
                outputUri = obj.optNullable("outputUri"),
                characterId = obj.optNullable("characterId"),
                formId = obj.optNullable("formId"),
                structuredDataVersion = obj.optInt("revision", 1),
                originatingOriginal = obj.optString("originatingOriginal","")
                    .takeIf{it.isNotEmpty()}?.let{CreationMode.valueOf(it)}
            )
        }
        tasks = parseList("tasks") { obj ->
            TaskRecord(
                id = obj.getString("id"),
                targetId = obj.getString("targetId"),
                kind = TaskKind.valueOf(obj.getString("kind")),
                createdAt = obj.getLong("createdAt"),
                status = TaskStatus.valueOf(obj.getString("status")),
                error = obj.optNullable("error"),
                progressCurrent = obj.optInt("progressCurrent", 0),
                progressTotal = obj.optInt("progressTotal", 0),
                progressBytes = obj.optLong("progressBytes", 0L),
                progressBytesTotal = obj.optLong("progressBytesTotal", 0L),
                stage = obj.optNullable("stage")
            )
        }
        assets = parseList("assets") { obj ->
            GradedAsset(
                uri = obj.getString("uri"),
                stars = obj.optInt("stars", 0),
                sourceSheetId = obj.optNullable("sourceSheetId"),
                autoScore = if (!obj.has("autoScore") || obj.isNull("autoScore")) null else obj.getDouble("autoScore"),
                sourceJobId = obj.optNullable("sourceJobId"),
                createdAt = obj.getLong("createdAt")
            )
        }
        val rawSettings = prefs.getString("settings", null)
        settings = if (rawSettings == null) GeneratorSettings() else runCatching {
            JSONObject(rawSettings).let {
                GeneratorSettings(
                    ratio = it.optString("ratio", "2:3"),
                    steps = it.optInt("steps", 20),
                    cfg = it.optDouble("cfg", 4.0).toFloat(),
                    batch = it.optInt("batch", 3),
                    denoise = it.optDouble("denoise", 0.45).toFloat(),
                    scheduler = it.optString("scheduler", "DPM++ 2M"),
                    karras = it.optBoolean("karras", false),
                    seed = it.optNullable("seed")?.toLongOrNull(),
                    automatic = it.optBoolean("automatic", true)
                )
            }
        }.getOrDefault(GeneratorSettings())
    
        }
    }

    @Synchronized fun addSheet(role: SheetRole, sourceUris: List<String>,
        referenceSlots: List<String>, originatingOriginal:CreationMode?=null): SheetRecord {
        synchronized(CATALOG_LOCK) {
            reload() // Refresh the current persisted snapshot under the global lock.
        require(sourceUris.isNotEmpty()) { "A sheet needs at least one actual reference." }
        require(sourceUris.size == referenceSlots.size) { "Every reference must have an explicit panel assignment." }
        require(referenceSlots.contains("PRIMARY")) { "A primary reference is required." }
        require(referenceSlots.all { slot -> com.asterion.art.model.SheetLayouts.isValid(role, slot) }) {
            "Reference panel not permitted for selected sheet kind." }
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val sheet = SheetRecord(role = role, label = role.display + " · " + date,
            sourceUris = sourceUris, referenceSlots = referenceSlots,
            originatingOriginal = originatingOriginal)
        val updated = sheets + sheet
        check(prefs.edit().putString("sheets", encodeSheets(updated)).commit())
        sheets = updated
        return sheet
    
        }
    }

    /** Reuses Core's finished reference unchanged; no nested sheet and no lossy recompression. */
    @Synchronized fun importCoreCharacter(
        characterId: String, sheetAssetId: String, name: String, mediaUri: String,
        verifiedSha256: String
    ): SheetRecord {
        synchronized(CATALOG_LOCK) {
            reload() // Refresh the current persisted snapshot under the global lock.
        require(characterId.isNotBlank() && sheetAssetId.isNotBlank())
        require(mediaUri.startsWith("content://com.ailm.android.asterionart.sheets/image/"))
        val existing = sheets.firstOrNull { it.sourceUris.contains(mediaUri) }
        if (existing != null) return existing
        val record = SheetRecord(
            role = SheetRole.CHARACTER,
            label = name,
            sourceUris = listOf(mediaUri),
            referenceSlots = listOf("PRIMARY"),
            outputUri = mediaUri,
            characterId = characterId,
            formId = characterId,
            structuredDataVersion = 1
        )
        val updated = sheets + record
        check(prefs.edit().putString("sheets",encodeSheets(updated)).commit())
        sheets = updated
        return record
    
        }
    }

    @Synchronized fun recordTask(targetId: String, kind: TaskKind): TaskRecord {
        synchronized(CATALOG_LOCK) {
            reload() // Refresh the current persisted snapshot under the global lock.
        val task = TaskRecord(targetId = targetId, kind = kind)
        val updated = tasks + task
        check(prefs.edit().putString("tasks", encodeTasks(updated)).commit())
        tasks = updated
        return task
    
        }
    }

    @Synchronized fun updateTask(id: String, status: TaskStatus, error: String? = null) {
        synchronized(CATALOG_LOCK) {
        val current = readPersistedTasks()
        val updated = current.map {
            if (it.id == id && it.status in
                setOf(TaskStatus.QUEUED,TaskStatus.RUNNING))
                it.copy(status = status, error = error,
                progressCurrent = if (status == TaskStatus.COMPLETED && it.progressTotal > 0)
                    it.progressTotal else it.progressCurrent) else it
        }
        check(prefs.edit().putString("tasks", encodeTasks(updated)).commit())
        tasks = updated
    
        }
    }

    @Synchronized fun updateTaskProgress(id: String, current: Int, total: Int,
                                         stage: String, bytes: Long = 0L,
                                         bytesTotal: Long = 0L) {
        synchronized(CATALOG_LOCK) {
        require(current >= 0 && total >= 0 && bytes >= 0 && bytesTotal >= 0)
        val updated = readPersistedTasks().map { task ->
            if (task.id == id && task.status in
                setOf(TaskStatus.QUEUED,TaskStatus.RUNNING)) {
                task.copy(progressCurrent = current.coerceAtMost(total.takeIf { it > 0 } ?: current),
                    progressTotal = total, progressBytes = bytes,
                    progressBytesTotal = bytesTotal, stage = stage)
            } else task
        }
        check(prefs.edit().putString("tasks", encodeTasks(updated)).commit())
        tasks = updated
    
        }
    }

    private fun readPersistedTasks(): List<TaskRecord> = parseList("tasks") { obj ->
        TaskRecord(
            id = obj.getString("id"),
            targetId = obj.getString("targetId"),
            kind = TaskKind.valueOf(obj.getString("kind")),
            createdAt = obj.getLong("createdAt"),
            status = TaskStatus.valueOf(obj.getString("status")),
            error = obj.optNullable("error"),
            progressCurrent = obj.optInt("progressCurrent", 0),
            progressTotal = obj.optInt("progressTotal", 0),
            progressBytes = obj.optLong("progressBytes", 0L),
            progressBytesTotal = obj.optLong("progressBytesTotal", 0L),
            stage = obj.optNullable("stage")
        )
    }

    @Synchronized fun finishSheet(id: String, outputUri: String) {
        synchronized(CATALOG_LOCK) {
            reload() // Refresh the current persisted snapshot under the global lock.
        val updated = sheets.map { if (it.id == id) it.copy(outputUri = outputUri) else it }
        check(prefs.edit().putString("sheets", encodeSheets(updated)).commit())
        sheets = updated
        registerAsset(outputUri, id)
    
        }
    }

    @Synchronized fun registerAsset(uri: String, sourceSheetId: String? = null,
        autoScore: Double? = null, sourceJobId: String? = null) {
        synchronized(CATALOG_LOCK) {
            reload() // Refresh the current persisted snapshot under the global lock.
        if (assets.any { it.uri == uri }) return
        val updated = assets + GradedAsset(uri = uri, sourceSheetId = sourceSheetId,
            autoScore = autoScore, sourceJobId = sourceJobId)
        check(prefs.edit().putString("assets", encodeAssets(updated)).commit())
        assets = updated
    
        }
    }

    @Synchronized fun setStars(uri: String, stars: Int) {
        synchronized(CATALOG_LOCK) {
            reload() // Refresh the current persisted snapshot under the global lock.
        val updated = assets.map {
            if (it.uri == uri) it.copy(stars = stars.coerceIn(0, 5)) else it
        }
        check(prefs.edit().putString("assets", encodeAssets(updated)).commit())
        assets = updated
    
        }
    }

    @Synchronized fun saveSettings(value: GeneratorSettings) {
        synchronized(CATALOG_LOCK) {
        val next = JSONObject().apply {
            put("ratio", value.ratio)
            put("steps", value.steps)
            put("cfg", value.cfg.toDouble())
            put("batch", value.batch)
            put("denoise", value.denoise.toDouble())
            put("scheduler", value.scheduler)
            put("karras", value.karras)
            put("automatic", value.automatic)
            if (value.seed != null) put("seed", value.seed)
        }
        check(prefs.edit().putString("settings", next.toString()).commit())
        settings = value
    
        }
    }

    private fun encodeSheets(items: List<SheetRecord>) = JSONArray().apply {
        items.forEach { sheet -> put(JSONObject().apply {
            put("id", sheet.id); put("role", sheet.role.name); put("label", sheet.label)
            put("sources", JSONArray(sheet.sourceUris)); put("slots", JSONArray(sheet.referenceSlots)); put("createdAt", sheet.createdAt)
            put("outputUri", sheet.outputUri); put("characterId", sheet.characterId)
            put("formId", sheet.formId); put("revision", sheet.structuredDataVersion)
            put("originatingOriginal",sheet.originatingOriginal?.name ?: "")
        }) }
    }.toString()

    private fun encodeTasks(items: List<TaskRecord>) = JSONArray().apply {
        items.forEach { task -> put(JSONObject().apply {
            put("id", task.id); put("targetId", task.targetId); put("kind", task.kind.name)
            put("createdAt", task.createdAt); put("status", task.status.name); put("error", task.error)
            put("progressCurrent", task.progressCurrent)
            put("progressTotal", task.progressTotal)
            put("progressBytes", task.progressBytes)
            put("progressBytesTotal", task.progressBytesTotal)
            put("stage", task.stage)
        }) }
    }.toString()

    private fun encodeAssets(items: List<GradedAsset>) = JSONArray().apply {
        items.forEach { asset -> put(JSONObject().apply {
            put("uri", asset.uri); put("stars", asset.stars); put("sourceSheetId", asset.sourceSheetId)
            put("sourceJobId", asset.sourceJobId)
            put("autoScore", asset.autoScore ?: JSONObject.NULL)
            put("createdAt", asset.createdAt)
        }) }
    }.toString()

    private fun <T> parseList(key: String, read: (JSONObject) -> T): List<T> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        (0 until array.length()).map { read(array.getJSONObject(it)) }
    }.getOrElse { emptyList() }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }

    private fun JSONObject.optNullable(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
