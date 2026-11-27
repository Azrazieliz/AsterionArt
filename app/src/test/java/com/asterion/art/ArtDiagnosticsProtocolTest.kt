package com.asterion.art

import com.asterion.art.integrations.ArtDiagnosticsProtocol
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ArtDiagnosticsProtocolTest {
    @Test fun acceptsOnlyOpaqueJobIdentifiers() {
        assertTrue(ArtDiagnosticsProtocol.validJobId("4b2f6a10-2a11-4b3f-a4c9"))
        assertFalse(ArtDiagnosticsProtocol.validJobId("../secrets"))
        assertFalse(ArtDiagnosticsProtocol.validJobId("short"))
    }

    @Test fun exposesOnlyNamedDiagnosticArtifacts() {
        assertTrue(ArtDiagnosticsProtocol.validArtifact("job-plan"))
        assertTrue(ArtDiagnosticsProtocol.validArtifact("native-prompt-trace"))
        assertTrue(ArtDiagnosticsProtocol.validArtifact("brain-plan"))
        assertFalse(ArtDiagnosticsProtocol.validArtifact("../../shared_prefs"))
        assertFalse(ArtDiagnosticsProtocol.validArtifact("image-output"))
    }

    @Test fun pagesLongPromptEvidenceWithoutTruncatingTheSource() {
        val source = "0123456789"
        val first = ArtDiagnosticsProtocol.page(source, 0, 4)
        val second = ArtDiagnosticsProtocol.page(source, first.nextOffset!!, 4)
        val last = ArtDiagnosticsProtocol.page(source, second.nextOffset!!, 4)
        assertEquals("0123", first.text)
        assertEquals("4567", second.text)
        assertEquals("89", last.text)
        assertNull(last.nextOffset)
        assertEquals(source.length, last.totalChars)
    }

    @Test fun diagnosticPathsStayInsideTheTwoAppPrivateFolders() {
        val root = createTempDir(prefix = "asterion-art-diagnostics")
        try {
            val file = ArtDiagnosticsProtocol.artifactFile(
                root, "4b2f6a10-2a11-4b3f-a4c9", "native-prompt-trace")
            assertTrue(file.path.startsWith(File(root, "generation-checkpoints").canonicalPath))
            assertEquals("native-prompt-trace.json", file.name)
        } finally {
            root.deleteRecursively()
        }
    }
}