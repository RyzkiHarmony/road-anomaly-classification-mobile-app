package com.pemalang.roaddamage.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProcessGpsReadingUseCaseTest {

    private lateinit var processGpsReadingUseCase: ProcessGpsReadingUseCase

    @Before
    fun setUp() {
        processGpsReadingUseCase = ProcessGpsReadingUseCase()
    }

    @Test
    fun testInvalidWhenLatitudeIsNaN() {
        val result = processGpsReadingUseCase(
            latitude = Double.NaN,
            longitude = 109.3813,
            accuracy = 10f,
            maxAccuracyMeters = 20f,
            prevLat = null,
            prevLon = null
        )
        assertFalse(result.isValid)
        assertEquals(0f, result.distanceDelta, 0.0001f)
    }

    @Test
    fun testInvalidWhenLongitudeIsNaN() {
        val result = processGpsReadingUseCase(
            latitude = -6.8892,
            longitude = Double.NaN,
            accuracy = 10f,
            maxAccuracyMeters = 20f,
            prevLat = null,
            prevLon = null
        )
        assertFalse(result.isValid)
        assertEquals(0f, result.distanceDelta, 0.0001f)
    }

    @Test
    fun testInvalidWhenAccuracyIsNaN() {
        val result = processGpsReadingUseCase(
            latitude = -6.8892,
            longitude = 109.3813,
            accuracy = Float.NaN,
            maxAccuracyMeters = 20f,
            prevLat = null,
            prevLon = null
        )
        assertFalse(result.isValid)
        assertEquals(0f, result.distanceDelta, 0.0001f)
    }

    @Test
    fun testInvalidWhenAccuracyExceedsMaxAccuracy() {
        // accuracy = 25m, max = 20m.
        val result = processGpsReadingUseCase(
            latitude = -6.8892,
            longitude = 109.3813,
            accuracy = 25.0f,
            maxAccuracyMeters = 20.0f,
            prevLat = null,
            prevLon = null
        )
        assertFalse(result.isValid)
        assertEquals(0f, result.distanceDelta, 0.0001f)
    }

    @Test
    fun testValidFirstPointReturnsZeroDistance() {
        // First point, prev coordinates are null
        val result = processGpsReadingUseCase(
            latitude = -6.8892,
            longitude = 109.3813,
            accuracy = 10f,
            maxAccuracyMeters = 20f,
            prevLat = null,
            prevLon = null
        )
        assertTrue(result.isValid)
        assertEquals(0f, result.distanceDelta, 0.0001f)
    }

    @Test
    fun testValidWhenAccuracyIsExactlyMaxAccuracy() {
        // accuracy = 20m, max = 20m.
        // It should be valid since the condition is accuracy > maxAccuracyMeters to be invalid.
        val result = processGpsReadingUseCase(
            latitude = -6.8892,
            longitude = 109.3813,
            accuracy = 20.0f,
            maxAccuracyMeters = 20.0f,
            prevLat = null,
            prevLon = null
        )
        assertTrue(result.isValid)
        assertEquals(0f, result.distanceDelta, 0.0001f)
    }

    @Test
    fun testValidConsecutivePointCalculatesCorrectDistance() {
        // Coordinates for Pemalang central area (approx)
        // Point A: -6.8892, 109.3813
        // Point B: -6.8900, 109.3820
        // Expected distance using haversine formula should be positive and non-zero
        val result = processGpsReadingUseCase(
            latitude = -6.8900,
            longitude = 109.3820,
            accuracy = 10f,
            maxAccuracyMeters = 20f,
            prevLat = -6.8892,
            prevLon = 109.3813
        )
        assertTrue(result.isValid)
        assertTrue(result.distanceDelta > 0f)
        
        // Let's compare with actual known calculation
        // -6.8892, 109.3813 to -6.8900, 109.3820 is approx 117.8 meters.
        assertEquals(117.8f, result.distanceDelta, 5.0f)
    }
}
