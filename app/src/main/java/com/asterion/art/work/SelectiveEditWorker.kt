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
import com.asterion.art.core.CoreResultUploader
import com.asterion.art.data.EditJobStore
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.engine.GenerationPlanner
import com.asterion.art.engine.UniversalCreativePlanner
import com.asterion.art.engine.DesignIntent
import com.asterion.art.engine.CreativeDomain
import com.asterion.art.engine.LocalDreamBridge
import com.asterion.art.engine.StillImageEngine
import com.asterion.art.nativeengine.EmbeddedEngineFactory
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
                .addTag("asterion-image-edit").addTag("asterion-job-" + editId)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-native-execution", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("edit") ?: return@withContext Result.failure()
        val taskId = inputData.getString("task") ?: return@withContext Result.failure()
        val repo = ArtRepository(applicationContext)
        var currentEngine: StillImageEngine? = null
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
            repo.updateTaskProgress(taskId,0,0,"Loading edit sources and mask")
            val job = EditJobStore(applicationContext).load(id)
            val source = GenerationJobStore(applicationContext).load(job.sourceJobId)
            val sheets = source.sheetIds.map { key ->
                repo.sheets.firstOrNull { it.id == key } ?: error("Source sheet unavailable")
            }
            val evidence = sheets.map { SheetTextReader.read(applicationContext,it) }
            val recipe = GenerationPlanner.compile(evidence, source.mode, source.settings)
            val editSpec = UniversalCreativePlanner.interpret(DesignIntent(
                CreativeDomain.IMAGE_EDIT, referenceRoles=evidence.map { it.role }))

            val engine = EmbeddedEngineFactory.open(applicationContext)
            currentEngine = engine
            val models = engine.installedModels()
            val modelId = GenerationPlanner.selectModelId(
                models.map { it.id to it.name }, source.mode)
                ?: error("No compatible editing model is installed")
            val model = models.first { it.id == modelId }
            val nativeSize = model.generationSize.coerceIn(512,1536)
            repo.updateTaskProgress(taskId,0,0,"Loading "+model.name)
            engine.prepare(model,nativeSize,nativeSize) { isStopped }
            val selectedPass = recipe.passes.last().copy(denoise = job.strength)
            engine.verifyText(selectedPass.positive)
            if (selectedPass.negative.isNotBlank()) engine.verifyText(selectedPass.negative)
            // The masked edit is conditioned on its own preservation contract.
            // Source pixel geometry remains authoritative; do not let the
            // full artwork template overwrite the local edit instruction.

            val original = applicationContext.contentResolver.openInputStream(Uri.parse(job.sourceUri))
                ?.use { BitmapFactory.decodeStream(it) }
                ?: error("Original image no longer accessible")
            val mask = BitmapFactory.decodeFile(job.maskPath)
                ?: error("User editing mask is missing")
            try {
                val patch = SelectiveImageEditor.prepare(original, mask,nativeSize,nativeSize)
                repo.updateTaskProgress(taskId,0,0,
                    "Masked image generation · backend progress unavailable")
                val generated = engine.generate(
                    selectedPass,source.settings,nativeSize,nativeSize,patch.imagePng,
                    source.settings.seed ?: source.createdAt,{isStopped},{step,total ->
                        if(total>0) repo.updateTaskProgress(taskId,0,0,
                            "Masked inference step "+step+"/"+total)
                    },
                    maskPng = patch.maskPng
                )
                repo.updateTaskProgress(taskId,0,0,"Compositing protected pixels")
                val blended = SelectiveImageEditor.composite(
                    original,generated.png,mask,patch.rectangle)
                repo.updateTaskProgress(taskId,0,0,"Saving edited image")
                val published = GeneratedImagePublisher.publish(
                    applicationContext,job.id,1,blended)
                val technical = TechnicalImageScorer.score(blended)
                repo.registerAsset(published.toString(),autoScore=technical.score,
                    sourceJobId=job.sourceJobId)
                runCatching {
                    CoreResultUploader(applicationContext).upload(
                        published,job.sourceJobId,source.mode,
                        GeneratedImagePublisher.sha256(blended))
                }
                repo.updateTask(taskId,TaskStatus.COMPLETED)
                Result.success()
            } finally {
                original.recycle()
                mask.recycle()
            }
        } catch (e: Exception) {
            repo.updateTask(taskId,TaskStatus.FAILED,e.message ?: "Selective edit failed")
            Result.failure()
        } finally {
            currentEngine?.release()
        }
    }
}
