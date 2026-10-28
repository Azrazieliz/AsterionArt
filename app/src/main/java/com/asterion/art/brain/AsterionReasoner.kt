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
 * Real inference: Qwen3 GGUF is loaded in CPU memory and released BEFORE QNN.
 *
 * Stage 1 interprets selected canonical knowledge.
 * Stage 2 authors distinct structures and interactions using learned preferences.
 * Stage 3 compiles fully detailed prompts appropriate to the output domain.
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
            "Answer only the requested structured artifact. /no_think"
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
            return withTimeout(25*60*1000L) {
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
            // LoRA is applied to a compatible Qwen3 base, never silently
            // substituted for an incompatible architecture.
            BrainAdapterLibrary(app).activeFor(weights.id)?.let { adapter ->
                engine.loadAdapter(adapter.absolutePath)
            }
            engine.setSystemPrompt(SYSTEM)
            val canonical=section(domain,concepts)
            val guidance=learnedEvidence(
                creationMode,renderMode,concepts,targetDesignId)
            onStage("Layer 1 · complete paged authority interpretation")
            val authority=listOf(
                "DOMAIN:\n"+domain.name,
                "FULL METHODOLOGY:\n"+canonicalMethod,
                "CANONICAL CONCEPT RECORDS:\n"+canonical,
                "SOURCE EVIDENCE:\n"+sheetSources.joinToString("\n"),
                "POSITIVE AUTHORITY:\n"+basePositive,
                "NEGATIVE AUTHORITY:\n"+baseNegative
            ).joinToString("\n\n")
            // Reserve native KV space for role templates and generated output.
            // Paging counts the exact loaded tokenizer, not words or characters.
            val pages=BrainEvidencePager.pages(authority,
                (contextSize-2200).coerceAtLeast(256)){text->
                engine.countTokens(text)
            }
            val notes=mutableListOf<String>()
            for(page in pages) {
                engine.restartConversation(SYSTEM)
                onStage("Layer 1 · authority page "+page.index+"/"+pages.size)
                notes+=answer(
                    "Extract every critical identity, physical mechanism, " +
                        "construction rule, concept interaction, prohibitions and " +
                        "output requirement in this authority page. " +
                        "Quote exact numbered constraints and preserve source names. " +
                        "Do not design yet. PAGE "+page.index+"/"+pages.size+"\n" +
                        page.text+"\n/no_think",700)
            }
            // Hierarchical consolidation is explicit. Full source pages remain
            // preserved in a manifest; the learned interpretation is compressed.
            var consolidated=notes.toList()
            repeat(5) { level ->
                val joined=consolidated.joinToString("\n\n")
                if(engine.countTokens(joined)<contextSize/3)
                    return@repeat
                val groups=BrainEvidencePager.pages(joined,
                    (contextSize/2).coerceAtLeast(256)){text->
                    engine.countTokens(text)
                }
                val newer=mutableListOf<String>()
                for(group in groups) {
                    engine.restartConversation(SYSTEM)
                    newer+=answer(
                        "Consolidate these already-interpreted constraints " +
                            "without losing named identities, required counts, " +
                            "construction mechanisms and forbidden changes. " +
                            "Keep each mandatory condition checkable.\n"+
                            group.text+"\n/no_think",1100)
                }
                require(newer.joinToString("").length<joined.length) {
                    "Neural consolidation is not reducing context; refusing source loss"
                }
                consolidated=newer
            }
            val first=consolidated.joinToString("\n\n")
            require(engine.countTokens(first)<contextSize/2) {
                "Full authority could not be safely condensed into the design context"
            }
            engine.restartConversation(SYSTEM)
            onStage("Layer 2 · distinct structural synthesis")
            val second=answer(
                "Construct "+candidateCount+" genuinely distinct production-quality "+
                    domain.name.lowercase().replace('_',' ')+" design(s) " +
                    "from the verified interpretation. Create causal interactions and " +
                    "specific geometry. For multiple outputs label designs D01 onward, " +
                    "with individually frozen geometry, silhouette, materials, anatomy, " +
                    "palette, attach points and differentiating features. " +
                    "Return NO generic boilerplate and no image model prompt yet.\n" +
                    "INTERPRETATION:\n"+first+"\nPREFERENCES:\n"+guidance+
                    "\nREQUIRED OUTPUT: follow every explicit constraint above.\n/no_think",if(candidateCount>=10)3800 else 1700)
            onStage("Layer 3 · model-facing prompt compilation")
            val request=if(domain==CreativeDomain.CHARACTER && candidateCount==15)
                "Produce THREE exact complete sheet prompts. Each has five different " +
                "designs (D01-D05, D06-D10, D11-D15) with exact same-identity " +
                "front/side/rear views, five entities per landscape sheet. " +
                "Give explicit anatomy and material details for each identity. " +
                "Format [SHEET_1]...[/SHEET_1], [SHEET_2]...[/SHEET_2], "+
                "[SHEET_3]...[/SHEET_3], then [NEGATIVE]...[/NEGATIVE]."
            else "Produce exactly one fully detailed POSITIVE model-facing image/video " +
                "prompt and one NEGATIVE prompt. Output [POSITIVE]...[/POSITIVE] " +
                "[NEGATIVE]...[/NEGATIVE]. Mention all unique construction mechanisms."
            engine.restartConversation(SYSTEM)
            val predict=if(candidateCount>=10)4500 else 2200
            val baseline=request+"\nFULL INTERPRETATION (from all "+pages.size+
                " authority page(s)):\n"+first+
                "\nDESIGNS:\n"+second+
                "\n/no_think"
            require(engine.countTokens(baseline)+predict+512<contextSize) {
                "Compiled design synthesis exceeds configured neural context"
            }
            val additional="\nEXACT ORIGINAL POSITIVE CONTRACT:\n"+basePositive+
                "\nEXACT ORIGINAL NEGATIVE CONTRACT:\n"+baseNegative
            val complete=if(engine.countTokens(baseline+additional)+predict+512<contextSize)
                baseline+additional else baseline
            val third=answer(complete,predict)
            val sheets=(1..3).map{parsePart(third,"SHEET_"+it)}
                .filter{it.isNotBlank()}
            val result=NeuralDesignPlan(
                weights.id,contextSize,domain,first,second,
                parsePart(third,"POSITIVE").ifBlank {
                    if(sheets.isNotEmpty())sheets.joinToString("\n\n") else third
                },
                parsePart(third,"NEGATIVE").ifBlank{baseNegative},sheets)
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
                put("authority_pages",pages.size)
                put("authority_chars",authority.length)
                put("all_authority_pages_processed",notes.size==pages.size)
                put("original_source_evidence",authority)
                put("hierarchical_consolidation",notes.size>1)
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
