package com.asterion.art

import com.asterion.art.models.HubPath
import com.asterion.art.models.HubPagination
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
    @Test fun followsEveryExpandedTreePageWithoutChangingRepository() {
        val revision="a".repeat(40)
        val current="https://huggingface.co/api/models/example/Anima-QNN/tree/"+
            revision+"?recursive=true&expand=true"
        val next=current+"&cursor=page2"
        val header="<"+next+">; rel=\"next\", <"+current+">; rel=\"prev\""
        assertEquals(next,HubPagination.nextUrl(current,header))
        assertNull(HubPagination.nextUrl(current,null))
        assertNull(HubPagination.nextUrl(current,"<"+current+">; rel=\"prev\""))
    }
    @Test fun rejectsCrossHostOrCrossModelPagination() {
        val revision="a".repeat(40)
        val current="https://huggingface.co/api/models/example/Anima-QNN/tree/"+
            revision+"?recursive=true&expand=true"
        assertTrue(runCatching{
            HubPagination.nextUrl(current,
                "<https://evil.example/api/models/example/Anima-QNN/tree/"+
                revision+"?cursor=2>; rel=\"next\"")
        }.isFailure)
        assertTrue(runCatching{
            HubPagination.nextUrl(current,
                "<https://huggingface.co/api/models/example/Other/tree/"+
                revision+"?cursor=2>; rel=\"next\"")
        }.isFailure)
    }
    @Test fun modelComponentsRemainSelectableWithoutBeingAdvertisedAsRunnable() {
        listOf("model.safetensors","diffusion_pytorch_model.bin",
            "tokenizer.json","config.json","merges.txt","sentencepiece.model",
            "model_index.json","weights.onnx","adapter.pt","model.gguf").forEach{
            assertTrue("Missing Hub dependency: "+it,HubPath.supportsFileSelection(it))
        }
        assertFalse(HubPath.supportsFileSelection("unsafe-script.py"))
        assertFalse(HubPath.supportsFileSelection("README.md"))
    }
    @Test fun repositorySearchAndTreeEndpointsBothRemainAllowed() {
        HubPagination.requireOfficialApiUrl(
            "https://huggingface.co/api/models?search=qwen3&limit=30")
        HubPagination.requireOfficialApiUrl(
            "https://huggingface.co/api/models/user/repository/tree/"+
                "a".repeat(40)+"?expand=true")
        assertTrue(runCatching{
            HubPagination.requireOfficialApiUrl(
                "https://huggingface.co.evil.example/api/models?search=qwen3")
        }.isFailure)
    }
}
