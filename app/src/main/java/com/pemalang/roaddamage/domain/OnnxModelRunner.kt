package com.pemalang.roaddamage.domain

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.exp

class OnnxModelRunner(private val context: Context) {

    private companion object {
        const val TAG = "OnnxModelRunner"
    }

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    fun initialize(modelBytesOverride: ByteArray? = null) {
        Log.d(TAG, "initialize() called, ortEnvironment=${ortEnvironment != null}")
        if (ortEnvironment != null) return
        ortEnvironment = OrtEnvironment.getEnvironment()
        
        val modelBytes = modelBytesOverride ?: run {
            val assetManager = context.assets
            assetManager.open("model_1dcnn.onnx").readBytes()
        }
        
        val options = OrtSession.SessionOptions()
        // Anda bisa mengaktifkan XNNPACK jika tersedia untuk optimasi ekstra
        
        ortSession = ortEnvironment?.createSession(modelBytes, options)
        Log.d(TAG, "Model initialized successfully! Session=$ortSession")
    }

    suspend fun predict(flatData: FloatArray): FloatArray = withContext(Dispatchers.Default) {
        Log.d(TAG, "predict() called with ${flatData.size} floats")
        val env = ortEnvironment ?: throw IllegalStateException("ONNX Environment not initialized")
        val session = ortSession ?: throw IllegalStateException("ONNX Session not initialized")

        // Bentuk input tensor: [Batch=1, Channels=3, Length=200]
        val shape = longArrayOf(1, 3, 200)
        
        val byteBuffer = java.nio.ByteBuffer.allocateDirect(flatData.size * 4)
        byteBuffer.order(java.nio.ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(flatData)
        floatBuffer.rewind()
        
        val tensor = OnnxTensor.createTensor(env, floatBuffer, shape)
        
        try {
            val inputName = session.inputNames.iterator().next()
            val inputs = mapOf(inputName to tensor)
            
            val result = session.run(inputs)
            try {
                // Output dari PyTorch adalah logits, kita harus melakukan Softmax
                val outputTensor = result.iterator().next().value as OnnxTensor
                val outFloatBuffer = outputTensor.floatBuffer
                val logits = FloatArray(3)
                outFloatBuffer.get(logits)
                
                val probs = softmax(logits)
                Log.d(TAG, "Prediction: logits=[${logits[0]}, ${logits[1]}, ${logits[2]}] -> probs=[${probs[0]}, ${probs[1]}, ${probs[2]}]")
                return@withContext probs
            } finally {
                result.close()
            }
        } finally {
            tensor.close()
        }
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
