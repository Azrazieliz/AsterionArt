package com.asterion.art.video

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Concatenates genuine MP4 tracks without re-encoding. Preserves *all* video
 * and audio tracks, not just the first video track, and refuses incompatible
 * segment streams rather than generating a partially corrupted export.
 *
 * This is timeline assembly, NOT inference or simultaneous multi-keyframe
 * image conditioning.
 */
object Mp4SegmentJoiner {
    private fun mime(format:MediaFormat):String =
        requireNotNull(format.getString(MediaFormat.KEY_MIME))
    private fun integer(format:MediaFormat,key:String):Int? =
        if(format.containsKey(key)) format.getInteger(key) else null
    private fun codecBytes(format:MediaFormat,key:String):List<Byte>? {
        val buffer=format.getByteBuffer(key)?.duplicate()?:return null
        val bytes=ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes.toList()
    }
    private fun matching(a:MediaFormat,b:MediaFormat):Boolean {
        if(mime(a)!=mime(b))return false
        val keys=if(mime(a).startsWith("video/"))
            listOf(MediaFormat.KEY_WIDTH,MediaFormat.KEY_HEIGHT)
        else listOf(MediaFormat.KEY_SAMPLE_RATE,MediaFormat.KEY_CHANNEL_COUNT)
        return keys.all{integer(a,it)==integer(b,it)} &&
            (0..2).all{n->codecBytes(a,"csd-"+n)==codecBytes(b,"csd-"+n)}
    }
    private data class Expected(val format:MediaFormat,val muxTrack:Int)

    fun join(parts:List<File>,target:File):File {
        require(parts.isNotEmpty()&&parts.all{it.isFile&&it.length()>1024L})
        if(parts.size==1) {
            parts.single().copyTo(target,overwrite=true)
            return target
        }
        target.parentFile?.mkdirs()
        val temp=File(target.absolutePath+".tmp")
        if(temp.exists())check(temp.delete())
        var muxer:MediaMuxer?=null
        var started=false
        try {
            val reference=MediaExtractor()
            val formats=try {
                reference.setDataSource(parts.first().absolutePath)
                (0 until reference.trackCount).map{
                    reference.getTrackFormat(it)
                }.filter{ mime(it).startsWith("video/") ||
                          mime(it).startsWith("audio/") }
            } finally { reference.release() }
            require(formats.any{mime(it).startsWith("video/")}) {
                "No video track in first segment"
            }
            val output=MediaMuxer(temp.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer=output
            val specs=formats.map {Expected(it,output.addTrack(it))}
            val metadata=MediaMetadataRetriever()
            try {
                metadata.setDataSource(parts.first().absolutePath)
                val rotation=metadata.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
                if(rotation!=null && rotation in listOf(90,180,270))
                    output.setOrientationHint(rotation)
            } finally {metadata.release()}
            output.start()
            started=true
            val buffer=ByteBuffer.allocateDirect(8*1024*1024)
            val info=MediaCodec.BufferInfo()
            val lastPts=LongArray(specs.size){-1L}
            var timelineOffsetUs=0L
            parts.forEachIndexed { segmentNumber,file ->
                val extractor=MediaExtractor()
                try {
                    extractor.setDataSource(file.absolutePath)
                    val indices=mutableSetOf<Int>()
                    val extractionToMux=mutableMapOf<Int,Int>()
                    specs.forEachIndexed { logical,expected ->
                        val matched=(0 until extractor.trackCount).firstOrNull { index ->
                            index !in indices &&
                                matching(expected.format,extractor.getTrackFormat(index))
                        } ?:error("Segment "+(segmentNumber+1)+
                            " has incompatible/missing video or audio tracks")
                        indices+=matched
                        extractionToMux[matched]=logical
                        extractor.selectTrack(matched)
                    }
                    val totalTracks=(0 until extractor.trackCount).count{
                        val type=mime(extractor.getTrackFormat(it))
                        type.startsWith("video/")||type.startsWith("audio/")
                    }
                    require(totalTracks==specs.size) {
                        "Segment "+(segmentNumber+1)+" contains extra tracks; cannot discard audio"
                    }
                    val segmentStartUs=extractor.sampleTime
                    require(segmentStartUs>=0L){"Empty MP4 segment"}
                    var wrote=0
                    var segmentEndUs=timelineOffsetUs
                    while(true) {
                        val trackIndex=extractor.sampleTrackIndex
                        if(trackIndex<0)break
                        val logical=extractionToMux[trackIndex]
                            ?:error("Unexpected MP4 track during extraction")
                        buffer.clear()
                        val bytes=extractor.readSampleData(buffer,0)
                        if(bytes<0)break
                        require(bytes>0&&bytes<=buffer.capacity()) {
                            "Empty/oversized MP4 packet; safe streaming reassembly unavailable"
                        }
                        val time=VideoTimelineMath.presentationTime(
                            extractor.sampleTime,segmentStartUs,timelineOffsetUs,lastPts[logical])
                        lastPts[logical]=time
                        info.offset=0
                        info.size=bytes
                        info.presentationTimeUs=time
                        info.flags=extractor.sampleFlags
                        output.writeSampleData(specs[logical].muxTrack,buffer,info)
                        segmentEndUs=maxOf(segmentEndUs,time)
                        wrote++
                        if(!extractor.advance())break
                    }
                    require(wrote>0){"Segment "+(segmentNumber+1)+" contains no samples"}
                    val videoFps=specs.firstOrNull{
                        mime(it.format).startsWith("video/")
                    }?.format?.let{integer(it,MediaFormat.KEY_FRAME_RATE)}
                        ?.coerceIn(1,240)?:24
                    val audioExtra=specs.filter{mime(it.format).startsWith("audio/")}
                        .mapNotNull{integer(it.format,MediaFormat.KEY_SAMPLE_RATE)}
                        .maxOfOrNull{(1024L*1_000_000L/it.coerceAtLeast(1)).coerceAtLeast(1L)}?:0L
                    timelineOffsetUs=VideoTimelineMath.nextStart(
                        timelineOffsetUs,segmentEndUs,
                        maxOf(1_000_000L/videoFps,audioExtra))
                } finally {extractor.release()}
            }
            output.stop()
            started=false
            output.release()
            muxer=null
            require(temp.length()>1024L){"MP4 join produced an empty result"}
            if(target.exists())check(target.delete())
            check(temp.renameTo(target)){"Could not finalize joined MP4"}
            return target
        } catch(error:Throwable) {
            if(started)runCatching{muxer?.stop()}
            runCatching{muxer?.release()}
            temp.delete()
            throw error
        }
    }
}

object VideoTimelineMath {
    fun presentationTime(sourceUs:Long,startUs:Long,offsetUs:Long,lastUs:Long):Long {
        require(sourceUs>=0&&startUs>=0&&offsetUs>=0&&lastUs>=-1)
        return maxOf(offsetUs+(sourceUs-startUs).coerceAtLeast(0L),lastUs+1)
    }
    fun nextStart(offsetUs:Long,maximumOutputPts:Long,nominalFrameUs:Long):Long {
        require(offsetUs>=0&&maximumOutputPts>=offsetUs&&nominalFrameUs>0)
        return maximumOutputPts+nominalFrameUs
    }
}
