package com.asterion.art.enhancement

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import java.io.ByteArrayOutputStream

/** In-app, CPU-bounded Image Toolbox functions; Apache-2.0 compatible pipeline. */
data class ImageToolSettings(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val sharpen: Float = 0f
) {
    init {
        require(brightness in -100f..100f)
        require(contrast in 0.5f..2f)
        require(saturation in 0f..2f)
        require(sharpen in 0f..1.75f)
    }
}

object AsterionImageToolbox {
    fun process(context:Context,source:Uri, settings:ImageToolSettings,
                cancelled:()->Boolean, progress:(Int,String)->Unit):ByteArray {
        progress(0,"Opening source")
        val raw=context.contentResolver.openInputStream(source)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: error("Source artwork unavailable")
        try {
            require(raw.width.toLong()*raw.height.toLong()<=40_000_000L) {
                "Image is too large for this CPU filter. Use native tiled enhancement instead."
            }
            if(cancelled()) error("Cancelled before filtering")
            val result=Bitmap.createBitmap(raw.width,raw.height,Bitmap.Config.ARGB_8888)
            try {
                val saturate=ColorMatrix().apply { setSaturation(settings.saturation) }
                val contrast=settings.contrast
                val brightness=settings.brightness
                val adjustment=ColorMatrix(floatArrayOf(
                    contrast,0f,0f,0f,(1f-contrast)*127.5f+brightness,
                    0f,contrast,0f,0f,(1f-contrast)*127.5f+brightness,
                    0f,0f,contrast,0f,(1f-contrast)*127.5f+brightness,
                    0f,0f,0f,1f,0f
                ))
                saturate.postConcat(adjustment)
                progress(15,"Applying color and tone")
                Canvas(result).drawBitmap(raw,0f,0f,
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        colorFilter=ColorMatrixColorFilter(saturate)
                    })
                if(cancelled()) error("Enhancement cancelled")
                if(settings.sharpen>0f) {
                    progress(45,"Preserving edges and fine detail")
                    val width=result.width
                    val height=result.height
                    val out=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
                    try {
                        Canvas(out).drawBitmap(result,0f,0f,null)
                        val rows=32
                        val pixels=IntArray(width*(rows+2))
                        val converted=IntArray(width*rows)
                        for(y0 in 1 until height-1 step rows) {
                            if(cancelled())error("Enhancement cancelled")
                            val valid=(height-y0-1).coerceAtMost(rows)
                            val inputRows=valid+2
                            result.getPixels(pixels,0,width,0,y0-1,width,inputRows)
                            for(dy in 0 until valid) {
                                for(x in 1 until width-1) {
                                    val i=(dy+1)*width+x
                                    val orig=pixels[i]
                                    val up=pixels[i-width]
                                    val down=pixels[i+width]
                                    val left=pixels[i-1]
                                    val right=pixels[i+1]
                                    fun ch(shift:Int):Int {
                                        val original=(orig shr shift) and 255
                                        val lap=original*4 - ((up shr shift) and 255) -
                                            ((down shr shift) and 255) -
                                            ((left shr shift) and 255) -
                                            ((right shr shift) and 255)
                                        return (original+lap*settings.sharpen*0.45f).toInt()
                                            .coerceIn(0,255)
                                    }
                                    converted[dy*width+x]=Color.argb(Color.alpha(orig),
                                        ch(16),ch(8),ch(0))
                                }
                            }
                            // Only interior pixels are overwritten; preserve source boundaries.
                            for(dy in 0 until valid) {
                                out.setPixels(converted,dy*width+1,width,1,y0+dy,width-2,1)
                            }
                            progress(45+((y0*45)/height),"Sharpening fine detail")
                        }
                        progress(92,"Encoding result")
                        return ByteArrayOutputStream().use { stream ->
                            check(out.compress(Bitmap.CompressFormat.PNG,100,stream))
                            stream.toByteArray()
                        }
                    } finally { out.recycle() }
                }
                progress(92,"Encoding result")
                return ByteArrayOutputStream().use { stream ->
                    check(result.compress(Bitmap.CompressFormat.PNG,100,stream))
                    stream.toByteArray()
                }
            } finally { result.recycle() }
        } finally { raw.recycle() }
    }
}
