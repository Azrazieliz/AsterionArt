package com.asterion.compiler.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RuntimeTokenReport(
    val count: Int,
    val maxLength: Int,
    val overflowOffset: Int,
) {
    val fits: Boolean get() = maxLength == 0 || (count <= maxLength && overflowOffset < 0)
}

/**
 * Queries the exact tokenizer of the running Local Dream backend. This is
 * different from the compiler's approximate, word-based authoring budget.
 * Loopback only; it must never expose an unauthenticated generation port to LAN.
 *
 * A token response cannot prove which checkpoint Local Dream has loaded;
 * the orchestrator must validate model identity before actual generation.
 */
class LocalDreamTokenGateway(
    private val endpoint: String = "http://127.0.0.1:8081",
) {
    init {
        require(endpoint == "http://127.0.0.1:8081" || endpoint == "http://localhost:8081") {
            "The standalone compiler may only connect to Local Dream over device loopback."
        }
    }

    suspend fun inspect(prompt: String): RuntimeTokenReport = withContext(Dispatchers.IO) {
        val connection = (URL("$endpoint/tokenize").openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 2000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val body = JSONObject().put("prompt", prompt).toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(body) }
            if (connection.responseCode != 200) {
                throw IllegalStateException("Local Dream tokenizer returned HTTP ${connection.responseCode}")
            }
            val result = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            RuntimeTokenReport(
                count = result.getInt("count"),
                maxLength = result.getInt("max_length"),
                overflowOffset = result.optInt("overflow_offset", -1),
            )
        } finally {
            connection.disconnect()
        }
    }
}
