package com.asterion.art.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.asterion.art.model.SheetRecord
import com.asterion.art.model.SheetRole
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Preserves source pixels for img2img and reference-conditioned QNN backends. */
object SourceReferenceEncoder {
    private const val MAX_IMAGES = 10
    private const val MAX_EDGE = 2048

    fun encode(context: Context,sheets: List<SheetRecord>): List<ByteArray> {
        val order=listOf(SheetRole.CHARACTER,SheetRole.ART_STYLE,
            SheetRole.THEME,SheetRole.ELEMENT)
        val ordered=sheets.sortedBy { order.indexOf(it.role) }
        val sources=ordered.flatMap { it.sourceUris }.distinct()
        return encodeUris(context,sources)
    }

    fun encodeUris(context:Context,uris:List<String>):List<ByteArray> {
        val sources=uris.distinct()
        require(sources.isNotEmpty())
        require(sources.size<=MAX_IMAGES) {
            "Reference-capable backend received more than 10 distinct reference images. " +
                "The source must be grouped into a bounded visual plan; no references were dropped."
        }
        return sources.map { uri ->
            val source=Uri.parse(uri)
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            requireNotNull(context.contentResolver.openInputStream(source)).use {
                BitmapFactory.decodeStream(it,null,bounds)
            }
            require(bounds.outWidth>0 && bounds.outHeight>0) { "Invalid source image" }
            var sample=1
            while(max(bounds.outWidth/sample,bounds.outHeight/sample)>MAX_EDGE*2)
                sample*=2
            val options=BitmapFactory.Options().apply {
                inSampleSize=sample
                inPreferredConfig=Bitmap.Config.ARGB_8888
            }
            val input=requireNotNull(context.contentResolver.openInputStream(source)).use {
                BitmapFactory.decodeStream(it,null,options)
            } ?: error("Could not decode image reference")
            try {
                val edge=max(input.width,input.height)
                val ratio=if(edge<=MAX_EDGE)1.0 else MAX_EDGE.toDouble()/edge
                val render=if(ratio==1.0)input else Bitmap.createScaledBitmap(
                    input,(input.width*ratio).toInt().coerceAtLeast(1),
                    (input.height*ratio).toInt().coerceAtLeast(1),true)
                try {
                    // Retain full precision whenever an image fits the
                    // native reference budget. JPEG-89 was throwing away
                    // identifying hair, line-art and texture information before
                    // generation had even started.
                    val lossless=ByteArrayOutputStream().use { out ->
                        check(render.compress(Bitmap.CompressFormat.PNG,100,out))
                        out.toByteArray()
                    }
                    if(lossless.size<=8_000_000)lossless
                    else ByteArrayOutputStream().use { out ->
                        check(render.compress(Bitmap.CompressFormat.JPEG,96,out))
                        out.toByteArray().also{bytes->
                            require(bytes.size<=8_000_000) {
                                "Image too large for the native reference "+
                                    "context even after high-quality encoding"
                            }
                        }
                    }
                } finally { if(render!==input)render.recycle() }
            } finally { input.recycle() }
        }
    }
}
