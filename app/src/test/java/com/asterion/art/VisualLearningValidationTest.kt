package com.asterion.art

import com.asterion.art.learning.*
import com.asterion.art.model.CreationMode
import org.junit.Assert.*
import org.junit.Test

class VisualLearningValidationTest {
    private fun grades(grouped:Boolean=false,constant:Boolean=false):List<DetailedGrade> =
        (0 until 48).map{i->
            val high=constant||i%2==0
            DetailedGrade(id="grade-validation-$i",
                assetUri=if(grouped)"content://same" else "content://image/$i",
                creationMode=CreationMode.CHARACTER,
                ratings=mapOf(GradeDimension.AESTHETICS to if(high)5 else 1),
                visualFeatures=List(VisualGradeFeatures.SIZE){j->
                    if(j%4==0)if(high)0.95 else 0.05 else 0.50
                },updatedAt=i*1000L)
        }
    @Test fun outOfSampleEvaluationReportsNonzeroEvidence() {
        val metric=VisualLearningValidation.evaluate(grades()).single()
        assertEquals("CHARACTER",metric.mode)
        assertEquals("AESTHETICS",metric.dimension)
        assertTrue(metric.heldOutImages>=4)
        assertNotNull(metric.baselineMae)
        assertNotNull(metric.modelMae)
        assertTrue("Correlated training images should improve held-out MAE",
            metric.validated)
        assertTrue((metric.improvement?:0.0)>0.0)
    }
    @Test fun repeatedGradesForOneImageNeverValidatePrediction() {
        val metric=VisualLearningValidation.evaluate(grades(grouped=true)).single()
        assertFalse(metric.validated)
        assertEquals(1,metric.trainingImages)
        assertEquals(0,metric.heldOutImages)
    }
    @Test fun constantScoresCannotClaimLearningOverPerfectBaseline() {
        val metric=VisualLearningValidation.evaluate(grades(constant=true)).single()
        assertFalse(metric.validated)
    }
    @Test fun ungradedOrNonfinitePixelEvidenceCannotTrain() {
        val records=grades().take(5)+
            DetailedGrade(assetUri="content://bad",ratings=mapOf(
                GradeDimension.AESTHETICS to 1),
                visualFeatures=List(64){Double.NaN})
        assertFalse(VisualLearningValidation.evaluate(records).single().validated)
    }
}
