package com.asterion.art.brain

/** The original full evidence is paged, never substring-discarded. */
data class EvidencePage(val index:Int,val total:Int,
                        val text:String,val tokenizerCount:Int)

/**
 * Accounts for the real Qwen tokenizer (supplied by llama.cpp JNI). Pages are
 * independent; every page is read and reasoned over, then folded into a
 * smaller interpretation before synthesis. No claim that an LLM can retain
 * every word when it composes a shorter learned interpretation.
 */
object BrainEvidencePager {
    suspend fun pages(text:String,tokenBudget:Int,
                      count:suspend(String)->Int):List<EvidencePage> {
        require(text.isNotEmpty())
        require(tokenBudget>=256)
        val chunks=mutableListOf<String>()
        var offset=0
        while(offset<text.length) {
            require(chunks.size<512) {
                "Methodology requires more than 512 context pages; refusing truncation"
            }
            var low=1
            var high=text.length-offset
            var safe=0
            while(low<=high) {
                val length=low+(high-low)/2
                val tokens=count(text.substring(offset,offset+length))
                require(tokens>=0)
                if(tokens<=tokenBudget) {safe=length;low=length+1}
                else high=length-1
            }
            require(safe>0){"Evidence tokenizer cannot fit even a character"}
            var length=safe
            if(offset+length<text.length &&
                Character.isHighSurrogate(text[offset+length-1]) &&
                Character.isLowSurrogate(text[offset+length]))length--
            require(length>0)
            if(offset+length<text.length) {
                val candidate=text.substring(offset,offset+length)
                val space=candidate.indexOfLast { it.isWhitespace() }
                if(space>length/2) {
                    val shorter=space+1
                    if(count(text.substring(offset,offset+shorter))<=tokenBudget)
                        length=shorter
                }
            }
            chunks+=text.substring(offset,offset+length)
            offset+=length
        }
        check(chunks.joinToString("")==text){"Evidence paging lost content"}
        return chunks.mapIndexed { i,chunk ->
            val n=count(chunk)
            require(n<=tokenBudget)
            EvidencePage(i+1,chunks.size,chunk,n)
        }
    }
}
