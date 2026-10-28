package com.asterion.art

import com.asterion.art.learning.SemanticGradeProtocol
import org.junit.Assert.*
import org.junit.Test

class SemanticGradingProtocolTest {
    private fun report(score:String="4"):String {
        val rows=SemanticGradeProtocol.axes.joinToString(","){key->
            """{"key":"$key","score":$score,"observation":"A specifically visible material or geometric feature.","correction":"Correct that exact region."}"""
        }
        return """{"axes":[$rows]}"""
    }
    @Test fun parsesAllNineCategories() {
        val grades=SemanticGradeProtocol.parse(report())
        assertEquals(SemanticGradeProtocol.axes,grades.map{it.key})
        assertTrue(grades.all{it.score==4})
    }
    @Test fun doesNotInventMissingScores() {
        assertTrue(SemanticGradeProtocol.parse(report("null")).all{it.score==null})
    }
    @Test fun rejectsIncompleteOrOutOfRangeJudgments() {
        assertTrue(runCatching{SemanticGradeProtocol.parse("""{"axes":[]}""")}.isFailure)
        assertTrue(runCatching{SemanticGradeProtocol.parse(report("9"))}.isFailure)
    }
}
