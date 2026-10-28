package com.asterion.art

import android.content.Context
import com.asterion.art.methodology.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import java.nio.file.Files

/** JVM integration checks for durable progress; does not require Qwen model weights. */
class CharacterPlanStorePersistenceTest {
    private fun withStore(block:(CharacterPlanStore)->Unit) {
        val root=Files.createTempDirectory("asterion-plan-store").toFile()
        try {
            val context=Mockito.mock(Context::class.java)
            Mockito.`when`(context.applicationContext).thenReturn(context)
            Mockito.`when`(context.filesDir).thenReturn(root)
            block(CharacterPlanStore(context))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun currentQwenStageSurvivesCheckpointReloadAndFailure()=withStore {store->
        val p=store.create(listOf("SC001","SC002"),1,123L)
        store.begin(p.id)
        store.stage(p.id,"Qwen3 authoring full anatomical identity D001")
        assertEquals("Qwen3 authoring full anatomical identity D001",
            store.load(p.id).progressStage)
        store.fail(p.id,"Missing face_eyes_hair")
        val failed=store.load(p.id)
        assertEquals("FAILED",failed.status)
        assertEquals("Resolution failed",failed.progressStage)
        assertEquals("Missing face_eyes_hair",failed.failure)
    }

    @Test fun restartingFailedResolutionKeepsItsCompletedFrozenIdentity()=withStore {store->
        val p=store.create(listOf("SC001","SC002"),1,124L)
        store.begin(p.id)
        val concepts=listOf(
            PrimaryConcept("SC001","SC001","SC001","fixed_canonical"),
            PrimaryConcept("SC002","SC002","SC002","fixed_canonical"))
        val evidence="SC001 and SC002 unite as a load bearing joint connecting "+
            "the anatomical mass into a fully developed and physically coherent skeleton. "
        val identity=FrozenCharacter(
            1,concepts,evidence,evidence,evidence,evidence,evidence,evidence,
            evidence.repeat(7),evidence,
            listOf(PairwisePhysicalFusion("SC001","SC002",evidence)))
        identity.validate()
        store.progress(p.id,listOf(CharacterSelection(1,concepts)),listOf(identity))
        store.fail(p.id,"Qwen3 missing a later character")
        val resume=store.retryFailed(p.id)
        assertEquals("PENDING",resume.status)
        assertEquals(1,resume.frozen.size)
        assertTrue(store.load(p.id).progressStage.contains("1/5"))
        assertEquals(identity.anatomy,store.begin(p.id).frozen.single().anatomy)
    }
}
