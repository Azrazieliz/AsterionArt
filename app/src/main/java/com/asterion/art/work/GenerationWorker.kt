package com.asterion.art.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.asterion.art.data.ArtRepository
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.core.CoreResultUploader
import com.asterion.art.data.GenerationJobStore
import com.asterion.art.engine.GenerationPlanner
import com.asterion.art.methodology.DesignBlueprintEngine
import com.asterion.art.engine.LocalDreamBridge
import com.asterion.art.engine.StillImageEngine
import com.asterion.art.nativeengine.EmbeddedEngineFactory
import com.asterion.art.nativeengine.ModelOutputRouting
import com.asterion.art.engine.availableUpscalers
import com.asterion.art.engine.upscaleImage
import com.asterion.art.learning.PreferenceLearning
import com.asterion.art.learning.CreativeMemoryStore
import com.asterion.art.learning.SavedDesign
import com.asterion.art.engine.UniversalCreativePlanner
import com.asterion.art.engine.ConceptTaxonomy
import com.asterion.art.methodology.DetailedMethodologies
import com.asterion.art.engine.GenerationRecipe
import com.asterion.art.engine.GenerationPass
import com.asterion.art.engine.SheetTextReader
import com.asterion.art.engine.TechnicalImageScorer
import com.asterion.art.enhancement.EmbeddedRealSr
import com.asterion.art.engine.SourceReferenceEncoder
import com.asterion.art.model.RenderMode
import com.asterion.art.model.CreationMode
import com.asterion.art.model.SheetRecord
import com.asterion.art.model.SheetRole
import com.asterion.art.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/** Foreground-compatible WorkManager pipeline using genuine sequential native passes. */
class GenerationWorker(context: Context, params: WorkerParameters):
    CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL = "asterion_art_generator"
        private const val JOB = "job_id"
        private const val TASK = "task_id"
        fun enqueue(context: Context, jobId: String, taskId: String) {
            val data = Data.Builder().putString(JOB, jobId).putString(TASK, taskId).build()
            val work = OneTimeWorkRequestBuilder<GenerationWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("asterion-native-generation").addTag("asterion-job-" + jobId).build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "asterion-native-execution", ExistingWorkPolicy.APPEND_OR_REPLACE, work)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val jobId = inputData.getString(JOB) ?: return@withContext Result.failure()
        val taskId = inputData.getString(TASK) ?: return@withContext Result.failure()
        val repo = ArtRepository(applicationContext)
        var currentEngine: StillImageEngine? = null
        try {
            foreground("Reading generation plan")
            val job = GenerationJobStore(applicationContext).load(jobId)
            require(job.mode != RenderMode.ANIMATION) {
                "Animation needs the video backend, not the still generator"
            }
            val sheets = job.sheetIds.map { id ->
                repo.sheets.firstOrNull { it.id == id }
                    ?: error("Source sheet missing: " + id)
            }
            require(sheets.all { it.outputUri != null }) {
                "Reference sheet creation is not complete"
            }
            repo.updateTask(taskId, TaskStatus.RUNNING)
            repo.updateTaskProgress(taskId,0,100,"Reading source sheets")
            foreground("Reading source sheet evidence")
            val evidence = sheets.map { SheetTextReader.read(applicationContext, it) }
            require(job.creationMode != CreationMode.STYLE_CORRECTION ||
                job.sourceImageUri != null) {
                "Style correction requires a source illustration, not just a style sheet."
            }
            val taxonomy=ConceptTaxonomy(applicationContext)
            val compilerAttempt = runCatching {
                if(!job.designPrompt.isNullOrBlank()) GenerationRecipe(
                    job.mode,listOf(GenerationPass(1,job.designPrompt,
                        job.designNegative.orEmpty(),1f)),job.sheetIds,job.settings)
                else if(job.creationMode in listOf(CreationMode.CHARACTER,CreationMode.CREATURE))
                    GenerationPlanner.compileConcepts(
                        job.concepts,requireNotNull(job.creationMode),job.mode,job.settings,taxonomy)
                else GenerationPlanner.compile(evidence,job.mode,job.settings,
                    job.creationMode,job.concepts,job.sourceImageUri!=null,taxonomy)
            }
            val engine = EmbeddedEngineFactory.open(applicationContext)
            currentEngine = engine
            val models = engine.installedModels()
            val taggedPreferred = ModelOutputRouting(applicationContext)
                .preferred(job.mode, models)
            val preferredId = taggedPreferred?.id ?: GenerationPlanner.selectModelId(
                models.map { it.id to it.name }, job.mode)
            val visualModel = models.firstOrNull { it.ditKind == "qwen21" }
                ?: models.firstOrNull { it.ditKind == "klein" }
            var model = models.firstOrNull { it.id == preferredId }
            var recipe = compilerAttempt.getOrNull()
            if (model == null || recipe == null) {
                model = visualModel ?: error(
                    "Text-only preferred model or authoritative text is unavailable and " +
                        "no reference-aware Qwen/Klein model is installed.")
                recipe = if(!job.designPrompt.isNullOrBlank()) GenerationRecipe(
                    job.mode,listOf(GenerationPass(1,job.designPrompt,
                        job.designNegative.orEmpty(),1f)),job.sheetIds,job.settings)
                else if(job.creationMode in listOf(CreationMode.CHARACTER,CreationMode.CREATURE))
                    GenerationPlanner.compileConcepts(
                        job.concepts,requireNotNull(job.creationMode),job.mode,job.settings,taxonomy)
                else GenerationPlanner.compileVisual(evidence,job.mode,job.settings,
                    job.creationMode,job.concepts,job.sourceImageUri!=null,taxonomy)
            }
            var size = model.generationSize.coerceIn(512, 1536)
            var resolution = if (job.settings.ratio == "16:9")
                model.resolutions.firstOrNull { (w,h) -> w*9==h*16 }
                    ?: (1024 to 576)
            else model.resolutions.firstOrNull() ?: (size to size)
            repo.updateTaskProgress(taskId,0,0,"Loading QNN model · "+model.name)
            foreground("Loading " + model.name)
            engine.prepare(model, resolution.first, resolution.second) { isStopped }
            // Character Creation prompts are per-sheet and must be checked AFTER
            // the actual five blueprint descriptions are compiled.
            val capacityCheck = runCatching {
                if (job.creationMode != CreationMode.CHARACTER) {
                    recipe.passes.forEach { pass ->
                        engine.verifyText(pass.positive)
                        if (pass.negative.isNotBlank()) engine.verifyText(pass.negative)
                    }
                }
            }
            if (capacityCheck.isFailure && model.ditKind != "qwen21" &&
                model.ditKind != "klein" && visualModel != null) {
                // Don't truncate authoritative prompt text. Switch to a genuine
                // reference-conditioned engine instead of dropping source facts.
                model = visualModel
                recipe = if(!job.designPrompt.isNullOrBlank()) GenerationRecipe(
                    job.mode,listOf(GenerationPass(1,job.designPrompt,
                        job.designNegative.orEmpty(),1f)),job.sheetIds,job.settings)
                else if(job.creationMode in listOf(CreationMode.CHARACTER,CreationMode.CREATURE))
                    GenerationPlanner.compileConcepts(
                        job.concepts,requireNotNull(job.creationMode),job.mode,job.settings,taxonomy)
                else GenerationPlanner.compileVisual(evidence,job.mode,job.settings,
                    job.creationMode,job.concepts,job.sourceImageUri!=null,taxonomy)
                size = model.generationSize.coerceIn(512, 1536)
                resolution = if(job.settings.ratio=="16:9")
                    model.resolutions.firstOrNull { (w,h)->w*9==h*16 } ?: (1024 to 576)
                else model.resolutions.firstOrNull() ?: (size to size)
                foreground("Prompt exceeded text context; using " + model.name + " visual conditioning")
                engine.prepare(model, resolution.first, resolution.second) { isStopped }
                recipe.passes.forEach { pass -> engine.verifyText(pass.positive) }
            } else {
                capacityCheck.getOrThrow()
            }
            val extraOriginal = job.sourceImageUri?.let { uri ->
                SheetRecord(role=SheetRole.CHARACTER,label="Original source image",
                    sourceUris=listOf(uri),referenceSlots=listOf("PRIMARY"))
            }
            val referenceInputs = if (model.ditKind == "qwen21" ||
                model.ditKind == "klein") SourceReferenceEncoder.encode(
                    applicationContext,if(extraOriginal!=null) sheets + extraOriginal else sheets)
                else emptyList()
            val originalImage = if(extraOriginal!=null) SourceReferenceEncoder.encode(
                applicationContext,listOf(extraOriginal)).single() else null

            val intermediate = File(applicationContext.filesDir,
                "generation-checkpoints/" + job.id).also { it.mkdirs() }
            if (job.creationMode == CreationMode.CHARACTER) {
                val capsules = JSONArray()
                DesignBlueprintEngine.population(job.concepts,resolver=taxonomy).forEach { design ->
                    capsules.put(JSONObject().apply {
                        put("id", design.id)
                        put("index", design.number)
                        put("concepts", JSONArray(design.concepts))
                        put("morphology", design.morphology)
                        put("face_and_hair", design.faceAndHair)
                        put("mechanics", design.internalMechanics)
                        put("fusion", design.conceptFusion)
                        put("renderer_description", design.render())
                        put("frozen", false)
                    })
                }
                GeneratedImagePublisher.atomicWrite(
                    File(applicationContext.filesDir,
                        "generation-plans/" + job.id + "-capsules.json"),
                    JSONObject().put("schema", "asterion.design-capsules.v1")
                        .put("job_id", job.id).put("designs", capsules)
                        .toString(2).toByteArray(Charsets.UTF_8))
            }
            val out = mutableListOf<JSONObject>()
            var technicalLeaderScore = Double.NEGATIVE_INFINITY
            var technicalLeader: ByteArray? = null
            var technicalLeaderUri: android.net.Uri? = null
            val count = if(!job.designPrompt.isNullOrBlank()) 1
                else job.settings.batch.coerceIn(1, 8)
            // v3 Character Creation explicitly prescribes ONE generation call
            // per fully specified analytical sheet; artwork uses five passes.
            val oneShotCharacterSheet = job.creationMode == CreationMode.CHARACTER ||
                !job.designPrompt.isNullOrBlank()
            val stageSequence = if (oneShotCharacterSheet)
                listOf(recipe.passes.first()) else recipe.passes
            val passesPerCandidate = stageSequence.size
            repo.updateTaskProgress(taskId,0,(count*passesPerCandidate+2)*100,"Beginning generation")
            for (candidate in 1..count) {
                if (isStopped) throw CancellationException("Job stopped")
                val seed = job.settings.seed ?: ((job.createdAt + candidate * 1049L) and 0x7FFFFFFFL)
                val candidateSettings = PreferenceLearning.candidate(job.settings,candidate - 1)
                var current: ByteArray? = originalImage
                for (stage in stageSequence) {
                    if (isStopped) throw CancellationException()
                    val path = File(intermediate, "candidate_" + candidate +
                        "_pass_" + stage.number + ".png")
                    if (path.isFile && path.length() > 100) {
                        current = path.readBytes()
                        continue
                    }
                    thermalGate()
                    val stageLabel = "Image " + candidate + "/" + count +
                        (if(oneShotCharacterSheet) " · one-shot master sheet"
                         else " · pass " + stage.number + "/5")
                    repo.updateTaskProgress(taskId, ((candidate-1)*passesPerCandidate+stage.number-1)*100,
                        (count*passesPerCandidate+2)*100, stageLabel)
                    foreground(stageLabel)
                    val methodStage = if(!job.designPrompt.isNullOrBlank()) stage
                    else if (job.creationMode==CreationMode.CHARACTER)
                        stage.copy(
                            positive=DesignBlueprintEngine.sheetPrompt(job.concepts,candidate-1,taxonomy),
                            negative=DesignBlueprintEngine.negative())
                    else stage
                    // Validate the *actual* model input, including page-specific
                    // character descriptions. The earlier bug checked only the base.
                    engine.verifyText(methodStage.positive)
                    if (methodStage.negative.isNotBlank())
                        engine.verifyText(methodStage.negative)
                    val result = engine.generate(
                        methodStage, candidateSettings, resolution.first, resolution.second,
                        current, seed, { isStopped }, { step, totalSteps ->
                            if (totalSteps > 0 && step >= 0 && step % 3 == 0) {
                                repo.updateTaskProgress(taskId, (candidate-1)*passesPerCandidate*100 +
                                    (stage.number-1)*100 + (step*100/totalSteps),
                                    (count*passesPerCandidate+2)*100, stageLabel + " · " + step + "/" + totalSteps)
                            }
                        },
                        referencePngs = referenceInputs)
                    current = result.png
                    GeneratedImagePublisher.atomicWrite(path, result.png)
                    setProgress(Data.Builder().putInt("candidate", candidate)
                        .putInt("pass", stage.number).putInt("total", count * passesPerCandidate).build())
                }
                val image = current ?: error("No image produced")
                val technical = TechnicalImageScorer.score(image)
                if (technical.score > technicalLeaderScore) {
                    technicalLeaderScore = technical.score
                    technicalLeader = image
                }
                val uri = GeneratedImagePublisher.publish(
                    applicationContext, job.id, candidate, image)
                if (technical.score == technicalLeaderScore) technicalLeaderUri = uri
                repo.registerAsset(uri.toString(), sourceSheetId = null,
                    autoScore = technical.score, sourceJobId = job.id)
                // Every design is independently selectable, including catalogue
                // slots from sheets produced by any Creative Studio. The sheet
                // remains a reference until separately regenerated as one image.
                val perSheet=if(job.designPrompt!=null) 1
                    else job.creationMode?.let {
                        DetailedMethodologies.all[it]?.catalogue?.designsPerImage
                    } ?: 1
                val memory=CreativeMemoryStore(applicationContext)
                val blueprints=if(job.creationMode==CreationMode.CHARACTER &&
                    job.designPrompt==null) DesignBlueprintEngine.population(job.concepts,resolver=taxonomy)
                    else emptyList()
                for(index in 0 until perSheet) {
                    val ordinal=(candidate-1)*perSheet+index+1
                    val specification=blueprints.getOrNull(ordinal-1)?.render()
                        ?: if(job.designPrompt!=null) job.designPrompt
                        else UniversalCreativePlanner.individual(
                            UniversalCreativePlanner.domain(job.creationMode),
                            job.concepts,index+(candidate-1)*perSheet,
                            stageSequence.first().positive)
                    memory.upsert(SavedDesign(
                        id=job.id+"-D"+ordinal.toString().padStart(3,'0'),
                        jobId=job.id,mode=job.mode,creationMode=job.creationMode,
                        ordinal=ordinal,sheetNumber=candidate,prompt=specification,
                        negative=if(job.creationMode==CreationMode.CHARACTER &&
                            job.designPrompt==null) DesignBlueprintEngine.negative()
                            else stageSequence.first().negative,sheetUri=uri.toString(),
                        status=if(job.designPrompt!=null)"REVISION" else "CANDIDATE",
                        parents=job.parentDesignIds,sourceSheetIds=job.sheetIds))
                }
                val coreSync = runCatching {
                    CoreResultUploader(applicationContext).upload(
                        uri, job.id,job.mode,GeneratedImagePublisher.sha256(image))
                }
                out += JSONObject().apply {
                    put("candidate", candidate)
                    put("uri", uri.toString())
                    put("prompt_positive", if (job.designPrompt!=null)
                        job.designPrompt
                        else if (job.creationMode==CreationMode.CHARACTER)
                        DesignBlueprintEngine.sheetPrompt(job.concepts,candidate-1,taxonomy)
                        else stageSequence.joinToString("\n\n") { it.positive })
                    put("prompt_negative", if(job.designPrompt!=null)
                        job.designNegative.orEmpty()
                        else if (job.creationMode==CreationMode.CHARACTER)
                        DesignBlueprintEngine.negative()
                        else stageSequence.joinToString("\n\n") { it.negative })
                    put("technical_score", technical.score)
                    put("contrast", technical.contrast)
                    put("detail", technical.detail)
                    put("clip_ratio", technical.clipped)
                    put("sha256", GeneratedImagePublisher.sha256(image))
                    put("core_saved_uri",coreSync.getOrNull()?.toString() ?: JSONObject.NULL)
                    put("core_sync_error",coreSync.exceptionOrNull()?.message ?: JSONObject.NULL)
                    put("seed", seed)
                    put("cfg", candidateSettings.cfg.toDouble())
                    put("steps", candidateSettings.steps)
                    put("denoise", candidateSettings.denoise.toDouble())
                }
            }
            repo.updateTaskProgress(taskId,count*passesPerCandidate*100,(count*passesPerCandidate+2)*100,
                "Ranking and enhancing results")
            var enhancementStatus = "NOT_REQUESTED"
            var enhancedUri: String? = null
            if (job.settings.automatic && technicalLeaderUri != null) {
                // Release the 6+ GB NPU model BEFORE starting a Vulkan SR model.
                currentEngine?.release()
                currentEngine = null
                if (EmbeddedRealSr.available(applicationContext)) {
                    foreground("Neural 4× enhancement")
                    enhancementStatus = "NEURAL_REALSR_PENDING"
                    val winner = requireNotNull(technicalLeaderUri)
                    runCatching {
                        val png=EmbeddedRealSr(applicationContext).enhance(
                            winner,4,128,{isStopped},{status->
                                repo.updateTaskProgress(taskId,
                                    count*passesPerCandidate*100,
                                    (count*passesPerCandidate+2)*100,status)
                            })
                        val uri=GeneratedImagePublisher.publish(
                            applicationContext,job.id,98,png)
                        val score=TechnicalImageScorer.score(png)
                        repo.registerAsset(uri.toString(),autoScore=score.score,
                            sourceJobId=job.id)
                        enhancedUri=uri.toString()
                        enhancementStatus="NATIVE_REALSR_4X_COMPLETED"
                    }.onFailure { error ->
                        enhancementStatus="NATIVE_REALSR_FAILED: "+
                            (error.message ?: error.javaClass.simpleName)
                    }
                } else enhancementStatus="NO_EMBEDDED_REALSR"
            }
            repo.updateTaskProgress(taskId,(count*passesPerCandidate+1)*100,(count*passesPerCandidate+2)*100,
                "Finalizing and saving")
            foreground("Saving generation provenance")
            val provenance = JSONObject().apply {
                put("schema", "asterion.generation.v1")
                put("job_id", job.id)
                put("model_id", model.id)
                put("five_real_passes", !oneShotCharacterSheet)
                put("one_shot_master_sheet_methodology", oneShotCharacterSheet)
                put("methodology_certified", false)
                put("reference_sheet_ids", JSONArray(job.sheetIds))
                put("results", JSONArray(out))
                put("automated_scoring", "technical-only; sheet fidelity not evaluated")
                put("native_reference_images", referenceInputs.size)
                put("reference_model_kind", model.ditKind)
                put("creation_methodology", job.creationMode?.name ?: JSONObject.NULL)
                put("selected_concepts", JSONArray(job.concepts))
                put("source_artwork", job.sourceImageUri ?: JSONObject.NULL)
                put("enhancement_status", enhancementStatus)
                put("enhanced_uri", enhancedUri ?: JSONObject.NULL)
            }
            GeneratedImagePublisher.atomicWrite(
                File(applicationContext.filesDir,
                    "generation-plans/" + job.id + "-results.json"),
                provenance.toString(2).toByteArray(Charsets.UTF_8))
            repo.updateTask(taskId, TaskStatus.COMPLETED)
            foreground("Generation completed")
            Result.success()
        } catch (cancel: CancellationException) {
            repo.updateTask(taskId, TaskStatus.FAILED, "Generation cancelled")
            Result.failure()
        } catch (e: Exception) {
            repo.updateTask(taskId, TaskStatus.FAILED, e.message ?: "Generation failed")
            Result.failure()
        } finally {
            currentEngine?.release()
        }
    }

    private suspend fun foreground(message: String) {
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL,
                "Asterion Art", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("Asterion Art")
            .setContentText(message)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
        setForeground(ForegroundInfo(7223, notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
    }

    private suspend fun thermalGate() {
        val power = applicationContext.getSystemService(PowerManager::class.java)
        val start = System.currentTimeMillis()
        while (Build.VERSION.SDK_INT >= 29 &&
            power.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
            if (isStopped) throw CancellationException()
            if (System.currentTimeMillis() - start > 12 * 60_000L)
                error("Thermal condition persisted; generation halted safely")
            foreground("Cooling device before next pass")
            delay(15_000L)
        }
    }
}
