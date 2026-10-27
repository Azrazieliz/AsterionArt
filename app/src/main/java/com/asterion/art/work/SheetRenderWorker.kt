package com.asterion.art.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.asterion.art.data.ArtRepository
import com.asterion.art.engine.UniversalCreativePlanner
import com.asterion.art.engine.DesignIntent
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.learning.CreativeMemoryStore
import com.asterion.art.learning.SavedDesign
import com.asterion.art.model.RenderMode
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import com.asterion.art.model.TaskStatus
import com.asterion.art.render.SheetRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SheetRenderWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repo = ArtRepository(applicationContext)
        val sheetId = inputData.getString(SHEET_ID) ?: return@withContext Result.failure()
        val taskId = inputData.getString(TASK_ID) ?: return@withContext Result.failure()
        val sheet = repo.sheets.firstOrNull { it.id == sheetId }
        if (sheet == null) {
            repo.updateTask(taskId, TaskStatus.FAILED, "Sheet was removed")
            return@withContext Result.failure()
        }
        repo.updateTask(taskId, TaskStatus.RUNNING)
        repo.updateTaskProgress(taskId,0,0,"Composing source sheet · progress unavailable")
        try {
            val specification=UniversalCreativePlanner.interpret(DesignIntent(
                UniversalCreativePlanner.sheetDomain(sheet.role),
                referenceRoles=listOf(sheet.role)))
            val output = SheetRenderer.render(applicationContext, sheet)
            // Distinguish reference composition from native generative sheets.
            val manifest=JSONObject().apply {
                put("schema","asterion.sheet-provenance.v2")
                put("sheet_id",sheet.id)
                put("role",sheet.role.name)
                put("generated_by_diffusion",false)
                put("method","source-composition")
                put("references",JSONArray(sheet.sourceUris))
                put("design_specification",specification.synthesis)
                put("positive_prompt_guide",specification.positive)
                put("negative_prompt_guide",specification.negative)
            }
            val dst=File(applicationContext.filesDir,"sheet-provenance").also{it.mkdirs()}
            GeneratedImagePublisher.atomicWrite(File(dst,sheet.id+".json"),
                manifest.toString(2).toByteArray(Charsets.UTF_8))
            repo.updateTaskProgress(taskId,0,0,"Saving sheet PNG")
            repo.finishSheet(sheetId, output.toString())
            CreativeMemoryStore(applicationContext).upsert(SavedDesign(
                id=sheet.id+"-S001",jobId=sheet.id,mode=RenderMode.OTHER,
                creationMode=null,ordinal=1,sheetNumber=1,
                prompt=specification.positive,negative=specification.negative,
                sheetUri=output.toString(),sourceSheetIds=listOf(sheet.id)))
            repo.updateTask(taskId, TaskStatus.COMPLETED)
            Result.success()
        } catch (error: Exception) {
            repo.updateTask(taskId, TaskStatus.FAILED, error.message ?: "Sheet rendering failed")
            Result.failure()
        }
    }

    companion object {
        private const val SHEET_ID = "sheet_id"
        private const val TASK_ID = "task_id"
        fun enqueue(context: Context, sheetId: String, taskId: String) {
            val data = Data.Builder().putString(SHEET_ID, sheetId)
                .putString(TASK_ID, taskId).build()
            val request = OneTimeWorkRequestBuilder<SheetRenderWorker>()
                .setInputData(data)
                .addTag("asterion-sheet-render")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-sheet-" + sheetId, ExistingWorkPolicy.KEEP, request)
        }
    }
}
