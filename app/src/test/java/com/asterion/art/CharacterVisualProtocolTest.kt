package com.asterion.art

import com.asterion.art.methodology.*
import com.asterion.art.vision.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CharacterVisualProtocolTest {
    private fun identity():FrozenCharacter {
        val input=listOf(
            PrimaryConcept("Oni","Oni",null,"fixed_temporary_literal"),
            PrimaryConcept("Crystal","Crystal",null,"fixed_temporary_literal"),
            PrimaryConcept("Cloud","Cloud",null,"fixed_temporary_literal"))
        val anatomy=("Oni, Crystal and Cloud reshape the exact pelvis, radius, "+
            "vertebrae and elbow load path without garments. ").repeat(2)
        val topology=("Each anatomical root has a visible joint, origin "+
            "and full mechanical support in three views. ").repeat(2)
        val fusion=("Oni Crystal Cloud create a consistent body-owned fused "+
            "nervous and skeletal architecture. ").repeat(2)
        val pairs=listOf(
            PairwisePhysicalFusion("Oni","Crystal",
                "Oni and Crystal share a physical cranial spicule hinge rooted "+
                    "in the cervical skeleton and paired facial muscle anchors."),
            PairwisePhysicalFusion("Oni","Cloud",
                "Oni and Cloud share a layered anatomical filtration membrane "+
                    "through the shoulder, rib, and respiratory architecture."),
            PairwisePhysicalFusion("Crystal","Cloud",
                "Crystal and Cloud share a refraction channel running from "+
                    "the torso core to each rooted posterior appendage."))
        return FrozenCharacter(1,input,anatomy,
            "Oni Crystal Cloud eyes and hair develop into a distinctive head "+
                "with different view-consistent planes, brows, and cranial roots.",
            topology,fusion,
            "White background, two-dimensional hand-drawn color hierarchy "+
                "and specific hard-edged material transitions.",anatomy,
            ("Oni Crystal Cloud, distinctive anatomy and materials; "+
                "full-body clinical unclothed female true front, exact side "+
                "and direct rear, all views matching identical skeleton, "+
                "limb roots, face, eye, hair, physical fusion and color; "+
                "premium two dimensional anime/manhwa visual grammar. ").repeat(5),
            "Review root attachments and distinctions between all three "+
                "views; unresolved view changes must be reported separately.",
            pairs,"Oni Crystal Cloud together form a coherent refractive "+
                "skeletal respiratory structure with integrated load paths, "+
                "filtration cavities and visible anatomical continuity.")
    }
    @Test fun visualProtocolUsesRealFrozenPairwiseAndHigherOrderAuthorities() {
        val prompt=CharacterVisualProtocol.prompt(identity())
        assertTrue(prompt.contains("Oni × Crystal"))
        assertTrue(prompt.contains("Crystal × Cloud"))
        assertTrue(prompt.contains("COMPLETE SET PHYSICAL FUSION"))
        assertTrue(prompt.contains("ACTUAL"))
        assertTrue(prompt.contains("not a user grade"))
    }
    @Test fun visualOutputCannotBeMistakenForApprovedGrade() {
        val map=JSONObject().put("Oni","Visible horns with roots")
            .put("Crystal","unclear")
            .put("Cloud","unclear")
        val response=JSONObject()
            .put("visible_features",JSONArray(listOf("three figures")))
            .put("concept_evidence",map)
            .put("anatomy_concerns",JSONArray(listOf("Root appears disconnected")))
            .put("view_consistency_concerns",JSONArray())
            .put("drawing_concerns",JSONArray())
            .put("uncertainty",JSONArray(listOf("Rear view too small")))
        val result=CharacterVisualProtocol.parse(identity(),1,
            "content://art/123","abcdef",response.toString())
        assertEquals("UNVERIFIED_MACHINE_OBSERVATION",result.status)
        assertEquals("unclear",result.conceptEvidence["Cloud"])
        assertTrue(result.correctionCandidates().contains("Root appears disconnected"))
        assertTrue(result.uncertainty.isNotEmpty())
    }
    @Test fun malformedVisionResponsesFailRatherThanInventEvidence() {
        assertTrue(runCatching {
            CharacterVisualProtocol.parse(identity(),1,
                "content://art/123","abcd","Nothing to inspect")
        }.isFailure)
    }
    @Test fun fiveTriptychsExactlyPartitionAContinuousWideCanvas(){
        val cells=CharacterSheetGeometry.cells()
        assertEquals(5,cells.size)
        assertEquals(0,cells.first().left)
        assertEquals(3200,cells.last().right)
        assertTrue(cells.all{it.top==0&&it.bottom==1800})
        assertTrue(cells.zipWithNext().all{(a,b)->a.right==b.left})
        assertTrue(runCatching{
            CharacterSheetGeometry.cells(3000,1800)
        }.isFailure)
    }
}
