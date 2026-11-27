package com.asterion.art.integrations

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Read-only Asterion v1 partner endpoint for Asterion Control.
 *
 * Both sides require owner approval: Art's local diagnostics switch must be on,
 * and Control must locally approve this installed package/signing identity.
 * No generation, editing, model loading, file mutation, image bytes or secrets
 * are exposed through this provider.
 */
class AsterionArtDiagnosticsProvider : ContentProvider() {
    companion object {
        private const val PREFS = "asterion_art_control_bridge"
        private const val ENABLED = "diagnostics_enabled"
        private const val CONTROL_PACKAGE = "com.asterion.control"
        private const val PROTOCOL_VERSION = 1
        private val REQUEST_ID = Regex("[A-Za-z0-9_.:-]{8,100}")
        private val ACTIONS = setOf(
            "diagnostics.snapshot",
            "diagnostics.job",
            "diagnostics.read"
        )

        fun isEnabled(context: Context): Boolean =
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(ENABLED, false)

        fun setEnabled(context: Context, enabled: Boolean) {
            check(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(ENABLED, enabled).commit()) {
                "Could not save Asterion Control diagnostics setting"
            }
        }
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = null
    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Read-only diagnostics provider")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Read-only diagnostics provider")
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("Read-only diagnostics provider")

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val app = context?.applicationContext ?: error("Art application context unavailable")
        authorize(app)
        val result = when (method) {
            "asterion.v1.capabilities" -> capabilities()
            "asterion.v1.invoke" -> invoke(app, extras)
            "asterion.v1.status" -> JSONObject()
                .put("ok", true).put("status", "completed")
                .put("request_id", arg ?: "").put("read_only", true)
            "asterion.v1.cancel" -> JSONObject()
                .put("ok", true).put("status", "not_cancellable")
                .put("request_id", arg ?: "").put("read_only", true)
            else -> throw IllegalArgumentException("Unsupported Asterion partner method")
        }
        return Bundle().apply { putString("result_json", result.toString()) }
    }

    private fun authorize(app: Context) {
        val callerUid = Binder.getCallingUid()
        val packages = app.packageManager.getPackagesForUid(callerUid).orEmpty()
        if (CONTROL_PACKAGE !in packages) {
            throw SecurityException("Only the installed Asterion Control package may read Art diagnostics")
        }
        if (!isEnabled(app)) {
            throw SecurityException("Asterion Art diagnostics sharing is disabled by the owner")
        }
    }

    private fun capabilities(): JSONObject = JSONObject()
        .put("pack_version", 1)
        .put("actions", JSONArray().apply {
            put(JSONObject().put("name", "diagnostics.snapshot").put("intent", "inspect"))
            put(JSONObject().put("name", "diagnostics.job").put("intent", "inspect"))
            put(JSONObject().put("name", "diagnostics.read").put("intent", "inspect"))
        })

    private fun invoke(app: Context, extras: Bundle?): JSONObject {
        require(extras?.getInt("protocol_version", 0) == PROTOCOL_VERSION) {
            "Unsupported Asterion partner protocol version"
        }
        val requestId = extras?.getString("request_id").orEmpty()
        require(REQUEST_ID.matches(requestId)) { "Invalid diagnostic request ID" }
        val action = extras?.getString("action").orEmpty()
        require(action in ACTIONS) { "Unsupported or mutating Art action" }
        val payloadText = extras?.getString("payload_json").orEmpty()
        require(payloadText.length <= 4096) { "Diagnostic request payload too large" }
        val payload = if (payloadText.isBlank()) JSONObject() else JSONObject(payloadText)
        val data = when (action) {
            "diagnostics.snapshot" -> snapshot(app)
            "diagnostics.job" -> job(app, payload)
            "diagnostics.read" -> readArtifact(app, payload)
            else -> error("Unsupported diagnostic action")
        }
        return JSONObject().put("ok", true).put("status", "completed")
            .put("request_id", requestId).put("read_only", true)
            .put("action", action).put("data", data)
    }

    private fun snapshot(app: Context): JSONObject {
        val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
        val prefs = app.getSharedPreferences("asterion_art_catalog_v1", Context.MODE_PRIVATE)
        val rawTasks = runCatching { JSONArray(prefs.getString("tasks", "[]")) }
            .getOrDefault(JSONArray())
        val tasks = JSONArray()
        val start = (rawTasks.length() - 12).coerceAtLeast(0)
        for (index in start until rawTasks.length()) {
            val item = rawTasks.optJSONObject(index) ?: continue
            tasks.put(JSONObject()
                .put("id", item.optString("id"))
                .put("target_id", item.optString("targetId"))
                .put("kind", item.optString("kind"))
                .put("status", item.optString("status"))
                .put("created_at", item.optLong("createdAt"))
                .put("stage", item.optString("stage"))
                .put("progress_current", item.optInt("progressCurrent"))
                .put("progress_total", item.optInt("progressTotal"))
                .put("error", item.optString("error").take(1200)))
        }
        val plans = File(app.filesDir, "generation-plans")
        val planFiles = plans.listFiles()?.filter {
            it.isFile && it.name.endsWith(".json") && !it.name.endsWith("-brain.json") &&
                !it.name.endsWith("-capsules.json")
        }.orEmpty().sortedByDescending { it.lastModified() }.take(12)
        val jobs = JSONArray().apply {
            planFiles.forEach { file ->
                val parsed = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return@forEach
                put(JSONObject().put("job_id", parsed.optString("id", file.name.removeSuffix(".json")))
                    .put("mode", parsed.optString("mode"))
                    .put("creation_mode", parsed.optString("creation_mode"))
                    .put("created_at", parsed.optLong("created"))
                    .put("plan_available", true))
            }
        }
        val versionCode = if (Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode
            else @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
        return JSONObject()
            .put("schema", "asterion.art.diagnostics.v1")
            .put("app", "Asterion Art")
            .put("package", app.packageName)
            .put("version_name", packageInfo.versionName ?: "")
            .put("version_code", versionCode)
            .put("android_release", Build.VERSION.RELEASE ?: "")
            .put("sdk", Build.VERSION.SDK_INT)
            .put("device", Build.MODEL ?: "")
            .put("sharing_enabled", true)
            .put("tasks", tasks)
            .put("recent_generation_jobs", jobs)
            .put("generation_plan_count", planFiles.size)
            .put("diagnostic_artifacts_are_app_private", true)
            .put("image_bytes_exported", false)
            .put("secrets_exported", false)
    }

    private fun job(app: Context, payload: JSONObject): JSONObject {
        val id = payload.optString("job_id")
        require(ArtDiagnosticsProtocol.validJobId(id)) { "Invalid generation job ID" }
        val planFile = File(app.filesDir, "generation-plans/$id.json").canonicalFile
        val plansRoot = File(app.filesDir, "generation-plans").canonicalFile
        require(planFile.path.startsWith(plansRoot.path + File.separator) && planFile.isFile) {
            "Saved generation job not found"
        }
        val plan = JSONObject(planFile.readText())
        val artifacts = JSONArray()
        for (name in listOf(
            "native-prompt-trace", "brain-plan", "native-stage-inputs",
            "last-failure", "model-load-ms", "character-capsules"
        )) {
            val file = ArtDiagnosticsProtocol.artifactFile(app.filesDir, id, name)
            if (file.isFile) artifacts.put(JSONObject()
                .put("name", name).put("bytes", file.length())
                .put("modified_at", file.lastModified()))
        }
        val direct = plan.optJSONObject("direct_reference_images") ?: JSONObject()
        val referenceCounts = JSONObject()
        direct.keys().forEach { role ->
            referenceCounts.put(role, direct.optJSONArray(role)?.length() ?: 0)
        }
        val brainFile = ArtDiagnosticsProtocol.artifactFile(app.filesDir, id, "brain-plan")
        val brain = runCatching { if (brainFile.isFile) JSONObject(brainFile.readText()) else null }
            .getOrNull()
        val traceFile = ArtDiagnosticsProtocol.artifactFile(app.filesDir, id, "native-prompt-trace")
        val trace = runCatching { if (traceFile.isFile) JSONObject(traceFile.readText()) else null }
            .getOrNull()
        return JSONObject()
            .put("job_id", id)
            .put("mode", plan.optString("mode"))
            .put("creation_mode", plan.optString("creation_mode"))
            .put("created_at", plan.optLong("created"))
            .put("concepts", plan.optJSONArray("concepts") ?: JSONArray())
            .put("selected_sheet_count", plan.optJSONArray("sheets")?.length() ?: 0)
            .put("reference_counts_by_role", referenceCounts)
            .put("user_scene_prompt", plan.optString("user_scene_prompt").take(2400))
            .put("user_negative_prompt", plan.optString("user_negative_prompt").take(1600))
            .put("design_prompt", plan.optString("design_prompt").take(2400))
            .put("design_negative", plan.optString("design_negative").take(1600))
            .put("settings", plan.optJSONObject("settings") ?: JSONObject())
            .put("image_model_id", trace?.optString("model_id") ?: JSONObject.NULL)
            .put("prompt_model_id", brain?.optString("model") ?: JSONObject.NULL)
            .put("prompt_domain", brain?.optString("domain") ?: JSONObject.NULL)
            .put("prompt_context_tokens", brain?.optInt("context_tokens") ?: 0)
            .put("prompt_authority_chars", brain?.optInt("authority_chars") ?: 0)
            .put("prompt_positive_chars", brain?.optString("final_positive")?.length ?: 0)
            .put("prompt_negative_chars", brain?.optString("final_negative")?.length ?: 0)
            .put("visual_semantics_verified", brain?.optBoolean("visual_semantics_verified") ?: false)
            .put("native_stage_count", trace?.optJSONArray("native_stage_prompts")?.length() ?: 0)
            .put("compiled_model", trace?.optString("compiled_model") ?: JSONObject.NULL)
            .put("pre_render_elapsed_ms", trace?.optLong("pre_render_elapsed_ms") ?: 0)
            .put("diagnostic_artifacts", artifacts)
    }

    private fun readArtifact(app: Context, payload: JSONObject): JSONObject {
        val id = payload.optString("job_id")
        val artifact = payload.optString("artifact")
        require(ArtDiagnosticsProtocol.validJobId(id)) { "Invalid generation job ID" }
        require(ArtDiagnosticsProtocol.validArtifact(artifact)) { "Unsupported diagnostic artifact" }
        val offset = payload.optInt("offset", 0)
        val limit = payload.optInt("limit", 5000)
        val file = ArtDiagnosticsProtocol.artifactFile(app.filesDir, id, artifact)
        require(file.isFile) { "Requested diagnostic artifact is not present" }
        val page = ArtDiagnosticsProtocol.page(file.readText(), offset, limit)
        return JSONObject().put("job_id", id).put("artifact", artifact)
            .put("offset", page.offset).put("total_chars", page.totalChars)
            .put("next_offset", page.nextOffset ?: JSONObject.NULL)
            .put("text", page.text)
    }
}