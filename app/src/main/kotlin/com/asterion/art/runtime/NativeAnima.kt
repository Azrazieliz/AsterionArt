package com.asterion.art.runtime

import android.graphics.Bitmap
import com.asterion.art.creation.ExecutionSignal
import com.asterion.art.creation.Region
import com.asterion.art.creation.RuntimeUnavailable
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal enum class AnimaBackend(val key: String) { CPU("cpu"), VULKAN("vulkan0") }

internal data class AnimaModels(
    val diffusion: File,
    val encoder: File,
    val vae: File,
)

internal data class AnimaImageRequest(
    val direction: String,
    val width: Int,
    val height: Int,
    val seed: Long,
    val steps: Int,
    val guidance: Float,
    val backend: AnimaBackend,
)

internal data class AnimaReference(
    val features: FloatTensor,
    val region: Region?,
    val weight: Float,
) {
    init {
        require(features.dimensions.size == 3 && features.dimensions.first() == 1)
        require(features.dimensions.last() == 1024 && weight.isFinite() && weight >= 0f)
    }
}

internal class NativeAnima(threads: Int) : AutoCloseable {
    private var handle: Long

    init {
        if (!available) throw RuntimeUnavailable("This build does not contain the native Anima renderer")
        handle = open(threads)
        check(handle != 0L)
    }

    @Synchronized
    fun render(
        models: AnimaModels,
        request: AnimaImageRequest,
        references: List<AnimaReference>,
        signal: ExecutionSignal,
    ): Bitmap {
        check(handle != 0L)
        signal.check()
        require(request.width > 0 && request.height > 0 && request.width % 16 == 0 && request.height % 16 == 0)
        val regions = references.map { reference -> patchWeights(request.width, request.height, reference) }
        val session = handle
        val guard = Any()
        var finished = false
        var cancelled: Exception? = null
        val watch = watchdog.scheduleWithFixedDelay({
            synchronized(guard) {
                if (!finished && cancelled == null) {
                    try {
                        signal.check()
                    } catch (stop: Exception) {
                        cancelled = stop
                        cancelRun(session)
                    }
                }
            }
        }, 500, 500, TimeUnit.MILLISECONDS)
        val output = try {
            execute(
                session, models.diffusion.absolutePath.encodeToByteArray(), models.encoder.absolutePath.encodeToByteArray(),
                models.vae.absolutePath.encodeToByteArray(), request.direction.encodeToByteArray(),
                request.backend.key.encodeToByteArray(), request.width, request.height, request.steps, request.seed,
                request.guidance, references.map { it.features.values }.toTypedArray(), regions.toTypedArray(),
            )
        } catch (error: IllegalStateException) {
            throw synchronized(guard) { cancelled } ?: error
        } finally {
            synchronized(guard) { finished = true }
            watch.cancel(false)
        }
        synchronized(guard) { cancelled }?.let { throw it }
        signal.check()
        val pixels = IntArray(Math.multiplyExact(request.width, request.height))
        require(output.size == Math.multiplyExact(pixels.size, 3))
        for (index in pixels.indices) {
            val offset = index * 3
            val red = (output[offset].toInt() and 255) shl 16
            val green = (output[offset + 1].toInt() and 255) shl 8
            pixels[index] = 0xff000000.toInt() or red or green or (output[offset + 2].toInt() and 255)
        }
        return Bitmap.createBitmap(pixels, request.width, request.height, Bitmap.Config.ARGB_8888)
    }

    @Synchronized
    override fun close() {
        if (handle != 0L) dispose(handle)
        handle = 0
    }

    private external fun open(threads: Int): Long
    private external fun dispose(handle: Long)
    private external fun cancelRun(handle: Long)
    private external fun execute(
        handle: Long, diffusion: ByteArray, encoder: ByteArray, vae: ByteArray, direction: ByteArray,
        backend: ByteArray, width: Int, height: Int, steps: Int, seed: Long, guidance: Float,
        features: Array<FloatArray>, regions: Array<FloatArray>,
    ): ByteArray

    companion object {
        val available: Boolean by lazy {
            try {
                System.loadLibrary("asterion_anima")
                true
            } catch (_: UnsatisfiedLinkError) {
                false
            }
        }
        private val watchdog = Executors.newSingleThreadScheduledExecutor { task ->
            Thread(task, "Art denoising cancellation").apply { isDaemon = true }
        }

        internal fun patchWeights(width: Int, height: Int, reference: AnimaReference): FloatArray {
            val columns = width / 16
            val rows = height / 16
            val region = reference.region
            return FloatArray(Math.multiplyExact(columns, rows)) { index ->
                if (region == null) {
                    reference.weight
                } else {
                    val x = index % columns
                    val y = index / columns
                    val horizontal = minOf(x + 1f, region.right * columns) - maxOf(x.toFloat(), region.left * columns)
                    val vertical = minOf(y + 1f, region.bottom * rows) - maxOf(y.toFloat(), region.top * rows)
                    reference.weight * maxOf(0f, horizontal) * maxOf(0f, vertical)
                }
            }
        }
    }
}
