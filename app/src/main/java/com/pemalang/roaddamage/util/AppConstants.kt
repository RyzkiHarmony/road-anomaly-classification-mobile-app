package com.pemalang.roaddamage.util

import com.google.android.gms.maps.model.LatLng

/**
 * Centralized application constants for geographic targets, sensor parameters,
 * and system thresholds across the Pemalang Road Damage Detection system.
 */
object AppConstants {
    // ── Geographic Defaults (Kabupaten Pemalang, Jawa Tengah) ──
    val PEMALANG_CENTER = LatLng(-6.8950, 109.3800)
    const val DEFAULT_MAP_ZOOM = 12.5f
    const val REGION_NAME = "Kabupaten Pemalang"

    // ── Sensor Sampling & Processing ──
    const val TARGET_SAMPLING_RATE_HZ = 100.0f
    const val MIN_SPEED_THRESHOLD_KMH = 5.0f

    // ── Network & Cache ──
    const val NETWORK_TIMEOUT_MS = 5000L
    const val RAG_CACHE_TTL_MS = 3600000L // 1 Hour

    // ── Anomaly Display Labels ──
    const val LABEL_POTHOLE = "Lubang Jalan"
    const val LABEL_SPEED_BUMP = "Polisi Tidur"
}
