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
                maximumPasses:Int=32):ConditioningPlan {
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
        // Split only at full paragraph or sentence boundaries. Each exact segment is retained
        // in the ledger, including punctuation; no original content is silently thrown away.
        val units=Regex("(?<=\\n)|(?<=[.!?])(?=\\s)").split(positive)
            .flatMap { text ->
                if(text.length<=1200)listOf(text) else text.chunked(1100)
            }.filter{it.isNotEmpty()}
        require(units.isNotEmpty())
        val chunks=mutableListOf<String>()
        var current=""
        units.forEach { unit ->
            val candidate=current+unit
            val prefix=if(chunks.isEmpty())"" else HEADER
            if(tokenizer(prefix+candidate).fits) {
                current=candidate
            } else {
                if(current.isNotEmpty()) {
                    chunks+=current
                    require(chunks.size<maximumPasses) {
                        "Full conditioning requires more than $maximumPasses passes"
                    }
                    current=""
                }
                val continuation=HEADER+unit
                require(tokenizer(continuation).fits) {
                    "A required individual segment exceeds native encoder capacity"
                }
                current=unit
            }
        }
        if(current.isNotEmpty())chunks+=current
        require(chunks.size in 1..maximumPasses)
        // Lossless evidence audit: content appears in original order, unabridged.
        require(chunks.joinToString("")==positive) {
            "Conditioning schedule dropped or reordered source text"
        }
        val parts=chunks.mapIndexed { i,chunk ->
            val text=if(i==0)chunk else HEADER+chunk
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
