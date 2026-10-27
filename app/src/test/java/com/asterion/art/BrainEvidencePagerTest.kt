package com.asterion.art

import com.asterion.art.brain.BrainEvidencePager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BrainEvidencePagerTest {
    @Test fun originalAuthorityIsPreservedExactlyAcrossPages()=runBlocking {
        val body=(1..120).joinToString("\n") {
            "METHOD §$it: anatomically coherent material, edge, geometry and structure."
        }
        val p=BrainEvidencePager.pages(body,256){it.length}
        assertTrue(p.size>1)
        assertEquals(body,p.joinToString(""){it.text})
        assertTrue(p.all{it.tokenizerCount<=256})
    }
    @Test fun unicodeSurrogatesAreNotSplit()=runBlocking {
        val body="Morphology ".repeat(40)+"🦋"+" geometry ".repeat(40)
        val p=BrainEvidencePager.pages(body,256){it.length}
        assertEquals(body,p.joinToString(""){it.text})
        assertTrue(p.none{it.text.lastOrNull()?.isHighSurrogate()==true})
    }
}
