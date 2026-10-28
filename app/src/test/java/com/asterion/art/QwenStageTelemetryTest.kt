package com.asterion.art

import com.asterion.art.brain.QwenStageTelemetry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class QwenStageTelemetryTest {
    @Test fun stagesAggregateElapsedDurationsWithoutExposingAuthority()=runBlocking {
        var nanos=0L
        val meter=QwenStageTelemetry{nanos}
        meter.measure("model_load"){nanos+=125_000_000L}
        meter.measure("identity_json"){nanos+=70_000_000L}
        meter.measure("identity_json"){nanos+=30_000_000L}
        val result=meter.snapshot("plan-example","COMPLETE")
        assertEquals(225L,result.getLong("total_elapsed_ms"))
        assertEquals(125L,result.getJSONObject("stages")
            .getJSONObject("model_load").getLong("elapsed_ms"))
        assertEquals(2,result.getJSONObject("stages")
            .getJSONObject("identity_json").getInt("calls"))
        assertEquals(100L,result.getJSONObject("stages")
            .getJSONObject("identity_json").getLong("elapsed_ms"))
        val serialized=result.toString()
        assertFalse(serialized.contains("anatomical study"))
        assertFalse(serialized.contains("prompt"))
    }

    @Test fun failedStagesStillRecordActualElapsedTime()=runBlocking {
        var nanos=0L
        val meter=QwenStageTelemetry{nanos}
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                meter.measure("pairwise_batch"){
                    nanos+=25_000_000L
                    throw IllegalStateException("invalid pair")
                }
            }
        }
        val data=meter.snapshot("plan-example","FAILED_OR_CANCELLED")
        assertEquals(25L,data.getJSONObject("stages")
            .getJSONObject("pairwise_batch").getLong("elapsed_ms"))
    }
}
