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
            assetManager.open("cnn_1d_model.onnx").readBytes()
        }
        
        val options = OrtSession.SessionOptions()
        // Aktifkan NNAPI untuk menekan latensi inference di device Android
        try {
            options.addNnapi()
            Log.d(TAG, "NNAPI execution provider enabled.")
        } catch (e: Exception) {
            Log.w(TAG, "NNAPI not available, fallback to CPU.")
        }
        
        ortSession = ortEnvironment?.createSession(modelBytes, options)
        Log.d(TAG, "Model initialized successfully! Session=$ortSession")
    }

    suspend fun predict(flatData: FloatArray): FloatArray = withContext(Dispatchers.Default) {
        // Log.d(TAG, "predict() called with ${flatData.size} floats")
        val env = ortEnvironment ?: throw IllegalStateException("ONNX Environment not initialized")
        val session = ortSession ?: throw IllegalStateException("ONNX Session not initialized")

        // Bentuk input tensor: [Batch=1, Channels=7, Length=200]
        val shape = longArrayOf(1, 7, 200)
        
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
                // Output dari ONNX model versi terbaru sudah berwujud Probabilitas (Softmax embedded).
                // Kita tidak perlu lagi melakukan Softmax/Sigmoid manual.
                val outputTensor = result.iterator().next().value as OnnxTensor
                val outFloatBuffer = outputTensor.floatBuffer
                val probs = FloatArray(3)
                outFloatBuffer.get(probs)
                
                // Log.d(TAG, "Prediction: probs=[${probs[0]}, ${probs[1]}, ${probs[2]}]")
                return@withContext probs
            } finally {
                result.close()
            }
        } finally {
            tensor.close()
        }
    }


    fun close() {
        ortSession?.close()
        ortEnvironment?.close()
    }
}
