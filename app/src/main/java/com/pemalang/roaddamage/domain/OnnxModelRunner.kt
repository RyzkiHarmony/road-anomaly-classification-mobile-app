package com.pemalang.roaddamage.domain

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.exp

class OnnxModelRunner(private val context: Context) {

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    fun initialize() {
        if (ortEnvironment != null) return
        ortEnvironment = OrtEnvironment.getEnvironment()
        
        // Membaca model dari folder assets
        val assetManager = context.assets
        val modelBytes = assetManager.open("model_1dcnn.onnx").readBytes()
        
        val options = OrtSession.SessionOptions()
        // Anda bisa mengaktifkan XNNPACK jika tersedia untuk optimasi ekstra
        
        ortSession = ortEnvironment?.createSession(modelBytes, options)
    }

    suspend fun predict(flatData: FloatArray): FloatArray = withContext(Dispatchers.Default) {
        val env = ortEnvironment ?: throw IllegalStateException("ONNX Environment not initialized")
        val session = ortSession ?: throw IllegalStateException("ONNX Session not initialized")

        // Bentuk input tensor: [Batch=1, Channels=3, Length=200]
        val shape = longArrayOf(1, 3, 200)
        
        val floatBuffer = FloatBuffer.wrap(flatData)
        val tensor = OnnxTensor.createTensor(env, floatBuffer, shape)
        
        val inputName = session.inputNames.iterator().next()
        val inputs = mapOf(inputName to tensor)
        
        val result = session.run(inputs)
        
        // Output dari PyTorch adalah logits, kita harus melakukan Softmax
        @Suppress("UNCHECKED_CAST")
        val outputArray = result[0].value as Array<FloatArray>
        val logits = outputArray[0]
        
        result.close()
        tensor.close()
        
        return@withContext softmax(logits)
    }

    private fun softmax(logits: FloatArray): FloatArray {
        var maxLogit = Float.NEGATIVE_INFINITY
        for (l in logits) {
            if (l > maxLogit) maxLogit = l
        }
        
        var sumExp = 0f
        val expLogits = FloatArray(logits.size)
        for (i in logits.indices) {
            val e = exp((logits[i] - maxLogit).toDouble()).toFloat()
            expLogits[i] = e
            sumExp += e
        }
        
        for (i in logits.indices) {
            expLogits[i] = expLogits[i] / sumExp
        }
        return expLogits
    }

    fun close() {
        ortSession?.close()
        ortEnvironment?.close()
    }
}
