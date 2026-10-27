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
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.engine.DreamModel
import com.asterion.art.engine.GenerationPass
import com.asterion.art.model.RenderMode
import com.asterion.art.model.TaskKind
import com.asterion.art.model.TaskStatus
import com.asterion.art.nativeengine.EmbeddedEngineFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.CancellationException

/**
 * Real end-to-end, model-selected native image inference, unrelated to
 * any Original's methodology. Output is retained in the normal gallery.
 */
object NativeImageSmokeChoices {
    fun dimensions(model:DreamModel):Pair<Int,Int> {
        val options=model.resolutions.filter { (w,h)->
            w in 256..2048 && h in 256..2048
        }
        return options.minByOrNull{(w,h)->w.toLong()*h}
            ?:model.generationSize.coerceIn(256,2048).let{it to it}
    }
}
class NativeImageSmokeWorker(ctx:Context,params:WorkerParameters):
    CoroutineWorker(ctx,params) {
    companion object {
        private const val CHANNEL="asterion-art-image-smoke"
        fun enqueue(context:Context,modelId:String):String {
            require(modelId.isNotBlank())
            val repo=ArtRepository(context)
            val task=repo.recordTask("native-smoke-"+modelId,TaskKind.STILL_IMAGE)
            val request=OneTimeWorkRequestBuilder<NativeImageSmokeWorker>()
                .setInputData(Data.Builder()
                    .putString("model",modelId).putString("task",task.id).build())
                .addTag("native-image-smoke")
                .addTag("asterion-smoke-"+task.id).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-native-image-smoke",ExistingWorkPolicy.APPEND_OR_REPLACE,
                request)
            return task.id
        }
    }
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val id=inputData.getString("model")?:return@withContext Result.failure()
        val task=inputData.getString("task")?:return@withContext Result.failure()
        val repo=ArtRepository(applicationContext)
        val channel=applicationContext.getSystemService(NotificationManager::class.java)
        channel.createNotificationChannel(NotificationChannel(
            CHANNEL,"Asterion image inference test",NotificationManager.IMPORTANCE_LOW))
        fun notice(stage:String)=Notification.Builder(applicationContext,CHANNEL)
            .setContentTitle("Native image inference · diagnostic")
            .setContentText(stage).setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setOngoing(true).setOnlyAlertOnce(true).build()
        var engine:com.asterion.art.engine.StillImageEngine?=null
        try {
            setForeground(ForegroundInfo(7129,notice("Initializing native image model"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            repo.updateTask(task,TaskStatus.RUNNING)
            engine=EmbeddedEngineFactory.open(applicationContext)
            val model=engine.installedModels().singleOrNull{it.id==id}
                ?:error("Selected QNN image model is no longer installed")
            val (width,height)=NativeImageSmokeChoices.dimensions(model)
            repo.updateTaskProgress(task,0,1,"Loading "+model.name+" · "+
                width+" × "+height)
            engine.prepare(model,width,height){isStopped}
            val prompt="A single hand-illustrated blue ceramic sphere on a "+
                "simple white background, centered and sharply focused."
            val negative="blurry, duplicate, text, watermark"
            engine.verifyText(prompt)
            engine.verifyText(negative)
            repo.updateTaskProgress(task,0,1,"Running genuine QNN diffusion")
            val config=repo.settings.copy(steps=12,batch=1,
                ratio=if(width==height)"1:1" else if(width>height)"4:3" else "3:4",
                seed=73461L,automatic=false)
            val generated=engine.generate(
                GenerationPass(1,prompt,negative,1f),config,width,height,null,
                73461L,{isStopped},{done,total->
                    repo.updateTaskProgress(task,done.coerceAtLeast(0),
                        total.coerceAtLeast(1),"Native inference · "+done+"/"+total)
                })
            check(generated.png.size>1000){"Native engine returned no image pixels"}
            val published=GeneratedImagePublisher.publish(applicationContext,
                "image-smoke-"+UUID.randomUUID().toString(),1,generated.png)
            repo.registerAsset(published.toString(),autoScore=null,sourceJobId=null)
            repo.updateTaskProgress(task,1,1,
                "PASS: real QNN image generated and saved to Gallery")
            repo.updateTask(task,TaskStatus.COMPLETED)
            Result.success()
        } catch(e:CancellationException) {
            repo.updateTask(task,TaskStatus.CANCELLED,"Image inference test cancelled")
            Result.failure()
        } catch(e:Throwable) {
            repo.updateTask(task,TaskStatus.FAILED,
                "Real image inference did not pass: "+(e.message?:e.javaClass.simpleName))
            Result.failure()
        } finally {
            runCatching{engine?.release()}
        }
    }
}
