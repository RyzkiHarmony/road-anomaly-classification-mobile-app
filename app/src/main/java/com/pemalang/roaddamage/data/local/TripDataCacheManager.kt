package com.pemalang.roaddamage.data.local

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

data class CachedTripData(
    val points: List<Pair<Double, Double>>,
    val magnitudes: List<Float>,
    val verticalG: List<Float>,
    val avgSpeedKmH: Float = 0f,
    val maxSpeedKmH: Float = 0f,
    val avgSamplingRate: Float = 0f
)

@Singleton
class TripDataCacheManager @Inject constructor() {
    // Cache up to 10 trips to save memory (approx 1MB RAM total)
    private val cache = LruCache<String, CachedTripData>(10)

    fun get(tripId: String): CachedTripData? = cache.get(tripId)

    fun put(tripId: String, data: CachedTripData) {
        cache.put(tripId, data)
    }
    
    fun remove(tripId: String) {
        cache.remove(tripId)
    }
}
