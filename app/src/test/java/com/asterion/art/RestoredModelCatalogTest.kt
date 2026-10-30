package com.asterion.art

import com.asterion.art.brain.BrainCatalog
import com.asterion.art.vision.VisionCatalog
import org.junit.Assert.*
import org.junit.Test

class RestoredModelCatalogTest {
    @Test fun legacyQwenReasonerIdsRemainRecognized() {
        val choices=BrainCatalog.models.associateBy{it.id}
        assertTrue(choices.containsKey("qwen3_06_q5"))
        assertTrue(choices.containsKey("qwen3_17_q4"))
        assertTrue(choices.containsKey("gemma3_4b_q4"))
        assertEquals("Qwen3-1.7B-Q4_K_M.gguf",
            choices.getValue("qwen3_17_q4").filename)
    }

    @Test fun originalQwenVisionPairAndOptionalGemmaRemainDistinct() {
        assertTrue(VisionCatalog.qwenLanguage.url
            .contains("Qwen3-VL-2B-Instruct-GGUF"))
        assertTrue(VisionCatalog.qwenProjector.name.startsWith("mmproj-Qwen3VL"))
        assertTrue(VisionCatalog.language.name.startsWith("google_gemma"))
        assertFalse(VisionCatalog.qwenLanguage.sha256
            .equals(VisionCatalog.language.sha256,true))
        // The legacy model's discovery must not trigger downloading both
        // multi-gigabyte model families in one installation operation.
        assertEquals(2,VisionCatalog.all.size)
    }
}
