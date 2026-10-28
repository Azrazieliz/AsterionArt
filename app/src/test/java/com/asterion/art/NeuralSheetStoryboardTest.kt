package com.asterion.art

import com.asterion.art.model.CreationMode
import com.asterion.art.model.SheetRole
import com.asterion.art.render.NeuralSheetStoryboard
import org.junit.Assert.*
import org.junit.Test

class NeuralSheetStoryboardTest {
    @Test fun everyMasterRoleHasNineIndependentGeneratedViews() {
        SheetRole.entries.forEach{role->
            val panels=NeuralSheetStoryboard.panels(role,null)
            assertEquals(role.name,9,panels.size)
            assertEquals(9,panels.map{it.key}.toSet().size)
            assertTrue(panels.all{it.instruction.contains("reference illustration")})
        }
    }
    @Test fun characterRequiresTrueNovelFullBodyProfileAndRear() {
        val text=NeuralSheetStoryboard.panels(SheetRole.CHARACTER,null)
        assertTrue(text.any{it.instruction.contains("90 degree left profile")})
        assertTrue(text.any{it.instruction.contains("strict rear")})
        assertTrue(text.any{it.title.contains("FACE IDENTITY")})
    }
    @Test fun originalConstructionOverridesGenericCharacterPanels() {
        val weapon=NeuralSheetStoryboard.panels(SheetRole.ELEMENT,CreationMode.WEAPON)
        assertTrue(weapon.any{it.title=="CROSS SECTIONS"})
        assertTrue(weapon.none{it.title=="EYES AND HAIR"})
        val outfit=NeuralSheetStoryboard.panels(SheetRole.CHARACTER,CreationMode.OUTFIT)
        assertTrue(outfit.any{it.title=="BACK CONSTRUCTION"})
    }
    @Test fun styleAndThemeDoNotReduceToCharacterTurnaround() {
        val style=NeuralSheetStoryboard.panels(SheetRole.ART_STYLE,null)
        val theme=NeuralSheetStoryboard.panels(SheetRole.THEME,null)
        assertTrue(style.any{it.title=="LINEWORK"})
        assertTrue(theme.any{it.title=="VISUAL HIERARCHY"})
        assertNotEquals(style.map{it.key},theme.map{it.key})
    }
}
