package com.pemalang.roaddamage.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class LinearAccelerationHandler(
    private val sensorManager: SensorManager,
    private var samplingDelayUs: Int
) : SensorEventListener {

    val readings = MutableSharedFlow<FloatArray>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var sensor: Sensor? = null

    fun start() {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val s = sensor ?: return
        val maxReportLatencyUs = 0
        sensorManager.registerListener(this, s, samplingDelayUs, maxReportLatencyUs)
    }

    fun updateDelay(newDelayUs: Int) {
        if (newDelayUs == samplingDelayUs) return
        samplingDelayUs = newDelayUs
        val s = sensor ?: return
        sensorManager.unregisterListener(this)
        val maxReportLatencyUs = 0
        sensorManager.registerListener(this, s, samplingDelayUs, maxReportLatencyUs)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        readings.tryEmit(floatArrayOf(event.values[0], event.values[1], event.values[2]))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
