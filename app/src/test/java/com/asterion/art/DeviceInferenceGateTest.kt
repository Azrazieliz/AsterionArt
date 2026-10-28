package com.asterion.art

import com.asterion.art.nativeengine.DeviceInferenceGate
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DeviceInferenceGateTest {
    @Test fun reasoningAndQnnCannotRunAtOnce() {
        DeviceInferenceGate.acquire()
        val second=CountDownLatch(1)
        val completed=CountDownLatch(1)
        val worker=Thread {
            try {
                DeviceInferenceGate.acquire()
                try {second.countDown()}
                finally {DeviceInferenceGate.release()}
            } finally {completed.countDown()}
        }
        try {
            worker.start()
            assertFalse(second.await(200,TimeUnit.MILLISECONDS))
        } finally { DeviceInferenceGate.release() }
        assertTrue(second.await(3,TimeUnit.SECONDS))
        assertTrue(completed.await(3,TimeUnit.SECONDS))
        worker.join(1000)
    }
    @Test fun cancelledWaiterNeverStealsTheSharedPermit() {
        assertTrue(runCatching {
            DeviceInferenceGate.acquire {true}
        }.isFailure)
        DeviceInferenceGate.acquire()
        DeviceInferenceGate.release()
    }
}
