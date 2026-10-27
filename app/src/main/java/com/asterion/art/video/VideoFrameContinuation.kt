package com.asterion.art.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Last-frame continuation for I2V endpoints which only accept a first frame.
 *
 * For a single selected source, successive clips can use the actual final
 * decoded frame of the previous clip rather than repeating a still reference.
 * This improves the boundary condition but does not guarantee seamless motion
 * or full multi-keyframe conditioning.
 */
object VideoFrameContinuation {
    fun extractEndFrame(context:Context,clip:File,output:File):Uri {
        require(clip.isFile && clip.length()>1024){"Finished video is missing"}
        val retriever=MediaMetadataRetriever()
        try {
            retriever.setDataSource(clip.absolutePath)
            val duration=retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: error("Generated video does not report duration")
            require(duration>=100){"Video duration too short"}
            // Seek slightly before exact end to avoid an out-of-range decoder request.
            val offsetUs=((duration-45).coerceAtLeast(0))*1000L
            val frame=retriever.getFrameAtTime(
                offsetUs,MediaMetadataRetriever.OPTION_CLOSEST)
                ?: error("Cannot decode last frame from generated MP4")
            try {
                output.parentFile?.mkdirs()
                val tmp=File(output.path+".part")
                try {
                    FileOutputStream(tmp).use { f ->
                        check(frame.compress(Bitmap.CompressFormat.PNG,100,f)) {
                            "Could not encode continuation frame"
                        }
                        f.fd.sync()
                    }
                    require(tmp.length()>128)
                    if(output.exists())check(output.delete())
                    check(tmp.renameTo(output)){"Could not commit continuation frame"}
                } finally{tmp.delete()}
                return Uri.fromFile(output)
            } finally{frame.recycle()}
        } finally{retriever.release()}
    }
}

data class ContinuitySegment(val index:Int,val seed:Long,
    val usePreviousEndFrame:Boolean,val expectedSeconds:Int)

object VideoContinuitySchedule {
    fun plan(referenceCount:Int,seconds:Int,seed:Long,
             continueSingleReference:Boolean,
             totalSegments:Int=referenceCount):List<ContinuitySegment> {
        require(referenceCount in 1..8)
        require(seconds in 1..5)
        require(totalSegments in 1..24)
        require(continueSingleReference || totalSegments==referenceCount)
        require(!continueSingleReference || referenceCount==1)
        return (0 until totalSegments).map { i ->
            ContinuitySegment(i,seed+i*1009L,
                continueSingleReference && i>0,seconds)
        }
    }
}
