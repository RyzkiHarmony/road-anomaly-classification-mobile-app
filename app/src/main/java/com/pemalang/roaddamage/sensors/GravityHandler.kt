package com.pemalang.roaddamage.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import com.pemalang.roaddamage.model.SensorEventData

class GravityHandler(
    private val sensorManager: SensorManager,
    private var samplingDelayUs: Int
) : SensorEventListener {

    val readings = MutableSharedFlow<SensorEventData>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var sensor: Sensor? = null
    private var currentDelay = samplingDelayUs

    fun start() {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val s = sensor ?: return
        val maxReportLatencyUs = 0
        sensorManager.registerListener(this, s, currentDelay, maxReportLatencyUs)
    }

    fun updateDelay(newDelayUs: Int) {
        if (newDelayUs == currentDelay) return
        currentDelay = newDelayUs
        val s = sensor ?: return
        sensorManager.unregisterListener(this)
        val maxReportLatencyUs = 0
        sensorManager.registerListener(this, s, currentDelay, maxReportLatencyUs)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        readings.tryEmit(SensorEventData(event.timestamp, floatArrayOf(event.values[0], event.values[1], event.values[2])))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
