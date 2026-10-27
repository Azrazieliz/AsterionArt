package com.asterion.art.brain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Streams only pinned, SHA-256 verified GGUF files; resumes interrupted downloads. */
class BrainDownloadWorker(context:Context,params:WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        private const val CHANNEL="asterion-brain-weights"
        fun enqueue(context:Context,modelId:String,sourceUri:String?=null) {
            require(BrainCatalog.get(modelId)!=null)
            val data=Data.Builder().putString("model",modelId)
                .putString("uri",sourceUri).build()
            val request=OneTimeWorkRequestBuilder<BrainDownloadWorker>()
                .setInputData(data).addTag("asterion-brain-download").addTag(modelId).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-brain-"+modelId,ExistingWorkPolicy.KEEP,request)
        }
    }
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val model=BrainCatalog.get(inputData.getString("model").orEmpty())
            ?:return@withContext Result.failure()
        val store=BrainModelStore(applicationContext)
        if(store.location(model)!=null)return@withContext Result.success()
        val manager=applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL,"Asterion reasoning model downloads",NotificationManager.IMPORTANCE_LOW))
        suspend fun show(received:Long) {
            val percentage=(100*received/model.bytes).toInt().coerceIn(0,100)
            val notification=Notification.Builder(applicationContext,CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Installing "+model.label)
                .setContentText(percentage.toString()+"% · verifying complete GGUF")
                .setOngoing(true).setOnlyAlertOnce(true).build()
            setForeground(ForegroundInfo(7282,notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            setProgress(Data.Builder().putLong("bytes",received)
                .putLong("total",model.bytes).putInt("percent",percentage).build())
        }
        val target=store.target(model)
        val partial=File(target.path+".partial")
        try {
            val uri=inputData.getString("uri")
            if(uri.isNullOrBlank()) {
                val received=if(partial.exists())partial.length() else 0L
                require(received in 0..model.bytes){"Invalid partial download"}
                val conn=URL(model.downloadUrl).openConnection() as HttpURLConnection
                conn.connectTimeout=25_000
                conn.readTimeout=60_000
                conn.instanceFollowRedirects=true
                conn.setRequestProperty("Accept-Encoding","identity")
                if(received>0)conn.setRequestProperty("Range","bytes="+received+"-")
                try {
                    val response=conn.responseCode
                    require(response in listOf(200,206)){"Hugging Face HTTP "+response}
                    if(response==206) {
                        val range=conn.getHeaderField("Content-Range").orEmpty()
                        require(range.startsWith("bytes "+received+"-")) {
                            "Server resumed at a different byte offset"
                        }
                    }
                    val append=response==206&&received>0
                    var total=if(append)received else 0L
                    conn.inputStream.use { input ->
                        java.io.FileOutputStream(partial,append).buffered().use { output ->
                            val buffer=ByteArray(1024*1024)
                            var last=0L
                            while(true) {
                                if(isStopped)throw CancellationException("Download cancelled")
                                val n=input.read(buffer)
                                if(n<0)break
                                total+=n
                                require(total<=model.bytes){"Download exceeded expected size"}
                                output.write(buffer,0,n)
                                if(total-last>=8L*1024*1024){
                                    show(total)
                                    last=total
                                }
                            }
                        }
                    }
                    require(total==model.bytes) {
                        "Incomplete GGUF: "+total+" / "+model.bytes
                    }
                } finally {conn.disconnect()}
            } else {
                // Import from SAF, without ever deleting the original file.
                val source=applicationContext.contentResolver
                    .openInputStream(Uri.parse(uri)) ?: error("Cannot read selected GGUF")
                source.use { input ->
                    var total=0L
                    partial.outputStream().use { output ->
                        val bytes=ByteArray(1024*1024)
                        while(true) {
                            if(isStopped)throw CancellationException()
                            val n=input.read(bytes)
                            if(n<0)break
                            total+=n
                            require(total<=model.bytes){"Original exceeds expected GGUF size"}
                            output.write(bytes,0,n)
                            if(total%(8L*1024*1024)<1024*1024)show(total)
                        }
                    }
                    require(total==model.bytes){"Imported GGUF size mismatch"}
                }
            }
            show(model.bytes)
            store.verifyAndCommit(model,partial)
            Result.success()
        } catch(e:CancellationException) {
            // Retain partial download to resume after cancellation.
            Result.failure(Data.Builder().putString("message","Cancelled").build())
        } catch(e:Exception) {
            Result.failure(Data.Builder()
                .putString("message",e.message?:"GGUF import failed").build())
        }
    }
}
