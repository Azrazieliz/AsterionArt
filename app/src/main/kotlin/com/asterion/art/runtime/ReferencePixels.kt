package com.asterion.art.runtime

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import java.io.File

@Suppress("DEPRECATION")
internal fun readProjectionPixels(file: File, imageNet: Boolean = false): FloatTensor {
    val target = 224
    val decoder = checkNotNull(BitmapRegionDecoder.newInstance(file.absolutePath, false)) {
        "The reference must be a decodable image"
    }
    val cropped = try {
        val shortest = minOf(decoder.width, decoder.height)
        val left = (decoder.width - shortest) / 2
        val top = (decoder.height - shortest) / 2
        var sample = 1
        while (shortest / sample >= target * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        checkNotNull(decoder.decodeRegion(Rect(left, top, left + shortest, top + shortest), options))
    } finally {
        decoder.recycle()
    }
    val sized = Bitmap.createScaledBitmap(cropped, target, target, true)
    try {
        val pixels = IntArray(target * target)
        sized.getPixels(pixels, 0, target, 0, 0, target, target)
        val values = FloatArray(pixels.size * 3)
        val offsets = intArrayOf(16, 8, 0)
        val means = if (imageNet) floatArrayOf(0.485f, 0.456f, 0.406f)
            else floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
        val deviations = if (imageNet) floatArrayOf(0.229f, 0.224f, 0.225f)
            else floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)
        for (channel in 0..2) {
            for (index in pixels.indices) {
                val value = ((pixels[index] shr offsets[channel]) and 255) / 255f
                values[channel * pixels.size + index] = (value - means[channel]) / deviations[channel]
            }
        }
        return FloatTensor(listOf(1, 3, target, target), values)
    } finally {
        if (sized !== cropped) sized.recycle()
        cropped.recycle()
    }
}
