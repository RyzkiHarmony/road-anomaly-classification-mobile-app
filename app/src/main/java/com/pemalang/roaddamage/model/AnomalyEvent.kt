package com.pemalang.roaddamage.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single anomaly detected by the ONNX model (e.g. Pothole or Speed Bump)
 * This is saved to SQLite for fast UI rendering on the Map.
 */
@Entity(tableName = "anomaly_events")
data class AnomalyEvent(
    @PrimaryKey val eventId: String = java.util.UUID.randomUUID().toString(),
    val tripId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val anomalyType: String, // "Pothole" or "Speed Bump"
    val confidence: Float // Probability from 0.51 to 1.0
)
