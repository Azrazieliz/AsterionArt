package com.asterion.art

import com.asterion.art.engine.DesignPreviewCrops
import org.junit.Assert.*
import org.junit.Test

class DesignPreviewCropsTest {
    @Test fun fiveCharacterColumnsHaveNoOverlaps() {
        val rects=(0 until 5).map { DesignPreviewCrops.rectangle(1024,576,5,1,it) }
        assertEquals(0,rects.first().x)
        assertEquals(1024,rects.last().x+rects.last().width)
        assertTrue(rects.zipWithNext().all { (a,b)->a.x+a.width==b.x })
        assertTrue(rects.all { it.y==0 && it.height==576 })
    }
    @Test fun twoByFiveCataloguesMapDistinctRows() {
        val first=DesignPreviewCrops.rectangle(1400,800,5,2,0)
        val last=DesignPreviewCrops.rectangle(1400,800,5,2,9)
        assertEquals(0,first.y)
        assertEquals(400,last.y)
        assertEquals(1120,last.x)
        assertEquals(280,last.width)
    }
    @Test fun invalidGridCannotBeCropped() {
        assertTrue(runCatching {
            DesignPreviewCrops.rectangle(10,10,5,2,10)
        }.isFailure)
    }
}
