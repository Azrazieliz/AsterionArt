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
import com.asterion.art.model.SheetRole
import com.asterion.art.model.SheetLayouts
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

        // Canonical panel ordering is determined by the typed sheet schema.
        // A weapon or wing panel appears only if the source includes it.
        val order = SheetLayouts.forRole(sheet.role).map { it.id }
        val entries = sheet.sourceUris.mapIndexed { index, uri ->
            val panel = sheet.referenceSlots.getOrNull(index) ?: "PRIMARY"
            Triple(uri,panel,index)
        }.sortedWith(compareBy<Triple<String,String,Int>> {
            val priority=order.indexOf(it.second)
            if(priority<0) Int.MAX_VALUE else priority
        }.thenBy { it.third })
        val slots = layout(sheet.role)
        val histogram = linkedMapOf<Int,Int>()
        entries.forEachIndexed { index, (uri,panel,_) ->
            val region=slots[index]
            val name=SheetLayouts.label(sheet.role,panel)
            drawTile(context,canvas,uri,region,index==0,index+1,name,histogram)
        }

        // Derived from actual supplied pixels, not generated color prose.
        // Exclude near-white paper-background colors; preserve greys, metals and blacks.
        val swatches = histogram.entries.sortedByDescending { it.value }
            .map { it.key }.distinct().take(7)
        val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT
        }
        canvas.drawText("REFERENCE PIXEL COLORS",45f,2584f,caption)
        val paintSwatch=Paint(Paint.ANTI_ALIAS_FLAG)
        swatches.forEachIndexed { i,color ->
            paintSwatch.color=color
            val x=370f+i*126f
            canvas.drawRoundRect(RectF(x,2545f,x+106f,2591f),5f,5f,paintSwatch)
        }
        caption.color=navy
        canvas.drawText("REV "+sheet.structuredDataVersion,1450f,2584f,caption)

        val encoded = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, encoded)) { "PNG encoding failed" }
        bitmap.recycle()
        return saveImage(context, sheet, encoded.toByteArray())
    }

    private fun layout(role: SheetRole): List<RectF> {
        val rects = mutableListOf<RectF>()
        when(role) {
            SheetRole.CHARACTER -> {
                rects+=RectF(45f,208f,1043f,1668f)
                repeat(3) { i ->
                    rects+=RectF(1073f,208f+i*486f,1555f,208f+(i+1)*486f-20f)
                }
                repeat(6) { i ->
                    val col=i%3
                    val row=i/3
                    rects+=RectF(45f+col*513f,1700f+row*407f,
                        45f+col*513f+484f,1700f+row*407f+376f)
                }
            }
            SheetRole.ART_STYLE -> {
                repeat(3) { i ->
                    rects+=RectF(45f+i*513f,208f,
                        45f+i*513f+484f,1228f)
                }
                repeat(7) { i ->
                    val col=i%3
                    val row=i/3
                    rects+=RectF(45f+col*513f,1250f+row*419f,
                        45f+col*513f+484f,1250f+row*419f+396f)
                }
            }
            SheetRole.THEME -> {
                rects+=RectF(45f,208f,1555f,1145f)
                repeat(9) { i ->
                    val col=i%3
                    val row=i/3
                    rects+=RectF(45f+col*513f,1168f+row*430f,
                        45f+col*513f+484f,1168f+row*430f+400f)
                }
            }
            SheetRole.ELEMENT -> {
                rects+=RectF(45f,208f,1555f,1398f)
                repeat(9) { i ->
                    val col=i%3
                    val row=i/3
                    rects+=RectF(45f+col*513f,1420f+row*355f,
                        45f+col*513f+484f,1420f+row*355f+331f)
                }
            }
        }
        return rects.take(MAX_REFS)
    }

    private fun drawTile(context: Context, canvas: Canvas, uri: String,
                         rect: RectF, primary: Boolean, index: Int, label: String,
                         histogram: MutableMap<Int,Int>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.color = Color.WHITE
        canvas.drawRoundRect(rect, 12f, 12f, paint)
        val inner = RectF(rect.left + 8f, rect.top + 8f, rect.right - 8f, rect.bottom - 46f)
        paint.color = Color.rgb(244, 244, 242)
        canvas.drawRect(inner, paint)
        val image = decodePreview(context, Uri.parse(uri), max(inner.width().toInt(), inner.height().toInt()))
        try {
            // Coarse deterministic reference-pixel histogram for sheet swatches.
            val nx=48
            val ny=48
            val miniature=Bitmap.createScaledBitmap(image,nx,ny,true)
            try {
                val samples=IntArray(nx*ny)
                miniature.getPixels(samples,0,nx,0,0,nx,ny)
                for(px in samples) {
                    val rr=(Color.red(px)/32)*32+16
                    val gg=(Color.green(px)/32)*32+16
                    val bb=(Color.blue(px)/32)*32+16
                    if(rr>232 && gg>232 && bb>232)continue
                    val key=Color.rgb(rr.coerceAtMost(255),
                        gg.coerceAtMost(255),bb.coerceAtMost(255))
                    histogram[key]=(histogram[key] ?: 0)+1
                }
            } finally {
                if(miniature!==image)miniature.recycle()
            }
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
