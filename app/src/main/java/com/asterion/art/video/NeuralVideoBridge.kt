package com.asterion.art.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.asterion.art.nativeengine.DeviceInferenceGate
import io.aatricks.llmedge.LLMEdge
import io.aatricks.llmedge.image.GenerationStreamEvent
import io.aatricks.llmedge.image.VideoGenerationRequest
import io.aatricks.llmedge.model.ModelSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

data class NeuralVideoClip(val file:File,val runId:String,val model:String)

/**
 * Completely in-APK Wan 2.2 TI2V diffusion through the pinned llmedge
 * stable-diffusion.cpp Android native runtime. The image is provided as
 * initImage (real pixel conditioning); video is decoded as generated frames,
 * then encoded to AVC by Android MediaCodec.
 */
class NeuralVideoBridge(private val context:Context) {
    fun available():Boolean=WanVideoModelPack.ready(context)!=null

    suspend fun generate(first:Uri,prompt:String,negative:String,seed:Long,
        seconds:Int,target:File,cancelled:()->Boolean,
        progress:(Int,Int)->Unit):NeuralVideoClip {
        require(prompt.isNotBlank()&&seconds in 1..5)
        val weights=WanVideoModelPack.ready(context)
            ?:error("Install the three verified Wan 2.2 components in Video Model Setup")
        val framesRequested=WanVideoRules.frameCount(seconds)
        val fps=WanVideoRules.outputFps(seconds)
        val init=readSource(first)
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
        DeviceInferenceGate.acquire(cancelled)
        val active=AtomicBoolean(true)
        var edge:LLMEdge?=null
        try {
            if(cancelled())throw CancellationException("Video request cancelled")
            val runtime=LLMEdge.create(context,scope)
            edge=runtime
            val watcher=Thread {
                try {
                    while(active.get()){
                        if(cancelled()){
                            runCatching{runtime.image.cancelGeneration()}
                            break
                        }
                        Thread.sleep(250L)
                    }
                }catch(_:InterruptedException){}
            }.apply{
                name="asterion-neural-video-cancel"
                isDaemon=true
                start()
            }
            try{
                var generated:List<Bitmap>?=null
                runtime.image.generateVideo(VideoGenerationRequest(
                    prompt=prompt,negative=negative,
                    width=init.width,height=init.height,
                    videoFrames=framesRequested,
                    steps=20,cfgScale=5f,seed=seed,
                    forceSequentialLoad=true,
                    initImage=init,strength=0.78f,
                    model=ModelSpec.LocalFile(weights.diffusion),
                    vae=ModelSpec.LocalFile(weights.vae),
                    textEncoder=ModelSpec.LocalFile(weights.encoder)
                )).collect{event->
                    if(cancelled()){
                        runtime.image.cancelGeneration()
                        throw CancellationException("Neural video cancelled")
                    }
                    when(event){
                        is GenerationStreamEvent.Progress->
                            progress(event.update.current,event.update.total)
                        is GenerationStreamEvent.Completed->generated=event.frames
                    }
                }
                if(cancelled())throw CancellationException()
                val frames=generated ?:error("Wan video model returned no frames")
                try{
                    require(frames.size>=5){
                        "Wan model returned "+frames.size+" frames; video needs at least five"
                    }
                    NeuralMp4Encoder.encode(frames,target,fps,cancelled)
                }finally{frames.forEach{if(!it.isRecycled)it.recycle()}}
                return NeuralVideoClip(target,"wan-local-"+seed,
                    "Wan 2.2 TI2V 5B Q4 · llmedge 0.4.7.2")
            }finally{
                active.set(false)
                watcher.interrupt()
            }
        }finally{
            active.set(false)
            try{edge?.close()}finally{
                scope.cancel()
                init.recycle()
                DeviceInferenceGate.release()
            }
        }
    }

    private fun readSource(uri:Uri):Bitmap{
        val app=context.applicationContext
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        requireNotNull(app.contentResolver.openInputStream(uri)).use{
            BitmapFactory.decodeStream(it,null,bounds)
        }
        require(bounds.outWidth>0&&bounds.outHeight>0){"Video source is not an image"}
        var sample=1
        while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1024)sample*=2
        val opts=BitmapFactory.Options().apply{inSampleSize=sample}
        val original=requireNotNull(app.contentResolver.openInputStream(uri)).use{
            BitmapFactory.decodeStream(it,null,opts)
        }?:error("Video source pixels cannot be decoded")
        // Wan model requires dimensions divisible by 64. Keep the source's
        // actual ratio instead of forcing every character into a square.
        val scale=512.0/maxOf(original.width,original.height)
        val width=((original.width*scale/64.0).toInt().coerceIn(4,15))*64
        val height=((original.height*scale/64.0).toInt().coerceIn(4,15))*64
        return try{Bitmap.createScaledBitmap(original,width,height,true)}
        finally{original.recycle()}
    }
}
