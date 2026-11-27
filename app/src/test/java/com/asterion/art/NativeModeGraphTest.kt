package com.asterion.art

import com.asterion.art.engine.GenerationPass
import com.asterion.art.methodology.NativeModeGraph
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import org.junit.Assert.*
import org.junit.Test

class NativeModeGraphTest {
    private fun artworkPasses()=listOf(1f,0.48f,0.32f,0.22f,0.13f)
        .mapIndexed{ index,strength ->
            GenerationPass(index+1,"Frozen owner Artwork visual authority","",strength)
        }

    @Test fun artworkAlwaysRequiresFiveRealStagesEvenWithApprovedDesignText() {
        val graph=NativeModeGraph.compile(null,RenderMode.ANIME)
        assertTrue(graph.isArtwork)
        assertEquals(5,graph.imageStages.size)
        graph.requirePasses(artworkPasses())
        assertThrows(IllegalArgumentException::class.java){
            graph.requirePasses(artworkPasses().take(1))
        }
    }

    @Test fun originalsNeverInheritArtworkStages() {
        for(mode in CreationMode.entries) {
            val graph=NativeModeGraph.compile(mode,RenderMode.ANIME)
            assertFalse(graph.isArtwork)
            assertEquals(1,graph.imageStages.size)
            graph.requirePasses(listOf(GenerationPass(1,"Frozen native design","",1f)))
            assertThrows(IllegalArgumentException::class.java){
                graph.requirePasses(artworkPasses())
            }
            assertEquals(
                com.asterion.art.methodology.DetailedMethodologies
                    .all.getValue(mode).catalogue.designsPerImage,
                graph.imageDesignCount
            )
        }
    }

    @Test fun animationDoesNotUseStillImageGraph() {
        assertThrows(IllegalArgumentException::class.java){
            NativeModeGraph.compile(null,RenderMode.ANIMATION)
        }
    }
}
