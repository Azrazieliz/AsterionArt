package com.asterion.compiler.art

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Joins compatible inferred MP4 segments without re-encoding. This preserves
 * the model-produced frames; it does NOT invent in-between frames or guarantee
 * alignment to the next storyboard anchor.
 */
object VideoAssembler {
    fun concat(files: List<File>, output: File, fps: Int = 24): File {
        require(files.isNotEmpty() && fps in 1..120)
        require(files.none { !it.exists() || it.length() <= 0L })
        if (files.size == 1) {
            files.single().copyTo(output, overwrite = true)
            return output
        }
        output.parentFile?.mkdirs()
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false
        try {
            var track = -1
            var firstFormat: MediaFormat? = null
            var offsetUs = 0L
            val nominalFrameUs = 1_000_000L / fps
            val buffer = ByteBuffer.allocateDirect(8 * 1024 * 1024)
            for (file in files) {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(file.absolutePath)
                    val sourceTrack = (0 until extractor.trackCount).firstOrNull { idx ->
                        extractor.getTrackFormat(idx).getString(MediaFormat.KEY_MIME)
                            ?.startsWith("video/") == true
                    } ?: error("No video stream in ${file.name}")
                    val format = extractor.getTrackFormat(sourceTrack)
                    if (!started) {
                        firstFormat = format
                        track = muxer.addTrack(format)
                        muxer.start()
                        started = true
                    } else {
                        check(compatible(firstFormat!!, format)) {
                            "Video codec/configuration differs: ${file.name}; preserving source clips"
                        }
                    }
                    extractor.selectTrack(sourceTrack)
                    var lastWrittenUs = offsetUs - nominalFrameUs
                    var wroteAny = false
                    while (true) {
                        buffer.clear()
                        val bytes = extractor.readSampleData(buffer, 0)
                        if (bytes < 0) break
                        require(bytes <= buffer.capacity()) { "Encoded frame exceeds mux buffer" }
                        val info = MediaCodec.BufferInfo()
                        info.offset = 0
                        info.size = bytes
                        val incoming = extractor.sampleTime.coerceAtLeast(0L)
                        info.presentationTimeUs = maxOf(lastWrittenUs + 1, offsetUs + incoming)
                        info.flags = extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC
                        muxer.writeSampleData(track, buffer, info)
                        wroteAny = true
                        lastWrittenUs = info.presentationTimeUs
                        extractor.advance()
                    }
                    check(wroteAny) { "No encoded video samples in ${file.name}" }
                    offsetUs = lastWrittenUs + nominalFrameUs
                } finally { extractor.release() }
            }
        } catch (e: Exception) {
            runCatching { if (started) muxer.stop() }
            muxer.release()
            output.delete()
            throw e
        }
        if (started) muxer.stop()
        muxer.release()
        return output
    }

    private fun compatible(a: MediaFormat, b: MediaFormat): Boolean {
        for (key in listOf(MediaFormat.KEY_MIME, MediaFormat.KEY_WIDTH, MediaFormat.KEY_HEIGHT)) {
            if (a.getValue(key) != b.getValue(key)) return false
        }
        for (key in listOf("csd-0", "csd-1")) {
            val x = a.getByteBuffer(key)?.duplicate()
            val y = b.getByteBuffer(key)?.duplicate()
            if ((x == null) != (y == null)) return false
            if (x != null && y != null && x != y) return false
        }
        return true
    }
}
