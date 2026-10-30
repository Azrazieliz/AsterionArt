package com.asterion.art.brain

import android.app.ActivityManager
import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.asterion.art.engine.ConceptTaxonomy
import com.asterion.art.learning.DetailedGradeStore
import com.asterion.art.learning.GradingGuidance
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.model.CreationMode
import com.asterion.art.nativeengine.DeviceInferenceGate
import com.asterion.art.methodology.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Random
import java.io.File

/**
 * Specialist Phase-1 author, not an image renderer. Never falls back
 * to predefined body/face arrays. Incomplete resolution blocks rendering.
 */
class CharacterBrainResolver(context:Context) {
    private val app=context.applicationContext
    private val taxonomy=ConceptTaxonomy(app)
    private val weights=BrainModelStore(app)
    private val plans=CharacterPlanStore(app)
    companion object {
        private const val SYSTEM=
            "Act as Asterion's specialist original female anime/manhwa character " +
            "designer. Author clinically unclothed anatomical reference characters, " +
            "never costumes, fetish poses, set dressing or external concept icons. " +
            "Every supplied primary concept physically constructs the body and " +
            "mutually transforms other concepts at major or meso scale. Three or " +
            "more concepts require consequential complete-set higher-order fusion. " +
            "Develop novel skeleton, mass distribution, face, eyes, hair, appendage " +
            "roots, anatomy, topology, material/value/colour architectures and " +
            "coherent hand-illustrated 2D appearance. Each character is independently " +
            "authored, aesthetically complete and legibly female despite radical " +
            "concept anatomy. No Design Direction, sub-concepts or uniform mannequins. " +
            "Return only the requested output. "
    }
    private fun memoryContext():Int {
        val info=ActivityManager.MemoryInfo()
        (app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getMemoryInfo(info)
        val free=info.availMem/(1024L*1024*1024)
        val target=weights.contextTokens()
        return when {
            free<2L->minOf(target,4096)
            free<4L->minOf(target,8192)
            free<6L->minOf(target,16384)
            else->target
        }
    }
    suspend fun resolve(id:String,progress:(Int,Int,String)->Unit={_,_,_->}):CharacterPhaseOnePlan =
        BrainInferenceLock.mutex.withLock {
            val plan=plans.load(id)
            require(plan.status=="DESIGNING")
            val model=weights.selected()?:error(
                "Install and select a local character reasoning model")
            val file=weights.location(model)?:error("Selected character reasoning model file not verified")
            val selector=com.asterion.art.engine.CanonicalSelectorGraph.load(app)
            val selections=CharacterPrimaryResolver.resolve(
                plan.slots,plan.sheets,taxonomy.list(),Random(plan.seed),selector)
            val engine=AiChat.getInferenceEngine(app)
            val context=memoryContext()
            val completed=plan.frozen.toMutableList()
            val timings=ReasoningStageTelemetry()
            var outcome="FAILED_OR_CANCELLED"
            suspend fun answer(question:String,maxOutput:Int,stage:String):String =
                timings.measure(stage) {
                    engine.restartConversation(SYSTEM)
                    val tokens=engine.countTokens(question)+engine.countTokens(SYSTEM)
                    require(tokens+maxOutput+160<context){
                        "Full concept evidence exceeds available local reasoning context"
                    }
                    // TTFT includes native prompt prefill and first-token decode.
                    // Stage stream duration excludes prompt processing.
                    val started=System.nanoTime()
                    var firstTokenAt:Long?=null
                    val response=StringBuilder()
                    try {
                        withTimeout(3L*60_000L) {
                            engine.sendUserPrompt(question,maxOutput).collect{piece->
                                if(firstTokenAt==null)firstTokenAt=System.nanoTime()
                                response.append(piece)
                            }
                        }
                    } finally {
                        val ended=System.nanoTime()
                        val first=firstTokenAt
                        timings.recordDuration(stage+"_ttft",(first?:ended)-started)
                        if(first!=null)timings.recordDuration(stage+"_stream",ended-first)
                    }
                    response.toString().trim().also {
                        require(it.isNotEmpty()){"Empty neural design evidence"}
                    }
                }
            val requestContext=currentCoroutineContext()
            DeviceInferenceGate.acquire{!requestContext.isActive}
            try {
                timings.measure("model_ready_wait") {
                    withTimeout(120_000L){
                        engine.state.first{
                            it is InferenceEngine.State.Initialized||
                            it is InferenceEngine.State.ModelReady||
                            it is InferenceEngine.State.Error
                        }
                    }
                }
                progress(completed.size,plan.total,"Loading selected character reasoning model")
                timings.measure("model_load") {
                    engine.loadModel(file.absolutePath,context)
                    BrainAdapterLibrary(app).activeFor(model.id,"CHARACTER")?.let{
                        engine.loadAdapter(it.absolutePath)
                    }
                    engine.setSystemPrompt(SYSTEM)
                }
                // Read the original canonical record ONCE, without asking the
                // reasoning model to paraphrase, shorten or re-summarize it.
                // Long records are an explicit native-context compatibility
                // issue; sequential paraphrases are not expanded attention.
                val explicitGrades=DetailedGradeStore(app).list()
                fun conceptEvidence(c:PrimaryConcept):String {
                    val record=taxonomy.find(c.canonicalId?:c.name)
                        ?:return "FIXED OPEN LITERAL: "+c.name+
                            ". Preserve the exact selected concept identifier."
                    return "CANONICAL "+record.id+": "+record.name+"\n"+
                        record.fullRecord
                }
                for((index,selection) in selections.withIndex()) {
                    if(index<completed.size)continue
                    progress(index,plan.total,"Designing "+selection.id)
                    val descriptions=selection.concepts.map(::conceptEvidence)
                    val prior=completed.takeLast(10).joinToString("\n"){
                        it.id+": "+it.anatomy.take(180)+"; "+it.topology.take(120)+
                            "; "+it.fusion.take(160)+"; "+it.faceEyesHair.take(110)
                    }
                    val grading=GradingGuidance.evidence(
                        GradingGuidance.applicable(
                            explicitGrades,CreationMode.CHARACTER,selection.names))
                    fun authorityFor(evidence:List<String>)=buildString{
                        append("AUTHOR COMPLETE CHARACTER ").append(selection.id)
                        append("\nRESOLVED FIXED CONCEPTS: ")
                        append(selection.names.joinToString(" × "))
                        append("\n").append(CharacterPhaseOne.AUTHORING)
                        append("\nDRAWING: ").append(CharacterPhaseOne.DRAWING)
                        append("\nPRIMARY CANONICAL CONCEPT EVIDENCE:\n")
                        append(evidence.joinToString("\n"))
                        append("\nEXISTING INDEPENDENT IDENTITIES (DO NOT COPY):\n")
                        append(prior)
                        if(grading.isNotBlank()){
                            append("\nPREVIOUS USER GRADING (scoped corrections, ")
                            append("not new methodology rules):\n")
                            append(grading)
                        }
                    }
                    // Model-generated designs are authoritative, but a single long
                    // JSON response must not be responsible for 8 fields, up to
                    // 28 pairwise interactions AND a higher-order structure.
                    // Missing schema fields caused repeated full expensive jobs
                    // on actual phone traces. Split the budget by output role.
                    val format="Return one FLAT valid JSON object with EXACTLY " +
                        "eight STRING keys: anatomy, face_eyes_hair, topology, " +
                        "fusion, materials, differentiation, render_description, " +
                        "audit_evidence. All values must be prose strings, NEVER " +
                        "nested JSON or arrays. Complete ALL eight fields " +
                        "before the token budget expires. Budget characters per " +
                        "field: anatomy 250-520, face_eyes_hair 160-450, " +
                        "topology 160-450, fusion 160-450, materials 160-400, " +
                        "differentiation 160-400, render_description 600-1100, " +
                        "audit_evidence 120-350. Do not spend the response " +
                        "on the first field. For exactly two concepts, the " +
                        "fusion field MUST name both and detail their actual " +
                        "shared anatomical load path and material interface. " +
                        "Fully design one original " +
                        "anatomical character, with " +
                        "complete feminine silhouette, original face/eyes/hair, " +
                        "rooted appendages, consequential integration of EVERY " +
                        "named primary concept, materials, identity-specific 2D " +
                        "drawing and independently authored facial structure. " +
                        "The render description is a complete positive self-contained " +
                        "2D reconstruction, not a brief summary. No clothes, no " +
                        "props, no scenes. Do not include pairwise or higher_order " +
                        "in THIS response; these are authored separately with an " +
                        "independent output budget. No Markdown or commentary."
                    // Keep all eight authored fields but avoid the 2300-token
                    // response budget that repeatedly ran into multi-minute
                    // decoding and truncated JSON on the phone.
                    val outputLimit=minOf(1400,context/3)
                    fun inputFor(evidence:String)=format+"\nCHARACTER "+selection.id+
                        "\nPRIMARY: "+selection.names.joinToString(" × ")+
                        "\n"+CharacterPhaseOne.DRAWING+
                        "\nDESIGN EVIDENCE:\n"+evidence+"\n"
                    suspend fun fits(evidence:String):Boolean =
                        engine.countTokens(inputFor(evidence))+
                            engine.countTokens(SYSTEM)+outputLimit+160<context
                    // A single complete authority must fit the measured
                    // context. Never spend hours reducing its binding detail.
                    val study=authorityFor(descriptions)
                    require(fits(study)) {
                        "The exact character methodology and canonical concepts "+
                            "exceed the installed model context. No multi-hour "+
                            "summarization or authority loss is permitted."
                    }
                    progress(index,plan.total,
                        "Using one complete canonical identity specification")
                    val baseInput=inputFor(study)
                    progress(index,plan.total,
                        "Authoring full anatomical identity "+selection.id)
                    val raw=answer(baseInput,outputLimit,"identity_json")
                    // One bounded structured authoring request, not a
                    // sequence of costly per-field hallucinated repairs.
                    // Invalid output remains recoverable from the saved plan;
                    // never certify a character with missing anatomy.
                    val data=CharacterOutputSchema.parseOrRecover(raw)
                    val missing=CharacterOutputSchema.missingFields(data)
                    require(missing.isEmpty()) {
                        selection.id+": incomplete original design fields: "+
                            missing.joinToString(", ")+
                            ". The model failed its required schema."
                    }
                    val names=CharacterOutputSchema.identityFields
                    val narrative=names.joinToString("\n"){field->
                        field+": "+data.getString(field).take(420)
                    }
                    val pairs=mutableListOf<PairwisePhysicalFusion>()
                    val required=CharacterOutputSchema.requiredPairs(selection.names)
                    for(group in required.chunked(3)) {
                        progress(index,plan.total,
                            "Resolving physical concept pairs "+pairs.size+
                                "/"+required.size+" for "+selection.id)
                        val specification=group.joinToString("\n"){
                            (first,second)->first+" + "+second
                        }
                        // One pair needs one explicit physical mechanism, not
                        // a JSON array round trip plus a second repair call.
                        // This is the common two-primary-concept Phase-1 path.
                        if(group.size==1) {
                            val (first,second)=group.single()
                            val authored=CharacterOutputSchema.validNamedFusion(
                                data.getString("fusion"),first,second)
                            if(authored!=null) {
                                pairs+=PairwisePhysicalFusion(first,second,authored)
                                continue
                            }
                            val mechanism=answer(
                                "Write ONLY one detailed 110+ character physical " +
                                "fusion mechanism shared by "+first+" AND "+second+
                                " in the SAME frozen anatomical character. " +
                                "Specify joint, material or load-path integration " +
                                "rather than ornaments or metaphor. Plain prose, " +
                                "not JSON.\nIDENTITY:\n"+narrative+"\n",
                                minOf(400,context/6),"pairwise_single")
                            require(mechanism.length>=65) {
                                selection.id+": unresolved physical fusion "+first+" + "+second
                            }
                            pairs+=PairwisePhysicalFusion(first,second,mechanism)
                            continue
                        }
                        // Author a complete verified batch once. Repeating
                        // a separate model call for every failed pair caused
                        // substantial unattended stalls and identity drift.
                        val response=CharacterOutputSchema.parseObject(answer(
                            "Return exactly one JSON object with key pairwise "+
                            "whose value is an ARRAY of "+group.size+
                            " objects. Every object has STRING keys first, "+
                            "second and mechanism. Each mechanism must detail "+
                            "at least 65 characters of concrete physical anatomy "+
                            "that integrates BOTH concepts with no decorative "+
                            "substitution. Required pairs:\n"+
                            specification+"\nFROZEN DESIGN:\n"+narrative+"\n",
                            minOf(1000,context/4),"pairwise_batch"))
                            .optJSONArray("pairwise")
                        for((first,second) in group) {
                            val mechanism=CharacterOutputSchema.validMechanism(
                                response,first,second)
                            require(!mechanism.isNullOrBlank()) {
                                selection.id+": invalid physical fusion of "+
                                    first+" + "+second+
                                    "; original methodology not satisfied."
                            }
                            pairs+=PairwisePhysicalFusion(first,second,mechanism)
                        }
                    }
                    val higher=if(selection.concepts.size<3)"" else answer(
                        "Write ONLY a concrete 140+ character anatomically "+
                        "consequential higher-order structure which physically "+
                        "requires ALL the primary concepts together, not merely "+
                        "pairwise associations. Same frozen identity. "+
                        "PRIMARY: "+selection.names.joinToString(" × ")+
                        "\nDESIGN:\n"+narrative+"\n",
                        minOf(500,context/6),"higher_order")
                    val character=FrozenCharacter(
                        selection.number,selection.concepts,
                        data.getString("anatomy"),data.getString("face_eyes_hair"),
                        data.getString("topology"),data.getString("fusion"),
                        data.getString("materials"),data.getString("differentiation"),
                        data.getString("render_description"),
                        data.getString("audit_evidence"),pairs,higher)
                        .also{it.validate()}
                    require(completed.none{it.signature()==character.signature()}) {
                        "Duplicate body/fusion architecture; population cannot render"
                    }
                    completed+=character
                    timings.measure("checkpoint_persist") {
                        plans.progress(id,selections,completed)
                    }
                }
                val failures=CharacterSheetCompiler.audit(completed,plan.sheets)
                require(failures.isEmpty()) {failures.joinToString("; ")}
                progress(plan.total,plan.total,"All characters resolved; freezing sheet contexts")
                timings.measure("final_certification") {
                    plans.certify(id,model.id,selections,completed)
                }.also{outcome="COMPLETE"}
            } finally {
                try {
                    withContext(NonCancellable+Dispatchers.IO) {
                        runCatching{engine.cleanUp()}
                    }
                } finally {
                    try {
                        val telemetry=File(app.filesDir,
                            "character-phase1/telemetry/"+id+".json")
                        telemetry.parentFile?.mkdirs()
                        GeneratedImagePublisher.atomicWrite(telemetry,
                            timings.snapshot(id,outcome).toString(2)
                                .toByteArray(Charsets.UTF_8))
                    } finally {
                        DeviceInferenceGate.release()
                    }
                }
            }
        }
}
