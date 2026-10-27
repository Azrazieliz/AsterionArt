package com.asterion.art.modelpack

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * First launch: reconstruct signed-APK-contained neural checkpoints on local
 * app-owned storage. No HTTP and no user-side model download/import.
 */
class EmbeddedModelInstallWorker(context:Context,params:WorkerParameters):
    CoroutineWorker(context,params) {
    companion object {
        const val TAG="asterion-embedded-model-install"
        const val WORK="asterion-offline-bundled-weights-v1"
        private const val CHANNEL="asterion-offline-model-install"
        fun enqueue(context:Context) {
            val pack=EmbeddedModelPack(context)
            if(!pack.packaged()||pack.allReady())return
            val work=OneTimeWorkRequestBuilder<EmbeddedModelInstallWorker>()
                .addTag(TAG).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK,ExistingWorkPolicy.KEEP,work)
        }
    }

    private suspend fun notify(progress:Int,name:String){
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL,
                "Preparing included neural models",
                NotificationManager.IMPORTANCE_LOW))
        val notice=Notification.Builder(applicationContext,CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Preparing Asterion Art's included AI models")
            .setContentText(name+" · "+progress+"%")
            .setOnlyAlertOnce(true).setOngoing(true).build()
        setForeground(ForegroundInfo(7289,notice,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
    }

    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val pack=EmbeddedModelPack(applicationContext)
        try {
            require(pack.packaged()) {
                "This APK does not contain an embedded model pack"
            }
            val models=pack.readManifest()
            val allBytes=models.sumOf{it.bytes}
            var prior=models.filter{pack.installed(it)}.sumOf{it.bytes}
            notify((prior*100/allBytes).toInt(),"Verifying bundled models")
            for(model in models) {
                if(isStopped)error("Cancelled model unpacking")
                if(pack.installed(model))continue
                pack.install(model,{bytes,_->
                    val percentage=((prior+bytes)*100/allBytes)
                        .toInt().coerceIn(0,100)
                    setProgressAsync(Data.Builder()
                        .putString("model",model.id)
                        .putInt("percent",percentage)
                        .putLong("processed",prior+bytes)
                        .putLong("total",allBytes)
                        .build())
                },{isStopped})
                prior+=model.bytes
                notify((prior*100/allBytes).toInt(),model.filename)
            }
            check(pack.allReady()) {
                "Bundled local models could not be verified on this device"
            }
            setProgress(Data.Builder().putInt("percent",100)
                .putString("model","ready").build())
            Result.success()
        }catch(e:Exception) {
            Result.failure(Data.Builder()
                .putString("error",e.message?:"Bundled model preparation failed")
                .build())
        }
    }
}
