package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.EditJobStore
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.engine.GenerationPlanner
import com.asterion.art.engine.LocalDreamBridge
import com.asterion.art.engine.SelectiveImageEditor
import com.asterion.art.engine.SheetTextReader
import com.asterion.art.engine.TechnicalImageScorer
import com.asterion.art.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** One real inpaint inference, with hard pixel preservation outside user mask. */
class SelectiveEditWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        fun enqueue(context: Context, editId: String, taskId: String) {
            val request = OneTimeWorkRequestBuilder<SelectiveEditWorker>()
                .setInputData(Data.Builder().putString("edit", editId)
                    .putString("task", taskId).build())
                .addTag("asterion-image-edit")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-edit-" + editId, ExistingWorkPolicy.KEEP, request)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("edit") ?: return@withContext Result.failure()
        val taskId = inputData.getString("task") ?: return@withContext Result.failure()
        val repo = ArtRepository(applicationContext)
        try {
            val channel = "asterion_art_edit"
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(channel,
                    "Asterion Art editing", NotificationManager.IMPORTANCE_LOW))
            val notification = Notification.Builder(applicationContext, channel)
                .setContentTitle("Asterion Art")
                .setContentText("Selective local image editing")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setOngoing(true)
                .build()
            setForeground(ForegroundInfo(
                7224, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))

            repo.updateTask(taskId, TaskStatus.RUNNING)
            val job = EditJobStore(applicationContext).load(id)
            val source = GenerationJobStore(applicationContext).load(job.sourceJobId)
            val sheets = source.sheetIds.map { key ->
                repo.sheets.firstOrNull { it.id == key } ?: error("Source sheet unavailable")
            }
            val evidence = sheets.map { SheetTextReader.read(applicationContext,it) }
            val recipe = GenerationPlanner.compile(evidence, source.mode, source.settings)
            val engine = LocalDreamBridge()
            val models = engine.installedModels()
            val modelId = GenerationPlanner.selectModelId(
                models.map { it.id to it.name }, source.mode)
                ?: error("No compatible editing model is installed")
            val model = models.first { it.id == modelId }
            val nativeSize = model.generationSize.coerceIn(512,1536)
            engine.prepare(model,nativeSize,nativeSize) { isStopped }
            val selectedPass = recipe.passes.last().copy(denoise = job.strength)
            engine.verifyText(selectedPass.positive)
            if (selectedPass.negative.isNotBlank()) engine.verifyText(selectedPass.negative)
            val original = applicationContext.contentResolver.openInputStream(Uri.parse(job.sourceUri))
                ?.use { BitmapFactory.decodeStream(it) }
                ?: error("Original image no longer accessible")
            val mask = BitmapFactory.decodeFile(job.maskPath)
                ?: error("User editing mask is missing")
            try {
                val patch = SelectiveImageEditor.prepare(original, mask,nativeSize,nativeSize)
                val generated = engine.generate(
                    selectedPass,source.settings,nativeSize,nativeSize,patch.imagePng,
                    source.settings.seed ?: source.createdAt,{isStopped},{_,_->},
                    maskPng = patch.maskPng
                )
                val blended = SelectiveImageEditor.composite(
                    original,generated.png,mask,patch.rectangle)
                val published = GeneratedImagePublisher.publish(
                    applicationContext,job.id,1,blended)
                val technical = TechnicalImageScorer.score(blended)
                repo.registerAsset(published.toString(),autoScore=technical.score,
                    sourceJobId=job.sourceJobId)
                repo.updateTask(taskId,TaskStatus.COMPLETED)
                Result.success()
            } finally {
                original.recycle()
                mask.recycle()
            }
        } catch (e: Exception) {
            repo.updateTask(taskId,TaskStatus.FAILED,e.message ?: "Selective edit failed")
            Result.failure()
        }
    }
}
