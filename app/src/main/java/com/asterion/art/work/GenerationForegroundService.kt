package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.asterion.art.data.ArtRepository
import com.asterion.art.model.TaskStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * User-initiated, resumable native image generation. JobScheduler foreground
 * workers are time-limited on Android 16+ and are repeatedly stopped by Samsung.
 * A direct foreground service executes the existing shared GenerationExecutor;
 * every pass and character checkpoint survives process/service restarts.
 */
class GenerationForegroundService:Service() {
    companion object {
        private const val TAG="AsterionRenderService"
        private const val CHANNEL="asterion_native_image_render"
        private const val NOTIFICATION_ID=7224
        private const val JOB_ID="job_id"
        private const val TASK_ID="task_id"
        private const val ACTION_CANCEL="com.asterion.art.CANCEL_RENDER"

        fun enqueue(context:Context,jobId:String,taskId:String) {
            require(jobId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
            require(taskId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
            // Persist before requesting Android to start the service. The user
            // can leave the Activity immediately and no queued job is lost.
            ForegroundRenderQueueStore(context).enqueue(jobId,taskId)
            val request=Intent(context,GenerationForegroundService::class.java)
                .putExtra(JOB_ID,jobId).putExtra(TASK_ID,taskId)
            context.startForegroundService(request)
        }
        fun cancel(context:Context,jobId:String) {
            ForegroundRenderQueueStore(context).remove(jobId)
            context.startService(Intent(context,GenerationForegroundService::class.java)
                .setAction(ACTION_CANCEL).putExtra(JOB_ID,jobId))
        }

        /** Called only while the user reopens the app, to resume orphaned jobs. */
        fun revivePending(context:Context) {
            val pending=ForegroundRenderQueueStore(context).next() ?: return
            context.startForegroundService(
                Intent(context,GenerationForegroundService::class.java)
                    .putExtra(JOB_ID,pending.jobId)
                    .putExtra(TASK_ID,pending.taskId))
        }
    }

    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var running:Job?=null
    private var activeJob:String?=null
    private var activeTask:String?=null
    private var lock:PowerManager.WakeLock?=null
    private val mainHandler=Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(
                CHANNEL,"Asterion native image generation",
                NotificationManager.IMPORTANCE_LOW))
    }

    private fun notification(stage:String):Notification =
        Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("Asterion Art · local image rendering")
            .setContentText(stage.take(120))
            .setOnlyAlertOnce(true).setOngoing(true).build()

    private fun beginForeground(stage:String) {
        val kind=if(Build.VERSION.SDK_INT>=35)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        startForeground(NOTIFICATION_ID,notification(stage),kind)
    }

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        val incomingJob=intent?.getStringExtra(JOB_ID)
        if(intent?.action==ACTION_CANCEL) {
            if(incomingJob==activeJob) {
                running?.cancel(CancellationException("User cancelled rendering"))
                activeTask?.let {
                    ArtRepository(applicationContext).updateTask(
                        it,TaskStatus.CANCELLED,"User cancelled native image rendering")
                }
            }
            return START_REDELIVER_INTENT
        }

        // The service may receive several starts while a single NPU is in
        // use. Their durable queue is drained sequentially, never failed as
        // "already running".
        val queue=ForegroundRenderQueueStore(applicationContext)
        val first=queue.next()
        if(first==null) {
            if(running?.isActive!=true)stopSelf()
            return START_NOT_STICKY
        }
        beginForeground("Reading saved image-generation queue")
        if(running?.isActive==true)return START_REDELIVER_INTENT

        val catalog=ArtRepository(applicationContext)
        val previous=catalog.tasks.firstOrNull { it.id==first.taskId }
        if(previous==null || previous.status in setOf(
                TaskStatus.COMPLETED,TaskStatus.CANCELLED,TaskStatus.FAILED)) {
            // A completed job can remain in the queue after a process
            // crash between saving its result and removing the queue item.
            queue.remove(first.jobId)
            mainHandler.post {
                if(queue.next()!=null)
                    revivePending(applicationContext)
                else stopSelf()
            }
            return START_REDELIVER_INTENT
        }

        val jobId=first.jobId
        val taskId=first.taskId
        activeJob=jobId
        activeTask=taskId
        lock=(getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,"AsterionArt:NativeRender").apply {
            setReferenceCounted(false)
            acquire(4L*60L*60L*1000L)
        }
        running=scope.launch {
            val ctx=currentCoroutineContext()
            try {
                val ok=GenerationExecutor(applicationContext,
                    stopped={!ctx.isActive},
                    notify={stage->
                        getSystemService(NotificationManager::class.java)
                            .notify(NOTIFICATION_ID,notification(stage))
                    }
                ).execute(jobId,taskId)
                Log.i(TAG,"Job "+jobId+" completed="+ok)
            } catch(e:CancellationException) {
                Log.w(TAG,"Native rendering stopped; checkpoints remain",e)
                throw e
            } catch(e:Throwable) {
                Log.e(TAG,"Uncaught native rendering failure",e)
                ArtRepository(applicationContext).updateTask(
                    taskId,TaskStatus.FAILED,e.message?:"Native inference failed")
            } finally {
                // If Android kills this process before here, the durable
                // FIFO entry survives and its checkpoints remain recoverable.
                val wasInterrupted=!ctx.isActive
                val persistedStatus=ArtRepository(applicationContext).tasks
                    .firstOrNull{it.id==taskId}?.status
                if(persistedStatus in setOf(
                        TaskStatus.COMPLETED,TaskStatus.CANCELLED,TaskStatus.FAILED))
                    queue.remove(jobId)
                releaseWakeLock()
                mainHandler.post {
                    running=null
                    activeJob=null
                    activeTask=null
                    val pending=queue.next()
                    if(pending!=null && !wasInterrupted) {
                        // Continue other explicitly requested renders while
                        // the user is no longer viewing this Activity.
                        onStartCommand(Intent(applicationContext,
                            GenerationForegroundService::class.java)
                            .putExtra(JOB_ID,pending.jobId)
                            .putExtra(TASK_ID,pending.taskId),0,startId)
                    } else {
                        // Interrupted entries stay durable until next launch
                        // or OS foreground-service redelivery.
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onTimeout(startId:Int,fgsType:Int) {
        activeTask?.let {
            runCatching{ArtRepository(applicationContext).updateTask(
                it,TaskStatus.FAILED,
                "Android media-processing foreground time limit; saved native checkpoints remain")}
        }
        running?.cancel()
        stopSelf(startId)
    }

    private fun releaseWakeLock() {
        runCatching { lock?.takeIf{it.isHeld}?.release() }
        lock=null
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent:Intent?):IBinder?=null
}
