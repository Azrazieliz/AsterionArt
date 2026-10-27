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
import com.asterion.art.model.TaskStatus
import com.asterion.art.video.Mp4SegmentJoiner
import com.asterion.art.video.NightmareVideoBridge
import com.asterion.art.video.VideoApiCredentials
import com.asterion.art.video.VideoPlanStore
import com.asterion.art.video.VideoContinuitySchedule
import com.asterion.art.video.VideoFrameContinuation
import com.asterion.art.video.VideoSeamMetrics
import com.asterion.art.brain.AsterionReasoner
import com.asterion.art.engine.CreativeDomain
import com.asterion.art.learning.CreativeMemoryStore
import com.asterion.art.learning.SavedDesign
import com.asterion.art.model.RenderMode
import com.asterion.art.video.VideoPublisher
import com.asterion.art.video.VideoResult
import com.asterion.art.video.VideoResultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CancellationException

/**
 * Real Wan video inference for each reference-frame anchor.
 * Multiple frames => separate inferred clips combined in order; the video
 * model does not see the NEXT anchor, so no false "joint conditioning" claim.
 */
class VideoGenerationWorker(context: Context,params: WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        private const val CHANNEL="asterion_art_video"
        fun enqueue(ctx: Context,id: String,task: String) {
            val data=Data.Builder().putString("job",id).putString("task",task).build()
            val request=OneTimeWorkRequestBuilder<VideoGenerationWorker>()
                .setInputData(data).addTag("asterion-video").build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "asterion-video-"+id, ExistingWorkPolicy.KEEP,request)
        }
    }


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id=inputData.getString("job") ?: return@withContext Result.failure()
        val task=inputData.getString("task") ?: return@withContext Result.failure()
        val repo=ArtRepository(applicationContext)
        try {
            val manager=applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL,
                "Asterion Art video",NotificationManager.IMPORTANCE_LOW))
            fun notification(message: String)=Notification.Builder(applicationContext,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Asterion Art · Video")
                .setContentText(message)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
            setForeground(ForegroundInfo(7225,notification("Preparing video"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            repo.updateTask(task,TaskStatus.RUNNING)
            val plan=VideoPlanStore(applicationContext).load(id)
            val designBrain=AsterionReasoner(applicationContext)
            val neuralPlan=if(designBrain.available())designBrain.compile(
                domain=CreativeDomain.VIDEO,
                canonicalMethod="Create a temporally continuous true video from the " +
                    "provided first-frame references; maintain identity across frames.",
                concepts=listOf(plan.preset.title,plan.direction),
                basePositive=plan.positivePrompt,
                baseNegative=plan.negativePrompt,
                jobId=plan.id,
                candidateCount=plan.imageUris.size.coerceAtLeast(1),
                onStage={ stage -> repo.updateTaskProgress(task,0,0,stage) }
            ) else null
            val inferredMotion=neuralPlan?.positive?.takeIf{it.isNotBlank()}
                ?:plan.positivePrompt
            val token=VideoApiCredentials(applicationContext).read()
                ?: error("Nightmare API token not connected in Asterion Art")
            val bridge=NightmareVideoBridge(applicationContext,token)
            require(bridge.available()) { "Wan/Neodragon video model not installed or available" }
            val folder=File(applicationContext.filesDir,"video-cache/"+id).also { it.mkdirs() }
            val timeline=VideoContinuitySchedule.plan(plan.imageUris.size,
                plan.secondsPerSegment,plan.seed,
                plan.imageUris.size==1 && plan.totalSegments>1,plan.totalSegments)
            repo.updateTaskProgress(task,0,timeline.size,"Preparing video")
            val segments=mutableListOf<File>()
            val runs=mutableListOf<String>()
            for(segment in timeline) {
                val index=segment.index
                val frame=plan.imageUris[if(segment.usePreviousEndFrame)0 else index]
                if(isStopped)throw CancellationException()
                setForeground(ForegroundInfo(7225,
                    notification("Generating segment "+(index+1)+"/"+timeline.size),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
                val file=File(folder,"segment_"+index+".mp4")
                if(file.isFile && file.length()>1024) {
                    segments+=file
                    runs+="recovered"
                    repo.updateTaskProgress(task,index+1,timeline.size,"Recovered segment")
                    continue
                }
                repo.updateTaskProgress(task,index,0,
                    "Rendering segment "+(index+1)+"/"+plan.imageUris.size+
                    " · model progress unavailable")
                val source=if(segment.usePreviousEndFrame) {
                    val preceding=File(folder,"segment_"+(index-1)+".mp4")
                    val anchor=File(folder,"continuation_"+index+".png")
                    if(anchor.isFile && anchor.length()>128)Uri.fromFile(anchor)
                    else VideoFrameContinuation.extractEndFrame(
                        applicationContext,preceding,anchor)
                } else Uri.parse(frame)
                val result=bridge.generate(
                    source,inferredMotion,
                    segment.seed,segment.expectedSeconds,
                    file,{isStopped})
                segments+=result.file
                runs+=result.runId
                repo.updateTaskProgress(task,index+1,timeline.size,"Segment " + (index+1) + " complete")
                setProgress(Data.Builder().putInt("segment",index+1)
                    .putInt("total",timeline.size).build())
            }
            require(segments.size==timeline.size)
            val seamMetrics=(1 until segments.size).mapNotNull { index ->
                runCatching {
                    VideoSeamMetrics.inspect(segments[index-1],
                        segments[index],index)
                }.getOrNull()
            }
            val combined=File(folder,"assembled.mp4")
            repo.updateTaskProgress(task,0,0,
                "Assembling MP4 · backend progress unavailable")
            Mp4SegmentJoiner.join(segments,combined)
            val uri=VideoPublisher.save(applicationContext,combined,id)
            VideoResultStore(applicationContext).record(VideoResult(
                id,uri.toString(),plan.imageUris,segments.size,
                "Nightmare-compatible Wan video API",System.currentTimeMillis()))
            CreativeMemoryStore(applicationContext).upsert(SavedDesign(
                id=id+"-V001",jobId=id,mode=RenderMode.ANIMATION,
                creationMode=null,ordinal=1,sheetNumber=1,
                prompt=plan.positivePrompt,negative=plan.negativePrompt,
                sheetUri=plan.imageUris.firstOrNull(),status="CANDIDATE"))
            val manifest=JSONObject().apply {
                put("schema_version",1)
                put("job_id",id)
                put("backend","WAN_I2V_SEGMENTS")
                put("reference_count",plan.imageUris.size)
                put("inference_mode","independent first-frame conditioned clips")
                put("native_multi_keyframe_conditioning",false)
                put("continuation_mode",if(timeline.any{it.usePreviousEndFrame})
                    "previous_clip_end_frame" else "independent_reference_frames")
                put("motion_continuity_verified",false)
                put("total_segment_count",timeline.size)
                put("seam_objective_measurement",JSONArray(
                    seamMetrics.map { metric ->
                        JSONObject()
                            .put("after_segment",metric.afterSegment)
                            .put("color_jump",metric.meanColorJump)
                            .put("edge_jump",metric.edgeJump)
                            .put("likely_hard_cut",metric.likelyHardCut)
                    }))
                put("seam_metrics_are_semantic_fidelity",false)
                put("assembled_mp4",uri.toString())
                put("nightmare_run_ids",JSONArray(runs))
                put("clip_is_approved",false)
                put("positive_prompt",inferredMotion)
                put("reasoning_model",neuralPlan?.modelId ?: "deterministic")
                put("negative_prompt_guide",neuralPlan?.negative ?: plan.negativePrompt)
                put("negative_prompt_transmitted",false)
            }
            com.asterion.art.data.GeneratedImagePublisher.atomicWrite(
                File(folder,"provenance.json"),manifest.toString(2).toByteArray())
            repo.updateTask(task,TaskStatus.COMPLETED)
            Result.success()
        } catch(e:CancellationException) {
            repo.updateTask(task,TaskStatus.FAILED,"Video generation cancelled")
            Result.failure()
        } catch(e:Exception) {
            repo.updateTask(task,TaskStatus.FAILED,e.message?:"Video rendering failed")
            Result.failure()
        }
    }
}
