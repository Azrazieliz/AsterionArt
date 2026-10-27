package com.asterion.art.brain

import android.app.ActivityManager
import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.asterion.art.engine.ConceptTaxonomy
import com.asterion.art.methodology.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Random

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
            "Return only the requested output. /no_think"
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
    private fun objectFrom(text:String):JSONObject {
        val start=text.indexOf('{')
        val end=text.lastIndexOf('}')
        require(start>=0&&end>start){"Qwen3 returned no complete structured character"}
        return JSONObject(text.substring(start,end+1))
    }
    suspend fun resolve(id:String,progress:(Int,Int,String)->Unit={}):CharacterPhaseOnePlan =
        BrainInferenceLock.mutex.withLock {
            val plan=plans.load(id)
            require(plan.status=="DESIGNING")
            val model=weights.selected()?:error(
                "Install Qwen3 before preparing original character designs")
            val file=weights.location(model)?:error("Qwen3 model file not verified")
            val selections=CharacterPrimaryResolver.resolve(
                plan.slots,plan.sheets,taxonomy.list(),Random(plan.seed))
            val engine=AiChat.getInferenceEngine(app)
            val context=memoryContext()
            val completed=mutableListOf<FrozenCharacter>()
            suspend fun answer(question:String,maxOutput:Int):String {
                engine.restartConversation(SYSTEM)
                val tokens=engine.countTokens(question)+engine.countTokens(SYSTEM)
                require(tokens+maxOutput+160<context){
                    "Full concept evidence exceeds available local Qwen3 context"
                }
                return withTimeout(25L*60_000L){
                    engine.sendUserPrompt(question,maxOutput).toList().joinToString("")
                }.trim().also{require(it.length>=80){"Empty neural design evidence"}}
            }
            try {
                withTimeout(120_000L){
                    engine.state.first{
                        it is InferenceEngine.State.Initialized||
                        it is InferenceEngine.State.ModelReady||
                        it is InferenceEngine.State.Error
                    }
                }
                progress(0,plan.total,"Loading Qwen3 for anatomical design")
                engine.loadModel(file.absolutePath,context)
                BrainAdapterLibrary(app).activeFor(model.id)?.let{
                    engine.loadAdapter(it.absolutePath)
                }
                engine.setSystemPrompt(SYSTEM)
                val readAuthorities=mutableMapOf<String,String>()
                suspend fun conceptEvidence(c:PrimaryConcept):String {
                    val record=taxonomy.find(c.canonicalId?:c.name)
                        ?:return "FIXED OPEN LITERAL: "+c.name+
                            ". Derive complete physical meaning; never replace or invent SC IDs."
                    readAuthorities[record.id]?.let{return it}
                    val full="CANONICAL "+record.id+": "+record.name+"\n"+record.fullRecord
                    val pages=BrainEvidencePager.pages(full,
                        (context-1900).coerceAtLeast(256)){engine.countTokens(it)}
                    val notes=mutableListOf<String>()
                    for(page in pages) {
                        notes+=answer(
                            "Extract ALL mandatory major recognizable anatomical, material, " +
                                "fusion, symbiosis and interpretation-boundary knowledge from " +
                                "this canonical concept. No character yet. PAGE "+
                                page.index+"/"+page.total+"\n"+page.text,
                            minOf(650,context/6))
                    }
                    val evidence="CANONICAL "+record.id+" "+record.name+"\n"+
                        notes.joinToString("\n")
                    readAuthorities[record.id]=evidence
                    return evidence
                }
                for((index,selection) in selections.withIndex()) {
                    progress(index,plan.total,"Designing "+selection.id)
                    val descriptions=mutableListOf<String>()
                    for(p in selection.concepts)descriptions+=conceptEvidence(p)
                    val prior=completed.takeLast(10).joinToString("\n"){
                        it.id+": "+it.anatomy.take(180)+"; "+it.topology.take(120)+
                            "; "+it.fusion.take(160)+"; "+it.faceEyesHair.take(110)
                    }
                    val authority=buildString{
                        append("AUTHOR COMPLETE CHARACTER ").append(selection.id)
                        append("\nRESOLVED FIXED CONCEPTS: ")
                        append(selection.names.joinToString(" × "))
                        append("\n").append(CharacterPhaseOne.AUTHORING)
                        append("\nDRAWING: ").append(CharacterPhaseOne.DRAWING)
                        append("\nPRIMARY CANONICAL CONCEPT EVIDENCE:\n")
                        append(descriptions.joinToString("\n"))
                        append("\nEXISTING INDEPENDENT IDENTITIES (DO NOT COPY):\n")
                        append(prior)
                    }
                    val authorityPages=BrainEvidencePager.pages(authority,
                        (context-1650).coerceAtLeast(256)){engine.countTokens(it)}
                    val notes=mutableListOf<String>()
                    for(page in authorityPages) {
                        notes+=answer("Analyze and preserve every binding character-design " +
                            "requirement, physical concept contribution and pairwise " +
                            "fusion mechanism. No final output yet. PAGE "+
                            page.index+"/"+page.total+"\n"+page.text,
                            minOf(620,context/6))
                    }
                    val study=notes.joinToString("\n")
                    val format="Return ONE valid JSON object with complete string fields: " +
                        "anatomy, face_eyes_hair, topology, fusion, materials, " +
                        "differentiation, render_description, audit_evidence. " +
                        "Body must have complete female silhouette, proportion, bones, " +
                        "joint systems and physically integrated concepts. Face/eyes/hair " +
                        "must be original. Topology must specify rooted body systems. " +
                        "Fusion must explicitly name EACH primary concept, " +
                        "all pairwise physical interactions and an overall higher-order " +
                        "construction for three or more. Materials specify illustrated " +
                        "material and colour/value behavior. Differentiation must " +
                        "explain independent authored identity. Render_description " +
                        "must be a SELF-CONTAINED positive 600+ character anatomical " +
                        "2D visual reconstruction covering EVERY named primary concept, " +
                        "without outfits, props, scenes or semantic labels as substitutes. " +
                        "Audit evidence must be concrete rather than a generic PASS. " +
                        "No Markdown, no surrounding prose."
                    suspend fun author(extra:String):FrozenCharacter {
                        val input=format+"\nCHARACTER "+selection.id+"\nPRIMARY: "+
                            selection.names.joinToString(" × ")+"\n"+
                            CharacterPhaseOne.DRAWING+"\nDESIGN STUDY:\n"+
                            study+"\n"+extra+"\n/no_think"
                        val raw=answer(input,minOf(2200,context/2))
                        val data=objectFrom(raw)
                        return FrozenCharacter(selection.number,selection.concepts,
                            data.getString("anatomy"),data.getString("face_eyes_hair"),
                            data.getString("topology"),data.getString("fusion"),
                            data.getString("materials"),data.getString("differentiation"),
                            data.getString("render_description"),data.getString("audit_evidence"))
                            .also{it.validate()}
                    }
                    var resolved=runCatching{author("")}
                    if(resolved.isFailure) {
                        val problem=resolved.exceptionOrNull()?.message.orEmpty()
                        resolved=runCatching{author(
                            "CORRECT FAILED FULL-RESOLUTION CHECK: "+problem+
                                ". Reconstruct the SAME primary concept character, "+
                                "do not omit required visible details.")}
                    }
                    val character=resolved.getOrThrow()
                    require(completed.none{it.signature()==character.signature()}) {
                        "Duplicate body/fusion architecture; population cannot render"
                    }
                    completed+=character
                    plans.progress(id,selections,completed)
                }
                val failures=CharacterSheetCompiler.audit(completed,plan.sheets)
                require(failures.isEmpty()) {failures.joinToString("; ")}
                progress(plan.total,plan.total,"All characters resolved; freezing sheet contexts")
                plans.certify(id,model.id,selections,completed)
            } finally {
                withContext(Dispatchers.IO){runCatching{engine.cleanUp()}}
            }
        }
}
