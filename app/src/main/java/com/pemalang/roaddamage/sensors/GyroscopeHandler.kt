package com.pemalang.roaddamage.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Handles Gyroscope sensor registration, lifecycle, and data emission.
 *
 * Emits raw rotational velocity (rad/s) as a [FloatArray] of `[x, y, z]`
 * via the [readings] SharedFlow.  The caller is responsible for calling
 * [start] and [stop] to manage the sensor lifecycle.
 *
 * Design note: mirrors [AccelerometerHandler] intentionally so both sensor
 * handlers share the same contract, making them easily interchangeable
 * or composable inside [RecordingService].
 *
 * @param sensorManager System [SensorManager] instance.
 * @param samplingDelayUs Desired sampling period in microseconds.
 */
class GyroscopeHandler(
    private val sensorManager: SensorManager,
    private var samplingDelayUs: Int
) : SensorEventListener {

    /**
     * Stream of gyroscope readings.  Each emission is a [FloatArray]
     * containing `[rotX, rotY, rotZ]` in rad/s.
     */
    val readings = MutableSharedFlow<FloatArray>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var sensor: Sensor? = null

    /**
     * Registers the gyroscope listener.  If the device lacks a gyroscope
     * sensor this method returns silently — [readings] will simply never emit,
     * and the consuming code fills the CSV columns with NaN.
     */
    fun start() {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val s = sensor ?: return
        // Best Practice: Use maxReportLatencyUs to enable hardware FIFO batching.
        // This allows the Application Processor to sleep while sensor collects data,
        // which is critical for motorcycle use where the phone stays on for long rides.
        val maxReportLatencyUs = 1_000_000 // 1 second
        sensorManager.registerListener(this, s, samplingDelayUs, maxReportLatencyUs)
    }

    /**
     * Dynamically change the sampling delay at runtime.
     * Re-registers the sensor listener with the new delay only if it actually changed.
     */
    fun updateDelay(newDelayUs: Int) {
        if (newDelayUs == samplingDelayUs) return
        samplingDelayUs = newDelayUs
        val s = sensor ?: return
        sensorManager.unregisterListener(this)
        val maxReportLatencyUs = 1_000_000
        sensorManager.registerListener(this, s, samplingDelayUs, maxReportLatencyUs)
    }

    /**
     * Unregisters the listener.  Safe to call even if [start] was never called
     * or the device has no gyroscope.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    // ── SensorEventListener ──────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        // Emit raw rotational velocity — no smoothing applied here.
        // Smoothing (if needed) should be done at the domain/ML layer,
        // keeping the sensor handler as a pure data source.
        readings.tryEmit(floatArrayOf(event.values[0], event.values[1], event.values[2]))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op: gyroscope accuracy changes are uncommon and not actionable here.
    }
}
