package com.asterion.compiler.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoRaKeywordRouterTest {
    private val checkpoint = EmbeddedProfiles.checkpointById("NoobAI-XL-v11")!!
    private val hair = LoRaAsset("hair", "Hair details", "content://hair", "sha256-hair", "SDXL", listOf("silver hair"), 0.8f)
    private val costume = LoRaAsset("costume", "Costume", "content://costume", "sha256-costume", "SDXL", listOf("armored dress"), 1.2f)

    @Test fun `keyword matches whole tags but not substrings`() {
        assertTrue(LoRaKeywordRouter.resolve("silver hair, anime", checkpoint, listOf(hair), emptyList()).triggered.size == 1)
        assertTrue(LoRaKeywordRouter.resolve("silver haired", checkpoint, listOf(hair), emptyList()).triggered.isEmpty())
    }

    @Test fun `no keyword leaves the base model selected`() {
        val result = LoRaKeywordRouter.resolve("sky and clouds", checkpoint, listOf(hair, costume), emptyList())
        assertTrue(result.ready)
        assertEquals(checkpoint.id, result.selectedModelId)
    }

    @Test fun `trigger without exact compiled variant blocks generation`() {
        val result = LoRaKeywordRouter.resolve("silver hair", checkpoint, listOf(hair), emptyList())
        assertFalse(result.ready)
        assertEquals(null, result.selectedModelId)
        assertTrue(result.blockers.single().contains("Conversion is required"))
    }

    @Test fun `exact active combination selects only verified converted model`() {
        val wrong = ConvertedModelVariant("other", checkpoint.id, mapOf("hair" to 0.8f), true)
        val correct = ConvertedModelVariant("hair-costume", checkpoint.id, mapOf("hair" to 0.8f, "costume" to 1.2f), true)
        val result = LoRaKeywordRouter.resolve("silver hair and armored dress", checkpoint,
            listOf(hair, costume), listOf(wrong, correct))
        assertTrue(result.ready)
        assertEquals("hair-costume", result.selectedModelId)
    }

    @Test fun `disabled and foreign family LoRAs cannot silently apply`() {
        val anima = LoRaAsset("anima", "Anima LoRA", "content://anima", "sha256-anima", "ANIMA", listOf("special lighting"))
        val result = LoRaKeywordRouter.resolve("special lighting", checkpoint, listOf(anima), emptyList())
        assertFalse(result.ready)
        assertTrue(result.blockers.single().contains("incompatible"))
        val disabled = LoRaKeywordRouter.resolve("silver hair", checkpoint, listOf(hair.copy(enabled = false)), emptyList())
        assertTrue(disabled.ready)
        assertEquals(checkpoint.id, disabled.selectedModelId)
    }
}
