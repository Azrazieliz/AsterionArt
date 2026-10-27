package com.asterion.art

import com.asterion.art.learning.*
import com.asterion.art.model.CreationMode
import org.junit.Assert.*
import org.junit.Test

class DetailedGradingContractTest {
    @Test fun arbitraryUnscoredAxesAreNotImplicitFailures() {
        val grade=DetailedGrade(
            assetUri="content://images/1",
            creationMode=CreationMode.CHARACTER,
            ratings=mapOf(GradeDimension.ANATOMY to 2),
            correct="Wing joint is physically unsupported")
        grade.validate()
        assertNull(grade.overall)
        assertFalse(grade.ratings.containsKey(GradeDimension.AESTHETICS))
    }
    @Test fun correctionsWithoutScoresAreAllowed(){
        DetailedGrade(assetUri="content://images/1",
            correct="Correct the left wrist joint only").validate()
    }
    @Test fun silentScoresOrImageOnlyAreNotAccepted(){
        assertTrue(runCatching {
            DetailedGrade(assetUri="content://images/1").validate()
        }.isFailure)
        assertTrue(runCatching {
            DetailedGrade(assetUri="content://images/1",
                ratings=mapOf(GradeDimension.ANATOMY to 0)).validate()
        }.isFailure)
    }
    @Test fun exactTargetRectCannotExceedOriginalImage(){
        val ok=GradeRegion(.1f,.2f,.5f,.4f)
        assertEquals(.5f,ok.width,0.00001f)
        assertTrue(runCatching{
            GradeRegion(.8f,.7f,.4f,.4f)
        }.isFailure)
    }
    @Test fun imageFeatureMetadataHasMatchingShape(){
        val v=List(64){0.5}
        DetailedGrade(assetUri="content://images/1",
            ratings=mapOf(GradeDimension.TECHNICAL to 3),
            visualFeatures=v).validate()
        assertTrue(runCatching {
            DetailedGrade(assetUri="content://images/1",
                ratings=mapOf(GradeDimension.TECHNICAL to 3),
                visualFeatures=listOf(.5,.5)).validate()
        }.isFailure)
    }
}
