package com.asterion.art

import com.asterion.art.engine.*
import org.junit.Assert.*
import org.junit.Test

class ConditioningScheduleTest {
    private val native:(String)->TokenCapacity = { s ->
        // Simulates a verified tokenizer with explicit limit, not word guesswork.
        val n=s.length
        TokenCapacity(n,410,if(n>410)410 else -1)
    }
    @Test fun singlePassStaysUnchanged() {
        val p=ConditioningSchedule.compile("Complete character anatomy.","",native)
        assertEquals(1,p.chunks.size)
        assertEquals("single_pass",p.mode)
        assertEquals(1f,p.chunks.first().denoise)
    }
    @Test fun longPromptPreservesEveryRequiredUnit() {
        val original=(1..12).joinToString(" "){"Segment $it with unique biological structures."}
        val plan=ConditioningSchedule.compile(original,"bad hands",native)
        assertTrue(plan.chunks.size>1)
        assertEquals(original.trim().length,plan.sourceCharacters)
        assertEquals(original.trim().length,plan.emittedCharacters)
        assertTrue(plan.chunks.all{native(it.positive).fits})
        assertEquals(1f,plan.chunks.first().denoise)
    }
    @Test fun impossibleSegmentFailsRatherThanSilentlyTruncates() {
        val impossible="X".repeat(1000)
        assertTrue(runCatching{
            ConditioningSchedule.compile(impossible,"",native,maximumPasses=2)
        }.isFailure)
    }
    @Test fun characterContinuationRetainsFrozenIdentityAnchor() {
        val header="SAME D017 Oni Crystal preserve roots and face.\n"
        val original=("Anatomical structure in dense clinical detail. ").repeat(19)
        val plan=ConditioningSchedule.compile(
            original,"",native,maximumPasses=20,
            continuationHeader=header)
        assertTrue(plan.chunks.size>1)
        assertEquals(original.length,plan.emittedCharacters)
        assertTrue(plan.chunks.drop(1).all{it.positive.startsWith(header)})
        assertTrue(plan.chunks.all{native(it.positive).fits})
    }
    @Test fun overlongFrozenAnchorFailsRatherThanInventingShorterAuthority() {
        assertTrue(runCatching{
            ConditioningSchedule.compile(
                "Character anatomy ".repeat(40),"",native,
                continuationHeader="Frozen character ".repeat(40))
        }.isFailure)
    }
    @Test fun unverifiedTokenizerCannotClaimExtendedContext() {
        val bad:(String)->TokenCapacity={TokenCapacity(100,0,-1)}
        assertTrue(runCatching {
            ConditioningSchedule.compile("test","",bad)
        }.isFailure)
    }
}
