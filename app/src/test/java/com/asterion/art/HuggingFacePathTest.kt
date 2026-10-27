package com.asterion.art

import com.asterion.art.models.HubPath
import org.junit.Assert.*
import org.junit.Test

class HuggingFacePathTest {
    @Test fun pinnedDownloadUrlsOnlyUseHuggingFaceHost() {
        val revision="a".repeat(40)
        assertEquals("https://huggingface.co/example/Anima-QNN/resolve/"+revision+
            "/checkpoints/model%20weights.zip?download=true",
            HubPath.downloadUrl("example/Anima-QNN",revision,
                "checkpoints/model weights.zip"))
    }
    @Test fun rejectTraversalAndUnpinnedBranches() {
        assertTrue(runCatching{HubPath.repository("../model")}.isFailure)
        assertTrue(runCatching{HubPath.repository("user/repo/../other")}.isFailure)
        assertTrue(runCatching{HubPath.revision("main")}.isFailure)
        assertTrue(runCatching{HubPath.file("../weights.gguf")}.isFailure)
        assertTrue(runCatching{HubPath.file("/secret")}.isFailure)
        assertTrue(runCatching{HubPath.file("some\\folder/model.gguf")}.isFailure)
    }
    @Test fun doesNotTreatARepositoryNameAsAnInferenceCompatibilityGuarantee() {
        assertTrue(HubPath.file("model.safetensors").endsWith(".safetensors"))
        assertTrue(HubPath.file("model.gguf").endsWith(".gguf"))
    }
}
