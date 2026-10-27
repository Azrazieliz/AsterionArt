package com.asterion.art.learning

import android.content.Context
import com.asterion.art.data.GeneratedImagePublisher
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * A REAL locally trained preference model (logistic linear ranker).
 * Learns parameters from keep/reject decisions; does NOT modify Qwen weights.
 * Pairwise train can make rejected aesthetics less likely when drafting future
 * candidates. Model parameters are versioned and stored only on this device.
 */
object DesignFeatureEncoder {
    const val DIMENSIONS=1024
    private val token=Regex("[\\p{L}\\p{N}]{3,}")
    fun vector(text:String):DoubleArray {
        val source=token.findAll(text.lowercase())
            .map { it.value }.take(4500).toList()
        val count=DoubleArray(DIMENSIONS)
        val buckets=source.map { word ->
            ((word.hashCode().toLong() and 0x7fffffff) % DIMENSIONS).toInt()
        }
        buckets.forEach { count[it]+=1.0 }
        val norm=sqrt(count.sumOf { it*it })
        if(norm>0.0)for(i in count.indices)count[i]/=norm
        return count
    }
}

data class PreferenceModelState(
    val weights:DoubleArray=DoubleArray(DesignFeatureEncoder.DIMENSIONS),
    val updates:Int=0,
    val revision:Int=1
) {
    init { require(weights.size==DesignFeatureEncoder.DIMENSIONS) }
}

/** No external training API, background compute or upload is required. */
object OnlinePreferenceTraining {
    private fun sigmoid(value:Double):Double =
        if(value>=0)1.0/(1.0+exp(-value))else exp(value)/(1.0+exp(value))
    fun score(state:PreferenceModelState,description:String):Double {
        val vector=DesignFeatureEncoder.vector(description)
        val logit=state.weights.indices.sumOf { state.weights[it]*vector[it] }
        return sigmoid(logit)
    }
    fun update(state:PreferenceModelState,
               description:String,liked:Boolean,
               rate:Double=0.11):PreferenceModelState {
        require(rate in 0.000001..1.0)
        val feature=DesignFeatureEncoder.vector(description)
        val pred=sigmoid(state.weights.indices.sumOf {state.weights[it]*feature[it]})
        val error=(if(liked)1.0 else 0.0)-pred
        val updated=state.weights.clone()
        for(i in updated.indices) {
            updated[i]=(updated[i]*0.9995)+rate*error*feature[i]
        }
        return PreferenceModelState(updated,state.updates+1,state.revision)
    }
    fun pairwise(state:PreferenceModelState,
                 preferred:String,rejected:String,rate:Double=0.15):PreferenceModelState {
        val kept=DesignFeatureEncoder.vector(preferred)
        val discard=DesignFeatureEncoder.vector(rejected)
        val diff=DoubleArray(kept.size){i->kept[i]-discard[i]}
        val before=diff.indices.sumOf {state.weights[it]*diff[it]}
        val grad=rate*(1.0-sigmoid(before))
        val updated=state.weights.clone()
        for(i in updated.indices)updated[i]=updated[i]*0.9995+grad*diff[i]
        return PreferenceModelState(updated,state.updates+1,state.revision)
    }
}

class LocalPreferenceModel(context:Context) {
    private val file=File(context.applicationContext.filesDir,"creative-memory/preference-weights.json")
    @Synchronized fun state():PreferenceModelState {
        if(!file.isFile)return PreferenceModelState()
        return runCatching {
            val json=JSONObject(file.readText())
            require(json.getInt("version")==1)
            val a=json.getJSONArray("weights")
            require(a.length()==DesignFeatureEncoder.DIMENSIONS)
            PreferenceModelState(
                DoubleArray(a.length()){a.getDouble(it)},
                json.optInt("updates",0))
        }.getOrDefault(PreferenceModelState())
    }
    @Synchronized fun observe(description:String,liked:Boolean):PreferenceModelState {
        val next=OnlinePreferenceTraining.update(state(),description,liked)
        save(next)
        return next
    }
    @Synchronized fun compare(kept:String,rejected:String):PreferenceModelState {
        val next=OnlinePreferenceTraining.pairwise(state(),kept,rejected)
        save(next)
        return next
    }
    private fun save(s:PreferenceModelState) {
        val content=JSONObject().apply {
            put("version",s.revision)
            put("updates",s.updates)
            put("weights",JSONArray(s.weights.toList()))
            put("label","user-controlled design preference linear model")
            put("qwen_weights_modified",false)
        }
        GeneratedImagePublisher.atomicWrite(file,
            content.toString().toByteArray(Charsets.UTF_8))
    }
    fun rerank(designs:List<SavedDesign>):List<SavedDesign> {
        val current=state()
        if(current.updates==0)return designs
        return designs.sortedByDescending {
            OnlinePreferenceTraining.score(current,it.prompt)
        }
    }
}
