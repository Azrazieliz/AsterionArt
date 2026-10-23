package com.asterion.art.engine

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CancellationException
import kotlin.math.max

/** Local-only compatibility layer; a native embedded engine will implement the same interface. */
interface StillImageEngine {
    fun installedModels(): List<DreamModel>
    fun prepare(model: DreamModel, width: Int, height: Int, cancelled: () -> Boolean)
    fun verifyText(prompt: String): TokenCapacity
    fun generate(
        pass: GenerationPass,
        settings: com.asterion.art.model.GeneratorSettings,
        width: Int,
        height: Int,
        previousPng: ByteArray?,
        seed: Long,
        cancelled: () -> Boolean,
        progress: (Int, Int) -> Unit,
    ): GeneratedImage
}

data class DreamModel(
    val id: String,
    val name: String,
    val generationSize: Int,
    val isAnima: Boolean,
    val isSdxl: Boolean,
    val isCustom: Boolean,
    val resolutions: List<Pair<Int, Int>>,
)
data class TokenCapacity(val count: Int, val limit: Int, val overflowOffset: Int) {
    val fits: Boolean get() = (limit <= 0 || count <= limit) && overflowOffset < 0
}
data class GeneratedImage(val png: ByteArray, val seed: Long, val timeMs: Long)
class BackendUnavailable(message: String): IllegalStateException(message)

/**
 * Talks ONLY to the existing on-device Local Dream host over loopback.
 * For a fully fused single APK a native provider must replace this adapter.
 */
class LocalDreamBridge : StillImageEngine {
    private val control = "http://127.0.0.1:8808"
    private val generation = "http://127.0.0.1:8081"

    private fun call(base: String, path: String, body: JSONObject? = null, timeout: Int = 15_000): JSONObject {
        val c = URL(base + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = if (body == null) "GET" else "POST"
            c.connectTimeout = 4_000
            c.readTimeout = timeout
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            if (c.responseCode !in 200..299) {
                val errorText = c.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw BackendUnavailable(path + " returned " + c.responseCode + ": " + errorText.take(400))
            }
            return JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } catch (e: java.net.ConnectException) {
            throw BackendUnavailable("On-device Local Dream host is not running. " +
                "Activate the compatible host backend or install the embedded QNN engine.")
        } finally {
            c.disconnect()
        }
    }

    override fun installedModels(): List<DreamModel> {
        val identity = call(control, "/info")
        if (identity.optString("app") != "localdream")
            throw BackendUnavailable("Unrecognized backend on Local Dream control port")
        val catalog = call(control, "/models")
        val models = catalog.optJSONArray("models") ?: JSONArray()
        return (0 until models.length()).mapNotNull { index ->
            val entry = models.optJSONObject(index) ?: return@mapNotNull null
            val id = entry.optString("id")
            if (id.isBlank()) return@mapNotNull null
            val resolutions = entry.optJSONArray("resolutions") ?: JSONArray()
            DreamModel(
                id = id,
                name = entry.optString("name", id),
                generationSize = entry.optInt("generation_size", 512),
                isAnima = entry.optBoolean("is_anima", false),
                isSdxl = entry.optBoolean("is_sdxl", false),
                isCustom = entry.optBoolean("is_custom", false),
                resolutions = (0 until resolutions.length()).mapNotNull { i ->
                    val p = resolutions.optJSONArray(i) ?: return@mapNotNull null
                    if (p.optInt(0) > 0 && p.optInt(1) > 0)
                        p.optInt(0) to p.optInt(1) else null
                }
            )
        }
    }

    override fun prepare(model: DreamModel, width: Int, height: Int, cancelled: () -> Boolean) {
        val request = JSONObject().put("model_id", model.id)
            .put("width", width).put("height", height)
        call(control, "/select", request)
        val deadline = System.nanoTime() + 240_000_000_000L
        while (System.nanoTime() < deadline) {
            if (cancelled()) throw CancellationException("User cancelled generation")
            val state = call(control, "/status")
            if (state.optString("state") == "error") {
                throw BackendUnavailable("Local Dream failed loading " + model.id +
                    ": " + state.optString("message"))
            }
            if (state.optString("state") == "running" &&
                state.optString("serving_model_id") == model.id &&
                state.optInt("width") == width && state.optInt("height") == height) return
            Thread.sleep(1_000L)
        }
        throw BackendUnavailable("Timed out loading exact model " + model.id + " " + width + "x" + height)
    }

    override fun verifyText(prompt: String): TokenCapacity {
        val result = call(generation, "/tokenize", JSONObject().put("prompt", prompt))
        val report = TokenCapacity(
            result.getInt("count"),
            result.optInt("max_length", 0),
            result.optInt("overflow_offset", -1),
        )
        if (!report.fits) {
            throw BackendUnavailable(
                "Prompt exceeds the active NPU context: " + report.count +
                    " tokens; maximum " + report.limit +
                    ". Source details were not silently dropped."
            )
        }
        return report
    }

    override fun generate(
        pass: GenerationPass,
        settings: com.asterion.art.model.GeneratorSettings,
        width: Int,
        height: Int,
        previousPng: ByteArray?,
        seed: Long,
        cancelled: () -> Boolean,
        progress: (Int, Int) -> Unit,
    ): GeneratedImage {
        if (cancelled()) throw CancellationException()
        val json = JSONObject().apply {
            put("prompt", pass.positive)
            put("negative_prompt", pass.negative)
            put("steps", settings.steps)
            put("cfg", settings.cfg.toDouble())
            put("scheduler", when (settings.scheduler) {
                "DPM++ 2M SDE" -> if (settings.karras) "dpm_sde_karras" else "dpm_sde"
                "Euler" -> if (settings.karras) "euler_karras" else "euler"
                "Euler a" -> if (settings.karras) "euler_a_karras" else "euler_a"
                else -> if (settings.karras) "dpm_karras" else "dpm"
            })
            put("seed", seed)
            put("width", width)
            put("height", height)
            put("aspect_ratio", settings.ratio)
            put("output_format", "png")
            put("show_diffusion_process", false)
            if (previousPng != null) {
                put("image", Base64.encodeToString(previousPng, Base64.NO_WRAP))
                put("denoise_strength", pass.denoise.toDouble())
            }
        }
        val conn = URL(generation + "/generate").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8_000
            conn.readTimeout = 900_000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "text/event-stream")
            conn.doOutput = true
            conn.outputStream.buffered().use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw BackendUnavailable("Generation request failed: " + err.take(800))
            }
            var event = ""
            val data = StringBuilder()
            conn.inputStream.bufferedReader().use { lines ->
                while (true) {
                    if (cancelled()) throw CancellationException("User cancelled generation")
                    val line = lines.readLine() ?: break
                    if (line.startsWith("event:")) event = line.substringAfter(':').trim()
                    else if (line.startsWith("data:")) data.append(line.substringAfter(':').trim())
                    else if (line.isEmpty() && data.isNotEmpty()) {
                        val message = JSONObject(data.toString())
                        when (event) {
                            "progress" -> progress(
                                message.optInt("step", 0),
                                message.optInt("total_steps", max(1, settings.steps))
                            )
                            "error" -> throw BackendUnavailable(message.optString("message", "Native inference error"))
                            "complete" -> {
                                if (message.optString("format") != "png")
                                    throw BackendUnavailable("Generation returned an unexpected format")
                                val bytes = Base64.decode(message.getString("image"), Base64.DEFAULT)
                                require(bytes.size > 100) { "Native engine returned empty image" }
                                return GeneratedImage(bytes,
                                    message.optLong("seed", seed),
                                    message.optLong("generation_time_ms", 0L))
                            }
                        }
                        data.setLength(0)
                        event = ""
                    }
                }
            }
            throw BackendUnavailable("Native image generation stream ended without a result")
        } catch (e: java.net.ConnectException) {
            throw BackendUnavailable("On-device generator port unavailable")
        } finally { conn.disconnect() }
    }
}
