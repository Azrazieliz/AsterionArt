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
            val token=VideoApiCredentials(applicationContext).read()
                ?: error("Nightmare API token not connected in Asterion Art")
            val bridge=NightmareVideoBridge(applicationContext,token)
            require(bridge.available()) { "Wan/Neodragon video model not installed or available" }
            val folder=File(applicationContext.filesDir,"video-cache/"+id).also { it.mkdirs() }
            repo.updateTaskProgress(task,0,plan.imageUris.size,"Preparing video")
            val segments=mutableListOf<File>()
            val runs=mutableListOf<String>()
            for((index,frame) in plan.imageUris.withIndex()) {
                if(isStopped)throw CancellationException()
                setForeground(ForegroundInfo(7225,
                    notification("Generating segment "+(index+1)+"/"+plan.imageUris.size),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
                val file=File(folder,"segment_"+index+".mp4")
                if(file.isFile && file.length()>1024) {
                    segments+=file
                    runs+="recovered"
                    repo.updateTaskProgress(task,index+1,plan.imageUris.size,"Recovered segment")
                    continue
                }
                val result=bridge.generate(
                    Uri.parse(frame),plan.preset.instruction,
                    plan.seed+index*1009L,plan.secondsPerSegment,
                    file,{isStopped})
                segments+=result.file
                runs+=result.runId
                repo.updateTaskProgress(task,index+1,plan.imageUris.size,"Segment " + (index+1) + " complete")
                setProgress(Data.Builder().putInt("segment",index+1)
                    .putInt("total",plan.imageUris.size).build())
            }
            require(segments.size==plan.imageUris.size)
            val combined=File(folder,"assembled.mp4")
            repo.updateTaskProgress(task,plan.imageUris.size,plan.imageUris.size,"Assembling MP4")
            Mp4SegmentJoiner.join(segments,combined)
            val uri=VideoPublisher.save(applicationContext,combined,id)
            VideoResultStore(applicationContext).record(VideoResult(
                id,uri.toString(),plan.imageUris,segments.size,
                "Nightmare-compatible Wan video API",System.currentTimeMillis()))
            val manifest=JSONObject().apply {
                put("schema_version",1)
                put("job_id",id)
                put("backend","WAN_I2V_SEGMENTS")
                put("reference_count",plan.imageUris.size)
                put("inference_mode","independent first-frame conditioned clips")
                put("native_multi_keyframe_conditioning",false)
                put("assembled_mp4",uri.toString())
                put("nightmare_run_ids",JSONArray(runs))
                put("clip_is_approved",false)
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
