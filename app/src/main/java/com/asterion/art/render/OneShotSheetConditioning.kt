package com.asterion.art.render

import com.asterion.art.engine.TokenCapacity

/**
 * Character Sheet V5: the final 9:16 master is rendered as ONE complete image.
 * Optional neural planning can enrich, but cannot displace its canonical layout
 * or force sequential diffusion passes with cumulative identity drift.
 */
object OneShotSheetConditioning {
    fun select(
        canonical:String,neural:String?,negative:String,
        capacity:(String)->TokenCapacity
    ):String {
        require(canonical.isNotBlank()) { "Canonical sheet instructions are empty" }
        val primary=capacity(canonical)
        require(primary.limit>0 && primary.fits) {
            "One-image master-sheet specification exceeds the active native " +
                "tokenizer. Select a compatible longer-context image model; " +
                "the application will not silently truncate the sheet or stitch crops."
        }
        if(negative.isNotBlank()) {
            val rejection=capacity(negative)
            require(rejection.limit>0 && rejection.fits) {
                "Master-sheet negative constraints exceed native context"
            }
        }
        val optional=neural?.trim().orEmpty()
        if(optional.isEmpty())return canonical
        val expanded=canonical+"\n\nNEURAL CONSTRUCTION NOTES:\n"+optional
        val proof=capacity(expanded)
        return if(proof.limit>0 && proof.fits)expanded else canonical
    }
}
