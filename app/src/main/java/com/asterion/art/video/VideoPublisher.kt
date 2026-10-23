package com.asterion.art.video

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object VideoPublisher {
    fun save(context: Context, source: File, id: String): Uri {
        require(source.isFile && source.length() > 1024)
        require(id.matches(Regex("[A-Za-z0-9_-]{8,90}")))
        val resolver=context.contentResolver
        val values=ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME,"Asterion_" + id.take(12) + ".mp4")
            put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/AsterionArt")
            put(MediaStore.Video.Media.IS_PENDING,1)
        }
        val uri=resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values)
            ?: error("Cannot create video library entry")
        try {
            requireNotNull(resolver.openOutputStream(uri,"w")).use { stream ->
                source.inputStream().buffered().use { it.copyTo(stream) }
            }
            require(resolver.update(uri,ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING,0)
            },null,null)>0)
            return uri
        } catch(t:Throwable) { resolver.delete(uri,null,null); throw t }
    }
}
