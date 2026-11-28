package com.asterion.art.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import com.asterion.art.R
import com.asterion.art.creation.CharacterTaxonomy
import com.asterion.art.creation.CreationExecutor
import com.asterion.art.creation.CreationRun
import com.asterion.art.creation.DurableWorkspace
import com.asterion.art.creation.ExecutionSignal
import com.asterion.art.creation.Family
import com.asterion.art.creation.RuntimeUnavailable
import com.asterion.art.studio.StudioActivity
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class ArtWorkspace(context: Context) {
    private val application = context.applicationContext
    val directory = File(application.filesDir, "art-reconstruction-v1")
    val journal = DurableWorkspace(directory)
    val references = ReferenceFiles(application, directory)
    val taxonomy by lazy(CharacterTaxonomy::load)
    val runs = callbackFlow {
        val events = FileObserver.CREATE or FileObserver.MOVED_TO or FileObserver.DELETE
        val observer = object : FileObserver(File(directory, "runs"), events) {
            override fun onEvent(event: Int, path: String?) {
                if (path?.endsWith(".json") == true) trySend(Unit)
            }
        }
        observer.startWatching()
        trySend(Unit)
        awaitClose { observer.stopWatching() }
    }.conflate().map { journal.list() }.flowOn(Dispatchers.IO)

    fun outputFiles(runId: String): AssetFiles {
        require(UUID.fromString(runId).toString() == runId)
        return AssetFiles(File(directory, "outputs/$runId"))
    }

    companion object {
        @Volatile private var active: ArtWorkspace? = null

        fun get(context: Context): ArtWorkspace = active ?: synchronized(this) {
            active ?: ArtWorkspace(context).also { active = it }
        }
    }
}

internal fun familyName(family: Family): String = when (family) {
    Family.ANIME -> "Anime"
    Family.COSPLAY -> "Cosplay"
    Family.REALISTIC -> "Realistic"
}

class CreationService : Service() {
    @Volatile private var finished = false
    private var started = false
    private var latestStartId = 0
    private val workspace by lazy { ArtWorkspace.get(this) }
    private val budget by lazy { DeviceBudget(this) }
    private val notifications by lazy { getSystemService(NotificationManager::class.java) }

    override fun onCreate() {
        super.onCreate()
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.render_channel), NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        val notice = notification("Opening the creation queue")
        if (Build.VERSION.SDK_INT >= 35) {
            startForeground(NOTIFICATION, notice, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        } else {
            startForeground(NOTIFICATION, notice)
        }
        if (!started) {
            started = true
            schedule(startId)
        }
        return START_STICKY
    }

    private fun schedule(startId: Int) {
        worker.execute { drain(startId) }
    }

    private fun drain(startId: Int) {
        try {
            workspace.journal.recoverInterrupted()
            val executor = CreationExecutor(workspace.journal, workspace.taxonomy)
            while (!finished) {
                val queueItem = workspace.journal.next() ?: break
                notifications.notify(NOTIFICATION, notification(queueItem.request.mode.title))
                executor.execute(
                    queueItem.id,
                    ExecutionSignal {
                        if (finished) throw InterruptedException()
                        budget.check()
                    },
                    ::openSession,
                )
            }
        } finally {
            mainExecutor.execute {
                if (!finished && latestStartId != startId) {
                    schedule(latestStartId)
                } else {
                    started = false
                    stopSelfResult(startId)
                }
            }
        }
    }

    private fun openSession(run: CreationRun, signal: ExecutionSignal): Nothing {
        signal.check()
        val family = familyName(run.request.family)
        throw RuntimeUnavailable("The production $family runtime for ${run.request.mode.title} is pending.")
    }

    private fun notification(title: String): Notification {
        val launch = Intent(this, StudioActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val destination = PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle(title)
            .setContentIntent(destination)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        finished = true
        stopSelfResult(startId)
    }

    override fun onDestroy() {
        finished = true
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "art-creations"
        private const val NOTIFICATION = 601
        private val worker = Executors.newSingleThreadExecutor()

        fun wake(context: Context) {
            context.startForegroundService(Intent(context, CreationService::class.java))
        }
    }
}
