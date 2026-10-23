package com.asterion.compiler.art

import org.junit.Assert.*
import org.junit.Test

class AsterionArtContractTest {
    private fun frame(id: String) = StoryboardFrame(id, "content://frames/$id", "turn and move", 2)
    @Test fun `multiple keyframes require true endpoint conditioning unless explicitly relaxed`() {
        val caps = VideoCapabilities(true, false, 5)
        assertTrue(VideoPlanner.plan(listOf(frame("a"), frame("b")), caps) is VideoPlanResult.Blocked)
        val approximate = VideoPlanner.plan(listOf(frame("a"), frame("b")), caps, true)
        assertTrue(approximate is VideoPlanResult.Ready)
        approximate as VideoPlanResult.Ready
        assertEquals(1, approximate.shots.size)
        assertEquals(VideoConditioning.FIRST_FRAME_ONLY, approximate.shots.first().conditioning)
        assertTrue(approximate.warning!!.contains("NOT"))
    }
    @Test fun `true first and last frame inference is represented honestly`() {
        val caps = VideoCapabilities(true, true, 5)
        val result = VideoPlanner.plan(listOf(frame("a"), frame("b"), frame("c")), caps)
        assertTrue(result is VideoPlanResult.Ready)
        result as VideoPlanResult.Ready
        assertEquals(2, result.shots.size)
        assertEquals(VideoConditioning.FIRST_AND_LAST, result.shots[0].conditioning)
        assertNull(result.warning)
    }
    @Test fun `oversized segment is blocked before generation`() {
        assertTrue(VideoPlanner.plan(listOf(frame("a").copy(segmentSeconds = 5)),
            VideoCapabilities(true, false, 2)) is VideoPlanResult.Blocked)
    }
    @Test fun `technical-only evidence does not pretend to be reliable total quality`() {
        val g = Grade("image", listOf(Evidence(Criterion.TECHNICAL, 1.0, 0.9, "measured")))
        val actual = CandidateGrader.rank(listOf(g), RankingWeights.DEFAULT).single()
        assertNull(actual.suggestedScore)
        assertTrue(actual.coverage < 0.25)
    }
    @Test fun `a hard failure disqualifies a superficially strong image`() {
        val good = Grade("valid", listOf(Evidence(Criterion.IDENTITY, 0.65, 0.9, "checked")))
        val bad = Grade("invalid", listOf(Evidence(Criterion.IDENTITY, 0.99, 0.9, "checked")),
            hardFailures = setOf("wrong character"))
        val results = CandidateGrader.rank(listOf(bad, good), RankingWeights.DEFAULT)
        assertEquals("valid", results.first().grade.imageId)
        assertFalse(results.last().eligible)
    }
    @Test fun `human preferences adjust relative weights without changing a checkpoint`() {
        val learner = PreferenceLearner()
        val good = Grade("good", listOf(Evidence(Criterion.IDENTITY, 0.95, 0.9, "yes"),
            Evidence(Criterion.STYLE, 0.3, 0.9, "no")))
        val bad = Grade("bad", listOf(Evidence(Criterion.IDENTITY, 0.2, 0.9, "no"),
            Evidence(Criterion.STYLE, 0.9, 0.9, "yes")))
        val previous = learner.snapshot().normalized()
        val next = learner.choose(good, bad).normalized()
        assertTrue(next.getValue(Criterion.IDENTITY) > previous.getValue(Criterion.IDENTITY))
    }
    @Test fun `reference validation requires the mandatory three kinds`() {
        val p = ArtProject("demo", ArtMode.COSPLAY, listOf(
            ReferenceSheet("character", SheetKind.CHARACTER, "content://c", "Character")
        ))
        assertEquals(2, p.problems().size)
    }
}
