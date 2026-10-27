package com.asterion.art

import com.asterion.art.model.SheetPresentation
import com.asterion.art.model.SheetRole
import com.asterion.art.model.SheetLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetPresentationTest {
    @Test fun allReferenceRolesUseTheSameCleanCharacterInspiredFrame() {
        val frames=SheetRole.entries.map { role ->
            val available=SheetLayouts.forRole(role).map{it.id}
            SheetPresentation.plan(role,available)
        }
        frames.forEach { plan ->
            assertEquals(1440,plan.pageWidth)
            assertEquals(2560,plan.pageHeight)
            assertEquals("PRIMARY",plan.placements.first().slot)
            assertTrue(plan.placements.first().principal)
            assertEquals(1,plan.placements.count{it.principal})
            assertTrue(plan.placements.first().frame.height>
                plan.placements.drop(1).first().frame.height)
            plan.placements.forEach { panel ->
                assertFalse(panel.frame.overlaps(plan.swatchArea))
            }
        }
        assertTrue(SheetPresentation.WIDTH*16==SheetPresentation.HEIGHT*9)
    }
    @Test fun everyReferenceHasExactlyOneNonOverlappingPanel() {
        SheetRole.entries.forEach { role ->
            val slots=SheetLayouts.forRole(role).map{it.id}
            for(count in 1..SheetPresentation.MAX_REFS) {
                val chosen=List(count){i->slots[i%slots.size]}
                val plan=SheetPresentation.plan(role,chosen)
                assertEquals(count,plan.placements.size)
                assertEquals((0 until count).toSet(),plan.placements.map{it.sourceIndex}.toSet())
                plan.placements.indices.forEach { i ->
                    for(j in i+1 until plan.placements.size)
                        assertFalse(plan.placements[i].frame.overlaps(plan.placements[j].frame))
                }
            }
        }
    }
    @Test fun secondaryReferencesRespectTheirSemanticPriorityAndStableSourceOrder() {
        val slots=listOf("HAIR","PRIMARY","FACE","EYES","HAIR")
        val plan=SheetPresentation.plan(SheetRole.CHARACTER,slots)
        assertEquals(listOf(1,2,3,0,4),plan.placements.map{it.sourceIndex})
    }
    @Test fun aSingleReferenceRemainsProminentWithoutInventedDetailPanels() {
        val plan=SheetPresentation.plan(SheetRole.THEME,listOf("PRIMARY"))
        assertEquals(1,plan.referenceCount)
        assertEquals(1,plan.placements.size)
        assertTrue(plan.placements.first().frame.width>1000)
    }
}
