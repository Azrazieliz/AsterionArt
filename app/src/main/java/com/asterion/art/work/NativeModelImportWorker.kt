package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.provider.OpenableColumns
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
import com.asterion.art.nativeengine.OriginalCheckpointLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

/** Big ZIPs are unpacked by streaming, without an extra archive copy. */
class NativeModelImportWorker(context:Context,params:WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        fun enqueue(context:Context,uri:String,label:String, family: String = "auto"):String {
            val repo=ArtRepository(context)
            val task=repo.recordTask("qnn-install",TaskKind.MODEL_IMPORT)
            val data=Data.Builder().putString("model_zip_uri",uri)
                .putString("model_label",label).putString("family",family)
                .putString("task_id",task.id).build()
            val request=OneTimeWorkRequestBuilder<NativeModelImportWorker>()
                .setInputData(data).addTag("asterion-native-import")
                .addTag("asterion-model-import-"+task.id).build()
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
            val original=OriginalCheckpointLibrary(applicationContext)
                .registerIfOriginal(Uri.parse(uri),label)
            if(original!=null) {
                repo.updateTaskProgress(taskId,100,100,
                    "Indexed "+original.format+" · conversion required for QNN",
                    original.sizeBytes.coerceAtLeast(0L),
                    original.sizeBytes.coerceAtLeast(0L))
                repo.updateTask(taskId,TaskStatus.COMPLETED)
                return@withContext Result.success(Data.Builder()
                    .putString("checkpoint_id",original.id)
                    .putString("runnable","false").build())
            }
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
            val totalBytes = applicationContext.contentResolver.query(Uri.parse(uri),
                arrayOf(OpenableColumns.SIZE), null, null, null)?.use { rows ->
                if (rows.moveToFirst()) rows.getLong(0) else 0L
            } ?: 0L
            repo.updateTaskProgress(taskId,0,if(totalBytes>0)100 else 0,
                "Checking model format",0L,totalBytes)
            var lastProgress=0L
            val record=NativeModelLibrary(applicationContext).importZip(
                Uri.parse(uri),label,
                progress={ compressedBytes, unpackedBytes ->
                    if (compressedBytes-lastProgress >= 4L*1024*1024) {
                        lastProgress=compressedBytes
                        repo.updateTaskProgress(taskId,
                            if (totalBytes > 0) (compressedBytes*100/totalBytes).toInt().coerceIn(0, 99)
                            else 0, if (totalBytes > 0) 100 else 0,
                            "Unpacking QNN files · "+(unpackedBytes/1048576L)+" MiB extracted",
                            compressedBytes,totalBytes)
                        manager.notify(7226,progress(unpackedBytes))
                    }
                },
                stopped={isStopped},
                family=inputData.getString("family"),
                onPhase={ phase ->
                    repo.updateTaskProgress(taskId,
                        if (totalBytes > 0) (lastProgress * 100 / totalBytes).toInt().coerceIn(0, 99) else 0,
                        if (totalBytes > 0) 100 else 0,
                        phase, lastProgress, totalBytes)
                }
            )
            repo.updateTaskProgress(taskId, 100, 100, "Validated and installed",
                totalBytes,totalBytes)
            repo.updateTask(taskId,TaskStatus.COMPLETED)
            Result.success(Data.Builder().putString("model_id",record.id).build())
        } catch(e:CancellationException) {
            repo.updateTask(taskId,TaskStatus.CANCELLED,"Model installation cancelled")
            Result.failure()
        } catch(e:Exception) {
            repo.updateTask(taskId,TaskStatus.FAILED,e.message ?: "Model installation failed")
            Result.failure()
        }
    }
}
