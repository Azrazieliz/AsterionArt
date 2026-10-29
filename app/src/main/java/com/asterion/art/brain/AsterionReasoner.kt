package com.asterion.art.brain

import android.content.Context
import android.app.ActivityManager
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.isModelLoaded
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.engine.ConceptTaxonomy
import com.asterion.art.engine.CreativeDomain
import com.asterion.art.learning.DetailedGradeStore
import com.asterion.art.learning.GradingGuidance
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import com.asterion.art.nativeengine.DeviceInferenceGate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class NeuralDesignPlan(
    val modelId:String, val actualContext:Int, val domain:CreativeDomain,
    val interpretation:String, val synthesis:String,
    val positive:String, val negative:String,
    val sheetPrompts:List<String> = emptyList()
)

/**
 * Real inference: Llama 3.2 GGUF is loaded in CPU memory and released BEFORE QNN.
 *
 * One bounded inference receives the complete canonical input and user feedback.
 * Deterministic collection/validation occurs before native generation.
 *
 * Not model-weight training; that is handled by the separate training pipeline.
 */
class AsterionReasoner(context:Context) {
    private val app=context.applicationContext
    private val models=BrainModelStore(app)
    private val taxonomy by lazy { ConceptTaxonomy(app) }
    private val grades by lazy { DetailedGradeStore(app) }
    companion object {
        private val nativeMutex=BrainInferenceLock.mutex
        private const val SYSTEM =
            "You are Asterion Design Intelligence, an offline production design planner. " +
            "You interpret the complete methodology and canonical concept evidence. " +
            "Write deeply specific physical shapes, materials, body systems, boundaries, " +
            "silhouettes, construction and visual relations. Every concept must causally " +
            "modify the others; never replace physical integration with theme stickers. " +
            "Preserve all user-required constraints and candidate counts. " +
            "Make viable, internally coherent, visually distinct designs. " +
            "Answer only the requested structured artifact. "
    }
    fun available()=models.selected()!=null

    private fun section(domain:CreativeDomain,concepts:List<String>):String {
        val entries=concepts.distinct().map { name ->
            val record=taxonomy.find(name)
            if(record==null) "CONCEPT: "+name else
                "CANONICAL CONCEPT: "+record.name+" ("+record.id+")\n"+
                    record.physicalInstruction(domain)+"\n"+
                    "AUTHORITY DETAILS:\n"+record.fullRecord
        }
        return entries.joinToString("\n\n")
    }
    private fun learnedEvidence(creationMode:CreationMode?,
                                renderMode:RenderMode?,
                                concepts:List<String>,
                                designId:String?):String =
        GradingGuidance.evidence(GradingGuidance.applicable(
            grades.list(),creationMode,concepts,designId=designId,
            renderMode=renderMode))

    private fun parsePart(raw:String,key:String):String {
        val match=Regex("\\["+key+"\\]([\\s\\S]*?)\\[/"+key+"\\]",
            RegexOption.IGNORE_CASE).find(raw)
        return match?.groupValues?.get(1)?.trim().orEmpty()
    }
    private fun contextForRam():Int {
        val mem=ActivityManager.MemoryInfo()
        (app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getMemoryInfo(mem)
        val available=mem.availMem/(1024*1024*1024)
        val requested=models.contextTokens()
        return when {
            available<2L -> minOf(requested,4096)
            available<4L -> minOf(requested,8192)
            available<6L -> minOf(requested,16384)
            else -> requested
        }
    }

    suspend fun compile(
        domain:CreativeDomain,
        canonicalMethod:String,
        concepts:List<String>,
        basePositive:String,
        baseNegative:String,
        jobId:String,
        candidateCount:Int=1,
        sheetSources:List<String> = emptyList(),
        onStage:(String)->Unit={},
        creationMode:CreationMode?=null,
        renderMode:RenderMode?=null,
        targetDesignId:String?=null
    ):NeuralDesignPlan?=nativeMutex.withLock {
        val weights=models.selected()?:return@withLock null
        val weightFile=models.location(weights)?:return@withLock null
        require(jobId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        require(candidateCount in 1..60)
        val engine=AiChat.getInferenceEngine(app)
        val contextSize=contextForRam()
        suspend fun answer(prompt:String,n:Int):String {
            return withTimeout(3*60*1000L) {
                engine.sendUserPrompt(prompt,n).toList().joinToString("").trim()
            }.also { require(it.isNotBlank()) { "Neural planner generated no text" } }
        }
        val requestContext=currentCoroutineContext()
        DeviceInferenceGate.acquire { !requestContext.isActive }
        try {
            withTimeout(120_000L) {
                engine.state.first {
                    it is InferenceEngine.State.Initialized ||
                    it is InferenceEngine.State.ModelReady ||
                    it is InferenceEngine.State.Error
                }
            }
            onStage("Loading "+weights.label+" with "+contextSize+" tokens")
            engine.loadModel(weightFile.absolutePath,contextSize)
            // LoRA is applied to a compatible Llama 3.2 base, never silently
            // substituted for an incompatible architecture.
            BrainAdapterLibrary(app).activeFor(weights.id)?.let { adapter ->
                engine.loadAdapter(adapter.absolutePath)
            }
            engine.setSystemPrompt(SYSTEM)
            val canonical=section(domain,concepts)
            val guidance=learnedEvidence(
                creationMode,renderMode,concepts,targetDesignId)
            val authority=listOf(
                "DOMAIN:\n"+domain.name,
                "FULL METHODOLOGY:\n"+canonicalMethod,
                "CANONICAL CONCEPT RECORDS:\n"+canonical,
                "SOURCE EVIDENCE:\n"+sheetSources.joinToString("\n"),
                "POSITIVE AUTHORITY:\n"+basePositive,
                "NEGATIVE AUTHORITY:\n"+baseNegative
            ).joinToString("\n\n")
            // The compiler already has the canonical methodology, taxonomy,
            // visual observations and owner feedback. No repeated Qwen evidence
            // summaries, consolidation loops or synthetic design-rewrite stages.
            // One bounded semantic compilation keeps the original authority intact.
            val outputBudget=if(candidateCount>=10)minOf(4200,contextSize/4)
                else minOf(2200,contextSize/4)
            val requestedOutput=if(domain==CreativeDomain.CHARACTER &&
                candidateCount==15)
                "Output exactly [SHEET_1], [SHEET_2], [SHEET_3] with five " +
                    "distinct, fully described frozen characters in each, then " +
                    "[NEGATIVE]. Preserve all methodology counts and viewpoints."
            else "Return [POSITIVE] one complete detailed image/video model " +
                    "instruction [/POSITIVE] and [NEGATIVE] constraints [/NEGATIVE]."
            val request="Compile the following verified owner authority into ONE " +
                "complete coherent native generation specification. " +
                "Preserve every binding methodology condition, reference identity, " +
                "output format, concept interaction and owner-approved correction. " +
                "Do not invent additional methodology stages. For distinct outputs " +
                "include all "+candidateCount+" independent designs.\n"+
                requestedOutput+"\nFULL AUTHORITY:\n"+authority+
                "\nOWNER REVIEW GUIDANCE:\n"+guidance+"\n"
            require(engine.countTokens(request)+engine.countTokens(SYSTEM)+
                outputBudget+256<contextSize) {
                "Complete owner methodology and reference evidence exceed " +
                    "the installed reasoning model's measured context; " +
                    "do not silently truncate or recursively rewrite authority."
            }
            onStage("One bounded Llama 3.2 compilation of complete original authority")
            engine.restartConversation(SYSTEM)
            val third=answer(request,outputBudget)
            val sheets=(1..3).map{parsePart(third,"SHEET_"+it)}
                .filter{it.isNotBlank()}
            val positive=parsePart(third,"POSITIVE").ifBlank {
                if(sheets.isNotEmpty())sheets.joinToString("\n\n") else third
            }
            require(positive.isNotBlank()) { "No complete generation prompt returned" }
            val result=NeuralDesignPlan(
                weights.id,contextSize,domain,authority,third,
                positive,parsePart(third,"NEGATIVE").ifBlank{baseNegative},sheets)
            val path=File(app.filesDir,"generation-plans/"+jobId+"-brain.json")
            val source=JSONObject().apply {
                put("schema","asterion.neural-planning.v1")
                put("model",result.modelId)
                put("context_tokens",result.actualContext)
                put("domain",result.domain.name)
                put("interpretation",result.interpretation)
                put("synthesis",result.synthesis)
                put("final_positive",result.positive)
                put("final_negative",result.negative)
                put("sheets",JSONArray(result.sheetPrompts))
                put("authority_pages",0)
                put("authority_chars",authority.length)
                put("all_authority_pages_processed",true)
                put("original_source_evidence",authority)
                put("hierarchical_consolidation",false)
                put("direct_original_authority_used",true)
                put("model_training_applied",false)
                put("learning_method","user-authored detailed grading; contextual evidence only")
                put("grading_evidence_applied",guidance.isNotBlank())
                put("visual_semantics_verified",false)
            }
            GeneratedImagePublisher.atomicWrite(path,source.toString(2).toByteArray())
            result
        } finally {
            try {
                withContext(NonCancellable+Dispatchers.IO) { engine.cleanUp() }
            } finally {
                DeviceInferenceGate.release()
            }
        }
    }
}
