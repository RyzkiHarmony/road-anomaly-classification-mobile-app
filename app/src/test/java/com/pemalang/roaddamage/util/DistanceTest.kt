package com.pemalang.roaddamage.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceTest {

    @Test
    fun testHaversineSameLocation() {
        val lat = -6.8892
        val lon = 109.3813
        val distance = Distance.haversine(lat, lon, lat, lon)
        assertEquals(0.0f, distance, 0.0001f)
    }

    @Test
    fun testHaversineKnownDistanceMonasToBundaranHI() {
        // Monas: -6.1754, 106.8272
        // Bundaran HI: -6.1950, 106.8229
        // Known distance is approximately 2230 - 2250 meters.
        val distance = Distance.haversine(
            lat1 = -6.1754,
            lon1 = 106.8272,
            lat2 = -6.1950,
            lon2 = 106.8229
        )
        
        // Assert distance is within a close threshold of expected value (~2240 meters)
        assertEquals(2240f, distance, 30f)
    }

    @Test
    fun testHaversineSmallDistance() {
        // Test a very small step (e.g. 10 meters move)
        // Lat: -6.88920, Lon: 109.38130 to Lat: -6.88920, Lon: 109.38139
        val distance = Distance.haversine(
            lat1 = -6.88920,
            lon1 = 109.38130,
            lat2 = -6.88920,
            lon2 = 109.38139
        )
        assertTrue(distance > 0f)
        assertTrue(distance < 20f)
    }
}
