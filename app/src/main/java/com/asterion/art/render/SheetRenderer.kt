package com.asterion.art.render

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.asterion.art.model.SheetRecord
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.min
import kotlin.math.max

/**
 * Deterministic image composition of real reference assets.
 * It does not invent or annotate traits not supplied by their source.
 */
object SheetRenderer {
    const val MAX_REFS = 10
    private const val WIDTH = 1600
    private const val HEIGHT = 2620
    private val navy = Color.rgb(31, 43, 68)
    private val line = Color.rgb(197, 201, 215)
    private val ivory = Color.rgb(249, 248, 246)
    private val gold = Color.rgb(171, 133, 68)

    fun render(context: Context, sheet: SheetRecord): Uri {
        require(sheet.sourceUris.isNotEmpty() && sheet.sourceUris.size <= MAX_REFS) {
            "Reference count must be between 1 and $MAX_REFS."
        }
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ivory)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy; textSize = 73f; typeface =
            android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD) }
        canvas.drawText(sheet.role.display.uppercase(Locale.ROOT) + " MASTER SHEET", 46f, 105f, paint)
        paint.color = gold
        canvas.drawRect(RectF(46f, 128f, 1554f, 138f), paint)
        paint.color = navy
        paint.textSize = 31f
        paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("ASTERION ART  /  CANONICAL VISUAL REFERENCES", 49f, 176f, paint)

        val slots = mutableListOf<RectF>()
        slots += RectF(45f, 208f, 1043f, 1668f)
        for (i in 0 until 3) {
            slots += RectF(1073f, 208f + i * 486f, 1555f, 208f + (i + 1) * 486f - 20f)
        }
        for (i in 0 until 6) {
            val column = i % 3
            val row = i / 3
            slots += RectF(45f + column * 513f, 1700f + row * 407f,
                45f + column * 513f + 484f, 1700f + row * 407f + 376f)
        }
        sheet.sourceUris.forEachIndexed { index, stringUri ->
            val rect = slots[index]
            val slot = sheet.referenceSlots.getOrNull(index) ?: "REFERENCE"
            val label = com.asterion.art.model.SheetLayouts.label(sheet.role, slot)
            drawTile(context, canvas, stringUri, rect, index == 0, index + 1, label)
        }

        paint.apply { color = navy; textSize = 22f; typeface = android.graphics.Typeface.DEFAULT }
        canvas.drawText("REFERENCE-BASED / NO UNSOURCED IDENTITY ATTRIBUTES", 46f, 2585f, paint)
        canvas.drawText("REV " + sheet.structuredDataVersion, 1440f, 2585f, paint)

        val encoded = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, encoded)) { "PNG encoding failed" }
        bitmap.recycle()
        return saveImage(context, sheet, encoded.toByteArray())
    }

    private fun drawTile(context: Context, canvas: Canvas, uri: String,
                         rect: RectF, primary: Boolean, index: Int, label: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.color = Color.WHITE
        canvas.drawRoundRect(rect, 12f, 12f, paint)
        val inner = RectF(rect.left + 8f, rect.top + 8f, rect.right - 8f, rect.bottom - 46f)
        paint.color = Color.rgb(244, 244, 242)
        canvas.drawRect(inner, paint)
        val image = decodePreview(context, Uri.parse(uri), max(inner.width().toInt(), inner.height().toInt()))
        try {
            val scale = min(inner.width() / image.width.toFloat(), inner.height() / image.height.toFloat())
            val w = image.width * scale
            val h = image.height * scale
            val dest = RectF(inner.centerX() - w / 2f, inner.centerY() - h / 2f,
                inner.centerX() + w / 2f, inner.centerY() + h / 2f)
            paint.color = Color.WHITE
            canvas.drawBitmap(image, null, dest, paint)
        } finally {
            image.recycle()
        }
        paint.color = line
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(rect, 12f, 12f, paint)
        paint.style = Paint.Style.FILL
        paint.color = navy
        paint.textSize = if (primary) 27f else 22f
        paint.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText(
            (if (primary) "PRIMARY · " else index.toString().padStart(2, '0') + " · ") + label.uppercase(Locale.ROOT).take(29),
            rect.left + 18f, rect.bottom - 15f, paint
        )
    }

    private fun decodePreview(context: Context, uri: Uri, target: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: error("Reference source unavailable: $uri")
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported reference image" }
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > max(700, target * 2))
            sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("Unable to decode reference: $uri")
    }

    private fun saveImage(context: Context, sheet: SheetRecord, png: ByteArray): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Asterion_" + sheet.role.name + "_" +
                sheet.id.take(12) + ".png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AsterionArt/Sheets")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore refused new sheet")
        try {
            requireNotNull(resolver.openOutputStream(uri, "w")).use { it.write(png) }
            val publish = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            check(resolver.update(uri, publish, null, null) > 0) { "Cannot publish sheet" }
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}
