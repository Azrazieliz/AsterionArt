package com.asterion.compiler.art

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class SheetStudioTest {
    // Valid minimal PNG (1x1); metadata is appended before IEND.
    private val png = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YxsvQAAAABJRU5ErkJggg=="
    )
    @Test fun authoredSheetsUseNeutralFourRoleSchemas() {
        SheetKind.entries.forEach { kind ->
            val fields = SheetTemplates.fields(kind)
            assertTrue(fields.size >= 7)
            assertTrue(fields.map { it.name }.distinct().size == fields.size)
        }
    }
    @Test fun referenceBoardsEmbedValidRoundTripFacts() {
        val text = """{"schema":"asterion.sheet.v1","title":"Très détaillé — 日本語","description":"wings and ornaments"}"""
        val packaged = SheetMetadata.embed(png, text)
        assertEquals(text, SheetMetadata.extract(packaged))
        assertNull(SheetMetadata.extract(png))
        val tampered = packaged.copyOf()
        val position = packaged.indexOf('T'.code.toByte())
        if (position >= 0) tampered[position] = 0
        assertNull(SheetMetadata.extract(tampered))
    }
    @Test fun authoredDescriptionRetainsComplexDetail() {
        val sheet = AuthoredSheet(
            "sample", SheetKind.ELEMENT, "Ornamental marking",
            listOf(SheetField("placement","Placement", "Centered exactly between navel and pelvis")),
            listOf("content://reference/1"))
        assertTrue(sheet.problems().isEmpty())
        assertTrue(sheet.promptFacts().contains("between navel and pelvis"))
    }
}
