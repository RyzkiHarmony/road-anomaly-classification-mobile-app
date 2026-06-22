package com.pemalang.roaddamage.domain

import android.content.Context
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OnnxModelRunnerTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @Test
    fun testOnnxModelExecution_NoExceptionAndValidProbabilities() = runTest {
        // Mock context (not used since we override bytes)
        val context = mockk<Context>()
        val runner = OnnxModelRunner(context)
        
        // Read actual ONNX file from assets (so we run the real C++ execution)
        var onnxFile = File("src/main/assets/model_1dcnn.onnx")
        if (!onnxFile.exists()) {
            onnxFile = File("app/src/main/assets/model_1dcnn.onnx")
        }
        assertTrue("ONNX file must exist in assets", onnxFile.exists())
        
        val modelBytes = onnxFile.readBytes()
        
        // Initialize with real bytes
        runner.initialize(modelBytes)
        
        // 1. Test with all zeros (Flat road, 0 speed)
        val flatDataZero = FloatArray(2000)
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
        assertTrue("Zero input should be overwhelmingly Non-Event", probsZero[0] > 0.8f)
        
        // 2. Test with severe anomaly (Pothole simulation: aVertical = 10.0, low speed)
        val flatDataAnomaly = FloatArray(2000)
        for (i in 0 until 200) {
            flatDataAnomaly[i] = 10.0f // Heavy vertical acceleration (Channel 0)
            flatDataAnomaly[2 * 200 + i] = 1.45f // Low speed (Channel 2)
            flatDataAnomaly[3 * 200 + i] = 5.0f // Crest factor (Channel 3)
            flatDataAnomaly[4 * 200 + i] = 120.0f // Jerk (Channel 4)
        }
        
        val probsAnomaly = runner.predict(flatDataAnomaly)
        val sumAnomaly = probsAnomaly[0] + probsAnomaly[1] + probsAnomaly[2]
        assertEquals("Probabilities must sum to 1.0", 1.0f, sumAnomaly, 0.001f)
        
        // For this extreme anomaly, Pothole or Speed Bump probability should jump significantly
        assertTrue("Anomaly should raise pothole or speed bump probability above 5%", probsAnomaly[1] > 0.05f || probsAnomaly[2] > 0.05f)
        
        runner.close()
    }
}

