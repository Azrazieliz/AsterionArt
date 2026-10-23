package com.asterion.art.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CancellationException

data class VideoClip(val file: File, val runId: String, val model: String)

/**
 * True Nightmare Wan/Neodragon inference via authenticated local video API.
 * Interim separate-process backend; engine must be ported for an embedded APK.
 */
class NightmareVideoBridge(private val ctx: Context, private val token: String) {
    private val base = "http://127.0.0.1:8820"
    private fun connection(path: String, method: String = "GET"): HttpURLConnection {
        require(path.startsWith("/") && !path.contains("..") && !path.contains("://"))
        return (URL(base+path).openConnection() as HttpURLConnection).apply {
            requestMethod=method
            connectTimeout=8_000
            readTimeout=1_200_000
            setRequestProperty("Authorization","Bearer "+token)
        }
    }
    private fun json(path: String, payload: JSONObject?=null): JSONObject {
        val c=connection(path,if(payload==null)"GET" else "POST")
        try {
            if(payload!=null) {
                c.doOutput=true
                c.setRequestProperty("Content-Type","application/json; charset=utf-8")
                c.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }
            if(c.responseCode !in 200..299) {
                val error=c.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("Nightmare API "+path+": HTTP "+c.responseCode+" "+error.take(350))
            }
            return JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } catch (e: java.net.ConnectException) {
            error("Nightmare local video API is unavailable; start it on the phone.")
        } finally { c.disconnect() }
    }
    fun cancel() {
        json("/cancel",JSONObject())
    }

    fun available(): Boolean {
        json("/info")
        val content=json("/models").toString()
        return content.contains("wan",true) || content.contains("neodragon",true)
    }
    fun generate(first: Uri, prompt: String, seed: Long, seconds: Int,
                 target: File, cancelled: () -> Boolean): VideoClip {
        require(seconds in 1..5 && prompt.isNotBlank())
        if(cancelled()) throw CancellationException()
        val raw=ctx.contentResolver.openInputStream(first)?.use { it.readBytes() }
            ?: error("First keyframe unavailable")
        require(raw.size in 128..20_000_000)
        val bitmap=BitmapFactory.decodeByteArray(raw,0,raw.size) ?: error("Invalid keyframe")
        val img=ByteArrayOutputStream().use { out ->
            try {
                check(bitmap.compress(Bitmap.CompressFormat.PNG,100,out))
                Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP)
            } finally { bitmap.recycle() }
        }
        val request=JSONObject().apply {
            put("flow","i2v")
            put("prompt",prompt)
            put("seed",seed)
            put("images",JSONObject().put("image",img))
            put("params",JSONObject().put("video",JSONObject()
                .put("seconds",seconds.toString())))
        }
        val completed=json("/run",request)
        if(cancelled()) throw CancellationException()
        val items=completed.optJSONArray("results") ?: JSONArray()
        val video=(0 until items.length()).mapNotNull { items.optJSONObject(it) }
            .firstOrNull { it.optString("kind")=="video" }
            ?: error("No video returned by Nightmare's inference backend")
        val path=video.getString("url")
        require(path.startsWith("/results/") && !path.contains(".."))
        val conn=connection(path)
        try {
            check(conn.responseCode==200) { "Video download failed" }
            check(conn.contentType?.substringBefore(';')=="video/mp4") { "Result is not MP4" }
            target.parentFile?.mkdirs()
            val temp=File(target.absolutePath+".tmp")
            try {
                conn.inputStream.use { input ->
                    temp.outputStream().use { output ->
                        val buffer=ByteArray(512*1024)
                        while(true) {
                            if(cancelled())throw CancellationException()
                            val n=input.read(buffer)
                            if(n<0) break
                            if(n>0) output.write(buffer,0,n)
                        }
                    }
                }
                check(temp.length()>1024) { "Empty MP4" }
                check(temp.renameTo(target)) { "Unable to save MP4" }
            } catch(t:Throwable) { temp.delete(); throw t }
        } finally { conn.disconnect() }
        return VideoClip(target,completed.optString("run","unknown"),
            video.optString("model","nightmare-video"))
    }
}
