package com.asterion.art.nativeengine

import java.util.concurrent.CancellationException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * One process-wide memory gate for the CPU GGUF planner, multimodal projector
 * and native QNN diffusion process. Separate runtime locks allowed the phone
 * to load multiple multi-GB models concurrently and be killed by Android LMK.
 *
 * A gate lease covers actual model loading and execution, not unrelated I/O.
 */
object DeviceInferenceGate {
    private val permit=Semaphore(1,true)
    fun acquire(cancelled:()->Boolean={false}) {
        while(true) {
            if(cancelled())throw CancellationException("Native model wait cancelled")
            if(permit.tryAcquire(500,TimeUnit.MILLISECONDS))return
        }
    }
    fun release() {permit.release()}
}
