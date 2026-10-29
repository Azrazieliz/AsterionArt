package com.asterion.art.engine

/**
 * Minimum visual-structure validation for actual diffusion output, not semantic
 * quality grading. A flat or uniformly blurred color field must never be
 * published as a successfully generated detailed illustration.
 */
object RenderFidelityGate {
    fun requireValidImage(png:ByteArray,subject:String) {
        require(png.size>1024) { "$subject: empty or corrupt native image" }
        val result=TechnicalImageScorer.score(png)
        require(!isDegenerate(result)) {
            "$subject: native model returned an effectively blank/blurred field " +
                "(contrast=${result.contrast}, detail=${result.detail}). " +
                "Review model, decoder, VAE, sampler and native inference logs. " +
                "Image was rejected rather than recorded as finished artwork."
        }
    }

    /** Only catches gross failure; high scores do NOT prove fidelity/anatomy. */
    fun isDegenerate(score:TechnicalScore):Boolean =
        !score.score.isFinite() ||
            (score.contrast < 0.065 && score.detail < 0.028)
}
