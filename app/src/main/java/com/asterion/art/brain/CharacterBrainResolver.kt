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
                "Install Gemma 3 before preparing original character designs")
            val file=weights.location(model)?:error("Gemma 3 model file not verified")
            val selections=CharacterPrimaryResolver.resolve(
                plan.slots,plan.sheets,taxonomy.list(),Random(plan.seed))
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
                        "Full concept evidence exceeds available local Gemma 3 context"
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
                progress(completed.size,plan.total,"Loading Gemma 3 for anatomical design")
                timings.measure("model_load") {
                    engine.loadModel(file.absolutePath,context)
                    BrainAdapterLibrary(app).activeFor(model.id)?.let{
                        engine.loadAdapter(it.absolutePath)
                    }
                    engine.setSystemPrompt(SYSTEM)
                }
                val canonicalFull=mutableMapOf<String,String>()
                val canonicalSummaries=mutableMapOf<String,String>()
                val explicitGrades=DetailedGradeStore(app).list()
                suspend fun conceptEvidence(c:PrimaryConcept,summarize:Boolean):String {
                    val record=taxonomy.find(c.canonicalId?:c.name)
                        ?:return "FIXED OPEN LITERAL: "+c.name+
                            ". Derive complete physical meaning; never replace or invent SC IDs."
                    val full=canonicalFull.getOrPut(record.id) {
                        "CANONICAL "+record.id+": "+record.name+"\n"+record.fullRecord
                    }
                    // Reading source directly does not require generating hundreds
                    // of new Gemma tokens. Preserve the exact canonical text when
                    // the full design request fits the loaded model's context.
                    if(!summarize)return full
                    canonicalSummaries[record.id]?.let{return it}
                    val pages=BrainEvidencePager.pages(full,
                        (context-1900).coerceAtLeast(256)){engine.countTokens(it)}
                    val notes=mutableListOf<String>()
                    for(page in pages) {
                        progress(completed.size,plan.total,
                            "Interpreting canonical "+record.id+" "+page.index+"/"+page.total)
                        notes+=answer(
                            "Extract ALL mandatory major recognizable anatomical, material, " +
                                "fusion, symbiosis and interpretation-boundary knowledge from " +
                                "this canonical concept. No character yet. PAGE "+
                                page.index+"/"+page.total+"\n"+page.text,
                            minOf(650,context/6),"canonical_evidence")
                    }
                    val evidence="CANONICAL "+record.id+" "+record.name+"\n"+
                        notes.joinToString("\n")
                    canonicalSummaries[record.id]=evidence
                    return evidence
                }
                for((index,selection) in selections.withIndex()) {
                    if(index<completed.size)continue
                    progress(index,plan.total,"Designing "+selection.id)
                    val descriptions=selection.concepts.map {conceptEvidence(it,false)}
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
                    // Route full canonical evidence to synthesis unchanged whenever
                    // it fits. Only call Gemma's expensive evidence summarization
                    // for requests that genuinely cannot fit the model's context.
                    val fullAuthority=authorityFor(descriptions)
                    val study=if(fits(fullAuthority)) {
                        progress(index,plan.total,"Using complete canonical evidence directly")
                        fullAuthority
                    } else {
                        val compressed=selection.concepts.map {conceptEvidence(it,true)}
                        val authority=authorityFor(compressed)
                        if(fits(authority))authority else {
                        val authorityPages=BrainEvidencePager.pages(authority,
                            (context-1650).coerceAtLeast(256)){engine.countTokens(it)}
                        val notes=mutableListOf<String>()
                        for(page in authorityPages) {
                            progress(index,plan.total,
                                "Reading full design evidence "+page.index+"/"+
                                    page.total+" for "+selection.id)
                            notes+=answer(
                                "Preserve all binding anatomical, material and " +
                                "pairwise concept-fusion requirements; no character " +
                                "output yet. PAGE "+page.index+"/"+page.total+
                                "\n"+page.text,minOf(620,context/6),"design_authority_pages")
                        }
                        notes.joinToString("\n")
                        }
                    }
                    val baseInput=inputFor(study)
                    progress(index,plan.total,
                        "Gemma 3 authoring full anatomical identity "+selection.id)
                    val raw=answer(baseInput,outputLimit,"identity_json")
                    val data=CharacterOutputSchema.parseOrRecover(raw).let{partial->
                        if(CharacterOutputSchema.missingFields(partial).size<
                            CharacterOutputSchema.identityFields.size)partial else {
                            // Only repeat the entire schema if not even one
                            // complete model-written field was recoverable.
                            CharacterOutputSchema.parseOrRecover(answer(
                                "Your prior response lacked complete design fields. "+
                                "Return exactly EIGHT flat string fields with anatomically "+
                                "specific independent construction; use 600+ characters "+
                                "for render_description. No nested objects. "+
                                "PRIOR OUTPUT:\n"+raw.take(4500)+
                                "\nPRIMARY: "+selection.names.joinToString(" × ")+
                                "\n",outputLimit,"identity_json_recovery"))
                        }
                    }
                    val names=CharacterOutputSchema.identityFields
                    // Recover several omitted fields in bounded pairs, not one
                    // expensive prompt-prefill/model call for every omitted key.
                    // Preserve the valid fields from the original response.
                    val omitted=CharacterOutputSchema.missingFields(data)
                    if(omitted.size>=3) {
                        for(group in omitted.chunked(2)) {
                            if(group.size<2)continue
                            progress(index,plan.total,"Recovering "+
                                group.joinToString(" + ")+" for "+selection.id)
                            val retained=names.mapNotNull{key->
                                (data.opt(key) as? String)?.takeIf{value->
                                    value.length>=CharacterOutputSchema.minimumLength(key)
                                }?.let{key+": "+it.take(260)}
                            }.joinToString("\n")
                            val keys=group.joinToString(", ")
                            val requirements=group.joinToString("; "){field->
                                field+" minimum "+CharacterOutputSchema.minimumLength(field)+
                                    " characters"
                            }
                            val batch=runCatching {
                                CharacterOutputSchema.parseOrRecover(answer(
                                    "Return ONLY one FLAT JSON object with EXACTLY "+
                                    "these two prose STRING keys: "+keys+
                                    ". "+requirements+". SAME independent "+
                                    "character "+selection.id+
                                    "; preserve the already frozen design "+
                                    "details. Both values must physically embody "+
                                    "all primary concepts, not decorative "+
                                    "references. No nested data or commentary."+
                                    "\nPRIMARY: "+selection.names.joinToString(" × ")+
                                    "\nRETAINED DESIGN:\n"+retained+
                                    "\nBINDING STUDY:\n"+study.take(1800)+
                                    "\n",
                                    minOf(1900,context/3),"field_repair_batch"))
                            }.onFailure {
                                if(it is kotlinx.coroutines.CancellationException) throw it
                            }.getOrNull()
                            for(field in group) {
                                val value=batch?.optString(field)?.trim().orEmpty()
                                if(value.length>=CharacterOutputSchema.minimumLength(field))
                                    data.put(field,value)
                            }
                        }
                    }
                    // Repair only fields still absent after the batch. Never
                    // redo a complete character or overwrite a valid field.
                    for(field in names) {
                        val existing=data.opt(field)
                        val minimum=CharacterOutputSchema.minimumLength(field)
                        if(field !in CharacterOutputSchema.missingFields(data))
                            continue
                        progress(index,plan.total,"Repairing "+field+
                            " for "+selection.id)
                        val others=names.mapNotNull{key->
                            (data.opt(key) as? String)?.takeIf{it.length>=60}
                                ?.let{key+": "+it.take(320)}
                        }.joinToString("\n")
                        val text=answer(
                            "Produce ONLY the missing character-design prose " +
                            "field "+field+" for the SAME independently authored "+
                            selection.id+". Minimum "+minimum+" characters. "+
                            "Use every primary concept in physically meaningful "+
                            "anatomy, not ornament; preserve the already designed "+
                            "identity. No JSON, no fields besides "+field+
                            ".\nPRIMARY: "+selection.names.joinToString(" × ")+
                            "\nRETAINED DESIGN:\n"+others+
                            "\nBINDING STUDY:\n"+study.take(2400)+
                            "\n",
                            minOf(if(minimum>=600)1000 else 550,context/4),"field_repair")
                        require(text.length>=minimum) {
                            selection.id+": neural "+field+" remains incomplete"
                        }
                        data.put(field,text)
                    }
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
                        val response=runCatching {
                            CharacterOutputSchema.parseObject(answer(
                                "Return exactly one JSON object with key pairwise "+
                                "whose value is an ARRAY of "+group.size+
                                " objects. Every object must have STRING keys "+
                                "first, second and mechanism. Mechanism must be "+
                                "at least 65 characters of concrete, shared "+
                                "physical anatomy; no labels, metaphors, icons "+
                                "or decorative attachment substitutes. The "+
                                "primary names must match exactly. REQUIRED PAIRS:\n"+
                                specification+"\nFROZEN DESIGN:\n"+narrative+
                                "\n",minOf(1000,context/4),"pairwise_batch"))
                        }.onFailure{if(it is kotlinx.coroutines.CancellationException)throw it}
                            .getOrNull()?.optJSONArray("pairwise")
                        for((first,second) in group) {
                            val mechanism=CharacterOutputSchema.validMechanism(
                                response,first,second)?:
                                answer(
                                    "Write ONLY a concrete 90+ character shared "+
                                    "physical anatomical mechanism for "+first+
                                    " AND "+second+" within THIS frozen female "+
                                    "character. No metaphor or decorative "+
                                    "substitution.\nIDENTITY:\n"+narrative+
                                    "\n",minOf(400,context/6),"pairwise_repair")
                            require(mechanism.trim().length>=65){
                                selection.id+": unresolved physical fusion "+
                                    first+" + "+second
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
