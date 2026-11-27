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
import com.asterion.art.render.OneShotSheetConditioning
import com.asterion.art.render.NeuralSheetPanelRenderer
import com.asterion.art.engine.SourceReferenceEncoder
import com.asterion.art.model.SheetRole
import com.asterion.art.model.TaskKind
import com.asterion.art.model.TaskStatus
import com.asterion.art.nativeengine.EmbeddedEngineFactory
import com.asterion.art.nativeengine.SheetModelSelection
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
 * Native, reference-conditioned master-sheet generation from ONE source image by default.
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
        suspend fun foreground(label:String,resetProgress:Boolean=true) {
            setForeground(ForegroundInfo(7308,Notification.Builder(app,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle("Generating "+sheet.role.display+" master sheet")
                .setContentText(label).setOngoing(true).build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
            if(resetProgress)repo.updateTaskProgress(task,0,0,label)
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
            val basePositive=SheetMasterPrompts.positive(
                sheet.role,observations,sheet.originatingOriginal)
            val baseNegative=SheetMasterPrompts.negative(sheet.role)
            // No generic GGUF/chat interpretation of a methodology at
            // runtime. This sheet path only uses observed source evidence
            // and the native image encoder. A genuinely trained mode-native
            // design operator can be integrated behind a verified adapter;
            // it is never approximated with an untrained generic chatbot.
            val negative=baseNegative
            engine=EmbeddedEngineFactory.open(app)
            val models=engine.installedModels()
            require(models.isNotEmpty()){"Install a compatible local image-generation model"}
            // A named sheet assignment takes precedence, then an installed
            // native pixel-conditioning model, then a single-image fallback.
            // More than one source can NEVER be silently reduced to one.
            val routing=com.asterion.art.nativeengine.ModelOutputRouting(app)
            val sheetAssignment=routing.forMode("SHEET_"+sheet.role.name,models)
            val preferred=routing.preferred(
                com.asterion.art.model.RenderMode.ANIME,models)
            val primary=SheetModelSelection.select(
                sheet.role,models,sheetAssignment,preferred,sources.size)
            val resolution=com.asterion.art.nativeengine.NativeResolutionPolicy
                .choose(primary,"9:16")
            // Anima/SDXL MUST load their fixed 1024-square QNN canvas and
            // request 9:16 via JSON aspect_ratio. Only dynamic DiT models
            // may request a genuinely non-square native canvas.
            if(!primary.isAnima&&!primary.isSdxl)
                require(abs(resolution.first.toDouble()/resolution.second-9.0/16.0)<0.16) {
                    "The selected DiT model does not support a 9:16 canvas"
                }
            foreground("Loading local QNN model · "+primary.name)
            engine.prepare(primary,resolution.first,resolution.second){isStopped}
            // The authoritative Character Sheet V5 calls for one complete
            // reference-conditioned image, not multiple partially repainted
            // panels or token-sliced denoising passes.
            val oneShot=runCatching {
                OneShotSheetConditioning.select(
                    basePositive,null,negative,engine::inspectText)
            }
            if(oneShot.isFailure) {
                val cause=requireNotNull(oneShot.exceptionOrNull())
                // The original owner Character Sheet V5 methodology requires
                // ONE completed 9:16 image in ONE native generation call.
                // Nine separately generated panels would constitute another
                // product, regardless of whether those panels are new pixels.
                if(sheet.role==SheetRole.CHARACTER) {
                    throw IllegalStateException(
                        "Character Sheet V5 requires one complete 9:16 render. "+
                        "The selected native model cannot accept its full "+
                        "specification; use a genuinely compatible encoder.",
                        cause)
                }
                val message=cause.message.orEmpty()
                if(!message.contains("context",ignoreCase=true) &&
                    !message.contains("exceeds",ignoreCase=true))
                    throw cause
                repo.updateTaskProgress(task,0,9,
                    "Model context is too short for one-shot sheet; "+
                    "generating nine distinct source-conditioned views instead")
            }
            val positive=oneShot.getOrNull()
            // Preserve the user's checkpoint-specific sampler settings.
            // Raising every model to 25 steps overrides distilled QNN defaults
            // and can degrade quality as well as generation time.
            val selected=com.asterion.art.nativeengine.NativeTurboDefaults
                .apply(repo.settings,primary).copy(batch=1,ratio="9:16",
                    automatic=false)
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
            if(isStopped)throw CancellationException()
            val output=if(positive!=null) {
                engine.verifyText(positive)
                if(negative.isNotBlank())engine.verifyText(negative)
                foreground("Reconstructing complete 9:16 sheet · one image",false)
                engine.generate(
                    GenerationPass(1,positive,negative,
                        if(previous==null)1.0f else 0.78f),
                    selected,resolution.first,resolution.second,
                    previous,selected.seed?:System.currentTimeMillis(),{isStopped},
                    {done,total->
                        repo.updateTaskProgress(task,done.coerceAtLeast(0),
                            total.coerceAtLeast(0),
                            "One complete image · native diffusion "+
                            done+"/"+total.coerceAtLeast(1))
                    },referencePngs=referencePixels).png
            } else {
                // The user supplies ONE image. Every panel below is a NEW
                // generated view; no source-image crop is substituted for a
                // missing view or passed off as a completed master sheet.
                foreground("Generating nine actual anatomical and detail views",false)
                NeuralSheetPanelRenderer.build(app,sheet.id,sheet.role,
                    sheet.originatingOriginal,engine,selected,
                    resolution.first,resolution.second,encoded.first(),
                    referencePixels,{isStopped},{completed,total,label->
                        repo.updateTaskProgress(task,completed,total,label)
                    })
            }
            com.asterion.art.engine.RenderFidelityGate.requireValidImage(
                output,"Generated "+sheet.role.display+" reference sheet")
            val info=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeByteArray(output,0,output.size,info)
            require(info.outWidth>0&&info.outHeight>0){"Native model returned invalid image"}
            require(abs(info.outWidth.toDouble()/info.outHeight-9.0/16.0)<0.06) {
                "The QNN pipeline returned "+info.outWidth+"×"+info.outHeight+
                    " instead of the required 9:16 sheet. A model with an "+
                    "operational aspect-ratio VAE encoder is needed; the "+
                    "square output will not be incorrectly saved as a sheet."
            }
            val saved=GeneratedImagePublisher.publish(app,sheet.id,1,output)
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
