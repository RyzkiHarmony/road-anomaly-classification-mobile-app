package com.pemalang.roaddamage.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorFusionProcessorTest {

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
        
        // 1. Feed 199 samples (buffer not full)
        for (i in 0 until 199) {
            processor.processLinearAcceleration(0f, 0f, 0f)
        }
        
        assertEquals("Should not emit before WINDOW_SIZE is reached", 0, emittedTensors)
        
        // 2. Feed the 200th sample (buffer is full, should emit immediately)
        processor.processLinearAcceleration(10f, 0f, 0f) // Feed an anomaly
        
        assertEquals("Should emit exactly once when WINDOW_SIZE is reached", 1, emittedTensors)
        assertEquals("Tensor should have exactly 600 floats", 600, lastTensor?.size)
        
        // 3. Feed 49 more samples (STRIDE is 50, so it shouldn't emit yet)
        for (i in 0 until 49) {
            processor.processLinearAcceleration(0f, 0f, 0f)
        }
        
        assertEquals("Should not emit until STRIDE is reached", 1, emittedTensors)
        
        // 4. Feed 1 more sample (reaches 50 STRIDE)
        processor.processLinearAcceleration(0f, 0f, 0f)
        
        assertEquals("Should emit again after STRIDE", 2, emittedTensors)
        
        job.cancel()
    }
}
