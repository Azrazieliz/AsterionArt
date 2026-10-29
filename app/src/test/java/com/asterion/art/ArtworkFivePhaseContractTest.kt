package com.asterion.art

import com.asterion.art.engine.ArtworkFivePhaseContract
import com.asterion.art.engine.GenerationPass
import org.junit.Assert.*
import org.junit.Test

class ArtworkFivePhaseContractTest {
    @Test fun recoveredOwnerStageOrderIsImmutable() {
        assertEquals(listOf(
            "FOUNDATION","IDENTITY_GEOMETRY","MATERIAL_LIGHT",
            "FINE_CONSTRUCTION","FINAL_COHERENCE"
        ),ArtworkFivePhaseContract.order.map{it.name})
    }
    @Test fun allFiveNativeStagesRetainCompletePromptAndConstrainedRefinement() {
        val base="Full binding visual specification: identity geometry theme "+
            "elements camera materials rendering and canonical source relationships"
        val passes=listOf(1f,.48f,.32f,.22f,.13f).mapIndexed{index,strength->
            GenerationPass(index+1,base+" consistent refinement", "",strength)
        }
        ArtworkFivePhaseContract.validate(passes)
        assertTrue(passes.all{it.positive.contains(base)})
    }
    @Test fun missingOrRepaintedOwnerPhaseIsRejected() {
        val passes=(1..5).map{
            GenerationPass(it,"Complete frozen visual specification","",
                if(it==1)1f else .2f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArtworkFivePhaseContract.validate(passes.take(4))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArtworkFivePhaseContract.validate(
                passes.map{if(it.number==3)it.copy(denoise=1f)else it})
        }
    }
    @Test fun noSixthGenericPhaseCanBeInvented() {
        assertThrows(IllegalArgumentException::class.java) {
            ArtworkFivePhaseContract.at(6)
        }
    }
}
