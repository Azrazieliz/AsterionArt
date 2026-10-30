package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.model.CreationMode
import java.util.concurrent.TimeUnit
/** Foreground-compatible WorkManager pipeline using genuine sequential native passes. */
class GenerationWorker(context: Context, params: WorkerParameters):
    CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL = "asterion_art_generator"
        private const val JOB = "job_id"
        private const val TASK = "task_id"
        fun enqueue(context: Context, jobId: String, taskId: String) {
            // All user-started native image generations run in an independent
            // foreground service, not an Activity. Requests are persisted
            // before service startup and executed sequentially.
            GenerationJobStore(context).load(jobId)
            val started=runCatching {
                GenerationForegroundService.enqueue(context,jobId,taskId)
            }.onFailure { error ->
                Log.w("AsterionGenerator","Foreground launch denied; using durable WorkManager",error)
                // WorkManager owns the fallback, avoiding two executors using
                // the same QNN and checkpoint directory after app reopening.
                ForegroundRenderQueueStore(context).remove(jobId)
            }.isSuccess
            if(started)return
            val data = Data.Builder().putString(JOB, jobId).putString(TASK, taskId).build()
            val work = OneTimeWorkRequestBuilder<GenerationWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("asterion-native-generation").addTag("asterion-job-" + jobId).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-native-execution", ExistingWorkPolicy.APPEND_OR_REPLACE, work)
        }
    }

    override suspend fun doWork():Result {
        val jobId=inputData.getString(JOB) ?: return Result.failure()
        val taskId=inputData.getString(TASK) ?: return Result.failure()
        // Migrate already-queued character WorkManager jobs after an APK
        // update. A background foreground-service start can be denied by
        // Android, in which case the existing worker remains a safe fallback.
        val plan=GenerationJobStore(applicationContext).load(jobId)
        if(plan.creationMode==CreationMode.CHARACTER &&
            plan.designPrompt.isNullOrBlank()) {
            val moved=runCatching {
                GenerationForegroundService.enqueue(applicationContext,jobId,taskId)
            }.onFailure{Log.w("AsterionGenerator","Foreground handoff unavailable",it)}
                .isSuccess
            if(moved)return Result.success()
        }
        val complete=GenerationExecutor(
            applicationContext,
            stopped={isStopped},
            notify={message->foreground(message)},
            taskProgress={candidate,pass->
                setProgress(Data.Builder().putInt("candidate",candidate)
                    .putInt("pass",pass).build())
            }
        ).execute(jobId,taskId)
        return if(complete)Result.success() else if(isStopped)Result.retry() else Result.failure()
    }

    private suspend fun foreground(message: String) {
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL,
                "Asterion Art", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("Asterion Art")
            .setContentText(message)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
        setForeground(ForegroundInfo(7223, notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
    }

}

