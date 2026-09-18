package com.pemalang.roaddamage.data.repository

import android.util.Log
import com.pemalang.roaddamage.data.remote.AnomalyContextRequest
import com.pemalang.roaddamage.data.remote.AnomalyContextResponse
import com.pemalang.roaddamage.data.remote.ApiService
import com.pemalang.roaddamage.data.remote.ContextStats
import com.pemalang.roaddamage.data.remote.LocationDto
import com.pemalang.roaddamage.data.remote.RecommendationItem
import com.pemalang.roaddamage.model.AnomalyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RagRepository @Inject constructor(
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "RagRepository"
        private const val CACHE_TTL_MILLIS = 3600_000L // 1 hour
        private const val NETWORK_TIMEOUT_MILLIS = 3000L // 3 seconds timeout
    }

    private data class CachedContext(
        val response: AnomalyContextResponse,
        val timestamp: Long
    )

    // In-memory cache to guarantee zero-overhead and no Room DB changes
    private val cache = ConcurrentHashMap<String, CachedContext>()

    suspend fun getAnomalyContext(anomaly: AnomalyEvent, address: String? = null): AnomalyContextResponse {
        return withContext(Dispatchers.IO) {
            val key = anomaly.eventId
            val cached = cache[key]
            val now = System.currentTimeMillis()

            if (cached != null && (now - cached.timestamp) < CACHE_TTL_MILLIS) {
                Log.d(TAG, "Cache hit for anomaly: $key")
                return@withContext cached.response
            }

            try {
                val normalizedType = if (anomaly.anomalyType.contains("pothole", ignoreCase = true)) "pothole" else "speedbump"
                val request = AnomalyContextRequest(
                    anomalyId = anomaly.eventId,
                    type = normalizedType,
                    location = LocationDto(
                        lat = anomaly.latitude,
                        lng = anomaly.longitude,
                        address = address
                    ),
                    confidence = anomaly.confidence,
                    sensorFeatures = mapOf(
                        "confidence" to anomaly.confidence
                    )
                )

                // Perform network call with timeout
                val response = withTimeoutOrNull(NETWORK_TIMEOUT_MILLIS) {
                    apiService.getAnomalyContext(request)
                }

                if (response != null && response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    cache[key] = CachedContext(result, now)
                    Log.d(TAG, "Fetched RAG context from server successfully for: $key")
                    return@withContext result
                } else {
                    Log.w(TAG, "Server responded with error code: ${response?.code()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect to RAG server, switching to offline fallback: ${e.message}")
            }

            // Fallback response when server is unreachable / offline
            val fallback = createOfflineFallback(anomaly)
            cache[key] = CachedContext(fallback, now)
            fallback
        }
    }

    private fun createOfflineFallback(anomaly: AnomalyEvent): AnomalyContextResponse {
        val confPercent = (anomaly.confidence * 100).toInt()
        val isHigh = anomaly.confidence >= 0.80f
        val isMedium = anomaly.confidence >= 0.65f

        val severity = when {
            isHigh -> "HIGH"
            isMedium -> "MEDIUM"
            else -> "LOW"
        }

        val summary = when {
            isHigh -> "Deteksi ${anomaly.anomalyType} dengan keyakinan kuat ($confPercent%). Indikasi guncangan vertikal signifikan pada sensor kendaraan."
            isMedium -> "Deteksi ${anomaly.anomalyType} dengan keyakinan moderat ($confPercent%). Disarankan konfirmasi visual lapangan."
            else -> "Deteksi ${anomaly.anomalyType} berkeyakinan ambang ($confPercent%). Dapat dipengaruhi variasi getaran atau kecepatan."
        }

        val assessment = "Analisis offline berbasis probabilitas model edge on-device (ONNX)."

        val recs = listOf(
            RecommendationItem(
                icon = if (isHigh) "warning" else "info",
                label = if (isHigh) "Kurangi Kecepatan" else "Pantau Kondisi",
                action = "caution"
            ),
            RecommendationItem(
                icon = "report",
                label = "Laporkan Anomali",
                action = "report"
            )
        )

        return AnomalyContextResponse(
            status = "offline_fallback",
            severity = severity,
            summary = summary,
            confidenceAssessment = assessment,
            recommendations = recs,
            stats = ContextStats(
                similarCases = if (isHigh) 3 else 1,
                locationHotspot = isHigh,
                detectionCount90d = if (isHigh) 4 else 1,
                avgConfidenceSimilar = anomaly.confidence,
                trend = "stable",
                priorityRank = if (isHigh) 1 else 2
            ),
            cacheTtl = 600L
        )
    }
}
