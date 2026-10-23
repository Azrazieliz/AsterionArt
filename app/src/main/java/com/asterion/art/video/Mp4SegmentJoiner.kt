package com.asterion.art.video

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * MediaMuxer combines independently inferred Nightmare MP4 segments without re-encoding.
 * This is timeline assembly, NOT native two-endpoint keyframe conditioning.
 */
object Mp4SegmentJoiner {
    fun join(parts: List<File>, target: File): File {
        require(parts.isNotEmpty() && parts.all { it.isFile })
        if(parts.size==1) {
            parts.single().copyTo(target,overwrite=true)
            return target
        }
        val temp=File(target.absolutePath+".tmp")
        var muxer: MediaMuxer?=null
        try {
            val extractor=MediaExtractor()
            try {
                extractor.setDataSource(parts.first().absolutePath)
                val index=(0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)
                        ?.startsWith("video/")==true
                } ?: error("No video track in first segment")
                val reference=extractor.getTrackFormat(index)
                val mime=reference.getString(MediaFormat.KEY_MIME)!!
                val width=reference.getInteger(MediaFormat.KEY_WIDTH)
                val height=reference.getInteger(MediaFormat.KEY_HEIGHT)
                muxer=MediaMuxer(temp.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val track=muxer.addTrack(reference)
                muxer.start()
                val bytes=ByteBuffer.allocateDirect(8*1024*1024)
                val bufferInfo=MediaCodec.BufferInfo()
                var offsetUs=0L
                for(file in parts) {
                    val segment=MediaExtractor()
                    try {
                        segment.setDataSource(file.absolutePath)
                        val vid=(0 until segment.trackCount).firstOrNull {
                            segment.getTrackFormat(it).getString(MediaFormat.KEY_MIME)
                                ?.startsWith("video/")==true
                        } ?: error("Segment contains no video")
                        val fmt=segment.getTrackFormat(vid)
                        require(fmt.getString(MediaFormat.KEY_MIME)==mime &&
                                fmt.getInteger(MediaFormat.KEY_WIDTH)==width &&
                                fmt.getInteger(MediaFormat.KEY_HEIGHT)==height) {
                            "Video segments use incompatible formats/resolutions"
                        }
                        segment.selectTrack(vid)
                        var last=0L
                        var count=0
                        while(true) {
                            bytes.clear()
                            val n=segment.readSampleData(bytes,0)
                            if(n<0) break
                            require(n>0 && n<=bytes.capacity()) { "Oversized or empty video packet" }
                            val timestamp=segment.sampleTime.coerceAtLeast(0)
                            bufferInfo.offset=0
                            bufferInfo.size=n
                            bufferInfo.presentationTimeUs=offsetUs+timestamp
                            bufferInfo.flags=segment.sampleFlags
                            muxer.writeSampleData(track,bytes,bufferInfo)
                            last=timestamp
                            count++
                            if(!segment.advance())break
                        }
                        require(count>0) { "Empty video segment" }
                        val frameUs=1_000_000L/24
                        offsetUs += last+frameUs
                    } finally { segment.release() }
                }
            } finally { extractor.release() }
            muxer.stop()
            muxer.release()
            muxer=null
            require(temp.isFile && temp.length()>1024)
            require(temp.renameTo(target)) { "Could not finalize joined MP4" }
            return target
        } catch(e:Throwable) {
            runCatching { muxer?.release() }
            temp.delete()
            throw e
        }
    }
}
