package com.asterion.art

import com.asterion.art.model.SheetRole
import com.asterion.art.model.SheetRecord
import com.asterion.art.model.SheetReferenceAuthority
import org.junit.Assert.*
import org.junit.Test

class SheetReferenceAuthorityTest {
    @Test fun completedSheetSupersedesItsConstructionPhotograph() {
        val sheet=SheetRecord(role=SheetRole.CHARACTER,label="Character",
            sourceUris=listOf("content://original/photo"),
            referenceSlots=listOf("PRIMARY"),
            outputUri="content://completed/master-sheet")
        assertEquals(listOf("content://completed/master-sheet"),
            SheetReferenceAuthority.primaryUris(listOf(sheet)))
        assertEquals("PRIMARY",SheetReferenceAuthority.primary(sheet).single().panel)
    }
    @Test fun importedRawMultiPanelSheetRetainsAllSelectedPanels() {
        val sheet=SheetRecord(role=SheetRole.THEME,label="Imported sheet",
            sourceUris=listOf("content://one","content://two"),
            referenceSlots=listOf("PRIMARY","ENVIRONMENT"))
        assertEquals(listOf("content://one","content://two"),
            SheetReferenceAuthority.primaryUris(listOf(sheet)))
        assertEquals(listOf("PRIMARY","ENVIRONMENT"),
            SheetReferenceAuthority.primary(sheet).map{it.panel})
    }
    @Test fun optionalElementSheetIsIncludedWithoutReplacingCoreRoles() {
        val sheets=listOf(
            SheetRecord(role=SheetRole.CHARACTER,label="C",
                sourceUris=listOf("content://c0"),outputUri="content://c1"),
            SheetRecord(role=SheetRole.ART_STYLE,label="S",
                sourceUris=listOf("content://s0"),outputUri="content://s1"),
            SheetRecord(role=SheetRole.THEME,label="T",
                sourceUris=listOf("content://t0"),outputUri="content://t1"),
            SheetRecord(role=SheetRole.ELEMENT,label="E",
                sourceUris=listOf("content://e0"),outputUri="content://e1"))
        assertEquals(listOf("content://c1","content://s1","content://t1",
            "content://e1"),SheetReferenceAuthority.primaryUris(sheets))
    }
}
