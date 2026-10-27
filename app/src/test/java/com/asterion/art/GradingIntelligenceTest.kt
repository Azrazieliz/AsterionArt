package com.asterion.art

import com.asterion.art.learning.*
import com.asterion.art.model.CreationMode
import com.asterion.art.model.RenderMode
import org.junit.Assert.*
import org.junit.Test

class GradingIntelligenceTest {
    private fun grade(
        id:String,dim:GradeDimension,score:Int, pixels:List<Double>,
        mode:CreationMode=CreationMode.CHARACTER,
        concepts:List<String> = listOf("Oni","Crystal"),
        feature:String="",correct:String="",preserve:String="",
        target:String?=null
    )=DetailedGrade(id=id,assetUri="content://test/images/"+id,
        targetDesignId=target,jobId="job-abc0001",
        creationMode=mode,renderMode=RenderMode.ANIME,
        concepts=concepts,ratings=mapOf(dim to score),
        feature=feature,correct=correct,preserve=preserve,
        visualFeatures=pixels)

    @Test fun pixelEvidenceRepresentsTheImageNotThePrompt() {
        val red=IntArray(64*64){0xffff0000.toInt()}
        val blue=IntArray(64*64){0xff0000ff.toInt()}
        val a=VisualGradeFeatures.extract(red,64,64)
        val b=VisualGradeFeatures.extract(blue,64,64)
        assertEquals(64,a.size)
        assertEquals(64,b.size)
        assertNotEquals(a,b)
        assertEquals(a,VisualGradeFeatures.extract(red,64,64))
        assertTrue(a.all{it in 0.0..1.0})
    }

    @Test fun onlyExplicitScoresTrainModeAndDimensionSeparatedPixelHeads() {
        val red=VisualGradeFeatures.extract(
            IntArray(16*16){0xffee1111.toInt()},16,16)
        val blue=VisualGradeFeatures.extract(
            IntArray(16*16){0xff1111ee.toInt()},16,16)
        val records=(1..12).flatMap{
            listOf(grade("red-"+it,GradeDimension.AESTHETICS,5,red),
                grade("blue-"+it,GradeDimension.AESTHETICS,1,blue))
        }+grade("other-mode",GradeDimension.AESTHETICS,1,red,
            CreationMode.WEAPON)
        val trained=GradingVisualTraining.train(records)
        val character=trained.single{it.mode=="CHARACTER"&&
            it.dimension=="AESTHETICS"}
        val weapon=trained.single{it.mode=="WEAPON"}
        assertEquals(24,character.samples)
        assertEquals(1,weapon.samples)
        assertTrue(character.predict(red)>character.predict(blue))
        assertTrue(GradingVisualTraining.train(records).first{
            it.mode=="CHARACTER"}.weights==character.weights)
    }

    @Test fun editingOrDeletingGradesRecomputesWithoutStaleSignals() {
        val red=List(VisualGradeFeatures.SIZE){0.75}
        val original=grade("same",GradeDimension.ANATOMY,5,red)
        val revised=original.copy(ratings=mapOf(GradeDimension.ANATOMY to 1))
        val high=GradingVisualTraining.train(listOf(original)).single()
        val low=GradingVisualTraining.train(listOf(revised)).single()
        assertTrue(high.predict(red)>low.predict(red))
        assertTrue(GradingVisualTraining.train(emptyList()).isEmpty())
    }

    @Test fun userCorrectionsAreScopedToModeConceptAndExactDesign() {
        val pixels=List(64){0.2}
        val general=grade("gen",GradeDimension.ANATOMY,2,pixels,
            correct="Fix wing junction",preserve="Keep horns")
        val targeted=grade("target",GradeDimension.ANATOMY,1,pixels,
            feature="left wrist",correct="Fix hand",
            target="D002")
        val other=grade("weapon",GradeDimension.AESTHETICS,1,pixels,
            mode=CreationMode.WEAPON,correct="Fix blade")
        val records=listOf(general,targeted,other)
        val c=GradingGuidance.applicable(records,CreationMode.CHARACTER,
            listOf("Crystal","Oni"))
        assertEquals(1,c.size)
        assertEquals("gen",c.single().id)
        val precise=GradingGuidance.applicable(records,CreationMode.CHARACTER,
            listOf("Oni"),designId="D002")
        assertEquals(2,precise.size)
        assertTrue(GradingGuidance.applicable(records,
            CreationMode.CREATURE,listOf("Oni")).isEmpty())
        assertTrue(GradingGuidance.applicable(records,
            CreationMode.CHARACTER,listOf("Cyberpunk")).isEmpty())
        val guidance=GradingGuidance.evidence(c)
        assertTrue(guidance.contains("Fix wing junction"))
        assertTrue(guidance.contains("Keep horns"))
        assertFalse(guidance.contains("Fix blade"))
    }

    @Test fun numericGradeAloneDoesNotGenerateCreativeInstructions(){
        val sample=grade("alone",GradeDimension.AESTHETICS,1,
            List(64){0.5})
        assertTrue(GradingGuidance.applicable(listOf(sample),
            CreationMode.CHARACTER,listOf("Oni")).isEmpty())
    }
}
