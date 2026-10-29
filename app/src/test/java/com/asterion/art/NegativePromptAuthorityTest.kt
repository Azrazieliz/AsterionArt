package com.asterion.art

import com.asterion.art.engine.NegativePromptAuthority
import org.junit.Assert.*
import org.junit.Test

class NegativePromptAuthorityTest {
    @Test fun allSourcesRetainExactOwnerInstructionsInOrder() {
        val joined=NegativePromptAuthority.merge(
            "incorrect source face, displaced composition",
            "wrong material and lighting",
            "owner-specific no extra accessories",
            "ignored source anatomy"
        )
        assertEquals(
            "incorrect source face, displaced composition, wrong material and lighting, owner-specific no extra accessories, ignored source anatomy",
            joined
        )
    }

    @Test fun modelGeneratedExclusionsCannotReplaceOwnerNegative() {
        val owner="owner exact negatives across all outputs"
        val model="additional model-detected visual defects"
        val joined=NegativePromptAuthority.merge(owner,model)
        assertTrue(joined.startsWith(owner))
        assertTrue(joined.contains(model))
    }

    @Test fun emptySourcesDoNotEraseOtherNegatives() {
        assertEquals("required exclusion",NegativePromptAuthority.merge(
            null,"", " required exclusion "," "))
    }
    @Test fun duplicateFullAuthoritiesAreNotRepeated() {
        assertEquals("one, two",NegativePromptAuthority.merge(
            "one","two","one"))
    }
}
