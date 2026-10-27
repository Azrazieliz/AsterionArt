package com.asterion.art.nativeengine

import android.content.Context
import android.app.ActivityManager
import com.asterion.art.engine.BackendUnavailable
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Local Dream ARM64 executable lives in THIS APK and runs in THIS app sandbox.
 * The NPU runtime and imported model packages are also owned by Asterion Art.
 */
class EmbeddedNativeRunner(private val context: Context) {
    private var process: Process? = null
    private var currentSignature: String? = null

    companion object {
        private const val BINARY = "libstable_diffusion_core.so"
        const val PORT = 18081

        fun isEmbedded(context: Context): Boolean =
            File(context.applicationInfo.nativeLibraryDir,BINARY).isFile

        private fun healthy(): Boolean {
            val c = URL("http://127.0.0.1:" + PORT + "/health")
                .openConnection() as HttpURLConnection
            return try {
                c.connectTimeout=800
                c.readTimeout=800
                c.responseCode == 200
            } catch (_: Exception) {
                false
            } finally { c.disconnect() }
        }
    }

    private fun prepareLibraries(): File {
        val runtime = File(context.filesDir,"asterion-qnn-runtime").also { it.mkdirs() }
        val present=context.assets.list("qnnlibs").orEmpty()
        if (!present.contains("libQnnHtp.so"))
            throw BackendUnavailable("Native QNN runtime missing from app. Rebuild with pinned upstream binary.")
        val modified=context.packageManager.getPackageInfo(context.packageName,0).lastUpdateTime
        for (category in listOf("qnnlibs","ditlibs")) {
            for (name in context.assets.list(category).orEmpty()) {
                require(name.matches(Regex("[A-Za-z0-9._-]+")))
                val file=File(runtime,name)
                val size=context.assets.open(category+"/"+name).use { it.available().toLong() }
                if (!file.exists() || file.length()!=size || file.lastModified()<modified) {
                    val temp=File(runtime,name+".part")
                    context.assets.open(category+"/"+name).use { src ->
                        temp.outputStream().use { src.copyTo(it,256*1024) }
                    }
                    require(temp.renameTo(file)) { "Unable to install native library: "+name }
                }
                file.setReadable(true,true)
                file.setExecutable(true,true)
            }
        }
        return runtime
    }

    @Synchronized fun prepare(model: InstalledQnnModel, width:Int,height:Int,
                              cancelled:()->Boolean) {
        require(width in 256..2048 && height in 256..2048)
        val profile=NativeInferenceProfiles.get(context)
        val signature=model.id+":"+width+":"+height+":"+profile.name
        if (process?.isAlive == true && currentSignature==signature && healthy()) return
        release()
        val executable=File(context.applicationInfo.nativeLibraryDir,BINARY)
        if (!executable.isFile) throw BackendUnavailable(
            "This Asterion Art APK has no embedded QNN executable")
        val libs=prepareLibraries()
        val command=mutableListOf(
            executable.absolutePath,"--type",model.backend,"--model_dir",
            model.root.absolutePath,"--port",PORT.toString()
        )
        when(model.backend) {
            "anima","sdxl" -> {
                val memory=ActivityManager.MemoryInfo()
                (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .getMemoryInfo(memory)
                val decision=NativeInferenceProfiles.decide(
                    model.backend,profile,memory.totalMem,memory.availMem)
                command+=listOf("--lib_dir",libs.absolutePath)
                if(decision.lowRam) command+="--lowram"
                if(decision.sequentialDiT) command+="--anima_seq_dit"
            }
            "zimage","klein","qwen21","krea2turbo" -> {
                val dit=File(context.applicationInfo.nativeLibraryDir,"libdit_engine.so")
                if(!dit.isFile) throw BackendUnavailable("DiT native engine missing")
                command+=listOf("--lib_dir",dit.parentFile!!.absolutePath,
                    "--qnn_lib_dir",libs.absolutePath)
            }
            else -> command+=listOf("--lib_dir",libs.absolutePath)
        }
        val appNativeDir=File(context.applicationInfo.nativeLibraryDir)
        val libPath=listOf(libs.absolutePath,appNativeDir.absolutePath,
            "/system/lib64","/vendor/lib64","/vendor/lib64/egl").joinToString(":")
        val builder=ProcessBuilder(command).apply {
            directory(appNativeDir)
            redirectErrorStream(true)
            val log=File(context.cacheDir,"asterion-native-last.log")
            if(log.length()>2_000_000)log.delete()
            redirectOutput(ProcessBuilder.Redirect.appendTo(log))
            environment()["LD_LIBRARY_PATH"]=libPath
            environment()["DSP_LIBRARY_PATH"]=libs.absolutePath
            environment()["ADSP_LIBRARY_PATH"]=listOf(libs.absolutePath,
                "/vendor/lib/rfsa/adsp","/vendor/dsp/cdsp","/dsp").joinToString(";")
        }
        try { process=builder.start() }
        catch (e: Exception) {
            throw BackendUnavailable("Failed to execute embedded engine: "+e.message)
        }
        val deadline=System.nanoTime()+180_000_000_000L
        while (System.nanoTime()<deadline) {
            if(cancelled()) {release();throw CancellationException()}
            if(process?.isAlive!=true) {
                val log=File(context.cacheDir,"asterion-native-last.log")
                val last=log.takeIf { it.isFile }?.readText()?.takeLast(1200).orEmpty()
                release()
                throw BackendUnavailable("Native engine exited unexpectedly. "+last)
            }
            if(healthy()) {
                currentSignature=signature
                return
            }
            Thread.sleep(500)
        }
        release()
        throw BackendUnavailable("Embedded NPU engine did not become healthy")
    }

    @Synchronized fun release() {
        val old=process
        process=null
        currentSignature=null
        old?.destroy()
        if(old?.isAlive==true) {
            old.waitFor(2,TimeUnit.SECONDS)
            if(old.isAlive) old.destroyForcibly()
        }
    }
}
