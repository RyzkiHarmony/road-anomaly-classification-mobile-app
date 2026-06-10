package com.pemalang.roaddamage.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class SensorReading(
    val timestamp: Long,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val magnitude: Float,
    val gyroX: Float = Float.NaN,
    val gyroY: Float = Float.NaN,
    val gyroZ: Float = Float.NaN,
    val linearAccelX: Float = Float.NaN,
    val linearAccelY: Float = Float.NaN,
    val linearAccelZ: Float = Float.NaN,
    val gravityX: Float = Float.NaN,
    val gravityY: Float = Float.NaN,
    val gravityZ: Float = Float.NaN,
    val latitude: Double = Double.NaN,
    val longitude: Double = Double.NaN,
    val altitude: Double = Double.NaN,
    val speed: Float = Float.NaN,
    val accuracy: Float = Float.NaN,
    val bearing: Float = Float.NaN,
    // Model Predictions (Forward-Filled)
    var probNone: Float = 1.0f,
    var probPothole: Float = 0.0f,
    var probSpeedbump: Float = 0.0f
)

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey val tripId: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val distance: Float,
    val dataFilePath: String,
    val uploadStatus: UploadStatus,
    val createdAt: Long
)

enum class UploadStatus {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED
}

