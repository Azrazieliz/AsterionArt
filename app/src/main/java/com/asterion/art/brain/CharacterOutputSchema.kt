package com.asterion.art.brain

import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure schema gate for model-authored character evidence. Never generates,
 * fabricates or substitutes anatomy: invalid output must be repaired by Qwen3.
 */
internal object CharacterOutputSchema {
    val identityFields=listOf("anatomy","face_eyes_hair","topology","fusion",
        "materials","differentiation","render_description","audit_evidence")

    fun minimumLength(field:String):Int {
        require(field in identityFields)
        return if(field=="render_description")600 else 60
    }

    /** Extract complete top-level JSON, not a partial brace-to-brace fragment. */
    fun parseObject(raw:String):JSONObject {
        val objects=mutableListOf<JSONObject>()
        var depth=0
        var start=-1
        var quoted=false
        var escaped=false
        for(index in raw.indices) {
            val c=raw[index]
            if(quoted) {
                if(escaped) escaped=false
                else if(c=='\\') escaped=true
                else if(c=='"') quoted=false
            } else when(c) {
                '"' -> if(depth>0)quoted=true
                '{' -> {
                    if(depth==0)start=index
                    depth++
                }
                '}' -> {
                    if(depth>0) {
                        depth--
                        if(depth==0&&start>=0) {
                            runCatching {
                                JSONObject(raw.substring(start,index+1))
                            }.getOrNull()?.let(objects::add)
                            start=-1
                        }
                    }
                }
            }
        }
        return objects.lastOrNull { candidate ->
            identityFields.any(candidate::has)||candidate.has("pairwise")
        }?:objects.lastOrNull()?:throw IllegalArgumentException(
            "Qwen3 returned no complete structured character JSON")
    }

    /**
     * A token-budget limit may cut JSON mid-string. Recover ONLY full,
     * correctly escaped prose string values actually emitted by Qwen3.
     * Any absent or nested fields still require actual model inference.
     */
    fun parseOrRecover(raw:String):JSONObject {
        val parsed=runCatching{parseObject(raw)}.getOrNull()
        if(parsed!=null && identityFields.any(parsed::has))return parsed
        val recovered=JSONObject()
        val pattern=Regex(
            "\"("+identityFields.joinToString("|")+")\"\\s*:\\s*"+
                "(\"(?:\\\\.|[^\"\\\\])*\")")
        for(match in pattern.findAll(raw)) {
            val name=match.groupValues[1]
            if(recovered.has(name))continue
            val fragment="{"+"\""+name+"\":"+match.groupValues[2]+"}"
            val candidate=runCatching{JSONObject(fragment).getString(name)}.getOrNull()
            if(candidate!=null&&candidate.trim().length>=minimumLength(name))
                recovered.put(name,candidate)
        }
        return recovered
    }

    fun missingFields(data:JSONObject):List<String> =
        identityFields.filter { name ->
            val value=data.opt(name)
            value !is String || value.trim().length<minimumLength(name)
        }

    fun requiredPairs(names:List<String>):List<Pair<String,String>> =
        names.flatMapIndexed {index,first->
            names.drop(index+1).map{second->first to second}
        }

    /**
     * Two-concept characters can use their model-authored fusion field as
     * the required physical pair mechanism. A decorative or unnamed fusion
     * never qualifies; those designs still require a separate Qwen call.
     */
    fun validNamedFusion(fusion:String, first:String, second:String):String? {
        val prose=fusion.trim()
        if(prose.length<110 || first.isBlank() || second.isBlank())return null
        if(!prose.contains(first,ignoreCase=true) ||
            !prose.contains(second,ignoreCase=true))return null
        val material=Regex(
            "joint|skeleton|bone|tendon|load|root|dermis|tissue|membrane|muscle|"+
            "cartilage|chitin|mass|vascular|structure|anatom|graft",
            RegexOption.IGNORE_CASE)
        if(!material.containsMatchIn(prose))return null
        return prose
    }

    /** Exact conceptual pair, either order; reject absent/short mechanisms. */
    fun validMechanism(items:JSONArray?,first:String,second:String):String? {
        if(items==null)return null
        for(index in 0 until items.length()) {
            val item=items.optJSONObject(index)?:continue
            val a=item.optString("first")
            val b=item.optString("second")
            val matches=(a.equals(first,true)&&b.equals(second,true))||
                (a.equals(second,true)&&b.equals(first,true))
            val mechanism=item.optString("mechanism").trim()
            if(matches&&mechanism.length>=65)return mechanism
        }
        return null
    }
}
