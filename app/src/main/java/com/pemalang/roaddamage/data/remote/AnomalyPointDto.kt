package com.pemalang.roaddamage.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for road anomalies fetched from PostgreSQL backend.
 * Used by the Mobile GIS Hotspot Map.
 */
data class AnomalyPointDto(
    @SerializedName("id") val id: String,
    @SerializedName("tripId") val tripId: String,
    @SerializedName("type") val type: String, // "pothole" or "speedbump"
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("sensorMagnitude") val sensorMagnitude: Float = 0f,
    @SerializedName("detectedAt") val detectedAt: String? = null
) {
    val isPothole: Boolean
        get() = type.equals("pothole", ignoreCase = true)

    val displayName: String
        get() = if (isPothole) "Lubang Jalan (Pothole)" else "Polisi Tidur (Speed Bump)"
}
