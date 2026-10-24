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
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.enhancement.AsterionImageToolbox
import com.asterion.art.enhancement.ImageToolSettings
import com.asterion.art.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

class ImageToolboxWorker(context:Context,p:WorkerParameters):CoroutineWorker(context,p) {
    companion object {
        fun enqueue(context:Context,taskId:String,original:String,
                    settings:ImageToolSettings) {
            val data=Data.Builder().putString("task",taskId)
                .putString("original",original)
                .putFloat("brightness",settings.brightness)
                .putFloat("contrast",settings.contrast)
                .putFloat("saturation",settings.saturation)
                .putFloat("sharpen",settings.sharpen).build()
            val job=OneTimeWorkRequestBuilder<ImageToolboxWorker>()
                .setInputData(data).addTag("asterion-image-toolbox")
                .addTag("asterion-task-"+taskId).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-image-toolbox-"+taskId,ExistingWorkPolicy.KEEP,job)
        }
    }
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val taskId=inputData.getString("task") ?: return@withContext Result.failure()
        val original=inputData.getString("original") ?: return@withContext Result.failure()
        val repo=ArtRepository(applicationContext)
        try {
            val channel="asterion-art-toolbox"
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(channel,"Image editing",
                    NotificationManager.IMPORTANCE_LOW))
            val notif=Notification.Builder(applicationContext,channel)
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle("Asterion Art · Image Toolbox")
                .setContentText("Processing artwork")
                .setOngoing(true).setOnlyAlertOnce(true).build()
            setForeground(ForegroundInfo(7238,notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            repo.updateTask(taskId,TaskStatus.RUNNING)
            val config=ImageToolSettings(
                inputData.getFloat("brightness",0f),
                inputData.getFloat("contrast",1f),
                inputData.getFloat("saturation",1f),
                inputData.getFloat("sharpen",0f))
            val result=AsterionImageToolbox.process(
                applicationContext,Uri.parse(original),config,{isStopped},
                {percent,phase->repo.updateTaskProgress(taskId,percent,100,phase)})
            val saved=GeneratedImagePublisher.publish(applicationContext,taskId,1,result)
            repo.registerAsset(saved.toString())
            repo.updateTask(taskId,TaskStatus.COMPLETED)
            Result.success(Data.Builder().putString("result",saved.toString()).build())
        } catch (c:CancellationException) {
            repo.updateTask(taskId,TaskStatus.CANCELLED,"Toolbox cancelled")
            Result.failure()
        } catch(e:Exception) {
            repo.updateTask(taskId,TaskStatus.FAILED,e.message ?: "Image processing failed")
            Result.failure()
        }
    }
}
