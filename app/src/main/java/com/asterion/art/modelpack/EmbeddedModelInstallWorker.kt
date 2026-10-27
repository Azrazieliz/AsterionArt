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
            val image=EmbeddedImageModelPack(context)
            if(!pack.packaged()||!image.packaged())return
            if(pack.allReady()&&image.installed())return
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
            val image=EmbeddedImageModelPack(applicationContext)
            require(image.packaged()){"This package lacks its included QNN image model"}
            val imageBytes=image.readManifest().bytes
            val allBytes=models.sumOf{it.bytes}+imageBytes
            var prior=models.filter{pack.installed(it)}.sumOf{it.bytes}
            if(image.installed())prior+=imageBytes
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
            if(!image.installed()) {
                notify((prior*100/allBytes).toInt(),"Preparing included Rin image model")
                image.install({bytes,_->
                    val percentage=((prior+bytes)*100/allBytes)
                        .toInt().coerceIn(0,100)
                    setProgressAsync(Data.Builder()
                        .putString("model",EmbeddedImageModelPack.IMAGE_ID)
                        .putInt("percent",percentage)
                        .putLong("processed",prior+bytes)
                        .putLong("total",allBytes).build())
                },{isStopped})
                prior+=imageBytes
            }
            check(pack.allReady()&&image.installed()) {
                "Complete offline reasoning, vision and image pack is not verified"
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
