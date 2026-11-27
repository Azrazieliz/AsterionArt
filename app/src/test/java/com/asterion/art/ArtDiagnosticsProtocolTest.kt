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
        assertTrue(ArtDiagnosticsProtocol.validArtifact("character-plan"))
        assertTrue(ArtDiagnosticsProtocol.validArtifact("character-telemetry"))
        assertTrue(ArtDiagnosticsProtocol.validArtifact("character-visual-audit"))
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

    @Test fun characterEvidencePathsStayInsideTheirDedicatedPrivateFolders() {
        val root = createTempDir(prefix = "asterion-art-character-diagnostics")
        try {
            val jobId = "4b2f6a10-2a11-4b3f-a4c9"
            val characterId = "charplan-123456"
            val plan = ArtDiagnosticsProtocol.artifactFile(
                root, jobId, "character-plan", characterId)
            val telemetry = ArtDiagnosticsProtocol.artifactFile(
                root, jobId, "character-telemetry", characterId)
            val audit = ArtDiagnosticsProtocol.artifactFile(
                root, jobId, "character-visual-audit")
            assertTrue(plan.path.startsWith(File(root, "character-phase1/plans").canonicalPath))
            assertTrue(telemetry.path.startsWith(File(root, "character-phase1/telemetry").canonicalPath))
            assertTrue(audit.path.startsWith(File(root, "vision-assessments").canonicalPath))
        } finally {
            root.deleteRecursively()
        }
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

    @Test fun bridgeEventJournalRetainsOnlyTheNewestBoundedRecords() {
        val root = createTempDir(prefix = "asterion-art-bridge-events")
        try {
            repeat(140) { sequence ->
                ArtDiagnosticsProtocol.appendBridgeEvent(
                    root, """{"sequence":$sequence,"outcome":"completed","padding":"${"x".repeat(1900)}"}"""
                )
            }
            val records = ArtDiagnosticsProtocol.readBridgeEvents(root).lines()
            assertEquals(128, records.size)
            assertTrue(records.first().contains("\"sequence\":12"))
            assertTrue(records.last().contains("\"sequence\":139"))
            assertFalse(records.any { it.contains("\"sequence\":0") })
            assertTrue(File(root, "asterion-control-bridge/events.jsonl").length() < 262_144L)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun bridgeEventJournalRejectsMultilineRecords() {
        val root = createTempDir(prefix = "asterion-art-bridge-events-invalid")
        try {
            val outcome = runCatching {
                ArtDiagnosticsProtocol.appendBridgeEvent(root, "{\"event\":1}\n{\"event\":2}")
            }
            assertTrue(outcome.isFailure)
            assertEquals("", ArtDiagnosticsProtocol.readBridgeEvents(root))
        } finally {
            root.deleteRecursively()
        }
    }
}