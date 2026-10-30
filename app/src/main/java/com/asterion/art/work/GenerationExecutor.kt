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

/**
 * Shared real rendering path used by WorkManager for legacy jobs and by
 * a user-initiated foreground service for quota-independent originals.
 * Checkpoints, provenance, model routing and gallery registration are shared.
 */
internal class GenerationExecutor(
    private val applicationContext: Context,
    private val stopped: ()->Boolean,
    private val notify: suspend (String)->Unit,
    private val taskProgress: suspend (Int,Int)->Unit = {_,_->}
) {
    suspend fun execute(jobId:String,taskId:String):Boolean = withContext(Dispatchers.IO) {
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
            if(job.creationMode==null && job.designPrompt.isNullOrBlank()) {
                val selectedRoles=sheets.map{it.role}.toSet()
                val absent=listOf(SheetRole.CHARACTER,SheetRole.THEME,
                    SheetRole.ART_STYLE).filter{it !in selectedRoles}
                require(absent.isEmpty()) {
                    "Artwork requires completed Character, Theme and Art Style "+
                        "sheet references. Missing: "+
                        absent.joinToString(", "){it.display}
                }
            }
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
            if(evidence.any{it.all().isEmpty()}) {
                // An unlabelled sheet must be visually interpreted before
                // the layered compiler can author its detailed specification.
                // Do not claim a generic text prompt describes unseen pixels.
                require(VisionModelStore(applicationContext).installed()!=null) {
                    "One or more selected sheets contain no readable design text. "+
                        "Install the matched local Gemma 3 vision projector for "+
                        "actual image-based pre-render interpretation."
                }
            }
            require(job.creationMode != CreationMode.STYLE_CORRECTION ||
                job.sourceImageUri != null) {
                "Style correction requires a source illustration, not just a style sheet."
            }
            val taxonomy=ConceptTaxonomy(applicationContext)
            val compilerAttempt = runCatching {
                if(characterPlan!=null) GenerationRecipe(
                    job.mode,listOf(GenerationPass(1,characterPlan.prompts.first(),
                        CharacterPhaseOne.NEGATIVE,1f)),job.sheetIds,job.settings)
                else if(job.methodologyTextPlanId!=null &&
                    job.creationMode==null) {
                    // Route B Artwork remains Artwork: all five recovered
                    // owner phases execute. The approved complete design text
                    // remains authoritative throughout every stage.
                    val strengths=listOf(1f,0.48f,0.32f,0.22f,0.13f)
                    val stages=(1..5).map { number ->
                        GenerationPass(number,
                            requireNotNull(job.designPrompt)+
                                "\nARTWORK QUALITY PHASE: "+
                                com.asterion.art.engine.ArtworkFivePhaseContract
                                    .at(number).name,
                            job.designNegative.orEmpty(),
                            strengths[number-1])
                    }
                    com.asterion.art.engine.ArtworkFivePhaseContract
                        .validate(stages)
                    GenerationRecipe(job.mode,stages,job.sheetIds,job.settings)
                }
                else if(!job.designPrompt.isNullOrBlank()) GenerationRecipe(
                    job.mode,listOf(GenerationPass(1,job.designPrompt,
                        job.designNegative.orEmpty(),1f)),job.sheetIds,job.settings)
                else if(job.creationMode==null && sheets.isEmpty() &&
                    (job.sourceImageUri!=null||job.designReferenceUris.isNotEmpty()))
                    GenerationPlanner.compileFromSource(job.mode,job.settings)
                else if(job.creationMode in listOf(CreationMode.CHARACTER,CreationMode.CREATURE))
                    GenerationPlanner.compileConcepts(
                        job.concepts,requireNotNull(job.creationMode),job.mode,job.settings,taxonomy)
                else if(evidence.any{it.all().isEmpty()}) {
                    // A legitimate image reference can have no OCR text.
                    // Select the reference-conditioned methodology compiler
                    // EXPLICITLY from the actual evidence type, not as a
                    // catch-all fallback after an arbitrary compiler failure.
                    GenerationPlanner.compileVisual(evidence,job.mode,job.settings,
                        job.creationMode,job.concepts,job.sourceImageUri!=null,taxonomy)
                } else GenerationPlanner.compile(evidence,job.mode,job.settings,
                    job.creationMode,job.concepts,job.sourceImageUri!=null,taxonomy)
            }
            val engine = EmbeddedEngineFactory.open(applicationContext)
            currentEngine = engine
            val models = engine.installedModels()
            val outputRouting = ModelOutputRouting(applicationContext)
            val taggedPreferred = outputRouting.forMode(
                UniversalCreativePlanner.domain(job.creationMode).name,models
            ) ?: outputRouting.preferred(job.mode, models)
            val preferredId = taggedPreferred?.id ?: GenerationPlanner.selectModelId(
                models.map { it.id to it.name }, job.mode)
            val visualModel = models.firstOrNull { it.ditKind == "qwen21" }
                ?: models.firstOrNull { it.ditKind == "klein" }
            var model = models.firstOrNull { it.id == preferredId }
            val imageSourceCount=(
                com.asterion.art.model.SheetReferenceAuthority.primaryUris(sheets)+
                job.designReferenceUris+job.directReferenceUris.values.flatten()+
                listOfNotNull(job.sourceImageUri)
            ).distinct().size
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
            // A failed Original/Artwork planning contract MUST remain a
            // failure. Generic visual recompilation silently changes
            // methodology, required input roles and output structure.
            var recipe=compilerAttempt.getOrElse { original ->
                throw IllegalArgumentException(
                    "Original authoritative prompt compilation failed: "+
                        (original.message ?: original.javaClass.simpleName),
                    original)
            }
            if(model==null) {
                model=visualModel ?: error(
                    "No installed QNN image model accepts the requested "+
                        job.mode.display+" output. The job was NOT replaced "+
                        "with an unrelated generic model or prompt.")
            }
            // Owner-authored additions compose with every approved image
            // methodology. Manual adjustments never silently replace the
            // original method, required sheet roles or its output count.
            if((job.userScenePrompt.isNotBlank()||
                job.userNegativePrompt.isNotBlank()) && characterPlan==null) {
                recipe=recipe.copy(passes=recipe.passes.map {pass ->
                    pass.copy(
                        positive=pass.positive+
                            if(job.userScenePrompt.isBlank())"" else
                                "\n\nUSER-APPROVED ADDITIONAL DIRECTION (PRESERVE ORIGINAL METHODOLOGY):\n"+
                                    job.userScenePrompt,
                        negative=com.asterion.art.engine.NegativePromptAuthority
                            .merge(pass.negative,job.userNegativePrompt))
                })
            }
            // Theme-only creative parameters are never attached to
            // Artwork or another Original; keep historical snapshots readable.
            if(job.themeTextPlanId!=null && job.intensities.isNotEmpty()) {
                val directives=com.asterion.art.engine.CreativeIntensity
                    .description(job.intensities)
                recipe=recipe.copy(passes=recipe.passes.map{stage->
                    stage.copy(positive=stage.positive+
                        "\n\nOWNER CREATIVE INTENSITIES:\n"+directives)
                })
            }
            // Recover and execute the original deterministic compiler for
            // compatible approved Artwork worksheets (never for Originals).
            // Invalid/non-structured PNGs are not misrepresented as compiled.
            val restoredCompilation=if(job.creationMode==null && sheets.isNotEmpty())
                runCatching {
                    com.asterion.art.engine.OriginalCompilerBridge.compile(
                        applicationContext,sheets)
                }.getOrNull() else null
            if(restoredCompilation!=null) {
                recipe=recipe.copy(passes=recipe.passes.map { stage ->
                    stage.copy(
                        positive=stage.positive+"\n\n"+restoredCompilation.text,
                        negative=com.asterion.art.engine.NegativePromptAuthority.merge(
                            stage.negative,restoredCompilation.negatives)
                    )
                })
                foreground("Original deterministic worksheet compiler: "+
                    restoredCompilation.passNames.joinToString(", "))
            }
            // Perform true neural interpretation/synthesis/prompt compilation
            // before the large QNN model is loaded. The GGUF context and KV
            // cache are released in AsterionReasoner.compile's finally block.
            val preRenderStart=android.os.SystemClock.elapsedRealtime()
            val neural=AsterionReasoner(applicationContext)
            val requiresAutoCompilation=characterPlan==null &&
                restoredCompilation==null && job.designPrompt.isNullOrBlank()
            require(!requiresAutoCompilation || neural.available()) {
                "The approved pre-render compilation is unavailable: no verified "+
                    "adaptable reasoning model is installed. Cannot skip it and "+
                    "pretend an uncompiled image prompt is the requested result."
            }
            // When the owner's original deterministic Compiler has already
            // validated the structured sheets, do not re-author its prompt
            // through a slow second language model. Optional manual edits
            // are already included in the frozen generation authority.
            // An owner-edited Route B or approved Theme text already IS the
            // frozen model-facing specification. Never overwrite it through
            // another unrelated neural design step after the bridge.
            val brainPlan=if(characterPlan!=null || restoredCompilation!=null ||
                !job.designPrompt.isNullOrBlank())
                null else if(neural.available()) {
                foreground("Asterion Brain Â· interpreting and designing")
                val domain=UniversalCreativePlanner.domain(job.creationMode)
                val spec=job.creationMode?.let { selected ->
                    if(selected==CreationMode.CHARACTER)
                        DetailedMethodologies.all.getValue(selected).rendererDirective
                    else com.asterion.art.methodology.MethodologyAuthority
                        .originalText(applicationContext,selected)
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
            require(!requiresAutoCompilation || brainPlan!=null) {
                "The mandatory complete pre-render compilation did not produce "+
                    "a verified design specification; native generation blocked."
            }
            if(brainPlan!=null &&
                !(job.creationMode==CreationMode.CHARACTER && job.designPrompt==null)) {
                recipe=recipe.copy(passes=recipe.passes.map { pass ->
                    pass.copy(
                        positive=pass.positive+"\n\nNEURAL DESIGN SPECIFICATION:\n"+
                            brainPlan.positive,
                        negative=com.asterion.art.engine.NegativePromptAuthority.merge(
                            pass.negative,brainPlan.negative))
                })
            }
            val preRenderElapsed=android.os.SystemClock.elapsedRealtime()-preRenderStart
            // Retain the exact authoritative text before native model loading.
            // Previously failure/crashes could leave no record of the actual
            // prompt fed to the image engine; output-only provenance is late.
            val promptTrace=JSONObject().apply {
                put("schema","asterion.prompt-trace.v1")
                put("job_id",job.id)
                put("methodology",job.creationMode?.name?:JSONObject.NULL)
                put("mode",job.mode.name)
                put("model_id",model.id)
                put("pre_render_elapsed_ms",preRenderElapsed)
                put("compiled_model",brainPlan?.modelId?:JSONObject.NULL)
                put("compiler_used",restoredCompilation!=null)
                put("selected_sheet_ids",JSONArray(job.sheetIds))
                put("native_stage_prompts",JSONArray().apply{
                    recipe.passes.forEach{stage ->
                        put(JSONObject().apply{
                            put("number",stage.number)
                            put("positive",stage.positive)
                            put("negative",stage.negative)
                            put("denoise",stage.denoise.toDouble())
                        })
                    }
                })
                put("source_data_preserved",true)
                put("pre_native_checkpoint",true)
            }
            val promptTracePath=File(applicationContext.filesDir,
                "generation-checkpoints/"+job.id+"/native-prompt-trace.json")
            GeneratedImagePublisher.atomicWrite(promptTracePath,
                promptTrace.toString(2).toByteArray(Charsets.UTF_8))
            var size = model.generationSize.coerceIn(512, 1536)
            var resolution = com.asterion.art.nativeengine.NativeResolutionPolicy
                .choose(model,if(characterPlan!=null)"16:45" else job.settings.ratio)
            repo.updateTaskProgress(taskId,0,0,"Loading QNN model Â· "+model.name)
            foreground("Loading " + model.name)
            val loadStarted=android.os.SystemClock.elapsedRealtime()
            engine.prepare(model, resolution.first, resolution.second) { stopped() }
            val loadElapsed=android.os.SystemClock.elapsedRealtime()-loadStarted
            if(characterPlan!=null) {
                GeneratedImagePublisher.atomicWrite(File(applicationContext.filesDir,
                    "generation-checkpoints/"+job.id+"/model-load-ms.txt"),
                    loadElapsed.toString().toByteArray())
                repo.updateTaskProgress(taskId,0,0,
                    "QNN model ready after "+loadElapsed+" ms")
            }
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
            if (capacityCheck.isFailure && model.ditKind !in setOf("qwen21","klein")
                && visualModel != null && visualModel.id!=model.id) {
                // Switch only the native backend. The complete approved prompt,
                // methodologies, source evidence and neural design stay frozen.
                // Recompiling a different, shorter fallback loses the exact
                // conditions whose overflow triggered this branch.
                val originalFailure=capacityCheck.exceptionOrNull()
                val replacementModel=visualModel
                val oldModel=model
                foreground("Verifying complete prompt on "+replacementModel.name)
                model=replacementModel
                resolution=com.asterion.art.nativeengine.NativeResolutionPolicy.choose(
                    model,if(characterPlan!=null)"16:45" else job.settings.ratio)
                try {
                    engine.prepare(model,resolution.first,resolution.second) { stopped() }
                    recipe.passes.forEach { pass ->
                        engine.verifyText(pass.positive)
                        if(pass.negative.isNotBlank())engine.verifyText(pass.negative)
                    }
                } catch(error:Throwable) {
                    throw com.asterion.art.engine.BackendUnavailable(
                        "Neither "+oldModel.name+" nor "+replacementModel.name+
                            " can condition on the complete frozen specification. "+
                            "First backend: "+(originalFailure?.message?:"unknown")+
                            "; replacement: "+(error.message?:"unknown"))
                }
            } else {
                capacityCheck.getOrThrow()
            }
            val extraOriginal = job.sourceImageUri?.let { uri ->
                SheetRecord(role=SheetRole.CHARACTER,label="Original source image",
                    sourceUris=listOf(uri),referenceSlots=listOf("PRIMARY"))
            }
            // The GENERATED authoritative master sheet must reach the
            // neural reference-conditioning path. Keep original source pixels
            // available as secondary evidence, without replacing the master.
            val combinedSourceUris=(
                com.asterion.art.model.SheetReferenceAuthority.primaryUris(sheets)+
                listOfNotNull(extraOriginal?.sourceUris?.firstOrNull())+
                job.designReferenceUris+
                job.directReferenceUris.values.flatten()
            ).distinct()
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
                ?:job.directReferenceUris.values.flatten().distinct().singleOrNull()
                // Anima/SDXL cannot consume multi-reference images as a
                // separate conditioning tensor. Keep the actual primary
                // character pixels as an img2img source rather than silently
                // replacing the selected character with prose alone.
                ?:if(job.creationMode==null)
                    com.asterion.art.model.SheetReferenceAuthority
                        .primaryUris(sheets).singleOrNull()
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
            val tunedSettings=com.asterion.art.nativeengine.NativeTurboDefaults
                .apply(job.settings,model,freeArtwork=job.creationMode==null &&
                    job.designPrompt.isNullOrBlank())
            val turboAuto=com.asterion.art.nativeengine.NativeTurboDefaults
                .applies(job.settings,model)
            if(turboAuto)repo.updateTaskProgress(taskId,0,0,
                "Anima Turbo Â· optimized for on-device NPU: 8 steps, CFG 1")
            val artworkBeam=job.creationMode==null &&
                (job.designPrompt.isNullOrBlank() ||
                    job.methodologyTextPlanId!=null)
            val count = if(artworkBeam) 1 else
                com.asterion.art.engine.OriginalOutputPolicy.imageCount(
                    job.creationMode,!job.designPrompt.isNullOrBlank(),
                    characterPlan?.sheets,tunedSettings.batch)
            // v3 Character Creation explicitly prescribes ONE generation call
            // per fully specified analytical sheet; artwork uses five passes.
            val oneShotCharacterSheet =
                (!job.designPrompt.isNullOrBlank() &&
                    !(job.methodologyTextPlanId!=null &&
                        job.creationMode==null)) ||
                characterPlan!=null
            val stageSequence = if (oneShotCharacterSheet)
                listOf(recipe.passes.first()) else recipe.passes
            if(job.creationMode==null && !oneShotCharacterSheet)
                com.asterion.art.engine.ArtworkFivePhaseContract.validate(stageSequence)
            val passesPerCandidate = stageSequence.size
            repo.updateTaskProgress(taskId,0,(count*passesPerCandidate+2)*100,"Beginning generation")
            for (candidate in 1..count) {
                if (stopped()) throw CancellationException("Job stopped")
                val seed = job.settings.seed ?: ((job.createdAt + candidate * 1049L) and 0x7FFFFFFFL)
                // Turbo distillation is calibrated to CFG=1; automatic
                // preference perturbations must not silently re-enable the
                // prohibitively expensive unconditional guidance path.
                val candidateSettings = if(turboAuto)tunedSettings else
                    PreferenceLearning.candidate(tunedSettings,candidate - 1)
                val candidateRenderStart=android.os.SystemClock.elapsedRealtime()
                val image:ByteArray=if(artworkBeam) {
                    // ONE highest-ranked image survives every stage.
                    // Each stage renders only the user-selected batch count,
                    // never batch Ã— number of previously retained candidates.
                    val beams=com.asterion.art.engine.ArtworkBeamRunner(
                        File(intermediate,"owner30-artwork-best-one-v2"))
                    val result=beams.execute(
                        stages=stageSequence,
                        baseSeed=seed,
                        initialPng=originalImage,
                        modelId=model.id,
                        candidatesPerPhase=tunedSettings.batch,
                        stopped=stopped,
                        render={pass,parent,childSeed,child,total,parentId->
                            thermalGate()
                            val label="Artwork "+
                                com.asterion.art.engine.ArtworkFivePhaseContract
                                    .at(pass.number).name+
                                " Â· variation "+child+"/"+total
                            foreground(label)
                            val applied=if(pass.number==1 && parent!=null &&
                                model.ditKind !in listOf("qwen21","klein"))
                                pass.copy(denoise=candidateSettings.denoise)
                            else pass
                            val trace=File(intermediate,
                                "actual-native-stage-inputs.jsonl")
                            val invocation=JSONObject().apply{
                                put("candidate",candidate)
                                put("stage",pass.number)
                                put("child",child)
                                put("parent_id",parentId?:JSONObject.NULL)
                                put("model",model.id)
                                put("positive",applied.positive)
                                put("negative",applied.negative)
                                put("denoise",applied.denoise.toDouble())
                                put("seed",childSeed)
                            }
                            GeneratedImagePublisher.atomicWrite(trace,
                                (trace.takeIf{it.exists()}?.readText().orEmpty()+
                                    invocation.toString()+"\\n")
                                    .toByteArray(Charsets.UTF_8))
                            engine.verifyText(applied.positive)
                            if(applied.negative.isNotBlank())
                                engine.verifyText(applied.negative)
                            val generated=engine.generate(
                                applied,candidateSettings.copy(batch=1),
                                resolution.first,resolution.second,
                                parent,childSeed,{stopped()},
                                {step,totalSteps->
                                    if(totalSteps>0 && step>=0 && step%3==0)
                                        repo.updateTaskProgress(taskId,
                                            (pass.number-1)*100+
                                                (child-1)*100/total+
                                                (step*100/totalSteps)/total,
                                            700,label)
                                },referencePngs=referenceInputs)
                            generated.png
                        },
                        onStage={number,survivors->
                            repo.updateTaskProgress(taskId,number*100,700,
                                "Retained single best Artwork "+
                                    com.asterion.art.engine.ArtworkFivePhaseContract
                                        .at(number).name+" outputs")
                            taskProgress(candidate,number)
                        }
                    )
                    GeneratedImagePublisher.atomicWrite(
                        File(intermediate,"owner30-artwork-beam-summary.json"),
                        JSONObject().apply{
                            put("schema","asterion.artwork.best1.v2")
                            put("phases",5)
                            put("retained_per_phase",1)
                            put("candidates_per_phase",tunedSettings.batch)
                            put("generated_per_phase",
                                JSONArray(result.generatedPerPhase))
                            put("total_native_invocations",
                                result.totalInvocations)
                            put("winner",result.winner.id)
                            put("winner_seed",result.winner.seed)
                            put("ranked_by","technical image score; not owner-grade")
                        }.toString(2).toByteArray(Charsets.UTF_8))
                    result.winner.png
                } else if(characterPlan!=null) {
                    // Five independent Character Creation triptychs use a
                    // 16:45 output crop. Anima/SDXL still load their fixed
                    // 1024-square QNN CANVAS; the native server crops 16:45.
                    val nativeSettings=candidateSettings.copy(ratio="16:45",batch=1)
                    val tiles=(0 until CharacterPhaseOne.PER_SHEET).map{cell ->
                        if(stopped())throw CancellationException("Generation cancelled")
                        val d=characterPlan.frozen[(candidate-1)*5+cell]
                        // A frozen identity is one COMPLETE native image instruction.
                        // Do not replay slices of its anatomy as separate img2img jobs.
                        // v2 checkpoint paths avoid reusing old sliced-prompt results.
                        val checkpoint=File(intermediate,
                            "unified_v2_sheet_"+candidate+"_"+d.id+"_triptych.png")
                        val triptych=if(checkpoint.isFile && checkpoint.length()>1024L) {
                            val existing=checkpoint.readBytes()
                            com.asterion.art.engine.RenderFidelityGate.requireValidImage(
                                existing,"Restored frozen character "+d.id)
                            existing
                        } else {
                            thermalGate()
                            val directive=CharacterSheetCompiler.individual(d)
                            engine.verifyText(directive)
                            val finalNegative=com.asterion.art.engine.NegativePromptAuthority.merge(
                                CharacterPhaseOne.NEGATIVE,recipe.passes.first().negative,
                                job.designNegative,job.userNegativePrompt)
                            engine.verifyText(finalNegative)
                            val actualPromptTrace=File(intermediate,
                                "actual-native-stage-inputs.jsonl")
                            val invocation=JSONObject().apply {
                                put("candidate",candidate)
                                put("identity",d.id)
                                put("stage",1)
                                put("model",model.id)
                                put("positive",directive)
                                put("negative",finalNegative)
                                put("denoise",1.0)
                                put("seed",seed+cell*104729L+1)
                            }
                            GeneratedImagePublisher.atomicWrite(actualPromptTrace,
                                (actualPromptTrace.takeIf{it.exists()}?.readText().orEmpty()+
                                    invocation.toString()+"\n").toByteArray(Charsets.UTF_8))
                            if(stopped())throw CancellationException()
                            val stageLabel="Sheet "+candidate+"/"+count+
                                " Â· identity "+d.id+" Â· complete native prompt"
                            foreground(stageLabel)
                            val started=android.os.SystemClock.elapsedRealtime()
                            val result=engine.generate(
                                GenerationPass(1,directive,finalNegative,1f),
                                nativeSettings,resolution.first,resolution.second,
                                null,seed+cell*104729L+1,
                                {stopped()},{step,totalSteps->
                                    if(totalSteps>0&&step>=0&&step%4==0)
                                        repo.updateTaskProgress(taskId,
                                            (candidate-1)*100+cell*20+
                                                step*20/(5*totalSteps),
                                            count*100,stageLabel)
                                },referencePngs=referenceInputs)
                            com.asterion.art.engine.RenderFidelityGate.requireValidImage(
                                result.png,"Frozen character "+d.id)
                            GeneratedImagePublisher.atomicWrite(checkpoint,result.png)
                            val duration=android.os.SystemClock.elapsedRealtime()-started
                            val metrics=File(intermediate,"native-unified-pass-timings.tsv")
                            GeneratedImagePublisher.atomicWrite(metrics,
                                (metrics.takeIf{it.exists()}?.readText().orEmpty()+
                                    d.id+"\t1\t"+duration+"\n").toByteArray(Charsets.UTF_8))
                            result.png
                        }
                        val published=File(intermediate,
                            "unified_v2_sheet_"+candidate+"_"+d.id+"_preview.uri")
                        if(!published.isFile) {
                            // Progressive gallery output: expose each completed,
                            // newly generated triptych before the full five-
                            // character sheet is assembled. Never crop an input.
                            val uri=GeneratedImagePublisher.publish(applicationContext,
                                "triptych-"+job.id+"-"+d.id,1,triptych)
                            GeneratedImagePublisher.atomicWrite(published,
                                uri.toString().toByteArray(Charsets.UTF_8))
                            repo.registerAsset(uri.toString(),sourceJobId=job.id)
                        }
                        repo.updateTaskProgress(taskId,
                            (candidate-1)*5+cell+1,count*5,
                            "Rendered original "+d.id+" Â· "+
                                (cell+1)+"/5 triptychs checkpointed")
                        triptych
                    }
                    CharacterSheetComposer.assemble(tiles)
                } else {
                var current: ByteArray? = originalImage
                for (stage in stageSequence) {
                    if (stopped()) throw CancellationException()
                    // Do not reuse checkpoints produced under the former
                    // anonymous five-pass sequence.
                    val version=if(job.creationMode==null && !oneShotCharacterSheet)
                        "owner23_artwork_v1_" else "unified_v2_"
                    val path = File(intermediate, version+"candidate_" + candidate +
                        "_pass_" + stage.number + ".png")
                    if (path.isFile && path.length() > 100) {
                        val saved=path.readBytes()
                        // Validate EACH restored stage, including Foundation.
                        // Never progress to stage 2+ from a blank checkpoint.
                        com.asterion.art.engine.RenderFidelityGate.requireValidImage(
                            saved,"Restored candidate "+candidate+" stage "+stage.number)
                        current=saved
                        continue
                    }
                    thermalGate()
                    val stageLabel = "Image " + candidate + "/" + count +
                        (if(oneShotCharacterSheet) " Â· one-shot master sheet"
                         else if(job.creationMode==null && stageSequence.size==5)
                             " Â· "+com.asterion.art.engine.ArtworkFivePhaseContract
                                 .at(stage.number).name+" Â· pass "+stage.number+"/5"
                         else " Â· original "+stage.number+"/"+stageSequence.size)
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
                        stage.copy(denoise=candidateSettings.denoise)
                    }
                    else if(characterPlan!=null)
                        stage.copy(positive=characterPlan.prompts[candidate-1],
                            negative=com.asterion.art.engine.NegativePromptAuthority.merge(
                                stage.negative,CharacterPhaseOne.NEGATIVE,
                                job.designNegative,job.userNegativePrompt))
                    else stage
                    // Every native call sees the complete methodology/Artwork
                    // stage prompt. Native tokenizer capacity is a real limit,
                    // NOT increased by feeding consecutive prompt fragments.
                    if(stopped())throw CancellationException()
                    // Record the EXACT stage inputs, after all overrides, before
                    // the native call. The early trace holds only the recipe.
                    val actualPromptTrace=File(intermediate,
                        "actual-native-stage-inputs.jsonl")
                    val invocation=JSONObject().apply {
                        put("candidate",candidate)
                        put("stage",stage.number)
                        put("model",model.id)
                        put("positive",methodStage.positive)
                        put("negative",methodStage.negative)
                        put("denoise",methodStage.denoise.toDouble())
                        put("seed",seed)
                    }
                    GeneratedImagePublisher.atomicWrite(actualPromptTrace,
                        (actualPromptTrace.takeIf{it.exists()}?.readText().orEmpty()+
                            invocation.toString()+"\n").toByteArray(Charsets.UTF_8))
                    engine.verifyText(methodStage.positive)
                    if(methodStage.negative.isNotBlank())
                        engine.verifyText(methodStage.negative)
                    val nativeStageStart=android.os.SystemClock.elapsedRealtime()
                    val result=engine.generate(
                        methodStage,candidateSettings,resolution.first,resolution.second,
                        current,seed,{stopped()},{step,totalSteps->
                            if(totalSteps>0&&step>=0&&step%3==0)
                                repo.updateTaskProgress(taskId,
                                    (candidate-1)*passesPerCandidate*100+
                                        (stage.number-1)*100+(step*100/totalSteps),
                                    (count*passesPerCandidate+2)*100,stageLabel)
                        },referencePngs=referenceInputs)
                    // A degenerate Foundation must never receive a second
                    // repaint, and failed refinements must not become final.
                    com.asterion.art.engine.RenderFidelityGate.requireValidImage(
                        result.png,"Candidate "+candidate+" phase "+stage.number)
                    val stageElapsed=android.os.SystemClock.elapsedRealtime()-nativeStageStart
                    val stageScore=TechnicalImageScorer.score(result.png)
                    val metrics=File(intermediate,"native-stage-measurements.tsv")
                    GeneratedImagePublisher.atomicWrite(metrics,
                        (metrics.takeIf{it.exists()}?.readText().orEmpty()+
                            candidate+"\t"+stage.number+"\t"+stageElapsed+
                            "\t"+stageScore.score+"\t"+stageScore.contrast+
                            "\t"+stageScore.detail+"\n").toByteArray(Charsets.UTF_8))
                    current=result.png
                    GeneratedImagePublisher.atomicWrite(path,result.png)
                    taskProgress(candidate,stage.number)
                }
                    current ?: error("No image produced")
                }
                com.asterion.art.engine.RenderFidelityGate.requireValidImage(
                    image,"Generated candidate "+candidate)
                val nativeRenderElapsed=android.os.SystemClock.elapsedRealtime()-candidateRenderStart
                val technical = TechnicalImageScorer.score(image)
                val decodedBounds=android.graphics.BitmapFactory.Options().apply{
                    inJustDecodeBounds=true
                }
                android.graphics.BitmapFactory.decodeByteArray(
                    image,0,image.size,decodedBounds)
                if (technical.score > technicalLeaderScore) {
                    technicalLeaderScore = technical.score
                    technicalLeader = image
                }
                val uri = GeneratedImagePublisher.publish(
                    applicationContext, job.id, candidate, image)
                if (technical.score == technicalLeaderScore) technicalLeaderUri = uri
                repo.registerAsset(uri.toString(), sourceSheetId = null,
                    autoScore = technical.score, sourceJobId = job.id)
                // An approved text Theme becomes a real reusable Theme Sheet
                // only after its native image has passed the fidelity gate.
                job.themeTextPlanId?.let { textId ->
                    val approved=com.asterion.art.methodology.ThemeTextStore(
                        applicationContext).load(textId)
                    require(approved.renderJobId==job.id &&
                        approved.approved!=null) {
                        "Theme render lacks a matching authorized text plan"
                    }
                    repo.finishThemeTextSheet(textId,
                        requireNotNull(approved.approved).name,uri.toString())
                }
                // Every design is independently selectable, including catalogue
                // slots from sheets produced by any Creative Studio. The sheet
                // remains a reference until separately regenerated as one image.
                val perSheet=if(job.methodologyTextPlanId!=null)
                    job.creationMode?.let {
                        DetailedMethodologies.all[it]?.catalogue?.designsPerImage
                    } ?: 1
                    else if(job.designPrompt!=null) 1
                    else if(characterPlan!=null) CharacterPhaseOne.PER_SHEET
                    else job.creationMode?.let {
                        DetailedMethodologies.all[it]?.catalogue?.designsPerImage
                    } ?: 1
                val memory=CreativeMemoryStore(applicationContext)
                val layout=if(job.designPrompt!=null &&
                    job.methodologyTextPlanId==null) null
                    else job.creationMode?.let {
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
                            "unified_v2_sheet_"+candidate+"_"+design.id+"_triptych.png")
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
                        negative=if(characterPlan!=null)
                            com.asterion.art.engine.NegativePromptAuthority.merge(
                                CharacterPhaseOne.NEGATIVE,stageSequence.first().negative,
                                job.designNegative,job.userNegativePrompt)
                            else stageSequence.first().negative,sheetUri=uri.toString(),
                        status=if(job.designPrompt!=null)"REVISION" else "CANDIDATE",
                        parents=job.parentDesignIds,sourceSheetIds=job.sheetIds,
                        previewUri=preview))
                }
                // Completed Original designs retain their own image pixels.
                // A reusable reference master is created ON DEMAND from the
                // Design Library via OriginalSheetReuse, preserving the source.
                // Do not launch five (or more) expensive neural sheet jobs
                // automatically for a single Original catalogue.
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
                    put("prompt_negative", if(characterPlan!=null)
                        com.asterion.art.engine.NegativePromptAuthority.merge(
                            CharacterPhaseOne.NEGATIVE,stageSequence.first().negative,
                            job.designNegative,job.userNegativePrompt)
                        else stageSequence.joinToString("\n\n") { it.negative })
                    put("pre_render_elapsed_ms",preRenderElapsed)
                    put("model_load_elapsed_ms",loadElapsed)
                    put("native_render_elapsed_ms",nativeRenderElapsed)
                    put("output_width",decodedBounds.outWidth)
                    put("output_height",decodedBounds.outHeight)
                    put("native_pass_count",if(characterPlan!=null)5 else stageSequence.size)
                    put("native_prompt_token_count",if(characterPlan!=null)
                        JSONObject.NULL
                        else runCatching {
                            engine.inspectText(stageSequence.last().positive).count
                        }.getOrNull()?:JSONObject.NULL)
                    put("native_prompt_token_limit",if(characterPlan!=null)
                        JSONObject.NULL
                        else runCatching {
                            engine.inspectText(stageSequence.last().positive).limit
                        }.getOrNull()?:JSONObject.NULL)
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
            if ((job.settings.automatic || artworkBeam) &&
                technicalLeaderUri != null) {
                // The NPU and RealSR must never occupy the device's large
                // model memory together. Release before enhancement.
                currentEngine?.release()
                currentEngine = null
                val winner=requireNotNull(technicalLeaderUri)
                try {
                    foreground("Automatically enhancing final winner")
                    val finish=com.asterion.art.enhancement.AutomaticImageEnhancer(
                        applicationContext).run(
                        winner=winner,
                        checkpointDir=File(intermediate,"automatic-enhancement"),
                        jobId=job.id,
                        cancelled=stopped,
                        progress={status->
                            repo.updateTaskProgress(taskId,
                                count*passesPerCandidate*100,
                                (count*passesPerCandidate+2)*100,status)
                        })
                    repo.registerAsset(finish.uri.toString(),
                        autoScore=TechnicalImageScorer.score(
                            requireNotNull(applicationContext.contentResolver
                                .openInputStream(finish.uri)).use{it.readBytes()}
                        ).score,sourceJobId=job.id)
                    enhancedUri=finish.uri.toString()
                    enhancementStatus=finish.stages.joinToString("+")
                } catch(interruption:CancellationException) {
                    throw interruption
                } catch(error:Exception) {
                    enhancementStatus="AUTOMATIC_ENHANCEMENT_FAILED: "+
                        (error.message ?: error.javaClass.simpleName)
                    // Preserve the original native winner and all completed
                    // intermediate enhancement checkpoints. Do not report a
                    // finished Artwork when its required enhancement failed.
                    if(artworkBeam)throw IllegalStateException(
                        "Artwork image saved, but mandatory automatic "+
                        "enhancement could not complete",error)
                }
            }
            repo.updateTaskProgress(taskId,(count*passesPerCandidate+1)*100,(count*passesPerCandidate+2)*100,
                "Finalizing and saving")
            foreground("Saving generation provenance")
            val provenance = JSONObject().apply {
                put("schema", "asterion.generation.v1")
                put("job_id", job.id)
                put("model_id", model.id)
                put("five_real_passes",job.creationMode==null &&
                    !oneShotCharacterSheet && stageSequence.size==5)
                put("one_shot_master_sheet_methodology", job.creationMode!=null&&
                    characterPlan==null && stageSequence.size==1)
                put("character_render_method",if(characterPlan!=null)
                    "five_separate_frozen_identity_triptychs_composited_white_16_9"
                    else JSONObject.NULL)
                put("owner_artwork_five_phase_definitions_restored",true)
                put("artwork_phase_order",JSONArray().apply {
                    if(job.creationMode==null && !oneShotCharacterSheet)
                        com.asterion.art.engine.ArtworkFivePhaseContract.order.forEach{
                            put(it.name)
                        }
                })
                put("owner_artwork_five_phase_contract_verified",false)
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
            true
        } catch (cancel: CancellationException) {
            // Android can stop a foreground worker/service even though the
            // owner has not cancelled. Preserve the plan, images and durable
            // queue so the next launch can continue from saved checkpoints.
            // An explicitly cancelled task is already CANCELLED and the
            // repository correctly refuses to overwrite a terminal status.
            repo.updateTask(taskId, TaskStatus.QUEUED,
                "Interrupted by Android; native checkpoints are retained")
            false
        } catch (e: Exception) {
            repo.updateTask(taskId, TaskStatus.FAILED, e.message ?: "Generation failed")
            false
        } finally {
            currentEngine?.release()
        }
    }

    private suspend fun foreground(message:String) = notify(message)

    private suspend fun thermalGate() {
        val power = applicationContext.getSystemService(PowerManager::class.java)
        val start = System.currentTimeMillis()
        while (Build.VERSION.SDK_INT >= 29 &&
            power.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
            if (stopped()) throw CancellationException()
            if (System.currentTimeMillis() - start > 12 * 60_000L)
                error("Thermal condition persisted; generation halted safely")
            foreground("Cooling device before next pass")
            delay(15_000L)
        }
    }
}
