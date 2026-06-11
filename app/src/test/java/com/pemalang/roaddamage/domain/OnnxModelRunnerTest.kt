package com.pemalang.roaddamage.domain

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OnnxModelRunnerTest {

    @Test
    fun testOnnxModelExecution_NoExceptionAndValidProbabilities() = runTest {
        // Mock context (not used since we override bytes)
        val context = mockk<Context>()
        val runner = OnnxModelRunner(context)
        
        // Read actual ONNX file from assets (so we run the real C++ execution)
        val onnxFile = File("src/main/assets/model_1dcnn.onnx")
        assertTrue("ONNX file must exist in assets", onnxFile.exists())
        
        val modelBytes = onnxFile.readBytes()
        
        // Initialize with real bytes
        runner.initialize(modelBytes)
        
        // 1. Test with all zeros (Flat road, 0 speed)
        val flatDataZero = FloatArray(600)
        var probsZero = floatArrayOf()
        
        try {
            probsZero = runner.predict(flatDataZero)
        } catch (e: Exception) {
            assert(false) { "Predict threw exception: ${e.message}" }
        }
        
        assertEquals("Probs should have 3 classes", 3, probsZero.size)
        
        // Ensure probabilities sum to 1.0
        val sumZero = probsZero[0] + probsZero[1] + probsZero[2]
        assertEquals("Probabilities must sum to 1.0", 1.0f, sumZero, 0.001f)
        
        // For zero input, model is highly biased to Non-Event (Class 0)
        assertTrue("Zero input should be overwhelmingly Non-Event", probsZero[0] > 0.9f)
        
        // 2. Test with severe anomaly (Pothole simulation: aVertical = 10.0, low speed)
        val flatDataAnomaly = FloatArray(600)
        for (i in 0 until 200) {
            flatDataAnomaly[i] = 10.0f // Heavy vertical acceleration
            flatDataAnomaly[400 + i] = 1.45f // Low speed
        }
        
        val probsAnomaly = runner.predict(flatDataAnomaly)
        val sumAnomaly = probsAnomaly[0] + probsAnomaly[1] + probsAnomaly[2]
        assertEquals("Probabilities must sum to 1.0", 1.0f, sumAnomaly, 0.001f)
        
        // For this extreme anomaly, Pothole probability (Class 1) should jump significantly
        // Note: the model is imbalanced, so it might only reach ~10%, but it should be > 0.05
        assertTrue("Anomaly should raise pothole probability above 5%", probsAnomaly[1] > 0.05f)
        
        runner.close()
    }
}
