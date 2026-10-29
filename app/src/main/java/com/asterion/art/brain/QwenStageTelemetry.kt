package com.asterion.art.brain

import org.json.JSONObject

/** Monotonic Reasoning stage timings, without prompts, concepts, or generated text. */
internal class QwenStageTelemetry(private val nowNanos:()->Long=System::nanoTime) {
    private val beganAt=nowNanos()
    private data class Measurement(var calls:Int=0,var nanos:Long=0)
    private val measured=linkedMapOf<String,Measurement>()

    suspend fun <T> measure(stage:String,block:suspend ()->T):T {
        require(stage.matches(Regex("[a-z_]{2,48}")))
        val started=nowNanos()
        try {
            return block()
        } finally {
            val elapsed=(nowNanos()-started).coerceAtLeast(0L)
            val row=measured.getOrPut(stage){Measurement()}
            row.calls++
            row.nanos+=elapsed
        }
    }

    /** Timing of first-token wait and streamed output per neural stage. */
    fun recordDuration(stage:String,nanos:Long) {
        require(stage.matches(Regex("[a-z_]{2,48}")))
        val row=measured.getOrPut(stage){Measurement()}
        row.calls++
        row.nanos+=nanos.coerceAtLeast(0L)
    }

    fun snapshot(planId:String,outcome:String):JSONObject=JSONObject().apply {
        put("schema","asterion.reasoning-stage-timings.v1")
        put("plan_id",planId)
        put("outcome",outcome)
        put("total_elapsed_ms",((nowNanos()-beganAt).coerceAtLeast(0L))/1_000_000L)
        put("stages",JSONObject().also{stages->
            measured.forEach{(stage,row)->
                stages.put(stage,JSONObject()
                    .put("calls",row.calls)
                    .put("elapsed_ms",row.nanos/1_000_000L))
            }
        })
    }
}
