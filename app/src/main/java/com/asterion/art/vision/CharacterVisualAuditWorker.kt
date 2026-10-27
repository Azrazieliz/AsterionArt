package com.asterion.art.vision

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.*
import com.asterion.art.brain.BrainInferenceLock
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.methodology.CharacterPlanStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Runs AFTER the image engine has been released. It observes real pixels using
 * an installed local VLM and never writes user grades or modifies frozen designs.
 */
class CharacterVisualAuditWorker(context:Context,params:WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        private const val KEY="job_id"
        const val PREFIX="asterion-character-visual-"
        fun enqueue(context:Context,jobId:String) {
            require(jobId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
            val req=OneTimeWorkRequestBuilder<CharacterVisualAuditWorker>()
                .setInputData(Data.Builder().putString(KEY,jobId).build())
                .addTag("character-visual-audit").build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                PREFIX+jobId,ExistingWorkPolicy.KEEP,req)
        }
    }
    private suspend fun foreground(label:String) {
        val manager=applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            "asterion-local-visual-audit","Local visual assessment",
            NotificationManager.IMPORTANCE_LOW))
        val n=Notification.Builder(applicationContext,"asterion-local-visual-audit")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Inspecting generated anatomical characters")
            .setContentText(label.take(125)).setOnlyAlertOnce(true)
            .setOngoing(true).build()
        setForeground(ForegroundInfo(7267,n,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
    }
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val jobId=inputData.getString(KEY)?:return@withContext Result.failure()
        val pair=VisionModelStore(applicationContext).installed()
            ?:return@withContext Result.failure(Data.Builder()
                .putString("error","No verified local Qwen3-VL projector installed")
                .build())
        val job=GenerationJobStore(applicationContext).load(jobId)
        val id=job.characterPlanId?:return@withContext Result.failure()
        val plan=CharacterPlanStore(applicationContext).requireAuthorized(id,jobId)
        val resultFile=File(applicationContext.filesDir,
            "generation-plans/"+jobId+"-results.json")
        require(resultFile.isFile){"Render results missing; refusing fake visual assessment"}
        val rendered=JSONObject(resultFile.readText(Charsets.UTF_8))
            .getJSONArray("results")
        require(rendered.length()==plan.sheets) {
            "Cannot audit an incomplete rendered character population"
        }
        val evidence=CharacterVisualReportStore(applicationContext)
        val saved=evidence.findings(jobId).toMutableList()
        BrainInferenceLock.mutex.withLock {
            foreground("Loading offline Qwen3-VL multimodal projector")
            NativeVisionSession(pair).use { vision ->
                for(sheetNumber in 1..plan.sheets) {
                    if(isStopped)break
                    val entry=rendered.getJSONObject(sheetNumber-1)
                    val uri=entry.getString("uri")
                    val sha=entry.getString("sha256")
                    val opts=BitmapFactory.Options().apply {inJustDecodeBounds=true}
                    applicationContext.contentResolver.openInputStream(Uri.parse(uri))
                        .use{stream->
                            requireNotNull(stream)
                            BitmapFactory.decodeStream(stream,null,opts)
                        }
                    require(opts.outWidth>0&&opts.outHeight>0)
                    var scale=1
                    while(opts.outWidth/scale>3000||opts.outHeight/scale>2000)scale*=2
                    val settings=BitmapFactory.Options().apply{inSampleSize=scale}
                    val image=applicationContext.contentResolver
                        .openInputStream(Uri.parse(uri)).use{stream->
                            requireNotNull(stream)
                            BitmapFactory.decodeStream(stream,null,settings)
                        } ?:error("Cannot decode generated character sheet")
                    try {
                        for(index in 0 until 5) {
                            if(isStopped)break
                            val identity=plan.frozen[(sheetNumber-1)*5+index]
                            if(saved.any{it.characterId==identity.id&&
                                it.sourceSha256==sha})continue
                            val left=image.width*index/5
                            val right=image.width*(index+1)/5
                            val crop=Bitmap.createBitmap(image,left,0,right-left,image.height)
                            val answer=try {
                                val frame=NativeVisionSession.rgb(crop)
                                foreground("Inspecting "+identity.id+" / "+plan.total)
                                vision.analyze(frame,CharacterVisualProtocol.prompt(identity),700)
                            } finally {crop.recycle()}
                            val finding=runCatching{
                                CharacterVisualProtocol.parse(identity,
                                    sheetNumber,uri,sha,answer)
                            }.getOrElse{problem->
                                CharacterVisualFinding(identity.id,sheetNumber,
                                    uri,sha,emptyList(),emptyMap(),
                                    emptyList(),emptyList(),emptyList(),
                                    listOf("Visual assessment was inconclusive: "+
                                        (problem.message?:"malformed response")),
                                    status="UNVERIFIED_INCONCLUSIVE")
                            }
                            saved.removeAll{it.characterId==finding.characterId}
                            saved+=finding
                            evidence.save(jobId,saved)
                            setProgress(Data.Builder()
                                .putInt("audited",saved.size)
                                .putInt("total",plan.total)
                                .putString("character",identity.id).build())
                        }
                    } finally {image.recycle()}
                }
            }
        }
        if(isStopped)Result.failure(Data.Builder().putString("error",
            "Assessment cancelled; completed observations retained").build())
        else Result.success(Data.Builder()
            .putInt("observed",saved.size).putInt("expected",plan.total).build())
    }
}
