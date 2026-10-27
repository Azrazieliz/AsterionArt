package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.*
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.engine.GenerationPass
import com.asterion.art.engine.SourceReferenceEncoder
import com.asterion.art.model.SheetRole
import com.asterion.art.model.TaskKind
import com.asterion.art.model.TaskStatus
import com.asterion.art.nativeengine.EmbeddedEngineFactory
import com.asterion.art.render.SheetMasterPrompts
import com.asterion.art.vision.VisionModelStore
import com.asterion.art.vision.ReferenceVisualInterpreter
import com.asterion.art.engine.SheetTextReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException
import kotlin.math.abs

/**
 * Real one-shot neural master-sheet generation from ONE source image by default.
 * No per-panel crop copying, no multi-photo requirement, no artificial success.
 */
class SheetMasterGenerationWorker(ctx:Context,params:WorkerParameters):
    CoroutineWorker(ctx,params) {
    companion object {
        private const val CHANNEL="asterion-sheet-neural"
        fun enqueue(context:Context,sheetId:String,taskId:String) {
            val request=OneTimeWorkRequestBuilder<SheetMasterGenerationWorker>()
                .setInputData(Data.Builder().putString("sheet",sheetId)
                    .putString("task",taskId).build())
                .addTag("asterion-sheet-neural").build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-sheet-"+sheetId,ExistingWorkPolicy.KEEP,request)
        }
    }
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val sheetId=inputData.getString("sheet")?:return@withContext Result.failure()
        val task=inputData.getString("task")?:return@withContext Result.failure()
        val app=applicationContext
        val repo=ArtRepository(app)
        val sheet=repo.sheets.firstOrNull{it.id==sheetId}
            ?:return@withContext Result.failure()
        val manager=app.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,
            "Neural reference sheet generation",NotificationManager.IMPORTANCE_LOW))
        suspend fun foreground(label:String) {
            setForeground(ForegroundInfo(7308,Notification.Builder(app,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle("Generating "+sheet.role.display+" master sheet")
                .setContentText(label).setOngoing(true).build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            repo.updateTaskProgress(task,0,0,label)
        }
        var engine:com.asterion.art.engine.StillImageEngine?=null
        try {
            repo.updateTask(task,TaskStatus.RUNNING)
            foreground("Reading your original image")
            val sources=sheet.sourceUris
            require(sources.isNotEmpty()){"Select at least one reference image"}
            val encoded=SourceReferenceEncoder.encodeUris(app,sources)
            val observations=if(VisionModelStore(app).installed()!=null){
                foreground("Analyzing actual reference pixels")
                val evidence=ReferenceVisualInterpreter(app).enrich(
                    listOf(sheet),listOf(SheetTextReader.read(app,sheet)),onProgress={
                        repo.updateTaskProgress(task,0,0,it)
                    })
                evidence.single().all().joinToString(" ")
            }else ""
            val positive=SheetMasterPrompts.positive(sheet.role,observations)
            val negative=SheetMasterPrompts.negative(sheet.role)
            engine=EmbeddedEngineFactory.open(app)
            val models=engine.installedModels()
            require(models.isNotEmpty()){"Install a compatible local image-generation model"}
            val conditioned=models.filter{
                it.ditKind in setOf("qwen21","klein")
            }
            val primary=conditioned.firstOrNull()?:models.first()
            val resolution=com.asterion.art.nativeengine.NativeResolutionPolicy
                .choose(primary,"9:16")
            // Do not pretend a square-only neural model rendered a 9:16
            // artwork. The output must honestly satisfy the sheet contract.
            require(abs(resolution.first.toDouble()/resolution.second-9.0/16.0)<0.16) {
                "The installed QNN model has no usable portrait generation size. "+
                "Install a compatible portrait-capable image model."
            }
            foreground("Loading local QNN model · "+primary.name)
            engine.prepare(primary,resolution.first,resolution.second){isStopped}
            engine.verifyText(positive)
            engine.verifyText(negative)
            val selected=repo.settings.copy(batch=1,ratio="9:16",
                automatic=false,steps=repo.settings.steps.coerceAtLeast(25))
            val referencePixels=if(primary.ditKind in setOf("qwen21","klein"))
                encoded else emptyList()
            val previous=if(referencePixels.isEmpty()) {
                // On Anima/SDXL use the source pixels through true img2img;
                // never discard the only reference.
                val decoded=BitmapFactory.decodeByteArray(encoded[0],0,encoded[0].size)
                    ?:error("Cannot decode canonical reference")
                try{
                    ByteArrayOutputStream().use{out->
                        check(decoded.compress(Bitmap.CompressFormat.PNG,100,out))
                        out.toByteArray()
                    }
                }finally{decoded.recycle()}
            }else null
            val denoise=if(previous!=null)0.78f else 1.0f
            foreground("Generating complete master sheet in one native inference")
            val output=engine.generate(GenerationPass(1,positive,negative,denoise),
                selected,resolution.first,resolution.second,previous,
                selected.seed?:System.currentTimeMillis(),
                {isStopped},{done,total->
                    repo.updateTaskProgress(task,done,total.coerceAtLeast(1),
                        "One-shot sheet · "+done+"/"+total)
                },referencePngs=referencePixels)
            val info=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeByteArray(output.png,0,output.png.size,info)
            require(info.outWidth>0&&info.outHeight>0){"Native model returned invalid image"}
            val saved=GeneratedImagePublisher.publish(app,sheet.id,1,output.png)
            repo.finishSheet(sheet.id,saved.toString())
            repo.updateTask(task,TaskStatus.COMPLETED)
            Result.success()
        }catch(e:CancellationException){
            repo.updateTask(task,TaskStatus.CANCELLED,"Neural sheet generation cancelled")
            Result.failure()
        }catch(e:Throwable) {
            repo.updateTask(task,TaskStatus.FAILED,
                "Neural sheet creation: "+(e.message?:e.javaClass.simpleName))
            Result.failure()
        }finally{runCatching{engine?.release()}}
    }
}
