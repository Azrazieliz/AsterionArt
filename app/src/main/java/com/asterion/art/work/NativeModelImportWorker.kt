package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.asterion.art.data.ArtRepository
import com.asterion.art.model.TaskKind
import com.asterion.art.model.TaskStatus
import com.asterion.art.nativeengine.NativeModelLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

/** Big ZIPs are unpacked by streaming, without an extra archive copy. */
class NativeModelImportWorker(context:Context,params:WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        fun enqueue(context:Context,uri:String,label:String):String {
            val repo=ArtRepository(context)
            val task=repo.recordTask("qnn-install",TaskKind.MODEL_IMPORT)
            val data=Data.Builder().putString("model_zip_uri",uri)
                .putString("model_label",label).putString("task_id",task.id).build()
            val request=OneTimeWorkRequestBuilder<NativeModelImportWorker>()
                .setInputData(data).addTag("asterion-native-import").build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-model-import-serial",ExistingWorkPolicy.APPEND_OR_REPLACE,request)
            return task.id
        }
    }
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val taskId=inputData.getString("task_id") ?: return@withContext Result.failure()
        val repo=ArtRepository(applicationContext)
        try {
            repo.updateTask(taskId,TaskStatus.RUNNING)
            val uri=inputData.getString("model_zip_uri")
                ?: error("QNN source ZIP is missing")
            val label=inputData.getString("model_label").orEmpty()
            val manager=applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                "asterion-native-import","Model imports",NotificationManager.IMPORTANCE_LOW))
            fun progress(bytes:Long):Notification=Notification.Builder(applicationContext,
                "asterion-native-import")
                .setContentTitle("Asterion Art · Model import")
                .setContentText("Unpacked "+bytes/1048576L+" MiB")
                .setOngoing(true).setOnlyAlertOnce(true)
                .setSmallIcon(android.R.drawable.stat_sys_download).build()
            setForeground(ForegroundInfo(7226,progress(0),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            var lastProgress=0L
            val record=NativeModelLibrary(applicationContext).importZip(
                Uri.parse(uri),label,
                progress={ bytes ->
                    if(bytes-lastProgress>=64L*1024*1024){
                        lastProgress=bytes
                        manager.notify(7226,progress(bytes))
                    }
                },
                stopped={isStopped}
            )
            repo.updateTask(taskId,TaskStatus.COMPLETED)
            Result.success(Data.Builder().putString("model_id",record.id).build())
        } catch(e:CancellationException) {
            repo.updateTask(taskId,TaskStatus.FAILED,"Model installation cancelled")
            Result.failure()
        } catch(e:Exception) {
            repo.updateTask(taskId,TaskStatus.FAILED,e.message ?: "Model installation failed")
            Result.failure()
        }
    }
}
