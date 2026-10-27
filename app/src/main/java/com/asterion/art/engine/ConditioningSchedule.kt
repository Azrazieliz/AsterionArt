package com.asterion.art.engine

/**
 * Lossless prompt scheduling for real, bounded native text encoders.
 *
 * This DOES NOT enlarge CLIP, Qwen image conditioning, or QNN tensors.
 * It schedules all evidence over distinct denoising passes, asking the
 * installed backend's real /tokenize endpoint to prove each pass fits.
 * No substring is quietly discarded. If even one unit cannot fit, fail.
 */
data class ConditioningChunk(
    val index:Int, val total:Int, val positive:String, val negative:String,
    val denoise:Float, val verifiedTokens:Int, val verifiedLimit:Int
)
data class ConditioningPlan(
    val sourceCharacters:Int, val emittedCharacters:Int,
    val chunks:List<ConditioningChunk>, val mode:String="sequential_passes"
)

object ConditioningSchedule {
    private const val HEADER="Asterion design continuation. Preserve all previously established " +
        "identity, anatomy, composition, materials and visual anchors. Add or correct the following " +
        "required details without deleting previously rendered content:\n"

    /**
     * tokenizer MUST count with the selected native model; don't use word estimates.
     * No backend model supports arbitrarily large simultaneous cross attention.
     */
    fun compile(prompt:String,negative:String,tokenizer:(String)->TokenCapacity,
                maximumPasses:Int=32,
                continuationHeader:String?=null):ConditioningPlan {
        require(prompt.isNotBlank())
        require(maximumPasses in 1..64)
        val positive=prompt.trim()
        val negativeClean=negative.trim()
        val negativeCount=if(negativeClean.isBlank())null else tokenizer(negativeClean).also {
            require(it.fits && it.limit>0){"Negative prompt exceeds native conditioning capacity"}
        }
        val direct=tokenizer(positive)
        require(direct.limit>0){"Native text encoder has not reported a measurable token limit"}
        if(direct.fits) {
            return ConditioningPlan(positive.length,positive.length,listOf(
                ConditioningChunk(1,1,positive,negativeClean,1.0f,
                    direct.count,direct.limit)),"single_pass")
        }
        // A complete lossless span ledger. Binary search for the largest span
        // the model's ACTUAL tokenizer can encode; keep all source characters.
        val chunks=mutableListOf<String>()
        var offset=0
        val shorter="Preserve prior image. Add these required details:\n"
        val preferred=continuationHeader?.takeIf{it.isNotBlank()}
        if(preferred!=null)require(tokenizer(preferred).fits){
            "Character identity anchor exceeds native tokenizer capacity"
        }
        val continuation=when {
            preferred!=null -> preferred
            tokenizer(HEADER).fits -> HEADER
            tokenizer(shorter).fits -> shorter
            else -> error("Native context is too small for safe continuation")
        }
        while(offset<positive.length) {
            require(chunks.size<maximumPasses) {
                "Required prompt needs more than $maximumPasses verified passes"
            }
            val prefix=if(chunks.isEmpty())"" else continuation
            var lo=1
            var hi=positive.length-offset
            var maxFit=0
            while(lo<=hi) {
                val middle=lo+(hi-lo)/2
                val candidate=prefix+positive.substring(offset,offset+middle)
                val proof=tokenizer(candidate)
                require(proof.limit>0){"Unmeasurable native encoder context"}
                if(proof.fits) {maxFit=middle;lo=middle+1}
                else hi=middle-1
            }
            require(maxFit>0){"A mandatory section cannot fit in native context"}
            var length=maxFit
            // Avoid cutting a UTF-16 surrogate pair in the original record.
            if(offset+length<positive.length &&
                Character.isHighSurrogate(positive[offset+length-1]) &&
                Character.isLowSurrogate(positive[offset+length])) {
                length--
            }
            require(length>0)
            // Prefer natural word breaks, without losing intervening whitespace.
            if(offset+length<positive.length) {
                val part=positive.substring(offset,offset+length)
                val lastSpace=part.indexOfLast{it.isWhitespace()}
                if(lastSpace>length/2 && lastSpace<length-1) {
                    val proposed=lastSpace+1
                    if(tokenizer(prefix+positive.substring(offset,offset+proposed)).fits)
                        length=proposed
                }
            }
            val chunk=positive.substring(offset,offset+length)
            require(tokenizer(prefix+chunk).fits)
            chunks+=chunk
            offset+=length
        }
        require(chunks.isNotEmpty())
        require(chunks.joinToString("")==positive) {
            "Prompt content was changed or lost during scheduling"
        }
        val parts=chunks.mapIndexed { i,chunk ->
            val text=if(i==0)chunk else continuation+chunk
            val proof=tokenizer(text)
            require(proof.fits && proof.limit>0)
            // Subsequent passes are controlled refinement, NOT a claim of
            // simultaneous attention to earlier textual evidence.
            val strength=when {
                i==0 -> 1f
                i<3 -> .44f
                i<8 -> .29f
                else -> .17f
            }
            ConditioningChunk(i+1,chunks.size,text,negativeClean,strength,
                proof.count,proof.limit)
        }
        return ConditioningPlan(positive.length,chunks.sumOf{it.length},parts)
    }
}
