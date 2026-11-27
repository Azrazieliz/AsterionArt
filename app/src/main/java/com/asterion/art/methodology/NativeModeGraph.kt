package com.asterion.art.methodology

import com.asterion.art.engine.ArtworkFivePhaseContract
import com.asterion.art.engine.GenerationPass
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode

/**
 * Executable, per-mode native rendering topology. Methodology documents are
 * development sources, never model-facing instructions or runtime inputs.
 *
 * The source-authority and learned-design stages have separate implementations;
 * this contract guards the exact native rendering phases, not image quality.
 * In particular, Originals MUST NOT inherit the five Artwork-only phases.
 */
data class NativeModeGraph private constructor(
    val id:String,
    val mode:CreationMode?,
    val renderFamily:RenderMode,
    val imageStages:List<String>,
    val protectedSystems:Set<String>,
    val imageDesignCount:Int
) {
    val isArtwork:Boolean get()=mode==null

    fun requirePasses(passes:List<GenerationPass>) {
        if(isArtwork) {
            ArtworkFivePhaseContract.validate(passes)
            require(imageStages==ArtworkFivePhaseContract.order.map { it.name })
        } else {
            // The Original may generate a catalogue with multiple designs
            // INSIDE one native image; it is not an Artwork refinement loop.
            require(passes.size==1 && passes.single().number==1) {
                "$id cannot execute Artwork phases or duplicate its catalogue"
            }
            require(passes.single().positive.isNotBlank()) {
                "$id has no frozen native render specification"
            }
        }
    }

    companion object {
        fun compile(mode:CreationMode?, family:RenderMode):NativeModeGraph {
            require(family!=RenderMode.ANIMATION) {
                "Video uses its own native timeline graph, not the still renderer"
            }
            if(mode==null) return NativeModeGraph(
                id="ARTWORK_V1",
                mode=null,renderFamily=family,
                imageStages=ArtworkFivePhaseContract.order.map { it.name },
                protectedSystems=setOf("Character identity","Artwork composition",
                    "Selected reference roles","Established major geometry"),
                imageDesignCount=1
            )
            val source=DetailedMethodologies.all.getValue(mode)
            val stages=listOf("FROZEN_ORIGINAL_CATALOGUE_RENDER")
            return NativeModeGraph(
                id="ORIGINAL_"+mode.name+"_V1",mode=mode,renderFamily=family,
                imageStages=stages,
                protectedSystems=source.protectedSystems,
                imageDesignCount=source.catalogue.designsPerImage
            )
        }
    }
}
