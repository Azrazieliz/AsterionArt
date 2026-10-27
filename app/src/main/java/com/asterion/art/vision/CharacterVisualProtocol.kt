package com.asterion.art.vision

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import com.asterion.art.methodology.FrozenCharacter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * These observations are machine-proposed evidence, never an automated
 * user-grade, a certified physical-anatomy diagnosis, or a revised identity.
 */
data class CharacterVisualFinding(
    val characterId:String,
    val sheet:Int,
    val sourceUri:String,
    val sourceSha256:String,
    val visibleFeatures:List<String>,
    val conceptEvidence:Map<String,String>,
    val anatomyConcerns:List<String>,
    val viewConsistencyConcerns:List<String>,
    val drawingConcerns:List<String>,
    val uncertainty:List<String>,
    val evaluator:String="Qwen3-VL-2B-Instruct-Q4_K_M+Q8-projector",
    val status:String="UNVERIFIED_MACHINE_OBSERVATION"
) {
    fun correctionCandidates():List<String> =
        (anatomyConcerns+viewConsistencyConcerns+drawingConcerns)
            .filter{it.isNotBlank()}.distinct()
}

object CharacterVisualProtocol {
    fun prompt(d:FrozenCharacter):String {
        d.validate()
        return buildString {
            append("You are an offline visual inspection assistant. Examine the ACTUAL ")
            append("image of ONE character's reference triptych. Do not assume that ")
            append("the image matches the specification. Never invent an unseen ")
            append("feature. If unreadable, report uncertainty. This is not a user grade. ")
            append("Check each primary concept's visible BODY OWNED physical evidence, ")
            append("each pair's shared physical mechanism, rooted appendages, ")
            append("anatomically consistent proportions, face/hair identity between ")
            append("true-front, exact side and rear, and hand-illustrated 2D grammar. ")
            append("Clinically unclothed anatomy is the approved presentation; ")
            append("do not propose covering it with garments. ")
            append("Respond using ONLY a JSON object with keys: ")
            append("visible_features (array of literal observations), ")
            append("concept_evidence (object keyed by each exact primary name; value ")
            append("a concrete visible physical observation or 'unclear'), ")
            append("anatomy_concerns (array), view_consistency_concerns (array), ")
            append("drawing_concerns (array), uncertainty (array). ")
            append("Do not include a numeric grade or a claim of certification.\n")
            append("IDENTITY ").append(d.id).append('\n')
            append("APPROVED PRIMARY CONCEPTS: ")
            append(d.primary.joinToString(" × "){it.name}).append('\n')
            append("FROZEN ANATOMY: ").append(d.anatomy.take(2200)).append('\n')
            append("FROZEN FACE AND HAIR: ").append(d.faceEyesHair.take(1300))
                .append('\n')
            append("FROZEN APPENDAGE ROOTS: ").append(d.topology.take(1500))
                .append('\n')
            append("PHYSICAL FUSION PAIRS:\n")
            d.pairwise.forEach{
                append(it.first).append(" × ").append(it.second)
                    .append(": ").append(it.mechanism.take(750)).append('\n')
            }
            if(d.higherOrder.isNotBlank()) {
                append("COMPLETE SET PHYSICAL FUSION: ")
                    .append(d.higherOrder.take(1200)).append('\n')
            }
            append("EXPECTED 2D DRAWING: ")
                .append(d.materials.take(1400)).append('\n')
        }
    }
    private fun readTextArray(source:JSONObject,key:String):List<String> {
        val array=source.getJSONArray(key)
        require(array.length()<=40){"Unbounded visual findings"}
        return (0 until array.length()).map{array.getString(it).trim()}
            .filter{it.isNotBlank()}.map{it.take(650)}
    }
    fun parse(d:FrozenCharacter,sheet:Int,uri:String,sha:String,
              raw:String):CharacterVisualFinding {
        val beginning=raw.indexOf('{')
        val end=raw.lastIndexOf('}')
        require(beginning>=0&&end>beginning) {
            "The visual model produced no parseable structured observations"
        }
        val j=JSONObject(raw.substring(beginning,end+1))
        val map=j.getJSONObject("concept_evidence")
        val conceptEvidence=d.primary.associate {concept->
            val name=concept.name
            name to map.optString(name,"unclear").ifBlank{"unclear"}.take(700)
        }
        return CharacterVisualFinding(d.id,sheet,uri,sha,
            readTextArray(j,"visible_features"),conceptEvidence,
            readTextArray(j,"anatomy_concerns"),
            readTextArray(j,"view_consistency_concerns"),
            readTextArray(j,"drawing_concerns"),
            readTextArray(j,"uncertainty"))
    }
}

class CharacterVisualReportStore(context:Context) {
    private val root=File(context.applicationContext.filesDir,"vision-assessments")
        .also{it.mkdirs()}
    private fun report(jobId:String):File {
        require(jobId.matches(Regex("[A-Za-z0-9_-]{8,100}")))
        return File(root,jobId+".json")
    }
    fun findings(jobId:String):List<CharacterVisualFinding> {
        val source=report(jobId)
        if(!source.isFile)return emptyList()
        val root=JSONObject(source.readText(Charsets.UTF_8))
        require(root.getString("schema")=="asterion.character-visual-findings.v1")
        val all=root.getJSONArray("findings")
        return (0 until all.length()).map{index->
            val v=all.getJSONObject(index)
            val evidence=v.getJSONObject("concept_evidence")
            val names=evidence.keys().asSequence().toList()
            fun values(k:String)=v.getJSONArray(k).let{a->
                (0 until a.length()).map{a.getString(it)}
            }
            CharacterVisualFinding(
                v.getString("id"),v.getInt("sheet"),
                v.getString("uri"),v.getString("sha256"),
                values("visible_features"),names.associateWith{evidence.getString(it)},
                values("anatomy_concerns"),values("view_consistency_concerns"),
                values("drawing_concerns"),values("uncertainty"),
                v.getString("model"),v.getString("status"))
        }
    }
    @Synchronized fun save(jobId:String,results:List<CharacterVisualFinding>) {
        require(results.map{it.characterId}.distinct().size==results.size)
        val root=JSONObject().apply {
            put("schema","asterion.character-visual-findings.v1")
            put("job_id",jobId)
            put("not_user_grading",true)
            put("not_automated_certification",true)
            put("findings",JSONArray(results.map{r->
                JSONObject().put("id",r.characterId)
                    .put("sheet",r.sheet).put("uri",r.sourceUri)
                    .put("sha256",r.sourceSha256)
                    .put("visible_features",JSONArray(r.visibleFeatures))
                    .put("concept_evidence",JSONObject(r.conceptEvidence))
                    .put("anatomy_concerns",JSONArray(r.anatomyConcerns))
                    .put("view_consistency_concerns",
                        JSONArray(r.viewConsistencyConcerns))
                    .put("drawing_concerns",JSONArray(r.drawingConcerns))
                    .put("uncertainty",JSONArray(r.uncertainty))
                    .put("model",r.evaluator)
                    .put("status",r.status)
            }))
        }
        GeneratedImagePublisher.atomicWrite(report(jobId),
            root.toString(2).toByteArray(Charsets.UTF_8))
    }
}
