package com.asterion.compiler.art

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class NightmareResult(
    val runId: String,
    val kind: String,
    val file: File,
    val width: Int,
    val height: Int,
)

/**
 * Real Nightmare Mobile API client, no screen taps. The endpoint is loopback,
 * and every request apart from GET /info includes the user's API bearer token.
 * The other app has to be installed and its Settings -> API enabled.
 */
class NightmareBridge(
    private val resolver: ContentResolver,
    private val directory: File,
    private val token: String,
) {
    private val root = "http://127.0.0.1:8820"
    init {
        require(token.isNotBlank()) { "Configure the Nightmare API token in Art settings." }
    }

    suspend fun getInfo(): JSONObject = withContext(Dispatchers.IO) { request("GET", "/info") }
    suspend fun availableModels(): JSONObject = withContext(Dispatchers.IO) { request("GET", "/models") }

    private fun request(method: String, path: String, body: JSONObject? = null): JSONObject {
        require(path.startsWith("/") && !path.contains(".."))
        val connection = URL(root + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 4_000
            connection.readTimeout = 60 * 60 * 1_000
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("Nightmare HTTP $status: ${text.take(400)}")
            return JSONObject(text)
        } finally { connection.disconnect() }
    }

    private fun download(relative: String, runId: String): File {
        require(relative.startsWith("/results/"))
        val pieces = relative.removePrefix("/results/").split('/')
        require(pieces.size == 2 && pieces[0] == runId)
        require(Regex("[A-Za-z0-9_-]+").matches(runId))
        require(Regex("[A-Za-z0-9_.-]+").matches(pieces[1]) && ".." !in pieces[1])
        val outputFolder = File(directory, "results/$runId").apply { mkdirs() }
        val destination = File(outputFolder, pieces[1])
        val temp = File(outputFolder, pieces[1] + ".part")
        val connection = URL(root + relative).openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 4_000
            connection.readTimeout = 60_000
            if (connection.responseCode != 200) error("Nightmare result could not be downloaded")
            connection.inputStream.use { source ->
                temp.outputStream().use { sink -> source.copyTo(sink, bufferSize = 256 * 1024) }
            }
            require(temp.length() > 0L) { "Downloaded result is empty" }
            check(temp.renameTo(destination)) { "Could not finalize result" }
            return destination
        } finally { connection.disconnect(); temp.delete() }
    }

    /** Auto-discovers recipe and image node from the running, installed backend. */
    suspend fun render(
        prompt: String,
        seed: Long,
        image: Uri? = null,
        requireVideo: Boolean = false,
    ): NightmareResult = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank())
        val info = request("GET", "/info")
        check(info.optString("app") == "nightmare") { "Not connected to Nightmare Mobile" }
        val flows = request("GET", "/flows").getJSONArray("recipes")
        val targetLabel = if (requireVideo) "Image to video" else "Text to image"
        var flowId: String? = null
        for (i in 0 until flows.length()) {
            val option = flows.getJSONObject(i)
            if (option.optString("label").equals(targetLabel, ignoreCase = true)
                && option.optBoolean("runs_here", false)) {
                flowId = option.getString("id")
                break
            }
        }
        val flow = flowId ?: error("Nightmare has no supported $targetLabel recipe for this device")
        val payload = JSONObject().put("flow", flow)
            .put("prompt", prompt)
            .put("seed", seed.toString())
            .put("stream", false)
        if (image != null) {
            val schema = request("GET", "/flows/$flow").getJSONArray("nodes")
            val nodes = mutableListOf<String>()
            for (i in 0 until schema.length()) {
                val node = schema.getJSONObject(i)
                if (node.optString("type") == "core.image") nodes += node.getString("id")
            }
            check(nodes.size == 1) { "Image-to-video flow must have exactly one image input, found ${nodes.size}" }
            val bytes = resolver.openInputStream(image)?.use { source ->
                // Bound the base64 upload: a full-resolution worksheet is not a video frame.
                source.readNBytes(20 * 1024 * 1024 + 1)
            } ?: error("Cannot read selected frame")
            require(bytes.size <= 20 * 1024 * 1024) { "Animation frame exceeds 20 MB; reduce its size" }
            payload.put("images", JSONObject().put(nodes.single(), Base64.encodeToString(bytes, Base64.NO_WRAP)))
        }
        val result = request("POST", "/run", payload)
        if (result.has("error")) error(result.getString("error"))
        val run = result.getString("run")
        val files = result.optJSONArray("results") ?: error("Nightmare returned no generated media")
        val expectedKind = if (requireVideo) "video" else "image"
        val entry = (0 until files.length()).map { files.getJSONObject(it) }
            .firstOrNull { it.optString("kind") == expectedKind }
            ?: error("Nightmare did not return a $expectedKind result")
        val downloaded = download(entry.getString("url"), run)
        NightmareResult(run, expectedKind, downloaded, entry.optInt("w"), entry.optInt("h"))
    }

    suspend fun cancel() = withContext(Dispatchers.IO) { request("POST", "/cancel", JSONObject()) }

    companion object {
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(256 * 1024)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (count > 0) digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
