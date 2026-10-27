package com.asterion.art.learning

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import org.json.JSONObject
import java.io.File

data class PairedPreferenceExample(
    val modality:String,
    val instruction:String,
    val preferred:String,
    val rejected:String,
    val preferredId:String,
    val rejectedId:String
)

/**
 * Private, provenance-preserving examples for future Qwen3 DPO/LoRA fine-tuning.
 *
 * Stores structured contrastive preference pairs from genuinely explicit
 * Keep/Reject decisions, matched within the same creation workflow.
 * No network request, model training, or automatic external upload occurs.
 */
object PreferenceCorpus {
    fun pairs(records:List<SavedDesign>,maximum:Int=5000):List<PairedPreferenceExample> {
        require(maximum in 1..50_000)
        val accepted=records.filter{it.status=="KEPT"}.sortedByDescending{it.createdAt}
        val rejected=records.filter{it.status=="REJECTED"}.sortedByDescending{it.createdAt}
        val output=mutableListOf<PairedPreferenceExample>()
        for(kept in accepted) {
            val mode=kept.creationMode?.name ?: kept.mode.name
            for(refused in rejected) {
                val refusalMode=refused.creationMode?.name ?: refused.mode.name
                if(refusalMode!=mode || kept.id==refused.id)continue
                // A single parent or job connects the pair to a comparable task.
                val sharedJob=kept.jobId==refused.jobId
                val sharedParent=kept.parents.intersect(refused.parents.toSet()).isNotEmpty()
                val sharedSource=kept.sourceSheetIds.intersect(
                    refused.sourceSheetIds.toSet()).isNotEmpty()
                if(!sharedJob && !sharedParent && !sharedSource)continue
                output+=PairedPreferenceExample(
                    modality=mode,
                    instruction="Generate a structurally coherent, high-quality "+
                        mode.lowercase().replace('_',' ')+
                        " design matching the supplied canonical concepts and reference authorities.",
                    preferred=kept.prompt,
                    rejected=refused.prompt,
                    preferredId=kept.id,
                    rejectedId=refused.id)
                if(output.size>=maximum)return output
            }
        }
        return output
    }
}

/** Never writes training records to public Downloads by default. */
class TrainingCorpusStore(context:Context) {
    private val app=context.applicationContext
    private val root=File(app.filesDir,"creative-memory/training").also{it.mkdirs()}
    fun export():File {
        val records=CreativeMemoryStore(app).list()
        val examples=PreferenceCorpus.pairs(records)
        val file=File(root,"qwen3-dpo-preferences.jsonl")
        val content=examples.joinToString("\n"){item->
            JSONObject().apply {
                put("schema","asterion.preference-pair.v1")
                put("modality",item.modality)
                put("prompt",item.instruction)
                put("chosen",item.preferred)
                put("rejected",item.rejected)
                put("chosen_id",item.preferredId)
                put("rejected_id",item.rejectedId)
                put("source","explicit_app_user_feedback")
                put("text_only",true)
            }.toString()
        }+if(examples.isEmpty())"" else "\n"
        GeneratedImagePublisher.atomicWrite(file,content.toByteArray(Charsets.UTF_8))
        return file
    }
}
