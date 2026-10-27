package com.asterion.art

import com.asterion.art.learning.*
import com.asterion.art.model.*
import org.junit.Assert.*
import org.junit.Test

class TrainingCorpusTest {
    private fun record(id:String,job:String,status:String,creation:CreationMode):SavedDesign=
        SavedDesign(id,job,RenderMode.ANIME,creation,1,1,
            "Distinct "+id+" architecture", "error exclusion",null,status)
    @Test fun exportsOnlyComparableExplicitFeedback(){
        val all=listOf(
            record("a","same","KEPT",CreationMode.WEAPON),
            record("b","same","REJECTED",CreationMode.WEAPON),
            record("c","different","REJECTED",CreationMode.WEAPON),
            record("d","same","REJECTED",CreationMode.OUTFIT),
            record("e","same","CANDIDATE",CreationMode.WEAPON))
        val pairs=PreferenceCorpus.pairs(all)
        assertEquals(1,pairs.size)
        assertEquals("a",pairs.single().preferredId)
        assertEquals("b",pairs.single().rejectedId)
    }
    @Test fun neverTrainsFromNoFeedback(){
        assertTrue(PreferenceCorpus.pairs(listOf(
            record("a","same","CANDIDATE",CreationMode.CHARACTER))).isEmpty())
    }
}
