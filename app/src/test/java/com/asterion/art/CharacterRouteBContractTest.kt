package com.asterion.art

import android.content.Context
import com.asterion.art.methodology.CharacterPhaseOne
import com.asterion.art.methodology.CharacterPlanStore
import com.asterion.art.methodology.CharacterSelection
import com.asterion.art.methodology.FrozenCharacter
import com.asterion.art.methodology.PairwisePhysicalFusion
import com.asterion.art.methodology.PrimaryConcept
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import java.nio.file.Files

/** Character Route B compiles exactly one output image with five independent edits. */
class CharacterRouteBContractTest {
    private val concepts=listOf(
        PrimaryConcept("SC001","Crystal","SC001","owner"),
        PrimaryConcept("SC002","Forest","SC002","owner")
    )
    private fun character(number:Int):FrozenCharacter {
        val base="Crystal and Forest physically interlock within character "+number+
            " as continuous rooted structures carrying form, silhouette, material and joints. "
        return FrozenCharacter(
            number,concepts,base+"body anatomy",base+"face eyes hair",
            base+"rooted topology",base+"physical fusion",base+"materials",
            base+"differentiation",base.repeat(9),base+"audit evidence",
            listOf(PairwisePhysicalFusion("Crystal","Forest",base))
        ).also{it.validate()}
    }

    @Test fun fiveIndividualEditsRetainFullSheetAndOriginalHistory() {
        val root=Files.createTempDirectory("asterion-route-b").toFile()
        try {
            val ctx=Mockito.mock(Context::class.java)
            Mockito.`when`(ctx.applicationContext).thenReturn(ctx)
            Mockito.`when`(ctx.filesDir).thenReturn(root)
            val store=CharacterPlanStore(ctx)
            val newPlan=store.create(listOf("Crystal","Forest"),1,23L)
            store.begin(newPlan.id)
            val originals=(1..5).map(::character)
            val selections=originals.map{CharacterSelection(it.number,concepts)}
            val ready=store.certify(newPlan.id,"owner-model",selections,originals)
            assertEquals(CharacterPhaseOne.PER_SHEET,ready.frozen.size)
            assertEquals(1,ready.prompts.size)
            val updated=originals.first().copy(
                anatomy=originals.first().anatomy+
                    " Original author-approved sculptural skeletal revision."
            )
            val revised=store.reviseReadyCharacter(ready.id,updated)
            revised.assertComplete()
            assertEquals(5,revised.frozen.size)
            assertEquals(1,revised.prompts.size)
            assertEquals(updated.anatomy,revised.frozen.first().anatomy)
            assertEquals(originals.drop(1),revised.frozen.drop(1))
            assertNotEquals(ready.prompts.single(),revised.prompts.single())
            val backup=root.resolve(
                "character-phase1/plans/history/"+ready.id+"-original.json")
            assertTrue("Revision must preserve the baseline",backup.isFile)
        } finally {root.deleteRecursively()}
    }

    @Test fun noEditingAlreadyAuthorizedRenderPlan() {
        val root=Files.createTempDirectory("asterion-route-b-locked").toFile()
        try {
            val ctx=Mockito.mock(Context::class.java)
            Mockito.`when`(ctx.applicationContext).thenReturn(ctx)
            Mockito.`when`(ctx.filesDir).thenReturn(root)
            val store=CharacterPlanStore(ctx)
            val p=store.create(listOf("Crystal","Forest"),1,24L)
            store.begin(p.id)
            val all=(1..5).map(::character)
            store.certify(p.id,"owner-model",
                all.map{CharacterSelection(it.number,concepts)},all)
            store.authorize(p.id,"job-123456789")
            try {
                store.reviseReadyCharacter(p.id,all[0].copy(
                    anatomy=all[0].anatomy+" attempted alteration"))
                fail("An active render could be silently modified")
            } catch(expected:IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("authorized"))
            }
        } finally {root.deleteRecursively()}
    }
}
