package com.asterion.art.brain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.*
import com.asterion.art.vision.VisionCatalog
import com.asterion.art.vision.VisionModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CancellationException

/**
 * Provision the APK's pre-verified Qwen3 reasoning and Qwen3-VL vision weights.
 * This does not download from a network and NEVER contains image checkpoints.
 */
class EmbeddedInferenceInstallWorker(ctx:Context,params:WorkerParameters):
    CoroutineWorker(ctx,params) {
    companion object {
        const val UNIQUE="asterion-offline-inference-asset-install"
        private const val CHANNEL="asterion-offline-inference"
        fun enqueueIfBundled(context:Context) {
            val files=context.assets.list("inference").orEmpty()
            val mandatory=listOf(BrainCatalog.models.first().filename)+
                VisionCatalog.all.map{it.name}
            if(!mandatory.all{it in files})return
            val brain=BrainModelStore(context)
            val vision=VisionModelStore(context)
            if(brain.location(BrainCatalog.models.first())!=null&&
                vision.installed()!=null)return
            val request=OneTimeWorkRequestBuilder<EmbeddedInferenceInstallWorker>()
                .addTag("asterion-embedded-inference")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,ExistingWorkPolicy.KEEP,request)
        }
    }
    override suspend fun doWork():Result=withContext(Dispatchers.IO){
        val app=applicationContext
        val brains=BrainModelStore(app)
        val visions=VisionModelStore(app)
        val entries=listOf(
            BrainCatalog.models.first().filename to BrainCatalog.models.first().bytes
        )+VisionCatalog.all.map{it.name to it.bytesMax}
        val manager=app.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,
            "Installing bundled offline inference models",NotificationManager.IMPORTANCE_LOW))
        suspend fun status(label:String,completed:Long,total:Long) {
            val percent=(completed*100/total).toInt().coerceIn(0,100)
            val text="Local model setup: "+percent+"%"
            val note=Notification.Builder(app,CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Preparing Asterion reasoning and vision")
                .setContentText(text+" · "+label).setOngoing(true).build()
            setForeground(ForegroundInfo(7299,note,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            setProgress(Data.Builder().putString("stage",label)
                .putInt("percent",percent).build())
        }
        try {
            val mandatory=entries.map{it.first}
            require(mandatory.all{it in app.assets.list("inference").orEmpty()}){
                "APK does not contain the complete inference model pack"
            }
            val total=entries.sumOf{it.second}
            var done=0L
            for((name,maxBytes) in entries){
                if(isStopped)throw CancellationException()
                val brain=BrainCatalog.models.first().takeIf{it.filename==name}
                val vision=VisionCatalog.all.firstOrNull{it.name==name}
                val exists=if(brain!=null)brains.location(brain)!=null
                    else visions.path(requireNotNull(vision))!=null
                if(exists){done+=maxBytes;continue}
                val target=if(brain!=null)brains.target(brain)
                    else visions.target(requireNotNull(vision))
                val partial=File(target.path+".partial")
                target.parentFile?.mkdirs()
                // Asset extraction is resumable and offset-accurate. A failed
                // integrity verification never promotes the partial file.
                var offset=if(partial.exists())partial.length() else 0L
                if(offset>maxBytes){partial.delete();offset=0L}
                val stream=app.assets.open("inference/"+name)
                stream.use{input->
                    var skipped=0L
                    while(skipped<offset){
                        val n=input.skip(offset-skipped)
                        if(n<=0){partial.delete();error("Cannot resume embedded model")}
                        skipped+=n
                    }
                    FileOutputStream(partial,offset>0).use{output->
                        val buffer=ByteArray(512*1024)
                        var last=offset
                        while(true){
                            if(isStopped)throw CancellationException()
                            val n=input.read(buffer)
                            if(n<0)break
                            output.write(buffer,0,n)
                            offset+=n
                            require(offset<=maxBytes) {
                                "Bundled inference asset exceeds verified maximum"
                            }
                            if(offset-last>8L*1024*1024){
                                status(name,done+offset,total)
                                last=offset
                            }
                        }
                        output.fd.sync()
                    }
                }
                status("Verifying "+name,done+offset,total)
                if(brain!=null) {
                    brains.verifyAndCommit(brain,partial)
                    if(brains.selected()==null)brains.select(brain.id)
                }else visions.verifyAndCommit(requireNotNull(vision),partial)
                done+=maxBytes
            }
            require(brains.selected()!=null&&visions.installed()!=null){
                "Offline reasoning or visual model not ready"
            }
            status("All local inference models ready",total,total)
            Result.success()
        }catch(e:CancellationException){
            Result.failure(Data.Builder().putString("error","Bundled setup interrupted").build())
        }catch(e:Throwable){
            Result.failure(Data.Builder().putString("error",
                e.message?:"Bundled inference model installation failed").build())
        }
    }
}
