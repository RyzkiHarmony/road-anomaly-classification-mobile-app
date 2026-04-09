package com.pemalang.roaddamage.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "camera_events")
data class CameraEvent(
    @PrimaryKey val eventId: String,
    val tripId: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val imagePath: String,
    val triggerMagnitude: Float
)
