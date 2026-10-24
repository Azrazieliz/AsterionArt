package com.asterion.art.integrations

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException

/**
 * Authorized Android ACTION_SEND handoff to the actual installed licensed apps.
 * It never copies, patches, redistributes or bypasses their proprietary engines.
 */
object InstalledImageEnhancers {
    enum class App(val title: String, val packageName: String) {
        SUPER_IMAGE_PRO("SuperImage Pro", "com.zhenxiang.superimage.pro"),
        IMAGE_TOOLBOX("Image Toolbox", "ru.tech.imageresizershrinker"),
        REALSR("RealSR", "com.tumuyan.ncnn.realsr")
    }

    fun share(context: Context, app: App, imageUri: Uri) {
        require(imageUri.scheme == "content") { "Share a saved artwork content URI" }
        val intent=Intent(Intent.ACTION_SEND).apply {
            type="image/*"
            setPackage(app.packageName)
            putExtra(Intent.EXTRA_STREAM,imageUri)
            clipData=ClipData.newUri(context.contentResolver,"Asterion artwork",imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            throw IllegalStateException(app.title+" does not expose a compatible image sharing activity.",e)
        }
    }
}
