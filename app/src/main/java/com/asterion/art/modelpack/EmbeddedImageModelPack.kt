package com.asterion.art.modelpack

import android.content.Context
import com.asterion.art.nativeengine.NativeModelLibrary
import org.json.JSONObject
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.Enumeration

/**
 * Complete preconverted image-generation model, embedded at package-assembly
 * time. The phone only reads local signed-package assets. No network calls,
 * model-file picker, remote inference, or intermediate 4.85GB ZIP copy.
 */
class EmbeddedImageModelPack(context:Context) {
    private val app=context.applicationContext
    companion object {
        const val ROOT="asterion_image_bundle"
        const val IMAGE_ID="rin_flanime_v1_turbo_qnn"
        const val LABEL="Rin Flanime v1 Turbo (Anima QNN)"
        const val UPSTREAM_SHA256=
            "e99dc63e7f2d6c125a44dbf0a347c011395fc60adc9a796cc5d02f1ee9e75b8c"
        private const val CHUNK=64L*1024L*1024L
        private const val MIN_BYTES=4_000_000_000L
        private const val MAX_BYTES=6_000_000_000L
        private val SHA=Regex("[0-9a-f]{64}")
    }
    data class Part(val path:String,val size:Long,val sha256:String)
    data class Image(val bytes:Long,val parts:List<Part>)

    fun readManifest():Image {
        val json=app.assets.open("$ROOT/manifest.json").use { stream ->
            val bytes=stream.readBytes()
            require(bytes.size<=64*1024){"Image asset manifest is oversized"}
            JSONObject(bytes.toString(Charsets.UTF_8))
        }
        require(json.getString("schema")=="asterion.embedded-image-pack.v1")
        require(json.getLong("chunk_bytes")==CHUNK)
        require(json.getBoolean("offline_after_install"))
        require(!json.getBoolean("on_device_download_required"))
        val data=json.getJSONObject("model")
        require(data.getString("id")==IMAGE_ID)
        require(data.getString("label")==LABEL)
        require(data.getString("backend")=="anima")
        require(data.getString("filename")==
            "rin_flanime_v1_turbo_qnn2.28_8gen3.zip")
        require(data.getString("sha256").lowercase()==UPSTREAM_SHA256)
        val length=data.getLong("bytes")
        require(length in MIN_BYTES..MAX_BYTES)
        val array=data.getJSONArray("parts")
        require(array.length() in 1..100)
        val parts=(0 until array.length()).map { index ->
            val part=array.getJSONObject(index)
            val expected="$IMAGE_ID/part-"+index.toString().padStart(4,'0')+".chunk"
            require(part.getString("path")==expected)
            val bytes=part.getLong("bytes")
            val sha=part.getString("sha256").lowercase()
            require(bytes in 1L..CHUNK && SHA.matches(sha))
            Part(expected,bytes,sha)
        }
        require(parts.sumOf{it.size}==length){"Missing image-generation weight bytes"}
        // All full-size parts except the final chunk are exactly 64 MiB.
        require(parts.dropLast(1).all{it.size==CHUNK})
        return Image(length,parts)
    }

    fun packaged():Boolean=runCatching {
        val image=readManifest()
        val files=app.assets.list("$ROOT/$IMAGE_ID")?.toSet().orEmpty()
        image.parts.all { it.path.substringAfterLast('/') in files }
    }.getOrDefault(false)

    fun installed():Boolean=NativeModelLibrary(app).list().any { installed ->
        runCatching {
            val marker=java.io.File(
                java.io.File(installed.root.path).let {
                    // The metadata belongs to the installation directory,
                    // potentially one level above the tensor-root subfolder.
                    java.io.File(app.filesDir,"asterion-qnn-models/${installed.id}")
                },"asterion_model.json")
            JSONObject(marker.readText(Charsets.UTF_8))
                .optString("embedded_archive_sha256")==UPSTREAM_SHA256
        }.getOrDefault(false)
    }

    fun install(progress:(Long,Long)->Unit,cancelled:()->Boolean) {
        if(installed())return
        val manifest=readManifest()
        val pieces=object:Enumeration<InputStream> {
            private var position=0
            override fun hasMoreElements():Boolean=position<manifest.parts.size
            override fun nextElement():InputStream {
                check(!cancelled()){"Included image model preparation was cancelled"}
                val part=manifest.parts[position++]
                return app.assets.open("$ROOT/${part.path}")
            }
        }
        SequenceInputStream(pieces).use { input ->
            NativeModelLibrary(app).installEmbeddedArchive(
                LABEL,"anima",UPSTREAM_SHA256,manifest.bytes,
                openArchive={input},progress=progress,stopped=cancelled)
        }
        check(installed()){"Image-generation checkpoint was not registered"}
    }
}
