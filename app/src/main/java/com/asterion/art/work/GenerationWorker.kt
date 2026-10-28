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
import com.asterion.art.methodology.CharacterPlanStore
import com.asterion.art.methodology.CharacterPhaseOne
import com.asterion.art.methodology.CharacterSheetCompiler
import com.asterion.art.methodology.CharacterSheetComposer
import com.asterion.art.vision.CharacterVisualAuditWorker
import com.asterion.art.vision.VisionModelStore
import com.asterion.art.vision.ReferenceVisualInterpreter
import kotlin.math.abs
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
import com.asterion.art.brain.AsterionReasoner
import com.asterion.art.engine.DesignPreviewCrops
import com.asterion.art.engine.ConceptTaxonomy
import com.asterion.art.methodology.DetailedMethodologies
import com.asterion.art.engine.ConditioningSchedule
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
import com.asterion.art.model.TaskKind
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
            val characterPlan=if(job.creationMode==CreationMode.CHARACTER &&
                job.designPrompt.isNullOrBlank()) {
                CharacterPlanStore(applicationContext).requireAuthorized(
                    job.characterPlanId ?: error("Resolve all characters before generation"),
                    job.id)
            } else null
            require(job.mode != RenderMode.ANIMATION) {
                "Animation needs the video backend, not the still generator"
            }
            val storedSheets = job.sheetIds.map { id ->
                repo.sheets.firstOrNull { it.id == id }
                    ?: error("Source sheet missing: " + id)
            }
            val directSheets=job.directReferenceUris.flatMap{(role,uris)->
                uris.map{uri->
                    SheetRecord(role=role,label="Direct "+role.display+" reference",
                        sourceUris=listOf(uri),referenceSlots=listOf("PRIMARY"))
                }
            }
            val sheets=storedSheets+directSheets
            require(storedSheets.all { it.outputUri != null }) {
                "Reference sheet creation is not complete"
            }
            repo.updateTask(taskId, TaskStatus.RUNNING)
            repo.updateTaskProgress(taskId,0,100,"Reading source sheets")
            foreground("Reading source sheet evidence")
            val transcriptions = sheets.map { SheetTextReader.read(applicationContext, it) }
            // Extract local multimodal visual facts from each actual reference
            // before loading the separate QNN generation model. This never
            // replaces source pixels and never writes a user training grade.
            val evidence = if(sheets.isNotEmpty() &&
                VisionModelStore(applicationContext).installed()!=null) {
                ReferenceVisualInterpreter(applicationContext).enrich(
                    sheets,transcriptions) { stage ->
                        repo.updateTaskProgress(taskId,0,100,stage)
                    }
            } else transcriptions
            require(job.creationMode != CreationMode.STYLE_CORRECTION ||
                job.sourceImageUri != null) {
                "Style correction requires a source illustration, not just a style sheet."
            }
            val taxonomy=ConceptTaxonomy(applicationContext)
            val compilerAttempt = runCatching {
                if(characterPlan!=null) GenerationRecipe(
                    job.mode,listOf(GenerationPass(1,characterPlan.prompts.first(),
                        CharacterPhaseOne.NEGATIVE,1f)),job.sheetIds,job.settings)
                else if(!job.designPrompt.isNullOrBlank()) GenerationRecipe(
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
            val imageSourceCount=(sheets.flatMap{it.sourceUris}+
                job.designReferenceUris+listOfNotNull(job.sourceImageUri)).distinct().size
            require(imageSourceCount<=10){
                "More than 10 source images; select a bounded visual reference set"
            }
            if(imageSourceCount>1 &&
                model?.ditKind !in listOf("qwen21","klein")) {
                model=visualModel ?: error(
                    "Conditioning on multiple image references requires an installed " +
                    "reference-capable Qwen/Klein QNN model; source pixels cannot " +
                    "be silently discarded or replaced with text alone.")
            }
            var recipe = compilerAttempt.getOrNull()
            if (model == null || recipe == null) {
                model = visualModel ?: error(
                    "Text-only preferred model or authoritative text is unavailable and " +
                        "no reference-aware Qwen/Klein model is installed.")
                recipe = if(characterPlan!=null) GenerationRecipe(
                    job.mode,listOf(GenerationPass(1,characterPlan.prompts.first(),
                        CharacterPhaseOne.NEGATIVE,1f)),job.sheetIds,job.settings)
                else if(!job.designPrompt.isNullOrBlank()) GenerationRecipe(
                    job.mode,listOf(GenerationPass(1,job.designPrompt,
                        job.designNegative.orEmpty(),1f)),job.sheetIds,job.settings)
                else if(job.creationMode in listOf(CreationMode.CHARACTER,CreationMode.CREATURE))
                    GenerationPlanner.compileConcepts(
                        job.concepts,requireNotNull(job.creationMode),job.mode,job.settings,taxonomy)
                else GenerationPlanner.compileVisual(evidence,job.mode,job.settings,
                    job.creationMode,job.concepts,job.sourceImageUri!=null,taxonomy)
            }
            // Additional user direction composes with, rather than replaces,
            // each reference authority. Originals retain their own method.
            if(job.creationMode==null &&
                (job.userScenePrompt.isNotBlank()||job.userNegativePrompt.isNotBlank())) {
                recipe=recipe.copy(passes=recipe.passes.map {pass ->
                    pass.copy(
                        positive=pass.positive+
                            if(job.userScenePrompt.isBlank())"" else
                                "\n\nUSER SCENE DIRECTION (PRESERVE CANONICAL REFERENCES):\n"+
                                    job.userScenePrompt,
                        negative=pass.negative+
                            if(job.userNegativePrompt.isBlank())"" else
                                ", "+job.userNegativePrompt)
                })
            }
            // Perform true neural interpretation/synthesis/prompt compilation
            // before the large QNN model is loaded. The GGUF context and KV
            // cache are released in AsterionReasoner.compile's finally block.
            val neural=AsterionReasoner(applicationContext)
            val brainPlan=if(characterPlan!=null) null else if(neural.available()) {
                foreground("Asterion Brain · interpreting and designing")
                val domain=UniversalCreativePlanner.domain(job.creationMode)
                val spec=job.creationMode?.let {
                    DetailedMethodologies.all[it]?.rendererDirective
                }.orEmpty()
                val catalogue=job.creationMode?.let {
                    DetailedMethodologies.all[it]?.catalogue?.designsPerImage
                } ?: 1
                neural.compile(
                    domain=domain,
                    canonicalMethod=spec.ifBlank { recipe.passes.first().positive },
                    concepts=job.concepts,
                    basePositive=recipe.passes.first().positive,
                    baseNegative=recipe.passes.first().negative,
                    jobId=job.id,
                    candidateCount=if(job.creationMode==CreationMode.CHARACTER &&
                        job.designPrompt==null)15 else catalogue.coerceIn(1,15),
                    sheetSources=evidence.flatMap { it.all() },
                    onStage={ stage -> repo.updateTaskProgress(taskId,0,0,stage) },
                    creationMode=job.creationMode,
                    renderMode=job.mode,
                    targetDesignId=job.parentDesignIds.singleOrNull()
                )
            } else null
            if(brainPlan!=null &&
                !(job.creationMode==CreationMode.CHARACTER && job.designPrompt==null)) {
                recipe=recipe.copy(passes=recipe.passes.map { pass ->
                    pass.copy(
                        positive=pass.positive+"\n\nNEURAL DESIGN SPECIFICATION:\n"+
                            brainPlan.positive,
                        negative=listOf(pass.negative,brainPlan.negative)
                            .filter { it.isNotBlank() }.distinct().joinToString(", "))
                })
            }
            var size = model.generationSize.coerceIn(512, 1536)
            var resolution = com.asterion.art.nativeengine.NativeResolutionPolicy
                .choose(model,if(characterPlan!=null)"16:45" else job.settings.ratio)
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
                recipe = if(characterPlan!=null) GenerationRecipe(
                    job.mode,listOf(GenerationPass(1,characterPlan.prompts.first(),
                        CharacterPhaseOne.NEGATIVE,1f)),job.sheetIds,job.settings)
                else if(!job.designPrompt.isNullOrBlank()) GenerationRecipe(
                    job.mode,listOf(GenerationPass(1,job.designPrompt,
                        job.designNegative.orEmpty(),1f)),job.sheetIds,job.settings)
                else if(job.creationMode in listOf(CreationMode.CHARACTER,CreationMode.CREATURE))
                    GenerationPlanner.compileConcepts(
                        job.concepts,requireNotNull(job.creationMode),job.mode,job.settings,taxonomy)
                else GenerationPlanner.compileVisual(evidence,job.mode,job.settings,
                    job.creationMode,job.concepts,job.sourceImageUri!=null,taxonomy)
                size = model.generationSize.coerceIn(512, 1536)
                // A fallback QNN engine must preserve the narrow, upright
                // individual-character triptych, never switch to the final
                // 16:9 five-character sheet's landscape resolution.
                resolution = com.asterion.art.nativeengine.NativeResolutionPolicy
                    .choose(model,if(characterPlan!=null)"16:45" else job.settings.ratio)
                foreground("Prompt exceeded text context; using " + model.name + " visual conditioning")
                engine.prepare(model, resolution.first, resolution.second) { isStopped }
                recipe.passes.forEach { pass ->
                    check(engine.inspectText(pass.positive).limit>0) {
                        "Selected model does not report native token capacity"
                    }
                }
            } else {
                if(capacityCheck.isFailure &&
                    capacityCheck.exceptionOrNull()?.message
                        ?.contains("exceeds",ignoreCase=true)!=true) {
                    capacityCheck.getOrThrow()
                }
            }
            val extraOriginal = job.sourceImageUri?.let { uri ->
                SheetRecord(role=SheetRole.CHARACTER,label="Original source image",
                    sourceUris=listOf(uri),referenceSlots=listOf("PRIMARY"))
            }
            // The GENERATED authoritative master sheet must reach the
            // neural reference-conditioning path. Keep original source pixels
            // available as secondary evidence, without replacing the master.
            val combinedSourceUris=sheets.flatMap { sheet ->
                listOfNotNull(sheet.outputUri)+sheet.sourceUris
            }.distinct()+
                listOfNotNull(extraOriginal?.sourceUris?.firstOrNull())+
                job.designReferenceUris
            val referenceInputs = if (model.ditKind == "qwen21" ||
                model.ditKind == "klein") {
                if(combinedSourceUris.isEmpty())emptyList()
                else SourceReferenceEncoder.encodeUris(
                    applicationContext,combinedSourceUris)
            } else emptyList()
            // Render changes from the selected individual design's real pixels,
            // never a full multi-character catalogue or only its saved prompt.
            val originalSource=job.designReferenceUris.singleOrNull()
                ?:extraOriginal?.sourceUris?.firstOrNull()
                // Anima/SDXL cannot consume multi-reference images as a
                // separate conditioning tensor. Keep the actual primary
                // character pixels as an img2img source rather than silently
                // replacing the selected character with prose alone.
                ?:if(job.creationMode==null)
                    sheets.firstOrNull{it.role==SheetRole.CHARACTER}
                        ?.sourceUris?.firstOrNull()
                  else null
            val originalImage=originalSource?.let {
                SourceReferenceEncoder.encodeUris(applicationContext,listOf(it)).single()
            }

            val intermediate = File(applicationContext.filesDir,
                "generation-checkpoints/" + job.id).also { it.mkdirs() }
            if(characterPlan!=null) {
                val capsules=JSONArray()
                characterPlan.frozen.forEach { d ->
                    capsules.put(JSONObject().apply {
                        put("id",d.id)
                        put("index",d.number)
                        put("concepts",JSONArray(d.primary.map{it.name}))
                        put("anatomy",d.anatomy)
                        put("face_eyes_hair",d.faceEyesHair)
                        put("topology",d.topology)
                        put("fusion",d.fusion)
                        put("materials",d.materials)
                        put("differentiation",d.differentiation)
                        put("render_description",d.renderDescription)
                        put("audit_evidence",d.auditEvidence)
                        put("frozen",true)
                    })
                }
                GeneratedImagePublisher.atomicWrite(
                    File(applicationContext.filesDir,
                        "generation-plans/"+job.id+"-capsules.json"),
                    JSONObject().put("schema","asterion.character-frozen.v2")
                        .put("job_id",job.id)
                        .put("design_plan_id",characterPlan.id)
                        .put("designs",capsules)
                        .toString(2).toByteArray(Charsets.UTF_8))
            }
            val out = mutableListOf<JSONObject>()
            var technicalLeaderScore = Double.NEGATIVE_INFINITY
            var technicalLeader: ByteArray? = null
            var technicalLeaderUri: android.net.Uri? = null
            val count = com.asterion.art.engine.OriginalOutputPolicy.imageCount(
                job.creationMode,!job.designPrompt.isNullOrBlank(),
                characterPlan?.sheets,job.settings.batch)
            // v3 Character Creation explicitly prescribes ONE generation call
            // per fully specified analytical sheet; artwork uses five passes.
            val oneShotCharacterSheet = !job.designPrompt.isNullOrBlank() ||
                characterPlan!=null
            val stageSequence = if (oneShotCharacterSheet)
                listOf(recipe.passes.first()) else recipe.passes
            val passesPerCandidate = stageSequence.size
            repo.updateTaskProgress(taskId,0,(count*passesPerCandidate+2)*100,"Beginning generation")
            for (candidate in 1..count) {
                if (isStopped) throw CancellationException("Job stopped")
                val seed = job.settings.seed ?: ((job.createdAt + candidate * 1049L) and 0x7FFFFFFFL)
                val candidateSettings = PreferenceLearning.candidate(job.settings,candidate - 1)
                val image:ByteArray=if(characterPlan!=null) {
                    // Five independent Character Creation triptychs use a
                    // 16:45 output crop. Anima/SDXL still load their fixed
                    // 1024-square QNN CANVAS; the native server crops 16:45.
                    val nativeSettings=candidateSettings.copy(ratio="16:45",batch=1)
                    val tiles=(0 until CharacterPhaseOne.PER_SHEET).map{cell ->
                        if(isStopped)throw CancellationException("Generation cancelled")
                        val d=characterPlan.frozen[(candidate-1)*5+cell]
                        val checkpoint=File(intermediate,
                            "sheet_"+candidate+"_"+d.id+"_triptych.png")
                        if(checkpoint.isFile && checkpoint.length()>1024L) {
                            val existing=checkpoint.readBytes()
                            check(TechnicalImageScorer.score(existing).score.isFinite())
                            existing
                        } else {
                            thermalGate()
                            val directive=CharacterSheetCompiler.individual(d)
                            val identityAnchor=
                                "Continue SAME frozen character "+d.id+
                                " with exact concept anatomy "+
                                d.primary.joinToString(" x "){it.name}+
                                ". Never change body, facial identity, appendage roots, "+
                                "clinical white field, or 2D illustration style.\n"
                            val conditioning=ConditioningSchedule.compile(
                                directive,CharacterPhaseOne.NEGATIVE,
                                engine::inspectText,continuationHeader=identityAnchor)
                            val ledger=JSONObject().apply{
                                put("schema","asterion.character-triptych-conditioning.v1")
                                put("job_id",job.id)
                                put("character",d.id)
                                put("native_image_encoder_expanded",false)
                                put("chunks",JSONArray(conditioning.chunks.map{part->
                                    JSONObject().put("index",part.index)
                                        .put("tokens",part.verifiedTokens)
                                        .put("limit",part.verifiedLimit)
                                        .put("positive",part.positive)
                                }))
                            }
                            GeneratedImagePublisher.atomicWrite(
                                File(intermediate,"sheet_"+candidate+"_"+d.id+"_tokens.json"),
                                ledger.toString(2).toByteArray(Charsets.UTF_8))
                            var previous:ByteArray?=null
                            for(part in conditioning.chunks) {
                                val pass=GenerationPass(part.index,part.positive,
                                    part.negative,part.denoise)
                                if(isStopped)throw CancellationException()
                                engine.verifyText(pass.positive)
                                if(pass.negative.isNotBlank())
                                    engine.verifyText(pass.negative)
                                val progressStage="Sheet "+candidate+"/"+count+
                                    " · identity "+d.id+
                                    " · conditioning "+part.index+"/"+conditioning.chunks.size
                                foreground(progressStage)
                                val result=engine.generate(pass,nativeSettings,
                                    resolution.first,resolution.second,previous,
                                    seed+cell*104729L+part.index,
                                    {isStopped},{step,totalSteps->
                                        if(totalSteps>0&&step>=0&&step%4==0)
                                            repo.updateTaskProgress(taskId,
                                                (candidate-1)*100+cell*20+
                                                    step*20/(5*totalSteps),
                                                count*100,progressStage)
                                    },referencePngs=referenceInputs)
                                previous=result.png
                            }
                            val completed=previous?:
                                error("Native model did not produce "+d.id)
                            GeneratedImagePublisher.atomicWrite(checkpoint,completed)
                            completed
                        }
                    }
                    CharacterSheetComposer.assemble(tiles)
                } else {
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
                    val methodStage = if(!job.designPrompt.isNullOrBlank()) stage.copy(
                        denoise=if(job.designReferenceUris.isNotEmpty() ||
                            job.sourceImageUri!=null)candidateSettings.denoise
                            else stage.denoise)
                    else if(job.creationMode==null && stage.number==1 &&
                        current!=null && model.ditKind !in listOf("qwen21","klein")) {
                        // One real source image provides visual identity on
                        // Anima/SDXL. Denoise=1.0 would erase that authority.
                        stage.copy(denoise=0.72f)
                    }
                    else if(characterPlan!=null)
                        stage.copy(positive=characterPlan.prompts[candidate-1],
                            negative=CharacterPhaseOne.NEGATIVE)
                    else stage
                    // Measure native conditioning after the exact final
                    // methodology and sheet prompt has been compiled.
                    val schedule=ConditioningSchedule.compile(
                        methodStage.positive,methodStage.negative,engine::inspectText)
                    if(schedule.chunks.size>1) {
                        val ledger=JSONObject().apply {
                            put("schema","asterion.conditioning-schedule.v1")
                            put("model",model.name)
                            put("job_id",job.id)
                            put("candidate",candidate)
                            put("stage",stage.number)
                            put("not_extended_cross_attention",true)
                            put("chunks",JSONArray(schedule.chunks.map { chunk ->
                                JSONObject().put("index",chunk.index)
                                    .put("count",chunk.verifiedTokens)
                                    .put("limit",chunk.verifiedLimit)
                                    .put("positive",chunk.positive)
                            }))
                        }
                        GeneratedImagePublisher.atomicWrite(
                            File(intermediate,"candidate_"+candidate+"_pass_"+
                                stage.number+"_conditioning.json"),
                            ledger.toString(2).toByteArray(Charsets.UTF_8))
                    }
                    schedule.chunks.forEach { chunk ->
                        if(isStopped)throw CancellationException()
                        val actual=methodStage.copy(
                            positive=chunk.positive,negative=chunk.negative,
                            denoise=if(chunk.index==1)methodStage.denoise
                                else minOf(methodStage.denoise,chunk.denoise))
                        engine.verifyText(actual.positive)
                        if(actual.negative.isNotBlank())
                            engine.verifyText(actual.negative)
                        val result=engine.generate(
                            actual,candidateSettings,resolution.first,resolution.second,
                            current,seed+chunk.index-1,{isStopped},{step,totalSteps->
                                if(totalSteps>0 && step>=0 && step%3==0) {
                                    repo.updateTaskProgress(taskId,
                                        (candidate-1)*passesPerCandidate*100+
                                        (stage.number-1)*100+(step*100/totalSteps),
                                        (count*passesPerCandidate+2)*100,
                                        stageLabel+" · conditioning "+
                                            chunk.index+"/"+schedule.chunks.size)
                                }
                            },referencePngs=referenceInputs)
                        current=result.png
                        if(schedule.chunks.size>1) {
                            GeneratedImagePublisher.atomicWrite(
                                File(intermediate,"candidate_"+candidate+"_pass_"+
                                    stage.number+"_chunk_"+chunk.index+".png"),result.png)
                        }
                    }
                    GeneratedImagePublisher.atomicWrite(path,
                        current ?: error("No rendered image from native pass"))
                    setProgress(Data.Builder().putInt("candidate", candidate)
                        .putInt("pass", stage.number).putInt("total", count * passesPerCandidate).build())
                }
                    current ?: error("No image produced")
                }
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
                    else if(characterPlan!=null) CharacterPhaseOne.PER_SHEET
                    else job.creationMode?.let {
                        DetailedMethodologies.all[it]?.catalogue?.designsPerImage
                    } ?: 1
                val memory=CreativeMemoryStore(applicationContext)
                val layout=if(job.designPrompt!=null) null else job.creationMode?.let {
                    DetailedMethodologies.all[it]?.catalogue }
                val columns=layout?.columns ?: 1
                val rows=layout?.rows ?: 1
                val frozen=characterPlan?.frozen.orEmpty()
                for(index in 0 until perSheet) {
                    val ordinal=(candidate-1)*perSheet+index+1
                    val specification=frozen.getOrNull(ordinal-1)?.renderDescription
                        ?: if(job.designPrompt!=null) job.designPrompt
                        else UniversalCreativePlanner.individual(
                            UniversalCreativePlanner.domain(job.creationMode),
                            job.concepts,index+(candidate-1)*perSheet,
                            stageSequence.first().positive)
                    // Character v3 already rendered each identity independently
                    // at full native resolution. Preserve THAT complete original
                    // rather than downsampling or cropping the five-design sheet.
                    val preview=if(characterPlan!=null) {
                        val design=requireNotNull(frozen.getOrNull(ordinal-1)) {
                            "Missing frozen Character Creation identity D"+ordinal
                        }
                        val full=File(intermediate,
                            "sheet_"+candidate+"_"+design.id+"_triptych.png")
                        require(full.isFile&&full.length()>1024L) {
                            "Full-resolution character image missing: "+design.id
                        }
                        android.net.Uri.fromFile(full).toString()
                    } else DesignPreviewCrops.generate(
                        applicationContext,image,job.id,ordinal,
                        index,columns,rows)
                    memory.upsert(SavedDesign(
                        id=job.id+"-D"+ordinal.toString().padStart(3,'0'),
                        jobId=job.id,mode=job.mode,creationMode=job.creationMode,
                        ordinal=ordinal,sheetNumber=candidate,prompt=specification,
                        negative=if(characterPlan!=null) CharacterPhaseOne.NEGATIVE
                            else stageSequence.first().negative,sheetUri=uri.toString(),
                        status=if(job.designPrompt!=null)"REVISION" else "CANDIDATE",
                        parents=job.parentDesignIds,sourceSheetIds=job.sheetIds,
                        previewUri=preview))
                }
                // Every independently generated character/creature and every
                // original-system design can become its own authoritative
                // sheet. The source is the newly created design's pixels,
                // not a pasted panel pretending to be a full reference.
                if(job.creationMode!=null &&
                    job.creationMode!=CreationMode.STYLE_CORRECTION) {
                    val role=if(job.creationMode==CreationMode.WEAPON)
                        SheetRole.ELEMENT else SheetRole.CHARACTER
                    val sourcesForSheet=memory.list().filter{
                        it.jobId==job.id && it.sheetNumber==candidate
                    }
                    sourcesForSheet.forEach { design ->
                        val source=design.previewUri ?:
                            if(perSheet==1)uri.toString() else return@forEach
                        runCatching {
                            val fresh=ArtRepository(applicationContext)
                            if(fresh.sheets.none{sheet->
                                sheet.role==role&&source in sheet.sourceUris
                            }) {
                                val individual=fresh.addSheet(
                                    role,listOf(source),listOf("PRIMARY"),
                                    originatingOriginal=job.creationMode)
                                val taskForSheet=fresh.recordTask(individual.id,
                                    TaskKind.SHEET_RENDER)
                                SheetMasterGenerationWorker.enqueue(applicationContext,
                                    individual.id,taskForSheet.id)
                            }
                        }.onFailure { error ->
                            android.util.Log.w("AsterionArt",
                                "Cannot queue source-linked design master sheet",error)
                        }
                    }
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
                        else if(characterPlan!=null)
                        characterPlan.prompts[candidate-1]
                        else stageSequence.joinToString("\n\n") { it.positive })
                    put("prompt_negative", if(job.designPrompt!=null)
                        job.designNegative.orEmpty()
                        else if(characterPlan!=null)
                        CharacterPhaseOne.NEGATIVE
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
                put("one_shot_master_sheet_methodology", characterPlan==null&&
                    oneShotCharacterSheet)
                put("character_render_method",if(characterPlan!=null)
                    "five_separate_frozen_identity_triptychs_composited_white_16_9"
                    else JSONObject.NULL)
                put("native_image_encoder_expanded",false)
                put("methodology_certified", false)
                put("reference_sheet_ids", JSONArray(job.sheetIds))
                put("local_vision_reference_observations",
                    evidence.map{it.all()}.flatten().any{
                        it.startsWith("Unverified local visual observation of actual source pixels:")
                    })
                put("reference_observations_user_grading",false)
                put("results", JSONArray(out))
                put("automated_scoring", "technical-only; sheet fidelity not evaluated")
                put("native_reference_images", referenceInputs.size)
                put("selected_design_image_count",job.designReferenceUris.size)
                put("selected_design_source_pixels_preserved_as_inputs",
                    job.designReferenceUris.isNotEmpty())
                put("reference_model_kind", model.ditKind)
                put("creation_methodology", job.creationMode?.name ?: JSONObject.NULL)
                put("selected_concepts", JSONArray(job.concepts))
                put("character_plan_id",characterPlan?.id ?: JSONObject.NULL)
                put("frozen_character_count",characterPlan?.frozen?.size ?: 0)
                put("full_resolution_before_render",characterPlan!=null)
                put("visual_anatomy_audit_passed",false)
                put("clinical_unclothed_phase1",characterPlan!=null)
                put("source_artwork", job.sourceImageUri ?: JSONObject.NULL)
                put("enhancement_status", enhancementStatus)
                put("enhanced_uri", enhancedUri ?: JSONObject.NULL)
            }
            GeneratedImagePublisher.atomicWrite(
                File(applicationContext.filesDir,
                    "generation-plans/" + job.id + "-results.json"),
                provenance.toString(2).toByteArray(Charsets.UTF_8))
            // Release the large QNN model BEFORE starting the separate
            // local 2B vision + projector runtime, never simultaneously.
            currentEngine?.release()
            currentEngine=null
            repo.updateTask(taskId, TaskStatus.COMPLETED)
            if(characterPlan!=null &&
                VisionModelStore(applicationContext).installed()!=null)
                CharacterVisualAuditWorker.enqueue(applicationContext,job.id)
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
