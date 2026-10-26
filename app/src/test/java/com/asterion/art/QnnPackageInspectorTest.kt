package com.asterion.art

import com.asterion.art.nativeengine.QnnPackageInspector
import org.junit.Assert.*
import org.junit.Test

class QnnPackageInspectorTest {
    private fun member(path: String, bytes: Long = 2_500_000L) =
        QnnPackageInspector.Member(path, bytes)

    @Test fun nestedMarkerWithArbitraryWrapperAndCaseIsDetected() {
        val report = QnnPackageInspector.inspect(listOf(
            member("my/package/with/many/nested/parent/folders/anima/stable_model.bin"),
            member("my/package/with/many/nested/parent/folders/anima/text_encoder.bin"),
            member("my/package/with/many/nested/parent/folders/anima/vae.bin")))
        assertEquals("anima", QnnPackageInspector.validate(report, null))
        assertEquals("my/package/with/many/nested/parent/folders", report.markerParent)
    }

    @Test fun originalSafetensorsInsideZipRequiresConversion() {
        val report = QnnPackageInspector.inspect(listOf(
            member("checkpoint/SDXL/illustrious_v34.safetensors")))
        val failure = runCatching { QnnPackageInspector.validate(report, null) }.exceptionOrNull()
        assertTrue(failure?.message.orEmpty().contains("Conversion is required"))
    }

    @Test fun opaqueCompiledPackageNeedsExplicitFamily() {
        val report = QnnPackageInspector.inspect(listOf(
            member("nested/weights/model.bin"),
            member("nested/weights/text_encoder.bin"),
            member("nested/weights/vae.bin")))
        assertTrue(runCatching { QnnPackageInspector.validate(report, null) }.isFailure)
        assertEquals("sdxl", QnnPackageInspector.validate(report, "sdxl"))
    }

    @Test fun conflictingModelsDoNotSilentlyPickOne() {
        val report = QnnPackageInspector.inspect(listOf(
            member("family/ANIMA/main.bin"),
            member("family/SDXL/main.bin"),
            member("family/ANIMA/vae.bin")))
        assertTrue(runCatching { QnnPackageInspector.validate(report, null) }.isFailure)
    }

    @Test fun modelNameMentionAloneIsNotProofOfArchitecture() {
        val report = QnnPackageInspector.inspect(listOf(
            member("random/guess_anima_checkpoint.bin"),
            member("random/vae.bin"),
            member("random/clip.bin")))
        assertTrue(report.detectedFamilies.isEmpty())
    }

    @Test fun missingCompiledComponentsGiveSpecificFailure() {
        val report = QnnPackageInspector.inspect(listOf(
            member("ANIMA/weights.safetensors"),
            member("ANIMA/README.txt", 150L)))
        val failure = runCatching { QnnPackageInspector.validate(report, null) }.exceptionOrNull()
        assertTrue(failure?.message.orEmpty().contains("no compiled QNN"))
    }
}
