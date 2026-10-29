package com.asterion.art

import com.asterion.art.engine.NativePromptIntegrity
import com.asterion.art.engine.TokenCapacity
import org.junit.Assert.*
import org.junit.Test

class NativePromptIntegrityTest {
    private val native: (String) -> TokenCapacity = { value ->
        TokenCapacity(value.length, 128, if(value.length>128)128 else -1)
    }

    @Test fun aCompleteFrozenPromptIsPassedUnchanged() {
        val original="A complete anatomical identity with exact three-view continuity."
        val result=NativePromptIntegrity.requireComplete(original,"no garments",native)
        assertEquals(original.length,result.count)
        assertTrue(result.fits)
    }

    @Test fun overflowingCharacterPromptCannotTriggerProgressiveRepainting() {
        val full="A unique mandatory anatomical structure. ".repeat(14)
        val error=assertThrows(IllegalArgumentException::class.java) {
            NativePromptIntegrity.requireComplete(full,"",native)
        }
        assertTrue(error.message!!.contains("blocked"))
    }

    @Test fun missingTokenizerCapacityMustNotPretendToFit() {
        assertThrows(IllegalArgumentException::class.java) {
            NativePromptIntegrity.requireComplete("Original identity","",
                {TokenCapacity(20,0,-1)})
        }
    }

    @Test fun negativeConstraintsCannotBeTruncated() {
        assertThrows(IllegalArgumentException::class.java) {
            NativePromptIntegrity.requireComplete("Full identity","bad limbs ".repeat(40),native)
        }
    }
}
