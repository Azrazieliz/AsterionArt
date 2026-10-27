package com.asterion.art.nativeengine

import android.content.Context
import com.asterion.art.engine.DreamModel
import com.asterion.art.engine.GeneratedImage
import com.asterion.art.engine.GenerationPass
import com.asterion.art.engine.LocalDreamBridge
import com.asterion.art.engine.StillImageEngine
import com.asterion.art.engine.TokenCapacity
import com.asterion.art.model.GeneratorSettings

/**
 * The native process is embedded in this APK. Only HTTP endpoints belonging
 * to THAT subprocess are used; no separate Local Dream app is required.
 */
class EmbeddedNpuEngine(context: Context): StillImageEngine {
    private val models=NativeModelLibrary(context)
    private val runner=EmbeddedNativeRunner(context)
    private val inferenceProtocol=LocalDreamBridge(EmbeddedNativeRunner.PORT)

    override fun installedModels(): List<DreamModel> =
        models.list().map { it.toDreamModel() }

    override fun prepare(model: DreamModel,width:Int,height:Int,
                         cancelled:()->Boolean) {
        val installed=models.list().firstOrNull { it.id==model.id }
            ?: error("QNN model is not installed in Asterion Art: "+model.id)
        runner.prepare(installed,width,height,cancelled)
    }

    override fun inspectText(prompt:String):TokenCapacity =
        inferenceProtocol.inspectText(prompt)

    override fun verifyText(prompt: String):TokenCapacity =
        inferenceProtocol.verifyText(prompt)

    override fun generate(pass:GenerationPass,settings:GeneratorSettings,
        width:Int,height:Int,previousPng:ByteArray?,seed:Long,
        cancelled:()->Boolean,progress:(Int,Int)->Unit,maskPng:ByteArray?,
        referencePngs:List<ByteArray>):GeneratedImage =
        inferenceProtocol.generate(pass,settings,width,height,previousPng,
            seed,cancelled,progress,maskPng,referencePngs)

    override fun release() { runner.release() }
}

object EmbeddedEngineFactory {
    fun open(context:Context):StillImageEngine =
        if(EmbeddedNativeRunner.isEmbedded(context)) EmbeddedNpuEngine(context)
        else LocalDreamBridge()
}
