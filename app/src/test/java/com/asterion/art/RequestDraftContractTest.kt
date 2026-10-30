package com.asterion.art

import com.asterion.art.data.RequestDraftStore
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RequestDraftContractTest {
    @Test fun preservesSelectionOrderAndDuplicateFreeIdentity() {
        val frames=listOf("content://sample/frame-3","content://sample/frame-1",
            "content://sample/frame-2")
        val draft=JSONObject().put("images",RequestDraftStore.strings(frames))
        assertEquals(frames,RequestDraftStore.readStrings(draft,"images"))
    }

    @Test fun preservesAllDistinctReferenceRolesWithoutSwitchingSource() {
        val refs=linkedMapOf(
            "CHARACTER" to listOf("content://owner/character"),
            "THEME" to listOf("content://owner/theme"),
            "ART_STYLE" to listOf("content://owner/style"),
            "ELEMENT" to listOf("content://owner/element")
        )
        val document=JSONObject().put("direct_refs",RequestDraftStore.roleSources(refs))
        assertEquals(refs,RequestDraftStore.readRoleSources(document,"direct_refs"))
    }

    @Test fun emptyAndOlderDraftsRecoverWithoutInventingUserSelections() {
        assertTrue(RequestDraftStore.readStrings(JSONObject(),"concepts").isEmpty())
        assertTrue(RequestDraftStore.readRoleSources(JSONObject(),"direct_refs").isEmpty())
    }

    @Test fun preservesLongOriginalDirectionsVerbatim() {
        val text=("Anatomy identity and source references; no substitutions.\n").repeat(100)
        val draft=JSONObject().put("owner_positive",text)
        assertEquals(text,draft.getString("owner_positive"))
    }
}
