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
import com.asterion.art.model.TaskKind
import com.asterion.art.model.TaskStatus
import com.asterion.art.video.Mp4SegmentJoiner
import com.asterion.art.video.VideoClipJoinStore
import com.asterion.art.video.VideoPublisher
import com.asterion.art.video.VideoResult
import com.asterion.art.video.VideoResultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CancellationException

/** Real offline MP4 assembly, never advertised as diffusion/video generation. */
class VideoJoinWorker(ctx:Context,params:WorkerParameters):CoroutineWorker(ctx,params) {
    companion object {
        private const val CHANNEL="asterion-video-join"
        fun enqueue(context:Context,clips:List<String>):String {
            val plan=VideoClipJoinStore(context).create(clips)
            val repo=ArtRepository(context)
            val task=repo.recordTask(plan.id,TaskKind.VIDEO)
            val request=OneTimeWorkRequestBuilder<VideoJoinWorker>()
                .setInputData(Data.Builder().putString("job",plan.id)
                    .putString("task",task.id).build())
                .addTag("asterion-offline-video-join").build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-video-join-"+plan.id,ExistingWorkPolicy.KEEP,request)
            return task.id
        }
    }
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val job=inputData.getString("job")?:return@withContext Result.failure()
        val task=inputData.getString("task")?:return@withContext Result.failure()
        val repo=ArtRepository(applicationContext)
        val cache=File(applicationContext.cacheDir,"asterion-video-join-"+job)
        val manager=applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL,"Offline video assembly",NotificationManager.IMPORTANCE_LOW))
        fun notification(stage:String)=Notification.Builder(applicationContext,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Asterion · MP4 assembly")
            .setContentText(stage).setOngoing(true).setOnlyAlertOnce(true).build()
        try {
            setForeground(ForegroundInfo(7130,notification("Preparing local clips"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            repo.updateTask(task,TaskStatus.RUNNING)
            val plan=VideoClipJoinStore(applicationContext).load(job)
            check(cache.mkdirs()||cache.isDirectory)
            val files=plan.clips.mapIndexed{n,source->
                if(isStopped)throw CancellationException()
                repo.updateTaskProgress(task,n,plan.clips.size+2,
                    "Copying local clip "+(n+1)+"/"+plan.clips.size)
                val path=File(cache,"input_"+n+".mp4")
                val uri=Uri.parse(source)
                requireNotNull(applicationContext.contentResolver.openInputStream(uri))
                    .use{input->
                        path.outputStream().buffered().use{output->
                            val buffer=ByteArray(256*1024)
                            while(true) {
                                if(isStopped)throw CancellationException()
                                val size=input.read(buffer)
                                if(size<0)break
                                if(size>0)output.write(buffer,0,size)
                            }
                        }
                    }
                check(path.length()>1024L){"Input MP4 "+(n+1)+" is empty"}
                path
            }
            val combined=File(cache,"joined.mp4")
            repo.updateTaskProgress(task,plan.clips.size,plan.clips.size+2,
                "Joining compatible video and audio tracks")
            Mp4SegmentJoiner.join(files,combined)
            if(isStopped)throw CancellationException()
            repo.updateTaskProgress(task,plan.clips.size+1,plan.clips.size+2,
                "Publishing MP4 to phone Gallery")
            val saved=VideoPublisher.save(applicationContext,combined,job)
            VideoResultStore(applicationContext).record(VideoResult(
                job,saved.toString(),plan.clips,plan.clips.size,
                "Offline clip join (not AI-generated)",System.currentTimeMillis()))
            repo.updateTask(task,TaskStatus.COMPLETED)
            Result.success()
        } catch(c:CancellationException) {
            repo.updateTask(task,TaskStatus.CANCELLED,"Offline clip joining cancelled")
            Result.failure()
        } catch(e:Throwable) {
            repo.updateTask(task,TaskStatus.FAILED,
                "MP4 assembly failed: "+(e.message?:e.javaClass.simpleName))
            Result.failure()
        } finally {
            cache.deleteRecursively()
        }
    }
}
