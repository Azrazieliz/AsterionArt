package com.asterion.art.engine

/**
 * A single image diffusion invocation must see the complete authoritative text.
 * Slicing text into sequential img2img denoising runs does NOT increase the
 * model's attention context and can repaint away earlier frozen identity facts.
 */
object NativePromptIntegrity {
    fun requireComplete(
        positive: String,
        negative: String,
        inspect: (String) -> TokenCapacity
    ): TokenCapacity {
        require(positive.isNotBlank()) { "Empty authoritative image specification" }
        val primary = inspect(positive)
        require(primary.limit > 0) {
            "Selected image model did not report a measurable native text capacity"
        }
        require(primary.fits) {
            "Complete image specification needs ${primary.count} tokens, but the " +
                "selected image model accepts ${primary.limit}. Generation has been " +
                "blocked rather than splitting the frozen specification into " +
                "separate repainting passes or silently discarding details."
        }
        if (negative.isNotBlank()) {
            val rejection = inspect(negative)
            require(rejection.limit > 0 && rejection.fits) {
                "Complete negative image constraints exceed the selected native " +
                    "text capacity. No constraints were dropped."
            }
        }
        return primary
    }
}
