package com.asterion.art.learning

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.asterion.art.data.GeneratedImagePublisher
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.max

/**
 * Real pixel evidence, independent of the text-only Qwen3 model. This does
 * NOT recognize anatomy or evaluate semantic quality. The user supplies those
 * judgments using targeted grading; the features learn visual correlations.
 */
object VisualGradeFeatures {
    const val SIZE=64
    fun extract(argb:IntArray,width:Int,height:Int):List<Double> {
        require(width>=4 && height>=4 && argb.size==width*height)
        val sums=DoubleArray(SIZE)
        val counts=IntArray(16)
        fun lum(pixel:Int):Int =
            (((pixel shr 16) and 255)*299 +
                ((pixel shr 8) and 255)*587 + (pixel and 255)*114)/1000
        for(y in 0 until height)for(x in 0 until width) {
            val index=y*width+x
            val v=argb[index]
            val block=(y*4/height)*4+(x*4/width)
            sums[block]+=((v shr 16) and 255)/255.0
            sums[16+block]+=((v shr 8) and 255)/255.0
            sums[32+block]+=(v and 255)/255.0
            val edge=if(x>0 && y>0) {
                (abs(lum(v)-lum(argb[index-1]))+
                    abs(lum(v)-lum(argb[index-width])))/510.0
            } else 0.0
            sums[48+block]+=edge
            counts[block]++
        }
        return sums.indices.map{index->
            (sums[index]/max(1,counts[index%16])).coerceIn(0.0,1.0)
        }
    }
}

data class CapturedGradingEvidence(
    val path:String,val sha256:String,val features:List<Double>
)

/**
 * The image itself, not its prompt, is sampled and the exact targeted crop is
 * stored as an app-private PNG. No cloud, no automated device permissions.
 */
class GradingImageEvidence(private val context:Context) {
    private val root=File(context.applicationContext.filesDir,"grading/evidence")
        .also{it.mkdirs()}
    fun capture(assetUri:String,region:GradeRegion,id:String):CapturedGradingEvidence {
        require(id.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        val uri=Uri.parse(assetUri)
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        context.contentResolver.openInputStream(uri).use{stream->
            requireNotNull(stream){"The original image cannot be opened"}
            BitmapFactory.decodeStream(stream,null,bounds)
        }
        require(bounds.outWidth>0 && bounds.outHeight>0) {
            "Image has no decodable dimensions"
        }
        var sample=1
        while(max(bounds.outWidth,bounds.outHeight)/sample>2048)sample*=2
        val options=BitmapFactory.Options().apply{inSampleSize=sample}
        val decoded=context.contentResolver.openInputStream(uri).use{stream->
            requireNotNull(stream){"Cannot reopen image for grading"}
            BitmapFactory.decodeStream(stream,null,options)
        }?:error("Image decoding failed")
        try {
            val left=(region.x*decoded.width).toInt()
                .coerceIn(0,decoded.width-1)
            val top=(region.y*decoded.height).toInt()
                .coerceIn(0,decoded.height-1)
            val right=((region.x+region.width)*decoded.width).toInt()
                .coerceIn(left+1,decoded.width)
            val bottom=((region.y+region.height)*decoded.height).toInt()
                .coerceIn(top+1,decoded.height)
            val crop=Bitmap.createBitmap(decoded,left,top,right-left,bottom-top)
            try {
                val evidenceSize=max(crop.width,crop.height)
                val scale=minOf(1.0,512.0/evidenceSize)
                val width=max(4,(crop.width*scale).toInt())
                val height=max(4,(crop.height*scale).toInt())
                val thumb=Bitmap.createScaledBitmap(crop,width,height,true)
                try {
                    val bytes=ByteArrayOutputStream().use{out->
                        check(thumb.compress(Bitmap.CompressFormat.PNG,100,out))
                        out.toByteArray()
                    }
                    require(bytes.size>=64)
                    val hash=MessageDigest.getInstance("SHA-256")
                        .digest(bytes).joinToString(""){"%02x".format(it)}
                    val path=File(root,id+".png")
                    GeneratedImagePublisher.atomicWrite(path,bytes)
                    val miniature=Bitmap.createScaledBitmap(thumb,64,64,true)
                    val features=try {
                        val pixels=IntArray(64*64)
                        miniature.getPixels(pixels,0,64,0,0,64,64)
                        VisualGradeFeatures.extract(pixels,64,64)
                    } finally {if(miniature!==thumb)miniature.recycle()}
                    return CapturedGradingEvidence(path.absolutePath,hash,features)
                } finally {if(thumb!==crop)thumb.recycle()}
            } finally {crop.recycle()}
        } finally {decoded.recycle()}
    }
}
