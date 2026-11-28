package com.asterion.art.runtime

import com.asterion.art.creation.ExecutionPaused
import com.asterion.art.creation.ExecutionSignal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeCancellationTest {
    @Test
    fun thermalPauseAbortsAnActiveCallAndKeepsItsOriginalReason() {
        val started = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val pause = ExecutionPaused("Waiting for the phone to cool down")
        val result = assertThrows(ExecutionPaused::class.java) {
            duringNativeExecution(ExecutionSignal { if (started.count == 0L) throw pause }, stopped::countDown) {
                started.countDown()
                assertTrue(stopped.await(5, TimeUnit.SECONDS))
                error("The native provider reported termination")
            }
        }
        assertSame(pause, result)
    }

    @Test
    fun aCancelledRequestNeverEntersTheNativeProvider() {
        val interrupt = InterruptedException("Cancelled")
        val result = assertThrows(InterruptedException::class.java) {
            duringNativeExecution(ExecutionSignal { throw interrupt }, { error("Unexpected abort") }) {
                error("Unexpected native execution")
            }
        }
        assertSame(interrupt, result)
    }
}
