package com.asterion.art.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/** Crop-to-mask image editing; changes outside the original mask remain exactly unchanged. */
data class EditPatch(
    val imagePng: ByteArray,
    val maskPng: ByteArray,
    val rectangle: Rect,
)

object SelectiveImageEditor {
    private fun encoded(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { stream ->
        require(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        stream.toByteArray()
    }

    fun prepare(source: Bitmap, mask: Bitmap, targetW: Int, targetH: Int): EditPatch {
        require(source.width == mask.width && source.height == mask.height)
        require(targetW >= 64 && targetH >= 64)
        var x0 = source.width
        var y0 = source.height
        var x1 = 0
        var y1 = 0
        var found = false
        val stride = max(1, min(source.width, source.height) / 1500)
        for (y in 0 until mask.height step stride) {
            for (x in 0 until mask.width step stride) {
                if ((Color.red(mask.getPixel(x, y))) > 30) {
                    found = true
                    x0 = min(x0, x)
                    y0 = min(y0, y)
                    x1 = max(x1, x)
                    y1 = max(y1, y)
                }
            }
        }
        require(found) { "Paint an editing mask on the image before submitting." }
        val pad = max(48, max(x1 - x0, y1 - y0) / 2)
        val cropW = min(source.width, max(targetW, x1 - x0 + pad * 2))
        val cropH = min(source.height, max(targetH, y1 - y0 + pad * 2))
        val left = (x0 + x1 - cropW) / 2
        val top = (y0 + y1 - cropH) / 2
        val rect = Rect(left.coerceIn(0, source.width - cropW),
            top.coerceIn(0, source.height - cropH), 0, 0).apply {
            right = this.left + cropW
            bottom = this.top + cropH
        }
        val imgCrop = Bitmap.createBitmap(source, rect.left, rect.top, cropW, cropH)
        val maskCrop = Bitmap.createBitmap(mask, rect.left, rect.top, cropW, cropH)
        try {
            val input = Bitmap.createScaledBitmap(imgCrop, targetW, targetH, true)
            val region = Bitmap.createScaledBitmap(maskCrop, targetW, targetH, false)
            try { return EditPatch(encoded(input), encoded(region), rect) }
            finally {
                if (input !== imgCrop) input.recycle()
                if (region !== maskCrop) region.recycle()
            }
        } finally {
            imgCrop.recycle()
            maskCrop.recycle()
        }
    }

    /** Mandatory hard guarantee: outside the mask we never write a pixel. */
    fun composite(original: Bitmap, nativeResultPng: ByteArray,
                  mask: Bitmap, patchRect: Rect): ByteArray {
        require(original.width == mask.width && original.height == mask.height)
        val native = BitmapFactory.decodeByteArray(nativeResultPng, 0, nativeResultPng.size)
            ?: error("Edit model returned an invalid PNG")
        val resized = Bitmap.createScaledBitmap(native, patchRect.width(), patchRect.height(), true)
        val output = original.copy(Bitmap.Config.ARGB_8888, true)
        try {
            for (y in patchRect.top until patchRect.bottom) {
                for (x in patchRect.left until patchRect.right) {
                    val alpha = Color.red(mask.getPixel(x, y)).coerceIn(0,255)
                    if (alpha <= 0) continue
                    val base = original.getPixel(x, y)
                    val mod = resized.getPixel(x - patchRect.left, y - patchRect.top)
                    fun mix(a: Int, b: Int): Int = (a * (255-alpha) + b * alpha + 127) / 255
                    output.setPixel(x, y, Color.argb(
                        Color.alpha(base),
                        mix(Color.red(base), Color.red(mod)),
                        mix(Color.green(base), Color.green(mod)),
                        mix(Color.blue(base), Color.blue(mod))))
                }
            }
            return encoded(output)
        } finally {
            output.recycle()
            if (resized !== native) resized.recycle()
            native.recycle()
        }
    }

    fun paintMask(width: Int, height: Int, strokes: List<List<Pair<Float,Float>>>,
                  radiusPx: Int): Bitmap {
        require(width > 0 && height > 0 && radiusPx > 0)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f * radiusPx
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        for (stroke in strokes) {
            val points = stroke.map { (x,y) ->
                Pair(x.coerceIn(0f,1f)*width, y.coerceIn(0f,1f)*height)
            }
            if (points.size == 1) canvas.drawCircle(points[0].first,points[0].second,
                radiusPx.toFloat(),Paint(brush).apply { style=Paint.Style.FILL })
            for (i in 1 until points.size) canvas.drawLine(points[i-1].first,
                points[i-1].second,points[i].first,points[i].second,brush)
        }
        return bitmap
    }
}
