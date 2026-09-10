package com.pemalang.roaddamage.domain

import android.content.Context
import android.util.Log
import org.json.JSONObject

data class ModelThresholds(
    val potholeThreshold: Float,
    val speedBumpThreshold: Float,
    val potholeClassIndex: Int,
    val speedBumpClassIndex: Int,
    val nonEventClassIndex: Int
)

object ThresholdConfigReader {
    private const val TAG = "ThresholdConfigReader"
    private var config: ModelThresholds? = null

    fun getConfig(context: Context): ModelThresholds {
        if (config == null) {
            try {
                val jsonString = context.assets.open("cnn_1d_thresholds.json").bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)
                config = ModelThresholds(
                    potholeThreshold = json.getDouble("pothole_threshold").toFloat(),
                    speedBumpThreshold = json.getDouble("speed_bump_threshold").toFloat(),
                    potholeClassIndex = json.getInt("pothole_class_index"),
                    speedBumpClassIndex = json.getInt("speed_bump_class_index"),
                    nonEventClassIndex = json.getInt("non_event_class_index")
                )
                Log.d(TAG, "Loaded dynamic ML thresholds: $config")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load cnn_1d_thresholds.json, falling back to defaults", e)
                // Default values if JSON is missing or corrupted
                config = ModelThresholds(
                    potholeThreshold = 0.5f,
                    speedBumpThreshold = 0.5f,
                    potholeClassIndex = 1,
                    speedBumpClassIndex = 2,
                    nonEventClassIndex = 0
                )
            }
        }
        return config!!
    }
}
