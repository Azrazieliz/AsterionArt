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
import com.asterion.art.model.SheetPresentation
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.min
import kotlin.math.max

/**
 * Deterministic image composition of real reference assets.
 * It does not invent or annotate traits not supplied by their source.
 */
object SheetRenderer {
    const val MAX_REFS=SheetPresentation.MAX_REFS
    private const val WIDTH=SheetPresentation.WIDTH
    private const val HEIGHT=SheetPresentation.HEIGHT
    private val navy=Color.rgb(34,46,69)
    private val line=Color.rgb(201,209,219)
    private val ivory=Color.rgb(249,250,252)
    private val light=Color.rgb(240,242,247)
    private val accent=Color.rgb(80,106,148)

    fun render(context: Context, sheet: SheetRecord): Uri {
        require(sheet.sourceUris.isNotEmpty()&&sheet.sourceUris.size<=MAX_REFS) {
            "Reference count must be between 1 and "+MAX_REFS
        }
        // Older imported sheets may not have individually assigned panels.
        // Their original source images must remain usable after an upgrade.
        val assignment=sheet.sourceUris.indices.map{index->
            sheet.referenceSlots.getOrNull(index)?:"PRIMARY"
        }
        val plan=SheetPresentation.plan(sheet.role,assignment)
        val bitmap=Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(bitmap)
            canvas.drawColor(ivory)
            val header=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color=navy
                typeface=android.graphics.Typeface.create(
                    "serif",android.graphics.Typeface.NORMAL)
                textSize=56f
            }
            canvas.drawText(plan.title,34f,91f,header)
            header.typeface=android.graphics.Typeface.DEFAULT
            header.textSize=25f
            header.color=accent
            canvas.drawText(
                "ASTERION ART  /  AUTHORITATIVE VISUAL REFERENCES",36f,135f,header)
            val linePaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
                color=line;strokeWidth=2f
            }
            canvas.drawLine(34f,157f,(WIDTH-34).toFloat(),157f,linePaint)
            val histogram=linkedMapOf<Int,Int>()
            plan.placements.forEachIndexed { index,placement ->
                val bounds=placement.frame
                val tile=RectF(bounds.left.toFloat(),bounds.top.toFloat(),
                    bounds.right.toFloat(),bounds.bottom.toFloat())
                drawTile(context,canvas,sheet.sourceUris[placement.sourceIndex],
                    tile,placement.principal,index+1,
                    SheetLayouts.label(sheet.role,placement.slot),histogram)
            }
            val footer=plan.swatchArea
            val text=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color=navy
                textSize=25f
                typeface=android.graphics.Typeface.DEFAULT
            }
            canvas.drawLine(footer.left.toFloat(),(footer.top-12).toFloat(),
                footer.right.toFloat(),(footer.top-12).toFloat(),linePaint)
            canvas.drawText("REFERENCE COLOR PALETTE",footer.left.toFloat(),
                (footer.top+29).toFloat(),text)
            text.textSize=19f
            text.color=accent
            canvas.drawText("DERIVED FROM SUPPLIED PIXELS",footer.left.toFloat(),
                (footer.top+60).toFloat(),text)
            val swatches=histogram.entries.sortedByDescending{it.value}
                .map{it.key}.distinct().take(7)
            val colorPaint=Paint(Paint.ANTI_ALIAS_FLAG)
            swatches.forEachIndexed {i,color->
                colorPaint.color=color
                val x=footer.left+480+i*123f
                canvas.drawRoundRect(
                    RectF(x,(footer.top+12).toFloat(),x+102f,
                        (footer.top+77).toFloat()),5f,5f,colorPaint)
            }
            text.color=navy
            text.textSize=20f
            canvas.drawText("REFERENCE "+sheet.role.display.uppercase(Locale.ROOT)+
                "  ·  "+sheet.sourceUris.size+" SOURCE(S)",34f,2526f,text)
            val revision="REV "+sheet.structuredDataVersion
            canvas.drawText(revision,(WIDTH-34).toFloat()-text.measureText(revision),
                2526f,text)
            val png=ByteArrayOutputStream().use { bytes ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes))
                bytes.toByteArray()
            }
            return saveImage(context,sheet,png)
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawTile(context: Context, canvas: Canvas, uri: String,
                         rect: RectF, primary: Boolean, index: Int, label: String,
                         histogram: MutableMap<Int,Int>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.color=Color.WHITE
        canvas.drawRoundRect(rect,5f,5f,paint)
        val inner=RectF(rect.left+8f,rect.top+8f,rect.right-8f,rect.bottom-53f)
        paint.color=light
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
        canvas.drawRoundRect(rect,5f,5f,paint)
        paint.style = Paint.Style.FILL
        paint.color = navy
        paint.textSize=if(primary)25f else 20f
        paint.typeface=android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD)
        val caption=(if(primary) "PRIMARY · " else
            index.toString().padStart(2,'0')+" · ")+label.uppercase(Locale.ROOT)
        val available=rect.width()-30f
        while(paint.measureText(caption)>available&&paint.textSize>12f)
            paint.textSize-=1f
        require(paint.measureText(caption)<=available) {
            "Reference title does not fit the selected clean sheet layout"
        }
        canvas.drawText(caption,rect.left+15f,rect.bottom-16f,paint)
    }

    private fun decodePreview(context: Context, uri: Uri, target: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // With inJustDecodeBounds=true, decodeStream returns null on SUCCESS:
        // the dimensions are populated in 'bounds'. Never apply an Elvis
        // missing-source check to decodeStream's intentionally-null return.
        val dimensionsStream=context.contentResolver.openInputStream(uri)
            ?:error("Reference source unavailable: $uri")
        dimensionsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
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
