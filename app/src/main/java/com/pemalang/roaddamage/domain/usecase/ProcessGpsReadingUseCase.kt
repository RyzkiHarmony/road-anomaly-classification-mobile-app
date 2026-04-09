package com.pemalang.roaddamage.domain.usecase

import com.pemalang.roaddamage.util.Distance
import javax.inject.Inject

/**
 * Result of processing a single GPS coordinate for distance accumulation.
 *
 * @property isValid       Whether the reading passed the accuracy filter.
 * @property distanceDelta Distance (meters) from the previous valid position,
 *                         or `0f` if this is the first valid point or the reading
 *                         was filtered out.
 */
data class GpsProcessingResult(
    val isValid: Boolean,
    val distanceDelta: Float
)

/**
 * Pure domain-layer Use Case that decides whether a GPS reading is accurate
 * enough to be accepted and, if so, calculates the incremental distance
 * from the previous accepted position using the Haversine formula.
 *
 * This class is **stateless** — the caller owns the "previous position" and
 * accumulated distance.  That makes it trivially testable with plain JUnit.
 *
 * ### Acceptance rule
 * A reading is accepted when `accuracy <= maxAccuracyMeters` and coordinates
 * are not `NaN`.
 */
class ProcessGpsReadingUseCase @Inject constructor() {

    /**
     * Evaluate a single GPS reading.
     *
     * @param latitude           Latitude from the reading (may be `NaN`).
     * @param longitude          Longitude from the reading (may be `NaN`).
     * @param accuracy           Reported accuracy in metres (may be `NaN`).
     * @param maxAccuracyMeters  Maximum acceptable accuracy (default 20 m).
     * @param prevLat            Previous accepted latitude, or `null` if none.
     * @param prevLon            Previous accepted longitude, or `null` if none.
     * @return [GpsProcessingResult] with validity flag and distance delta.
     */
    operator fun invoke(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        maxAccuracyMeters: Float = DEFAULT_MAX_ACCURACY,
        prevLat: Double?,
        prevLon: Double?
    ): GpsProcessingResult {
        // Filter out invalid or inaccurate readings
        if (latitude.isNaN() || longitude.isNaN() || accuracy.isNaN() || accuracy > maxAccuracyMeters) {
            return GpsProcessingResult(isValid = false, distanceDelta = 0f)
        }

        // First valid point — no distance yet
        if (prevLat == null || prevLon == null) {
            return GpsProcessingResult(isValid = true, distanceDelta = 0f)
        }

        // Calculate incremental distance using Haversine
        val delta = Distance.haversine(prevLat, prevLon, latitude, longitude)
        return GpsProcessingResult(isValid = true, distanceDelta = delta)
    }

    companion object {
        /** Default GPS accuracy threshold in metres. */
        const val DEFAULT_MAX_ACCURACY = 20.0f
    }
}
