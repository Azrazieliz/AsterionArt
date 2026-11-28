package com.asterion.art.runtime

import com.asterion.art.creation.ExecutionSignal
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val nativeWatchdog = Executors.newSingleThreadScheduledExecutor { command ->
    Thread(command, "Art native cancellation").apply { isDaemon = true }
}

internal fun <T> duringNativeExecution(signal: ExecutionSignal, stop: () -> Unit, execute: () -> T): T {
    signal.check()
    val guard = Any()
    var completed = false
    var interruption: Exception? = null
    val watch = nativeWatchdog.scheduleWithFixedDelay({
        synchronized(guard) {
            if (!completed && interruption == null) {
                try {
                    signal.check()
                } catch (requested: Exception) {
                    interruption = requested
                    try {
                        stop()
                    } catch (failure: Exception) {
                        requested.addSuppressed(failure)
                    }
                }
            }
        }
    }, 500, 500, TimeUnit.MILLISECONDS)
    try {
        val result = execute()
        synchronized(guard) {
            completed = true
            interruption?.let { throw it }
        }
        signal.check()
        return result
    } catch (failure: Exception) {
        throw synchronized(guard) { interruption } ?: failure
    } finally {
        synchronized(guard) { completed = true }
        watch.cancel(false)
    }
}
