package com.asterion.art.runtime

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtProvider
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import com.asterion.art.creation.ExecutionPaused
import com.asterion.art.creation.ExecutionSignal
import com.asterion.art.creation.RuntimeUnavailable
import java.io.File
import java.nio.FloatBuffer

internal enum class ComputeBackend { CPU, QNN_HTP, QNN_GPU }

internal data class FloatTensor(val dimensions: List<Int>, val values: FloatArray) {
    init {
        require(dimensions.isNotEmpty() && dimensions.all { it > 0 })
        require(dimensions.fold(1, Math::multiplyExact) == values.size)
    }
}

internal class NativeGraph(
    model: File,
    backend: ComputeBackend,
    cpuThreads: Int,
) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val options: OrtSession.SessionOptions
    private val session: OrtSession

    init {
        require(model.isFile)
        val providers = OrtEnvironment.getAvailableProviders()
        if (backend != ComputeBackend.CPU && OrtProvider.QNN !in providers) {
            throw RuntimeUnavailable("This Android runtime has no Qualcomm QNN provider")
        }
        options = OrtSession.SessionOptions()
        try {
            options.setIntraOpNumThreads(cpuThreads)
            options.setInterOpNumThreads(1)
            if (backend != ComputeBackend.CPU) {
                options.addConfigEntry("session.disable_cpu_ep_fallback", "1")
                val library = if (backend == ComputeBackend.QNN_HTP) "libQnnHtp.so" else "libQnnGpu.so"
                options.addQnn(mapOf("backend_path" to library))
            }
            session = environment.createSession(model.absolutePath, options)
        } catch (failure: Throwable) {
            options.close()
            throw failure
        }
    }

    fun execute(
        inputs: Map<String, FloatTensor>,
        outputs: Set<String>,
        signal: ExecutionSignal,
    ): Map<String, FloatTensor> {
        signal.check()
        val required = session.inputInfo
        require(inputs.keys == required.keys && outputs.isNotEmpty() && outputs.all(session.outputNames::contains))
        val native = LinkedHashMap<String, OnnxTensor>()
        try {
            for ((name, tensor) in inputs) {
                val signature = required.getValue(name).info as? TensorInfo
                    ?: throw RuntimeUnavailable("The model input $name is not a tensor")
                require(signature.type == OnnxJavaType.FLOAT)
                require(signature.shape.size == tensor.dimensions.size)
                require(signature.shape.zip(tensor.dimensions).all { (declared, actual) ->
                    declared < 0 || declared == actual.toLong()
                }) { "The graph cannot accept the full $name tensor" }
                native[name] = OnnxTensor.createTensor(
                    environment, FloatBuffer.wrap(tensor.values), tensor.dimensions.map(Int::toLong).toLongArray(),
                )
            }
            val data = session.run(native, outputs).use { result ->
                outputs.associateWith { name ->
                    val value = result.get(name).orElseThrow() as OnnxTensor
                    val buffer = checkNotNull(value.floatBuffer)
                    val floats = FloatArray(buffer.remaining()).also(buffer::get)
                    check(floats.all(Float::isFinite)) { "The graph returned non-finite $name values" }
                    FloatTensor(value.info.shape.map(Math::toIntExact), floats)
                }
            }
            signal.check()
            return data
        } finally {
            native.values.reversed().forEach(OnnxTensor::close)
        }
    }

    override fun close() {
        try {
            session.close()
        } finally {
            options.close()
        }
    }
}

internal class DeviceBudget(context: Context) {
    private val activity = context.getSystemService(ActivityManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)

    fun check(residentBytes: Long = 0) {
        val memory = ActivityManager.MemoryInfo().also(activity::getMemoryInfo)
        if (memory.lowMemory || memory.availMem - memory.threshold < residentBytes) {
            throw ExecutionPaused("Waiting for available memory")
        }
        if (power.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
            throw ExecutionPaused("Waiting for the phone to cool down")
        }
    }

    fun threads(): Int {
        val capacity = Runtime.getRuntime().availableProcessors()
        return if (power.isPowerSaveMode ||
            power.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
        ) maxOf(1, capacity / 3) else maxOf(1, capacity / 2)
    }
}
