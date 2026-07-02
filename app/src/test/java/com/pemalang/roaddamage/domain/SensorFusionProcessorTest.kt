package com.pemalang.roaddamage.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import com.pemalang.roaddamage.model.SensorEventData

@OptIn(ExperimentalCoroutinesApi::class)
class SensorFusionProcessorTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    private fun feedSensors(processor: SensorFusionProcessor, ax: Float, ay: Float, az: Float, gx: Float, gy: Float, gz: Float, grx: Float, gry: Float, grz: Float, tsNs: Long) {
        processor.addLinearAccel(SensorEventData(tsNs, floatArrayOf(ax, ay, az)))
        processor.addGyro(SensorEventData(tsNs, floatArrayOf(gx, gy, gz)))
        processor.addGravity(SensorEventData(tsNs, floatArrayOf(grx, gry, grz)))
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
        
        // Feed initial speed to ensure static override is not triggered incorrectly
        processor.updateSpeed(5.0f)
        
        // 1. Feed 199 samples (buffer not full)
        // Because interpolate requires two points, the first event (i=0) won't produce a sample.
        // So feeding i=0..199 (200 events) produces exactly 199 resampled points.
        for (i in 0 until 180) {
            feedSensors(processor, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 9.81f, i * 10_000_000L)
        }
        for (i in 180..199) {
            feedSensors(processor, 0f, 0f, 10f, 0f, 0f, 0f, 0f, 0f, 9.81f, i * 10_000_000L)
        }
        
        assertEquals("Should not emit before WINDOW_SIZE is reached", 0, emittedTensors)
        
        // 2. Feed the 200th sample (buffer is full, should emit immediately)
        // i=200 produces the 200th resampled point
        feedSensors(processor, 0f, 0f, 10f, 0f, 0f, 0f, 0f, 0f, 9.81f, 200 * 10_000_000L)
        
        assertEquals("Should emit exactly once when WINDOW_SIZE is reached", 1, emittedTensors)
        assertEquals("Tensor should have exactly 3600 floats (18 channels * 200)", 3600, lastTensor?.size)
        
        // 3. Feed 49 more samples (STRIDE is 50, so it shouldn't emit yet)
        for (i in 1..49) {
            feedSensors(processor, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 9.81f, (200 + i) * 10_000_000L)
        }
        
        assertEquals("Should not emit until STRIDE is reached", 1, emittedTensors)
        
        // 4. Feed 1 more sample (reaches 50 STRIDE)
        feedSensors(processor, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 9.81f, 250 * 10_000_000L)
        
        assertEquals("Should emit again after STRIDE", 2, emittedTensors)
        
        job.cancel()
    }
}
