package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.asterion.art.brain.CharacterBrainResolver
import com.asterion.art.methodology.CharacterPlanStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import org.json.JSONObject

/** Prepares an entire frozen Phase-1 population. Never calls an image backend. */
class CharacterPreparationWorker(context:Context,params:WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        private const val KEY="character_plan_id"
        private const val CHANNEL="asterion_character_design"
        fun enqueue(context:Context,planId:String) {
            val data=Data.Builder().putString(KEY,planId).build()
            val work=OneTimeWorkRequestBuilder<CharacterPreparationWorker>()
                .setInputData(data).addTag("asterion-character-design")
                .addTag("character-plan-"+planId).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "character-design-"+planId,ExistingWorkPolicy.KEEP,work)
        }
    }
    private suspend fun foreground(stage:String) {
        val manager=applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL,"Asterion character design",NotificationManager.IMPORTANCE_LOW))
        val alert=Notification.Builder(applicationContext,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Resolving original characters")
            .setContentText(stage.take(125))
            .setOnlyAlertOnce(true).setOngoing(true).build()
        setForeground(ForegroundInfo(7251,alert,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
    }
    override suspend fun doWork():Result {
        val planId=inputData.getString(KEY)?:return Result.failure()
        val store=CharacterPlanStore(applicationContext)
        return try {
            foreground("Beginning complete anatomical design resolution")
            store.begin(planId)
            val ready=CharacterBrainResolver(applicationContext).resolve(planId){
                    done,total,stage->
                store.stage(planId,stage)
                setProgressAsync(Data.Builder().putInt("completed",done)
                    .putInt("total",total).putString("stage",stage).build())
            }
            ready.assertComplete()
            foreground("All characters resolved; awaiting your Generate confirmation")
            Result.success()
        } catch(e:TimeoutCancellationException) {
            store.fail(planId,"Qwen3 request exceeded its deadline at "+
                store.load(planId).progressStage+
                "; previous frozen identities remain available for retry")
            Result.failure()
        } catch(e:CancellationException) {
            store.fail(planId,"Design worker cancelled by Android (stop reason "+
                stopReason+") at "+store.load(planId).progressStage+
                "; completed identities preserved")
            Result.failure()
        } catch(e:Exception) {
            store.fail(planId,e.message ?: "Unresolved neural character design")
            Result.failure()
        }
    }

    override fun onStopped() {
        super.onStopped()
        // JobScheduler may stop long-running foreground work without the
        // coroutine reaching catch. Retain the original plan and reason.
        val planId=inputData.getString(KEY) ?: return
        runCatching {
            val store=CharacterPlanStore(applicationContext)
            val stage=store.load(planId).progressStage
            store.fail(planId,"Android stopped character worker (reason "+
                stopReason+") at "+stage+"; checkpoints preserved")
        }
    }
}