package com.pemalang.roaddamage.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class ButterworthFilterTest {

    @Test
    fun testFilterInitialization() {
        val filter = ButterworthFilter()
        
        // When feeding constant value, the output should stabilize quickly
        // and match the input since it's a DC signal (0 Hz), which passes through a low-pass filter
        val constantValue = 9.81f
        var output = 0f
        
        for (i in 0 until 100) {
            output = filter.filter(constantValue)
        }
        
        assertEquals("Output should match DC input", constantValue, output, 0.01f)
    }

    @Test
    fun testHighFrequencyAttenuation() {
        val filter = ButterworthFilter()
        
        // Generate a high frequency signal (e.g., alternating +10 and -10 at 50Hz Nyquist)
        var maxOutput = 0f
        
        // Allow the filter to stabilize first with 0
        for (i in 0 until 100) {
            filter.filter(0f)
        }
        
        // Feed high frequency noise
        for (i in 0 until 100) {
            val noise = if (i % 2 == 0) 10f else -10f
            val output = filter.filter(noise)
            if (i > 20) { // ignore transient response
                maxOutput = maxOf(maxOutput, abs(output))
            }
        }
        
        // Output should be heavily attenuated (cutoff is 6Hz, signal is 50Hz)
        assert(maxOutput < 1.0f) { "High frequency noise was not attenuated enough: maxOutput=$maxOutput" }
    }
}
