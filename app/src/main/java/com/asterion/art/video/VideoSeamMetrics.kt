package com.asterion.art.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/** Objective seam screening, NOT identity recognition or temporal semantic grading. */
data class VideoBoundaryMetric(
    val afterSegment:Int,
    val meanColorJump:Double,
    val edgeJump:Double,
    val likelyHardCut:Boolean
)

object VideoSeamMetrics {
    private fun frame(file:File,last:Boolean):Bitmap {
        require(file.isFile && file.length()>1024)
        val r=MediaMetadataRetriever()
        try {
            r.setDataSource(file.absolutePath)
            val duration=r.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?:error("MP4 duration unavailable")
            val micros=if(last)max(0L,duration-50L)*1000L else 0L
            return r.getFrameAtTime(micros,MediaMetadataRetriever.OPTION_CLOSEST)
                ?:error("Cannot decode boundary frame")
        } finally {r.release()}
    }
    private fun thumb(bitmap:Bitmap):IntArray {
        val scaled=Bitmap.createScaledBitmap(bitmap,80,80,true)
        return try {IntArray(6400).apply{scaled.getPixels(this,0,80,0,0,80,80)}}
        finally{if(scaled!==bitmap)scaled.recycle()}
    }
    fun inspect(previous:File,next:File,afterSegment:Int):VideoBoundaryMetric {
        val left=frame(previous,true)
        val right=frame(next,false)
        try {
            val a=thumb(left)
            val b=thumb(right)
            var color=0.0
            var edge=0.0
            var edgeCount=0
            fun y(value:Int)=((((value shr 16)and 255)*299+
                ((value shr 8)and 255)*587+(value and 255)*114)/1000)
            for(i in a.indices) {
                val ar=(a[i] shr 16)and 255
                val ag=(a[i] shr 8)and 255
                val ab=a[i] and 255
                val br=(b[i] shr 16)and 255
                val bg=(b[i] shr 8)and 255
                val bb=b[i] and 255
                color+=(abs(ar-br)+abs(ag-bg)+abs(ab-bb))/(765.0*a.size)
                if(i%80!=79) {
                    val old=abs(y(a[i])-y(a[i+1]))
                    val new=abs(y(b[i])-y(b[i+1]))
                    edge+=abs(old-new)/255.0
                    edgeCount++
                }
            }
            val finalEdge=edge/max(1,edgeCount)
            return VideoBoundaryMetric(afterSegment,color,finalEdge,
                color>0.32 && finalEdge>0.18)
        } finally{left.recycle();right.recycle()}
    }
}
