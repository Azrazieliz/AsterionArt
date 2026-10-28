package com.asterion.art.video

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * Encode real Wan-generated Bitmap frames through Android's native AVC encoder.
 * No still-image animations, fake inference, or external ffmpeg/server.
 */
object NeuralMp4Encoder {
    private const val MIME=MediaFormat.MIMETYPE_VIDEO_AVC

    fun encode(frames:List<Bitmap>,file:File,fps:Int,cancelled:()->Boolean) {
        require(frames.size>=5){"Neural model produced insufficient video frames"}
        require(fps in 6..30)
        val width=frames.first().width
        val height=frames.first().height
        require(width in 128..1920 && height in 128..1920 &&
            width%2==0 && height%2==0)
        require(frames.all{!it.isRecycled&&it.width==width&&it.height==height}){
            "Neural video returned inconsistent frame dimensions"
        }
        val candidate=MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .firstOrNull{codec->
                codec.isEncoder&&codec.supportedTypes.any{it.equals(MIME,true)}&&
                    runCatching{codec.getCapabilitiesForType(MIME)
                        .colorFormats.contains(
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                    }.getOrDefault(false)
            }?:error("This device has no AVC YUV420 encoder for neural video")
        val codec=MediaCodec.createByCodecName(candidate.name)
        val temporary=File(file.path+".partial")
        temporary.parentFile?.mkdirs()
        var muxer:MediaMuxer?=null
        var started=false
        var track=-1
        var muxing=false
        val info=MediaCodec.BufferInfo()
        try {
            val format=MediaFormat.createVideoFormat(MIME,width,height).apply{
                setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE,(width*height*fps*0.15)
                    .toInt().coerceIn(700_000,12_000_000))
                setInteger(MediaFormat.KEY_FRAME_RATE,fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2)
            }
            codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            started=true
            muxer=MediaMuxer(temporary.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            fun drain(untilEos:Boolean):Boolean {
                var idle=0
                while(true) {
                    if(cancelled())throw java.util.concurrent.CancellationException()
                    val index=codec.dequeueOutputBuffer(info,12_000)
                    when(index) {
                        MediaCodec.INFO_TRY_AGAIN_LATER->{
                            if(!untilEos)return false
                            if(++idle>160)error("Neural AVC encoder did not finish")
                        }
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED->{
                            check(track<0){"AVC encoder changed format unexpectedly"}
                            track=requireNotNull(muxer).addTrack(codec.outputFormat)
                            requireNotNull(muxer).start()
                            muxing=true
                        }
                        else->if(index>=0) {
                            try{
                                if(info.size>0 && info.flags and
                                        MediaCodec.BUFFER_FLAG_CODEC_CONFIG==0){
                                    check(muxing){"AVC encoder emitted samples before format"}
                                    val buffer=requireNotNull(codec.getOutputBuffer(index))
                                    buffer.position(info.offset)
                                    buffer.limit(info.offset+info.size)
                                    requireNotNull(muxer).writeSampleData(track,buffer,info)
                                }
                                if(info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0)
                                    return true
                            }finally{codec.releaseOutputBuffer(index,false)}
                        }
                    }
                }
            }
            val pixels=IntArray(width*height)
            for((number,frame) in frames.withIndex()) {
                if(cancelled())throw java.util.concurrent.CancellationException()
                var index=-1
                var attempts=0
                while(index<0){
                    index=codec.dequeueInputBuffer(10_000)
                    if(index<0) {
                        drain(false)
                        require(++attempts<120){"AVC input queue is stalled"}
                    }
                }
                val image=codec.getInputImage(index)
                    ?:error("AVC encoder does not expose YUV input planes")
                try {
                    require(image.planes.size==3){"Unexpected AVC YUV pixel layout"}
                    frame.getPixels(pixels,0,width,0,0,width,height)
                    fillYuv(pixels,width,height,image.planes)
                }finally{image.close()}
                codec.queueInputBuffer(index,0,width*height*3/2,
                    number*1_000_000L/fps,0)
                drain(false)
            }
            var eosIndex=-1
            while(eosIndex<0) {
                eosIndex=codec.dequeueInputBuffer(12_000)
                if(eosIndex<0)drain(false)
            }
            codec.queueInputBuffer(eosIndex,0,0,
                frames.size*1_000_000L/fps,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            check(drain(true)){"AVC output ended without EOS"}
            check(muxing){"No AVC frames were encoded"}
        }catch(t:Throwable){
            temporary.delete()
            throw t
        }finally {
            runCatching{if(started)codec.stop()}
            runCatching{codec.release()}
            runCatching{if(muxing)muxer?.stop()}
            runCatching{muxer?.release()}
        }
        require(temporary.isFile&&temporary.length()>1024L) {
            "Neural model encoded no usable MP4"
        }
        if(file.exists())check(file.delete()){"Cannot replace previous video"}
        check(temporary.renameTo(file)){"Cannot commit encoded video"}
    }

    private fun fillYuv(pixels:IntArray,width:Int,height:Int,
        planes:Array<android.media.Image.Plane>) {
        val yPlane=planes[0].buffer
        val uPlane=planes[1].buffer
        val vPlane=planes[2].buffer
        fun luma(r:Int,g:Int,b:Int):Int =
            ((66*r+129*g+25*b+128 shr 8)+16).coerceIn(0,255)
        fun chromaU(r:Int,g:Int,b:Int):Int =
            ((-38*r-74*g+112*b+128 shr 8)+128).coerceIn(0,255)
        fun chromaV(r:Int,g:Int,b:Int):Int =
            ((112*r-94*g-18*b+128 shr 8)+128).coerceIn(0,255)
        for(y in 0 until height step 2)for(x in 0 until width step 2) {
            var r=0;var g=0;var b=0
            for(dy in 0..1)for(dx in 0..1) {
                val p=pixels[(y+dy)*width+x+dx]
                val rr=p shr 16 and 255
                val gg=p shr 8 and 255
                val bb=p and 255
                yPlane.put((y+dy)*planes[0].rowStride+
                    (x+dx)*planes[0].pixelStride,luma(rr,gg,bb).toByte())
                r+=rr;g+=gg;b+=bb
            }
            val uvPosU=(y/2)*planes[1].rowStride+(x/2)*planes[1].pixelStride
            val uvPosV=(y/2)*planes[2].rowStride+(x/2)*planes[2].pixelStride
            uPlane.put(uvPosU,chromaU(r/4,g/4,b/4).toByte())
            vPlane.put(uvPosV,chromaV(r/4,g/4,b/4).toByte())
        }
    }
}
