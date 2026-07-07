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
        every { Log.w(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
    }

    @Test
    fun testOnnxModelExecution_NoExceptionAndValidProbabilities() = runTest {
        // Mock context (not used since we override bytes)
        val context = mockk<Context>()
        val runner = OnnxModelRunner(context)
        
        // Read actual ONNX file from assets (so we run the real C++ execution)
        var onnxFile = File("src/main/assets/cnn_1d_model.onnx")
        if (!onnxFile.exists()) {
            onnxFile = File("app/src/main/assets/cnn_1d_model.onnx")
        }
        assertTrue("ONNX file must exist in assets", onnxFile.exists())
        
        val modelBytes = onnxFile.readBytes()
        
        // Initialize with real bytes
        runner.initialize(modelBytes)
        
        // 1. Test with all zeros (Flat road, 0 speed)
        val flatDataZero = FloatArray(7 * 200)
        var probsZero = floatArrayOf()
        
        try {
            probsZero = runner.predict(flatDataZero)
        } catch (e: Exception) {
            assert(false) { "Predict threw exception: ${e.message}" }
        }
        
        assertEquals("Probs should have 3 classes", 3, probsZero.size)
        
        // Ensure probabilities are valid (0.0 to 1.0)
        assertTrue("Probs must be >= 0", probsZero.all { it >= 0.0f })
        assertTrue("Probs must be <= 1", probsZero.all { it <= 1.0f })
        
        println("Actual Probs for Zero Input (Unstandardized 0.0):")
        println("  Non-Event: ${probsZero[0]}")
        println("  Pothole:   ${probsZero[1]}")
        println("  SpeedBump: ${probsZero[2]}")
        
        // 2. Test with severe anomaly (Pothole simulation: aVertical = 10.0, low speed)
        val flatDataAnomaly = FloatArray(7 * 200)
        for (i in 0 until 200) {
            flatDataAnomaly[i] = 10.0f // Heavy vertical acceleration (Channel 0)
            flatDataAnomaly[2 * 200 + i] = 1.45f // Low speed (Channel 2)
            flatDataAnomaly[3 * 200 + i] = 5.0f // Crest factor (Channel 3)
            flatDataAnomaly[4 * 200 + i] = 120.0f // Jerk (Channel 4)
        }
        
        val probsAnomaly = runner.predict(flatDataAnomaly)
        assertTrue("Probs must be >= 0", probsAnomaly.all { it >= 0.0f })
        assertTrue("Probs must be <= 1", probsAnomaly.all { it <= 1.0f })
        
        println("Actual Probs for Simulated Anomaly (Unstandardized 10.0 Vert):")
        println("  Non-Event: ${probsAnomaly[0]}")
        println("  Pothole:   ${probsAnomaly[1]}")
        println("  SpeedBump: ${probsAnomaly[2]}")
        
        // 3. Latency Benchmark Loop (100 Iterations)
        println("=== STARTING LATENCY BENCHMARK ===")
        val iterations = 100
        val times = LongArray(iterations)
        
        // Warmup runs to allow JVM JIT compiler compilation
        for (i in 0 until 10) {
            runner.predict(flatDataAnomaly)
        }
        
        for (i in 0 until iterations) {
            val startTime = System.nanoTime()
            runner.predict(flatDataAnomaly)
            val duration = System.nanoTime() - startTime
            times[i] = duration / 1_000_000 // Convert to ms
        }
        
        val avgTime = times.average()
        val minTime = times.minOrNull() ?: 0L
        val maxTime = times.maxOrNull() ?: 0L
        
        println("Benchmark Results (100 runs):")
        println("  Average Latency: ${String.format("%.4f", avgTime)} ms")
        println("  Min Latency: $minTime ms")
        println("  Max Latency: $maxTime ms")
        println("===================================")
        
        runner.close()
    }
}

