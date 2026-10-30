package com.asterion.art.engine

import com.asterion.art.data.GeneratedImagePublisher
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Owner Artwork-only five-phase beam search.
 *
 * Phase 1 creates three complete foundations. Each later phase generates
 * three alternatives from EACH of the previous phase's three best images.
 * Nine resulting images are quality-screened, then only the best THREE
 * continue into the next phase. The final phase ranks its nine images and
 * selects ONE result for automatic enhancement.
 *
 * Intermediate native pixels, lineage, exact prompts, scores and seeds are
 * checkpointed atomically. No new inference/model selection is introduced.
 */
data class ArtworkBeamNode(
    val phase:Int,
    val child:Int,
    val parentId:String?,
    val id:String,
    val png:ByteArray,
    val score:Double,
    val seed:Long
)
data class ArtworkBeamResult(
    val winner:ArtworkBeamNode,
    val survivorsByPhase:List<List<ArtworkBeamNode>>,
    val generatedPerPhase:List<Int>
) {
    val totalInvocations:Int get()=generatedPerPhase.sum()
}
object ArtworkBeamPolicy {
    const val survivors=3
    const val defaultVariationsPerSurvivor=3
    fun <T> top(items:List<T>,value:(T)->Double):List<T> =
        items.sortedWith(compareByDescending<T>{value(it)}).take(survivors)
    fun schedule(parentCount:Int,phase:Int,
        variationsPerSurvivor:Int=defaultVariationsPerSurvivor):Int {
        require(phase in 1..5)
        require(variationsPerSurvivor>=1)
        require(parentCount==if(phase==1)0 else survivors)
        return if(phase==1)survivors else parentCount*variationsPerSurvivor
    }
}

class ArtworkBeamRunner(private val checkpointRoot:File) {
    private fun sha(bytes:ByteArray):String=MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString(""){"%02x".format(it)}
    private fun seed(base:Long,phase:Int,parent:Int,branch:Int):Long =
        (base xor (phase.toLong()*0x5DEECE66DL) xor
            (parent.toLong()*0x9E3779B1L) xor
            (branch.toLong()*0x6C8E9CF5L)) and Long.MAX_VALUE

    /**
     * render callback performs the actual native invocation and thermal gate.
     * stage/child are for progress and full native prompt traces.
     */
    suspend fun execute(
        stages:List<GenerationPass>,
        baseSeed:Long,
        initialPng:ByteArray?,
        modelId:String,
        variationsPerSurvivor:Int=ArtworkBeamPolicy.defaultVariationsPerSurvivor,
        stopped:()->Boolean,
        render:suspend (pass:GenerationPass,parent:ByteArray?,
            seed:Long,child:Int,total:Int,parentId:String?)->ByteArray,
        onStage:suspend (Int,List<ArtworkBeamNode>)->Unit={_,_->}
    ):ArtworkBeamResult {
        ArtworkFivePhaseContract.validate(stages)
        checkpointRoot.mkdirs()
        var parents=emptyList<ArtworkBeamNode>()
        val history=mutableListOf<List<ArtworkBeamNode>>()
        val generated=mutableListOf<Int>()
        stages.forEach { stage ->
            check(!stopped()){"Artwork beam interrupted"}
            val inputs:List<Pair<String?,ByteArray?>> = 
                if(stage.number==1)listOf(null to initialPng)
                else parents.map{it.id to it.png}
            val candidates=mutableListOf<ArtworkBeamNode>()
            val total=ArtworkBeamPolicy.schedule(
                parents.size,stage.number,variationsPerSurvivor)
            for((parentIndex,parent) in inputs.withIndex()) {
                val children=if(stage.number==1)ArtworkBeamPolicy.survivors
                    else variationsPerSurvivor
                for(branch in 0 until children) {
                    if(stopped())throw java.util.concurrent.CancellationException(
                        "Artwork rendering interrupted")
                    val child=parentIndex*children+branch+1
                    val id="p"+stage.number+"-c"+child
                    val nonce=seed(baseSeed,stage.number,parentIndex,branch)
                    val evidence=(modelId+"|"+stage.number+"|"+nonce+"|"+
                        stage.positive+"|"+stage.negative+"|"+stage.denoise+"|"+
                        (parent.second?.let{sha(it)}?:"NO_SOURCE"))
                    val fingerprint=sha(evidence.toByteArray(Charsets.UTF_8))
                    val pngFile=File(checkpointRoot,"beam_"+id+".png")
                    val metaFile=File(checkpointRoot,"beam_"+id+".json")
                    val restored=runCatching{
                        val meta=JSONObject(metaFile.readText())
                        require(meta.getString("fingerprint")==fingerprint)
                        val bytes=pngFile.readBytes()
                        RenderFidelityGate.requireValidImage(bytes,
                            "Restored Artwork "+id)
                        require(sha(bytes)==meta.getString("sha256"))
                        bytes
                    }.getOrNull()
                    val image=restored ?: render(stage,parent.second,nonce,child,
                        total,parent.first).also{png->
                        RenderFidelityGate.requireValidImage(png,
                            "Artwork phase "+stage.number+" child "+child)
                        GeneratedImagePublisher.atomicWrite(pngFile,png)
                        GeneratedImagePublisher.atomicWrite(metaFile,
                            JSONObject().apply{
                                put("phase",stage.number);put("child",child)
                                put("id",id);put("parent_id",parent.first)
                                put("seed",nonce);put("model",modelId)
                                put("fingerprint",fingerprint)
                                put("sha256",sha(png))
                                put("technical_score",
                                    TechnicalImageScorer.score(png).score)
                                put("source_pixels_preserved",true)
                            }.toString(2).toByteArray(Charsets.UTF_8))
                    }
                    candidates+=ArtworkBeamNode(
                        stage.number,child,parent.first,id,image,
                        TechnicalImageScorer.score(image).score,nonce)
                }
            }
            generated+=candidates.size
            require(candidates.size==total){"Incomplete phase candidate population"}
            parents=ArtworkBeamPolicy.top(candidates){it.score}
            require(parents.size==ArtworkBeamPolicy.survivors)
            history+=parents
            GeneratedImagePublisher.atomicWrite(
                File(checkpointRoot,"beam_phase_"+stage.number+"_survivors.json"),
                JSONObject().apply{
                    put("phase",stage.number)
                    put("stage_name",ArtworkFivePhaseContract.at(stage.number).name)
                    put("generated",candidates.size)
                    put("retained",JSONArray(parents.map{it.id}))
                    put("technical_only_ranking",true)
                }.toString(2).toByteArray(Charsets.UTF_8))
            onStage(stage.number,parents)
        }
        return ArtworkBeamResult(requireNotNull(parents.firstOrNull()),
            history,generated)
    }
}
