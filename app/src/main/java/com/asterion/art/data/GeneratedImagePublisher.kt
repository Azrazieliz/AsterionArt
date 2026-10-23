package com.asterion.art.data

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.security.MessageDigest

object GeneratedImagePublisher {
    fun publish(context: Context, jobId: String, index: Int, png: ByteArray): Uri {
        require(index in 1..99 && png.size > 100)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(png, 0, png.size, options)
        require(options.outWidth > 0 && options.outHeight > 0) { "Invalid image" }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,
                "Asterion_" + jobId.take(12) + "_" + index.toString().padStart(2, '0') + ".png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AsterionArt/Generated")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not reserve image URI")
        try {
            requireNotNull(resolver.openOutputStream(uri, "w")).use { it.write(png) }
            resolver.update(uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null, null)
            return uri
        } catch (e: Throwable) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    fun publishEnhancedJpeg(context: Context, jobId: String, jpeg: ByteArray): Uri {
        require(jpeg.size > 100)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg,0,jpeg.size,options)
        require(options.outWidth > 0 && options.outHeight > 0)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Asterion_" + jobId.take(12) + "_enhanced.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AsterionArt/Enhanced")
            put(MediaStore.Images.Media.IS_PENDING,1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
            ?: error("Cannot reserve enhanced image")
        try {
            requireNotNull(resolver.openOutputStream(uri,"w")).use { it.write(jpeg) }
            resolver.update(uri,ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING,0)
            },null,null)
            return uri
        } catch (e: Throwable) {
            resolver.delete(uri,null,null)
            throw e
        }
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    fun atomicWrite(target: File, data: ByteArray) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".tmp")
        java.io.FileOutputStream(temp).use { it.write(data); it.fd.sync() }
        require(temp.renameTo(target)) { "Could not persist checkpoint" }
    }
}
