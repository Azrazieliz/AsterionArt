package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.asterion.art.brain.CharacterBrainResolver
import com.asterion.art.methodology.CharacterPlanStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * User-initiated media creation that must not occupy JobScheduler quota.
 * WorkManager foreground workers can be stopped for quota on Android 16+.
 * Every completed identity remains in CharacterPlanStore, and START_REDELIVER
 * resumes the same plan after an unexpected service/process restart.
 */
class CharacterPreparationService : Service() {
    companion object {
        private const val TAG="AsterionCharacterService"
        private const val CHANNEL="asterion_character_design"
        private const val ID=7251
        private const val PLAN_ID="character_plan_id"

        fun enqueue(context:Context,planId:String) {
            require(planId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
            context.startForegroundService(
                Intent(context,CharacterPreparationService::class.java)
                    .putExtra(PLAN_ID,planId))
        }
    }

    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val main=Handler(Looper.getMainLooper())
    private var running:Job?=null
    private var activePlan:String?=null
    private var awake:PowerManager.WakeLock?=null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(
                CHANNEL,"Asterion character design",NotificationManager.IMPORTANCE_LOW))
    }

    private fun notification(message:String):Notification =
        Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Asterion · original character design")
            .setContentText(message.take(120))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()

    private fun show(stage:String) {
        getSystemService(NotificationManager::class.java).notify(ID,notification(stage))
    }

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        val id=intent?.getStringExtra(PLAN_ID)
        if(id.isNullOrBlank()) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        val type=if(Build.VERSION.SDK_INT>=35)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        startForeground(ID,notification("Preparing frozen anatomical identities"),type)
        if(running?.isActive==true) {
            if(activePlan!=id) {
                CharacterPlanStore(this).fail(id,
                    "Another character plan is being resolved; no data discarded")
            }
            return START_REDELIVER_INTENT
        }
        activePlan=id
        val manager=getSystemService(POWER_SERVICE) as PowerManager
        awake=manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
            "AsterionArt:CharacterDesign").apply {
            setReferenceCounted(false)
            acquire(4L*60L*60L*1000L)
        }
        running=scope.launch {
            val store=CharacterPlanStore(applicationContext)
            try {
                val existing=store.load(id)
                if(existing.status=="READY"||existing.status=="AUTHORIZED") {
                    Log.i(TAG,"Plan already finalized; no duplicate inference")
                } else {
                    store.begin(id)
                    CharacterBrainResolver(applicationContext).resolve(id){done,total,stage->
                        store.stage(id,stage)
                        show("Designed $done/$total · $stage")
                    }.assertComplete()
                    Log.i(TAG,"Complete five-character population certified")
                }
            } catch(e:TimeoutCancellationException) {
                runCatching { store.fail(id,"Selected reasoning model timed out at "+
                    store.load(id).progressStage+"; checkpoints preserved") }
            } catch(e:CancellationException) {
                runCatching { store.fail(id,"Character service interrupted at "+
                    store.load(id).progressStage+"; checkpoints preserved") }
                throw e
            } catch(e:Exception) {
                Log.e(TAG,"Character resolution failed at a persisted stage",e)
                runCatching { store.fail(id,(e.message ?:
                    "Local character inference failed")+"; completed identities preserved") }
            } finally {
                main.post {
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelfResult(startId)
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onTimeout(startId:Int,fgsType:Int) {
        activePlan?.let { id ->
            runCatching {
                val store=CharacterPlanStore(applicationContext)
                store.fail(id,"Android media-processing service time limit reached at "+
                    store.load(id).progressStage+"; frozen identities preserved")
            }
        }
        running?.cancel()
        stopSelf(startId)
    }

    private fun releaseWakeLock() {
        runCatching { awake?.takeIf { it.isHeld }?.release() }
        awake=null
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent:Intent?):IBinder?=null
}
