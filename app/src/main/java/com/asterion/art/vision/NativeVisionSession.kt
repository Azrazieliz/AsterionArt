package com.asterion.art.vision

import com.asterion.art.nativeengine.DeviceInferenceGate
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.math.max

/**
 * JNI to the pinned llama.cpp libmtmd multimodal pathway.
 *
 * A Gemma 3 2B GGUF + its matching vision projector are both REQUIRED.
 * The text-only Gemma3 planning engine is never claimed to see pixels.
 * Keep a single instance per assessment batch, close before QNN rendering.
 */
class NativeVisionSession(pair:VisionInstalledPair):Closeable {
    companion object {
        init {System.loadLibrary("asterion-vision")}
        private const val INPUT_MAX=512
        fun rgb(context:Context,uri:Uri):VisionFrame {
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            requireNotNull(context.contentResolver.openInputStream(uri)).use{
                BitmapFactory.decodeStream(it,null,bounds)
            }
            require(bounds.outWidth>0&&bounds.outHeight>0)
            var factor=1
            while(max(bounds.outWidth,bounds.outHeight)/factor>INPUT_MAX)
                factor*=2
            val options=BitmapFactory.Options().apply{inSampleSize=factor}
            val original=requireNotNull(
                context.contentResolver.openInputStream(uri)).use {
                BitmapFactory.decodeStream(it,null,options)
            } ?:error("Visual analysis input is not a decodable image")
            try {return rgb(original)} finally {original.recycle()}
        }
        fun rgb(bitmap:Bitmap):VisionFrame {
            require(bitmap.width>0&&bitmap.height>0)
            val ratio=minOf(1.0,INPUT_MAX.toDouble()/
                max(bitmap.width,bitmap.height))
            val w=max(8,(bitmap.width*ratio).toInt())
            val h=max(8,(bitmap.height*ratio).toInt())
            val scaled=Bitmap.createScaledBitmap(bitmap,w,h,true)
            try {
                val pixels=IntArray(w*h)
                scaled.getPixels(pixels,0,w,0,0,w,h)
                val rgb=ByteArray(w*h*3)
                for(index in pixels.indices) {
                    val p=pixels[index]
                    val a=p ushr 24 and 255
                    val r=p shr 16 and 255
                    val g=p shr 8 and 255
                    val b=p and 255
                    // Correctly composite transparent pixels onto white
                    // to preserve a clinical white-background reference.
                    rgb[index*3]=((r*a+255*(255-a))/255).toByte()
                    rgb[index*3+1]=((g*a+255*(255-a))/255).toByte()
                    rgb[index*3+2]=((b*a+255*(255-a))/255).toByte()
                }
                return VisionFrame(w,h,rgb)
            } finally {if(scaled!==bitmap)scaled.recycle()}
        }
    }
    private external fun nativeOpen(llm:String,projector:String,contextTokens:Int):Long
    private external fun nativeAnalyze(
        handle:Long,rgb:ByteArray,width:Int,height:Int,
        question:String,outputTokens:Int):String
    private external fun nativeClose(handle:Long)
    private var gateHeld=false
    private var handle:Long=0L
    init {
        DeviceInferenceGate.acquire()
        gateHeld=true
        try {
            handle=nativeOpen(pair.language.absolutePath,
                pair.projector.absolutePath,4096)
            require(handle!=0L) {
                "Could not initialize local Gemma 3 multimodal session"
            }
        } catch(error:Throwable) {
            gateHeld=false
            DeviceInferenceGate.release()
            throw error
        }
    }
    @Synchronized fun analyze(frame:VisionFrame,question:String,
                              outputTokens:Int=550):String {
        check(handle!=0L)
        require(frame.width in 8..768&&frame.height in 8..768)
        require(frame.rgb.size==frame.width*frame.height*3)
        return nativeAnalyze(handle,frame.rgb,frame.width,frame.height,
            question,outputTokens).also{require(it.isNotBlank())}
    }
    @Synchronized override fun close() {
        val current=handle
        handle=0
        try {
            if(current!=0L)nativeClose(current)
        } finally {
            if(gateHeld) {
                gateHeld=false
                DeviceInferenceGate.release()
            }
        }
    }
}
data class VisionFrame(val width:Int,val height:Int,val rgb:ByteArray)
