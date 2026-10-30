package com.asterion.art

import com.asterion.art.methodology.ModeTextDesign
import com.asterion.art.methodology.ModeTextSchemas
import com.asterion.art.methodology.ThemeText
import com.asterion.art.methodology.ThemeTextPlan
import com.asterion.art.model.CreationMode
import com.asterion.art.model.SheetRole
import org.junit.Assert.*
import org.junit.Test

class ModeSpecificTextContractTest {
    @Test fun eachOriginalHasItsOwnCompleteOneImageDesignPopulation() {
        val expected=mapOf(
            CreationMode.OUTFIT to 10,
            CreationMode.WEAPON to 10,
            CreationMode.TRANSFORMATION to 5,
            CreationMode.MUTATION to 5,
            CreationMode.CREATURE to 5,
            CreationMode.ALTERNATE_UNIVERSE to 5,
            CreationMode.FEMINISATION to 1,
            CreationMode.STYLE_CORRECTION to 1
        )
        expected.forEach{(mode,count)->
            val spec=ModeTextSchemas.forMode(mode)
            assertEquals(count,spec.designsPerImage)
            assertTrue(spec.fields.size>=5)
            assertTrue(spec.fields.map{it.key}.distinct().size==spec.fields.size)
            assertTrue(spec.directive.isNotBlank())
            val authored=(1..count).map{index->
                ModeTextDesign(index,"independent_"+index,
                    spec.fields.associate{it.key to
                        "Owner-visible fully elaborated "+it.title+" "+index})
            }
            spec.validate(authored)
            if(count>1) {
                try {spec.validate(authored.dropLast(1))
                    fail("Partial catalogue was accepted")
                } catch(expected:IllegalArgumentException) {Unit}
            }
        }
    }

    @Test fun artworkStillRequiresThreeDistinctSheetAuthorities() {
        val artwork=ModeTextSchemas.forMode(null)
        assertEquals(1,artwork.designsPerImage)
        assertEquals(setOf(SheetRole.CHARACTER,SheetRole.THEME,
            SheetRole.ART_STYLE),artwork.requiredRoles)
        assertTrue(artwork.directive.contains("five Artwork"))
    }

    @Test fun themeTextDoesNotProduceAnImageUntilExplicitApproval() {
        val five=(1..5).map{n->
            ThemeText("T"+n,"Independent Theme "+n,
                listOf("Basalt","Cobalt","Radial"),
                "An independently described physical visual system "+n)
        }
        val base=ThemeTextPlan("text-plan-1234567",123,
            35,47,52,listOf("owner-word"),
            mapOf("owner-axis" to "owner-defined intensity"),
            null,null,123,status="READY",themes=five)
        base.requireFive()
        assertNull(base.approved)
        try {
            base.renderAuthority()
            fail("An unapproved text theme entered image rendering")
        } catch(expected:IllegalArgumentException){Unit}
        val ready=base.copy(status="APPROVED",approvedThemeId="T3")
        assertTrue(ready.renderAuthority().contains("Independent Theme 3"))
        assertTrue(ready.renderAuthority().contains("35/47/52"))
        assertTrue(ready.renderAuthority().contains("owner-axis"))
    }
}
