package com.asterion.art

import com.asterion.art.video.WanVideoChecksumVerifier
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CancellationException

class WanVideoChecksumVerifierTest {
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString(""){"%02x".format(it)}

    @Test fun correctWeightsPassButCorruptionIsRejected() {
        val file=File.createTempFile("wan-verified-",".gguf")
        try {
            val data=ByteArray(8192){it.toByte()}
            file.writeBytes(data)
            WanVideoChecksumVerifier.check(file,sha(data))
            val observedModified=file.lastModified()
            file.writeBytes(ByteArray(data.size){42})
            check(file.setLastModified(observedModified+3000L))
            assertTrue(runCatching {
                WanVideoChecksumVerifier.check(file,sha(data))
            }.isFailure)
        }finally{file.delete()}
    }
    @Test fun rejectsWrongChecksumAndCancellation() {
        val file=File.createTempFile("wan-verified-",".gguf")
        try {
            file.writeBytes(ByteArray(512){1})
            assertTrue(runCatching{
                WanVideoChecksumVerifier.check(file,"0".repeat(64))
            }.isFailure)
            assertTrue(runCatching{
                WanVideoChecksumVerifier.check(file,sha(file.readBytes())){true}
            }.exceptionOrNull() is CancellationException)
        }finally{file.delete()}
    }
}
