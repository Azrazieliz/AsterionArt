package com.asterion.compiler.art

import android.graphics.BitmapFactory
import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Actual pixel-derived suggestion: exposure and edge detail, never claims
 * character identity, anatomical accuracy or aesthetic preference.
 */
object ImageTechnicalScore {
    fun inspect(file: File): Evidence {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        require(options.outWidth > 0 && options.outHeight > 0) { "Not a supported image" }
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > 360) sample *= 2
        val bitmap = requireNotNull(BitmapFactory.decodeFile(file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }))
        try {
            val w = bitmap.width
            val h = bitmap.height
            val stride = 2
            var all = 0
            var dark = 0
            var bright = 0
            var gradients = 0.0
            for (y in 1 until h step stride) {
                for (x in 1 until w step stride) {
                    val now = bitmap.getPixel(x, y)
                    val lum = (android.graphics.Color.red(now) * 0.2126 +
                        android.graphics.Color.green(now) * 0.7152 +
                        android.graphics.Color.blue(now) * 0.0722) / 255.0
                    if (lum < 0.015) dark++
                    if (lum > 0.985) bright++
                    if (x > 1 && y > 1) {
                        val previous = bitmap.getPixel(x - 1, y)
                        val old = (android.graphics.Color.red(previous) * 0.2126 +
                            android.graphics.Color.green(previous) * 0.7152 +
                            android.graphics.Color.blue(previous) * 0.0722) / 255.0
                        gradients += kotlin.math.abs(lum - old)
                    }
                    all++
                }
            }
            val clipped = (dark + bright).toDouble() / all.coerceAtLeast(1)
            val clarity = (gradients / all.coerceAtLeast(1) * 10.0).coerceIn(0.0, 1.0)
            val quality = ((1.0 - clipped) * 0.6 + clarity * 0.4).coerceIn(0.0, 1.0)
            return Evidence(Criterion.TECHNICAL, quality, 0.65,
                "Sampled image clipped fraction ${"%.1f".format(clipped * 100)}%. " +
                "Approximate local contrast ${"%.2f".format(clarity)}; no identity or style judgment.")
        } finally { bitmap.recycle() }
    }
}

class ArtRatingStore(private val context: Context) {
    fun save(result: NightmareResult, rating: Int, mode: ArtMode) {
        require(rating in 0..10)
        val file = File(context.filesDir, "asterion_art_ratings.jsonl")
        val record = JSONObject()
            .put("sha256", NightmareBridge.sha256(result.file))
            .put("image", result.file.name)
            .put("run_id", result.runId)
            .put("mode", mode.name)
            .put("rating", rating)
            .put("time_ms", System.currentTimeMillis())
            .toString()
        // Append-only feedback history: ratings never overwrite older opinions.
        file.appendText(record + "\n")
    }
}
