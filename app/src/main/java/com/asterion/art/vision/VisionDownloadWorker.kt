package com.asterion.art.vision

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Free public model download. User initiates installation explicitly once;
 * the models thereafter run entirely offline. No paid API, telemetry or upload.
 */
class VisionDownloadWorker(context:Context,params:WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        const val UNIQUE="asterion-local-qwen3vl-install"
        private const val CHANNEL="asterion-vision-download"
        fun enqueue(context:Context) {
            val request=OneTimeWorkRequestBuilder<VisionDownloadWorker>()
                .addTag("asterion-vision-install").build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,ExistingWorkPolicy.KEEP,request)
        }
    }
    private suspend fun show(label:String,received:Long,total:Long) {
        val manager=applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,
            "Asterion visual model installation",NotificationManager.IMPORTANCE_LOW))
        val percent=(100.0*received/total).toInt().coerceIn(0,100)
        val note=Notification.Builder(applicationContext,CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Installing offline Asterion vision model")
            .setContentText(label+" · "+percent+"%")
            .setOngoing(true).setOnlyAlertOnce(true).build()
        setForeground(ForegroundInfo(7266,note,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
        setProgress(Data.Builder().putString("file",label)
            .putInt("percent",percent).putLong("downloaded",received)
            .putLong("expected_maximum",total).build())
    }
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val store=VisionModelStore(applicationContext)
        try {
            for(model in VisionCatalog.all) {
                if(isStopped)throw CancellationException()
                if(store.path(model)!=null)continue
                val target=store.target(model)
                val partial=File(target.path+".partial")
                var received=if(partial.isFile)partial.length() else 0L
                require(received in 0L..model.bytesMax)
                val conn=URL(model.url).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects=true
                conn.connectTimeout=25_000
                conn.readTimeout=75_000
                conn.setRequestProperty("Accept-Encoding","identity")
                if(received>0)conn.setRequestProperty("Range","bytes="+received+"-")
                try {
                    val response=conn.responseCode
                    require(response==200||response==206) {
                        "Official Hugging Face download HTTP "+response
                    }
                    val resumed=response==206&&received>0
                    if(resumed) {
                        val value=conn.getHeaderField("Content-Range").orEmpty()
                        require(value.startsWith("bytes "+received+"-")) {
                            "Server did not resume at the stored byte offset"
                        }
                    } else received=0L
                    FileOutputStream(partial,resumed).buffered().use{output->
                        conn.inputStream.use{input->
                            val buffer=ByteArray(1024*1024)
                            var last=received
                            while(true) {
                                if(isStopped)throw CancellationException()
                                val count=input.read(buffer)
                                if(count<0)break
                                received+=count
                                require(received<=model.bytesMax){
                                    "Vision download exceeds bounded official size"
                                }
                                output.write(buffer,0,count)
                                if(received-last>8_000_000) {
                                    show(model.name,received,model.bytesMax)
                                    last=received
                                }
                            }
                        }
                    }
                } finally {conn.disconnect()}
                show("Verifying SHA-256 · "+model.name,model.bytesMax,model.bytesMax)
                try {
                    store.verifyAndCommit(model,partial)
                } catch(e:Exception) {
                    // A corrupted complete file cannot be resumed into a valid
                    // GGUF. Delete only the app's partial copy and permit retry.
                    if(e.message.orEmpty().contains("SHA-256",true)||
                        e.message.orEmpty().contains("Not a GGUF",true))
                        partial.delete()
                    throw e
                }
            }
            require(store.installed()!=null){"Both neural components are required"}
            Result.success()
        } catch(e:CancellationException) {
            // .partial remains resumable; no remote service inference involved.
            Result.failure(Data.Builder().putString("error","Cancelled").build())
        } catch(e:Exception) {
            Result.failure(Data.Builder()
                .putString("error",e.message?:"Offline vision model unavailable").build())
        }
    }
}
