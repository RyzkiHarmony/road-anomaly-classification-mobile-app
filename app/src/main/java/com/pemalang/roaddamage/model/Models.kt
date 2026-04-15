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
    val latitude: Double = Double.NaN,
    val longitude: Double = Double.NaN,
    val altitude: Double = Double.NaN,
    val speed: Float = Float.NaN,
    val accuracy: Float = Float.NaN,
    val bearing: Float = Float.NaN
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

