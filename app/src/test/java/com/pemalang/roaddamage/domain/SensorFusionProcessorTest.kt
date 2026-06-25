package com.pemalang.roaddamage.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorFusionProcessorTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    @Test
    fun testBufferFillingAndInferenceTrigger() = runTest {
        val processor = SensorFusionProcessor()
        
        var emittedTensors = 0
        var lastTensor: FloatArray? = null
        
        // Collect inference triggers
        val job = launch(UnconfinedTestDispatcher()) {
            processor.inferenceTrigger.collect { tensor ->
                emittedTensors++
                lastTensor = tensor
            }
        }
        
        // 1. Feed 199 samples (buffer not full), with an anomaly ramp-up starting at 180
        for (i in 0 until 180) {
            processor.processLinearAcceleration(0f, 0f, 0f, i * 10L)
        }
        for (i in 180 until 199) {
            processor.processLinearAcceleration(0f, 0f, 10f, i * 10L)
        }
        
        assertEquals("Should not emit before WINDOW_SIZE is reached", 0, emittedTensors)
        
        // 2. Feed the 200th sample (buffer is full, should emit immediately)
        processor.processLinearAcceleration(0f, 0f, 10f, 199 * 10L) // Feed an anomaly
        
        assertEquals("Should emit exactly once when WINDOW_SIZE is reached", 1, emittedTensors)
        assertEquals("Tensor should have exactly 2800 floats", 2800, lastTensor?.size)
        
        // 3. Feed 49 more samples (STRIDE is 50, so it shouldn't emit yet)
        for (i in 0 until 49) {
            processor.processLinearAcceleration(0f, 0f, 0f, (200 + i) * 10L)
        }
        
        assertEquals("Should not emit until STRIDE is reached", 1, emittedTensors)
        
        // 4. Feed 1 more sample (reaches 50 STRIDE)
        processor.processLinearAcceleration(0f, 0f, 0f, 249 * 10L)
        
        assertEquals("Should emit again after STRIDE", 2, emittedTensors)
        
        job.cancel()
    }
}
