package com.asterion.art.enhancement

import android.content.Context
import android.net.Uri
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.engine.TechnicalImageScorer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CancellationException

data class AutomaticEnhancementResult(
    val uri:Uri,
    val stages:List<String>,
    val neuralSrApplied:Boolean,
    val toolboxApplied:Boolean
)

/**
 * Fully internal unattended enhancement after the final Artwork selection.
 *
 * RealSR/Real-ESRGAN runs first when packaged. In-app Image Toolbox handles
 * edge/tone finishing after neural SR; it also provides an in-app fallback
 * when the native SR binary is unavailable. An installed external proprietary
 * application has only an ACTION_SEND integration, NOT an autonomous API;
 * pretending that SuperImage Pro ran here would be incorrect.
 *
 * Every real intermediate remains checkpointed and source pixels are kept.
 */
class AutomaticImageEnhancer(private val app:Context) {
    suspend fun run(
        winner:Uri,
        checkpointDir:File,
        jobId:String,
        cancelled:()->Boolean,
        progress:(String)->Unit
    ):AutomaticEnhancementResult {
        checkpointDir.mkdirs()
        val stages=mutableListOf<String>()
        var source=winner
        var realSr=false
        var toolbox=false
        fun verifyStopped(){
            if(cancelled())throw CancellationException("Automatic enhancement cancelled")
        }
        verifyStopped()
        val neural=File(checkpointDir,"neural-realsr-x4.png")
        if(EmbeddedRealSr.available(app)) {
            progress("Automatic neural RealSR 4×")
            val data=if(neural.exists() && neural.length()>100L)
                neural.readBytes()
            else EmbeddedRealSr(app).enhance(
                source,4,128,cancelled,progress).also{
                    GeneratedImagePublisher.atomicWrite(neural,it)
                }
            verifyStopped()
            val uri=GeneratedImagePublisher.publish(
                app,jobId,97,data)
            stages+="embedded_real_esrgan_4x"
            source=uri
            realSr=true
        } else stages+="neural_sr_unavailable_internal_fallback"
        verifyStopped()
        progress("Automatic Image Toolbox finishing")
        val best=File(checkpointDir,"image-toolbox-finish.png")
        val bytes=if(best.exists()&&best.length()>100L)best.readBytes()
        else {
            val score=app.contentResolver.openInputStream(source)?.use{
                com.asterion.art.engine.TechnicalImageScorer.score(it.readBytes())
            } ?:error("Source image is inaccessible for automatic enhancement")
            val settings=ImageToolSettings(
                contrast=1.02f,saturation=1.0f,
                sharpen=if(score.detail<0.40)0.23f else 0.08f)
            AsterionImageToolbox.process(
                app,source,settings,cancelled,{_,status->progress(status)})
                .also{GeneratedImagePublisher.atomicWrite(best,it)}
        }
        verifyStopped()
        val uri=GeneratedImagePublisher.publish(app,jobId,98,bytes)
        stages+="in_app_image_toolbox_finishing"
        toolbox=true
        val receipt=JSONObject().apply {
            put("schema","asterion.automatic-enhancement.v1")
            put("winner",winner.toString())
            put("output",uri.toString())
            put("real_sr",realSr);put("toolbox",toolbox)
            put("superimage_pro_automatic",false)
            put("superimage_reason",
                "No unattended/background integration API has been validated")
            put("stages",JSONArray(stages))
            put("preserved_intermediates",true)
        }
        GeneratedImagePublisher.atomicWrite(File(checkpointDir,
            "automatic-enhancement.json"),receipt.toString(2).toByteArray())
        return AutomaticEnhancementResult(uri,stages,realSr,toolbox)
    }
}
