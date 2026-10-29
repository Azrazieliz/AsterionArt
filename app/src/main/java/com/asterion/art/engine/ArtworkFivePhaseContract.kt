package com.asterion.art.engine

/**
 * Binding Artwork-only stage order recovered from the owner's approved
 * 2026-10-23 architecture/workflow_registry.json, corroborated by the original
 * Prompt Writing methodology (Pass 1 establishes all major structures).
 *
 * Do not reuse these phases for Originals or sheet creation. This class
 * defines planning/trace semantics, NOT proof of successful native execution.
 */
object ArtworkFivePhaseContract {
    enum class Phase {
        FOUNDATION,
        IDENTITY_GEOMETRY,
        MATERIAL_LIGHT,
        FINE_CONSTRUCTION,
        FINAL_COHERENCE
    }

    val order:List<Phase> = Phase.entries.toList()

    fun at(number:Int):Phase {
        require(number in 1..5){"Invalid owner Artwork phase: "+number}
        return order[number-1]
    }

    fun validate(passes:List<GenerationPass>) {
        require(passes.size==order.size) {
            "Artwork requires exactly five owner-defined quality stages"
        }
        passes.forEachIndexed {index,pass->
            require(pass.number==index+1 && pass.positive.isNotBlank()) {
                "Artwork phase "+(index+1)+" is missing or out of order"
            }
            require(pass.denoise in 0f..1f) {
                "Artwork phase "+(index+1)+" has invalid strength"
            }
        }
        // Pass 1 alone establishes structure. All others preserve its
        // identity/composition: they must use constrained continuation.
        require(passes.drop(1).all{it.denoise<1f}) {
            "Artwork refinements must not redraw the complete foundation"
        }
    }
}
