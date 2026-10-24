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
import com.asterion.art.enhancement.EmbeddedRealSr
import com.asterion.art.engine.TechnicalImageScorer
import com.asterion.art.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

class NeuralEnhancementWorker(context:Context, params:WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        fun enqueue(context:Context,sourceUri:String,taskId:String,scale:Int=4) {
            val data=Data.Builder().putString("source_uri",sourceUri)
                .putString("task_id",taskId).putInt("scale",scale).build()
            val work=OneTimeWorkRequestBuilder<NeuralEnhancementWorker>()
                .setInputData(data).addTag("asterion-enhancement")
                .addTag("asterion-task-"+taskId).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-enhancement-"+taskId,ExistingWorkPolicy.KEEP,work)
        }
    }

    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val taskId=inputData.getString("task_id") ?: return@withContext Result.failure()
        val uri=inputData.getString("source_uri") ?: return@withContext Result.failure()
        val repo=ArtRepository(applicationContext)
        try {
            val channel="asterion-art-neural-enhancement"
            val manager=applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(channel,
                "Asterion Art · Enhancement",NotificationManager.IMPORTANCE_LOW))
            suspend fun progress(stage:String) {
                repo.updateTaskProgress(taskId,0,0,stage)
                setForeground(ForegroundInfo(7237,
                    Notification.Builder(applicationContext,channel)
                        .setSmallIcon(android.R.drawable.ic_menu_gallery)
                        .setContentTitle("Asterion Art · RealSR")
                        .setContentText(stage).setOngoing(true).setOnlyAlertOnce(true).build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            }
            progress("Preparing native neural enhancement")
            repo.updateTask(taskId,TaskStatus.RUNNING)
            val enhanced=EmbeddedRealSr(applicationContext).enhance(
                Uri.parse(uri),inputData.getInt("scale",4),128,
                stop={isStopped},progress={ stage ->
                    // WorkManager's next foreground update occurs at a suspend point;
                    // the source CLI exposes no reliable numeric GPU progress.
                    repo.updateTaskProgress(taskId,0,0,stage)
                })
            progress("Publishing enhanced image")
            val saved=GeneratedImagePublisher.publish(
                applicationContext,taskId,1,enhanced)
            repo.registerAsset(saved.toString(),
                autoScore=TechnicalImageScorer.score(enhanced).score)
            repo.updateTask(taskId,TaskStatus.COMPLETED)
            Result.success(Data.Builder().putString("output_uri",saved.toString()).build())
        } catch(e:CancellationException) {
            repo.updateTask(taskId,TaskStatus.CANCELLED,"Enhancement cancelled")
            Result.failure()
        } catch(e:Exception) {
            repo.updateTask(taskId,TaskStatus.FAILED,e.message ?: "Neural enhancement failed")
            Result.failure()
        }
    }
}
