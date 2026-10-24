package com.asterion.art.enhancement
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/** In-app MIT-licensed RealSR/Real-ESRGAN neural inference, NOT SuperImage Pro. */
class EmbeddedRealSr(private val context: Context) {
    companion object {
        fun available(context: Context) =
            File(context.applicationInfo.nativeLibraryDir, "libasterion_realsr_cli.so").isFile
    }

    private fun install(): File {
        val root=File(context.filesDir,"asterion-realsr-native").apply { mkdirs() }
        val version=context.packageManager.getPackageInfo(context.packageName,0).lastUpdateTime
        for(name in listOf("libncnn.so","libc++_shared.so","libomp.so",
            "models-Real-ESRGAN-anime/x4.bin","models-Real-ESRGAN-anime/x4.param",
            "models-Real-ESRGANv3-anime/x2.bin","models-Real-ESRGANv3-anime/x2.param")) {
            val target=File(root,name)
            target.parentFile?.mkdirs()
            if(!target.isFile || target.lastModified()<version) {
                val tmp=File(root,name+".partial")
                context.assets.open("realsr/"+name).use { source ->
                    FileOutputStream(tmp).use { targetStream ->
                        source.copyTo(targetStream,512*1024)
                        targetStream.fd.sync()
                    }
                }
                check(tmp.renameTo(target)) { "RealSR extraction failed: "+name }
            }
        }
        return root
    }

    fun enhance(original:Uri,scale:Int=4,tile:Int=128,
                stop:()->Boolean,progress:(String)->Unit):ByteArray {
        require(scale in setOf(2,4) && tile in 32..512)
        check(available(context)) { "In-app RealSR engine not packaged" }
        val runtime=install()
        val work=File(context.cacheDir,"realsr-"+System.nanoTime())
        check(work.mkdirs())
        val input=File(work,"input.png")
        val output=File(work,"output.png")
        var process:Process?=null
        try {
            progress("Reading source")
            context.contentResolver.openInputStream(original)?.use { stream ->
                FileOutputStream(input).use { stream.copyTo(it,512*1024) }
            } ?: error("Original artwork is no longer accessible")
            require(input.length()>100L)
            val model=File(runtime,if(scale==2)
                "models-Real-ESRGANv3-anime" else "models-Real-ESRGAN-anime")
            val exe=File(context.applicationInfo.nativeLibraryDir,"libasterion_realsr_cli.so")
            val args=listOf(exe.absolutePath,"-i",input.absolutePath,
                "-o",output.absolutePath,"-s",scale.toString(),
                "-t",tile.toString(),"-m",model.absolutePath,"-g","0","-j","1:2:1")
            progress("Neural super-resolution "+scale+"×")
            val log=File(work,"runtime.log")
            process=ProcessBuilder(args).apply {
                directory(work)
                redirectErrorStream(true)
                redirectOutput(ProcessBuilder.Redirect.to(log))
                environment()["LD_LIBRARY_PATH"]=runtime.absolutePath+":"+
                    context.applicationInfo.nativeLibraryDir+":/system/lib64:/vendor/lib64"
            }.start()
            while(process.isAlive) {
                if(stop() || Thread.currentThread().isInterrupted)
                    throw CancellationException("Enhancement interrupted")
                Thread.sleep(500)
            }
            if(process.exitValue()!=0)
                error("RealSR error "+process.exitValue()+": "+
                    log.takeIf { it.isFile }?.readText()?.takeLast(500).orEmpty())
            require(output.isFile && output.length()>100L) { "Neural upscaler wrote no image" }
            progress("Enhancement complete")
            return output.readBytes()
        } finally {
            if(process?.isAlive==true) {
                process.destroy()
                if(!process.waitFor(2,TimeUnit.SECONDS)) process.destroyForcibly()
            }
            work.deleteRecursively()
        }
    }
}
