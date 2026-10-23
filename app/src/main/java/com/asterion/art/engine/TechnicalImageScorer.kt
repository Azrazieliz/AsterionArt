package com.asterion.art.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Source-independent technical screening, never mislabeled as identity or style fidelity. */
data class TechnicalScore(val score: Double, val contrast: Double, val detail: Double, val clipped: Double)
object TechnicalImageScorer {
    fun score(bytes: ByteArray): TechnicalScore {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Image decoder rejected candidate")
        try { return score(bitmap) } finally { bitmap.recycle() }
    }

    fun score(bitmap: Bitmap): TechnicalScore {
        val width = bitmap.width
        val height = bitmap.height
        require(width >= 16 && height >= 16) { "Image too small to grade" }
        val w = min(width, 256)
        val h = min(height, 256)
        val preview = Bitmap.createScaledBitmap(bitmap, w, h, true)
        try {
            val pixels = IntArray(w * h)
            preview.getPixels(pixels, 0, w, 0, 0, w, h)
            var sum = 0.0
            var sumSquare = 0.0
            var clipped = 0
            var edge = 0.0
            var adjacent = 0
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val value = pixels[y * w + x]
                    val r = (value shr 16) and 255
                    val g = (value shr 8) and 255
                    val b = value and 255
                    val luminance = (r * 299 + g * 587 + b * 114) / 1000
                    sum += luminance
                    sumSquare += luminance.toDouble() * luminance
                    if (luminance < 4 || luminance > 251) clipped++
                    if (x > 0) {
                        val other = pixels[y * w + x - 1]
                        val before = (((other shr 16) and 255) * 299 +
                            ((other shr 8) and 255) * 587 + (other and 255) * 114) / 1000
                        edge += abs(luminance - before)
                        adjacent++
                    }
                }
            }
            val n = w * h
            val mean = sum / n
            val deviation = kotlin.math.sqrt(max(0.0, sumSquare / n - mean * mean))
            val contrast = (deviation / 65.0).coerceIn(0.0, 1.0)
            val detail = ((edge / max(1, adjacent)) / 22.0).coerceIn(0.0, 1.0)
            val clipRatio = clipped.toDouble() / n
            val technical = (0.55 * contrast + 0.45 * detail -
                0.20 * clipRatio).coerceIn(0.0, 1.0)
            return TechnicalScore(technical * 100, contrast, detail, clipRatio)
        } finally {
            if (preview !== bitmap) preview.recycle()
        }
    }
}
