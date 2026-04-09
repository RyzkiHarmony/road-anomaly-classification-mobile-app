package com.pemalang.roaddamage.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class AccelerometerHandler(private val sensorManager: SensorManager, private var currentDelay: Int) :
        SensorEventListener {
    val readings =
            MutableSharedFlow<FloatArray>(
                    replay = 0,
                    extraBufferCapacity = 64,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
    private var sensor: Sensor? = null
    private var lastMag: Float = 0f
    private val alpha = 0.2f

    fun start() {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val s = sensor ?: return
        // Best Practice: Use maxReportLatencyUs to enable hardware FIFO batching.
        // This allows the AP to sleep while sensor collects data.
        // We buffer up to 1 second of data.
        val maxReportLatencyUs = 1_000_000 // 1 second
        sensorManager.registerListener(this, s, currentDelay, maxReportLatencyUs)
    }

    /**
     * Battery Optimization: dynamically change the sampling delay at runtime.
     * Re-registers the sensor listener with the new delay only if it actually changed.
     */
    fun updateDelay(newDelayUs: Int) {
        if (newDelayUs == currentDelay) return
        currentDelay = newDelayUs
        val s = sensor ?: return
        sensorManager.unregisterListener(this)
        val maxReportLatencyUs = 1_000_000
        sensorManager.registerListener(this, s, currentDelay, maxReportLatencyUs)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val rawMag = sqrt(x * x + y * y + z * z).toFloat()
        val m = alpha * lastMag + (1 - alpha) * rawMag
        lastMag = m
        readings.tryEmit(floatArrayOf(x, y, z, m))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
