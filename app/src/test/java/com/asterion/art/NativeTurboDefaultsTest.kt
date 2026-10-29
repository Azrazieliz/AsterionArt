package com.asterion.art

import com.asterion.art.engine.DreamModel
import com.asterion.art.model.GeneratorSettings
import com.asterion.art.nativeengine.NativeTurboDefaults
import org.junit.Assert.*
import org.junit.Test

class NativeTurboDefaultsTest {
    private fun model(name:String,isAnima:Boolean=true)=DreamModel(
        "model",name,1024,isAnima,false,true,"anima",emptyList())

    @Test fun animaTurboAutoUsesOfficialLowGuidancePreset() {
        val chosen=NativeTurboDefaults.apply(GeneratorSettings(),
            model("rin_flanime_v1_turbo_qnn228_8gen3"),freeArtwork=true)
        assertEquals(8,chosen.steps)
        assertEquals(1f,chosen.cfg,0f)
        assertEquals(3,chosen.batch)
        assertEquals("Euler",chosen.scheduler)
    }
    @Test fun originalSheetCountsAreNotReduced() {
        val chosen=NativeTurboDefaults.apply(GeneratorSettings(),
            model("Anima Turbo"),freeArtwork=false)
        assertEquals(3,chosen.batch)
    }
    @Test fun manualSettingsAlwaysWin() {
        val exact=GeneratorSettings(steps=25,cfg=5f,batch=4,automatic=false)
        assertEquals(exact,NativeTurboDefaults.apply(exact,model("Anima Turbo"),true))
    }
    @Test fun neverTreatUnrelatedBackendsAsAnimaTurbo() {
        val base=GeneratorSettings()
        assertEquals(base,NativeTurboDefaults.apply(base,model("SDXL turbo",false),true))
        assertEquals(base,NativeTurboDefaults.apply(base,model("Anima base"),true))
    }
}
