package com.pemalang.roaddamage.data.remote

import com.google.gson.annotations.SerializedName

data class LocationDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("address") val address: String? = null
)

data class AnomalyContextRequest(
    @SerializedName("anomaly_id") val anomalyId: String,
    @SerializedName("type") val type: String,
    @SerializedName("location") val location: LocationDto,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("sensor_features") val sensorFeatures: Map<String, Float>? = null
)

data class RecommendationItem(
    @SerializedName("icon") val icon: String = "info",
    @SerializedName("label") val label: String,
    @SerializedName("action") val action: String
)

data class ContextStats(
    @SerializedName("similar_cases") val similarCases: Int = 0,
    @SerializedName("location_hotspot") val locationHotspot: Boolean = false,
    @SerializedName("detection_count_90d") val detectionCount90d: Int = 0,
    @SerializedName("avg_confidence_similar") val avgConfidenceSimilar: Float = 0f,
    @SerializedName("trend") val trend: String = "stable",
    @SerializedName("days_since_last_repair") val daysSinceLastRepair: Int? = null,
    @SerializedName("priority_rank") val priorityRank: Int = 3
)

data class AnomalyContextResponse(
    @SerializedName("status") val status: String = "success",
    @SerializedName("severity") val severity: String = "MEDIUM", // HIGH, MEDIUM, LOW
    @SerializedName("summary") val summary: String = "",
    @SerializedName("confidence_assessment") val confidenceAssessment: String = "",
    @SerializedName("recommendations") val recommendations: List<RecommendationItem> = emptyList(),
    @SerializedName("stats") val stats: ContextStats = ContextStats(),
    @SerializedName("cache_ttl") val cacheTtl: Long = 3600L
)
