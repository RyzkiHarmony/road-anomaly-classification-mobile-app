package com.pemalang.roaddamage.domain

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.max
import kotlin.math.sqrt
import com.pemalang.roaddamage.model.SensorEventData
import com.pemalang.roaddamage.model.SensorReading

/**
 * Menerima data dari Linear Acceleration dan Gravity, melakukan resampling berbasis waktu (100Hz),
 * menjalankan fusi sensor, dan menyimpan dalam Ring Buffer berukuran [WINDOW_SIZE].
 * Jika buffer penuh, akan menembakkan event untuk inferensi model.
 */
class SensorFusionProcessor {



    companion object {
        private const val TAG = "SensorFusion"
        const val WINDOW_SIZE = 200
        const val STRIDE = 50 // inferensi setiap 0.5 detik


    }

    // Buffers for sliding window channels (size 200)
    // Buffers for 13 raw channels
    private val speedBuffer = FloatArray(WINDOW_SIZE)
    private val axBuffer = FloatArray(WINDOW_SIZE)
    private val ayBuffer = FloatArray(WINDOW_SIZE)
    private val azBuffer = FloatArray(WINDOW_SIZE)
    private val gxBuffer = FloatArray(WINDOW_SIZE)
    private val gyBuffer = FloatArray(WINDOW_SIZE)
    private val gzBuffer = FloatArray(WINDOW_SIZE)
    private val linAxBuffer = FloatArray(WINDOW_SIZE) 
    private val linAyBuffer = FloatArray(WINDOW_SIZE) 
    private val linAzBuffer = FloatArray(WINDOW_SIZE)
    private val gravXBuffer = FloatArray(WINDOW_SIZE)
    private val gravYBuffer = FloatArray(WINDOW_SIZE)
    private val gravZBuffer = FloatArray(WINDOW_SIZE)
    private val tsBuffer = LongArray(WINDOW_SIZE)

    private var bufferIndex = 0
    private var samplesSinceLastInference = 0

    // Queue for raw samples waiting for time-based resampling (100Hz)
    private val accelQueue = ArrayList<SensorEventData>()
    private val linAccelQueue = ArrayList<SensorEventData>()
    private val gyroQueue = ArrayList<SensorEventData>()
    private val gravityQueue = ArrayList<SensorEventData>()
    private var nextResampleTimestampNs = -1L



    // Flow untuk mengirim data yang sudah matang ke OnnxModelRunner
    private val _inferenceTrigger = MutableSharedFlow<FloatArray>(
        extraBufferCapacity = 10,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val inferenceTrigger = _inferenceTrigger.asSharedFlow()

    // Flow untuk mengirim hasil fusi independen ke Recorder (CSV logging)
    private val _fusedSensorStream = MutableSharedFlow<SensorReading>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val fusedSensorStream = _fusedSensorStream.asSharedFlow()

    private var lastSpeed = 0f

    fun updateSpeed(speed: Float) {
        if (speed != lastSpeed) {
            // Log.d(TAG, "Speed updated: $speed m/s")
        }
        lastSpeed = speed
    }

    @Synchronized
    fun addAccel(data: SensorEventData) {
        accelQueue.add(data)
        processQueues()
    }

    @Synchronized
    fun addLinearAccel(data: SensorEventData) {
        linAccelQueue.add(data)
        processQueues()
    }

    @Synchronized
    fun addGyro(data: SensorEventData) {
        gyroQueue.add(data)
        processQueues()
    }

    @Synchronized
    fun addGravity(data: SensorEventData) {
        gravityQueue.add(data)
        processQueues()
    }

    private fun processQueues() {
        if (accelQueue.isEmpty() || linAccelQueue.isEmpty() || gyroQueue.isEmpty() || gravityQueue.isEmpty()) return

        if (nextResampleTimestampNs == -1L) {
            val tAcc = accelQueue[0].timestampNs
            val tLin = linAccelQueue[0].timestampNs
            val tGyr = gyroQueue[0].timestampNs
            val tGra = gravityQueue[0].timestampNs
            nextResampleTimestampNs = maxOf(tAcc, maxOf(tLin, maxOf(tGyr, tGra)))
        }

        while (true) {
            if (accelQueue.isEmpty() || linAccelQueue.isEmpty() || gyroQueue.isEmpty() || gravityQueue.isEmpty()) break

            val lastAcc = accelQueue.last().timestampNs
            val lastLin = linAccelQueue.last().timestampNs
            val lastGyr = gyroQueue.last().timestampNs
            val lastGra = gravityQueue.last().timestampNs

            if (lastAcc < nextResampleTimestampNs || lastLin < nextResampleTimestampNs || lastGyr < nextResampleTimestampNs || lastGra < nextResampleTimestampNs) {
                break
            }

            val accInterp = interpolate(accelQueue, nextResampleTimestampNs)
            val linInterp = interpolate(linAccelQueue, nextResampleTimestampNs)
            val gyrInterp = interpolate(gyroQueue, nextResampleTimestampNs)
            val graInterp = interpolate(gravityQueue, nextResampleTimestampNs)

            if (accInterp != null && linInterp != null && gyrInterp != null && graInterp != null) {
                if (accInterp.isEmpty() || linInterp.isEmpty() || gyrInterp.isEmpty() || graInterp.isEmpty()) {
                    // Gap > 50ms detected! Reset window buffer to prevent hallucinated predictions.
                    bufferIndex = 0
                    samplesSinceLastInference = 0
                } else {
                    val cutoff = nextResampleTimestampNs - 4_000_000_000L
                    pruneQueue(accelQueue, cutoff)
                    pruneQueue(linAccelQueue, cutoff)
                    pruneQueue(gyroQueue, cutoff)
                    pruneQueue(gravityQueue, cutoff)
    
                    val timestampMs = nextResampleTimestampNs / 1_000_000L
    
                    val reading = SensorReading(
                        timestamp = timestampMs,
                        accelX = accInterp[0],
                        accelY = accInterp[1],
                        accelZ = accInterp[2],
                        magnitude = 0f,
                        gyroX = gyrInterp[0],
                        gyroY = gyrInterp[1],
                        gyroZ = gyrInterp[2],
                        linearAccelX = linInterp[0],
                        linearAccelY = linInterp[1],
                        linearAccelZ = linInterp[2],
                        gravityX = graInterp[0],
                        gravityY = graInterp[1],
                        gravityZ = graInterp[2],
                        speed = lastSpeed
                    )
    
                    _fusedSensorStream.tryEmit(reading)
    
                    processResampledData(
                        accInterp[0], accInterp[1], accInterp[2],
                        linInterp[0], linInterp[1], linInterp[2],
                        gyrInterp[0], gyrInterp[1], gyrInterp[2],
                        graInterp[0], graInterp[1], graInterp[2],
                        lastSpeed, timestampMs
                    )
                }
            }

            nextResampleTimestampNs += 10_000_000L
        }
    }

    private fun pruneQueue(queue: ArrayList<SensorEventData>, cutoffNs: Long) {
        while (queue.isNotEmpty() && queue[0].timestampNs < cutoffNs) {
            queue.removeAt(0)
        }
    }

    private fun interpolate(queue: ArrayList<SensorEventData>, targetNs: Long): FloatArray? {
        var r1: SensorEventData? = null
        var r2: SensorEventData? = null

        for (i in 0 until queue.size - 1) {
            if (queue[i].timestampNs <= targetNs && queue[i + 1].timestampNs >= targetNs) {
                r1 = queue[i]
                r2 = queue[i + 1]
                break
            }
        }

        if (r1 == null || r2 == null) return null

        val t1 = r1.timestampNs
        val t2 = r2.timestampNs
        val diff = t2 - t1
        
        // PENTING: Limit interpolasi 50ms (50_000_000 Ns). Jika gap lebih besar, tolak resample ini.
        if (diff > 50_000_000L) {
            return FloatArray(0)
        }
        
        val fraction = if (diff > 0) (targetNs - t1).toFloat() / diff else 0f

        val res = FloatArray(r1.values.size)
        for (i in res.indices) {
            res[i] = r1.values[i] + fraction * (r2.values[i] - r1.values[i])
        }
        return res
    }

    private fun processResampledData(
        ax: Float, ay: Float, az: Float,
        linAx: Float, linAy: Float, linAz: Float,
        gx: Float, gy: Float, gz: Float,
        grx: Float, gry: Float, grz: Float,
        speed: Float, timestamp: Long
    ) {

        // Raw features from direct TYPE_ACCELEROMETER
        val totalAx = ax
        val totalAy = ay
        val totalAz = az

        // 5. Masukkan ke Ring Buffer
        if (bufferIndex < WINDOW_SIZE) {
            speedBuffer[bufferIndex] = speed
            axBuffer[bufferIndex] = totalAx
            ayBuffer[bufferIndex] = totalAy
            azBuffer[bufferIndex] = totalAz
            gxBuffer[bufferIndex] = gx
            gyBuffer[bufferIndex] = gy
            gzBuffer[bufferIndex] = gz
            linAxBuffer[bufferIndex] = linAx
            linAyBuffer[bufferIndex] = linAy
            linAzBuffer[bufferIndex] = linAz
            gravXBuffer[bufferIndex] = grx
            gravYBuffer[bufferIndex] = gry
            gravZBuffer[bufferIndex] = grz
            tsBuffer[bufferIndex] = timestamp
            bufferIndex++
            samplesSinceLastInference++
        } else {
            // Shift left
            System.arraycopy(speedBuffer, 1, speedBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(axBuffer, 1, axBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(ayBuffer, 1, ayBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(azBuffer, 1, azBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(gxBuffer, 1, gxBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(gyBuffer, 1, gyBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(gzBuffer, 1, gzBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(linAxBuffer, 1, linAxBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(linAyBuffer, 1, linAyBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(linAzBuffer, 1, linAzBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(gravXBuffer, 1, gravXBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(gravYBuffer, 1, gravYBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(gravZBuffer, 1, gravZBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(tsBuffer, 1, tsBuffer, 0, WINDOW_SIZE - 1)

            speedBuffer[WINDOW_SIZE - 1] = speed
            axBuffer[WINDOW_SIZE - 1] = totalAx
            ayBuffer[WINDOW_SIZE - 1] = totalAy
            azBuffer[WINDOW_SIZE - 1] = totalAz
            gxBuffer[WINDOW_SIZE - 1] = gx
            gyBuffer[WINDOW_SIZE - 1] = gy
            gzBuffer[WINDOW_SIZE - 1] = gz
            linAxBuffer[WINDOW_SIZE - 1] = linAx
            linAyBuffer[WINDOW_SIZE - 1] = linAy
            linAzBuffer[WINDOW_SIZE - 1] = linAz
            gravXBuffer[WINDOW_SIZE - 1] = grx
            gravYBuffer[WINDOW_SIZE - 1] = gry
            gravZBuffer[WINDOW_SIZE - 1] = grz
            tsBuffer[WINDOW_SIZE - 1] = timestamp

            samplesSinceLastInference++
        }

        // 6. Trigger Inference
        if (bufferIndex == WINDOW_SIZE && samplesSinceLastInference >= STRIDE) {
            samplesSinceLastInference = 0
            runInferencePipeline()
        }
    }

    var lastPipelineStartTime: Long = 0L

    private fun runInferencePipeline() {
        val currentSpeed = speedBuffer[WINDOW_SIZE - 1]
        
        // Hentikan eksekusi AI (Inference) sepenuhnya jika kecepatan lambat
        // Ini mencegah False Positive karena guncangan saat diam/berjalan sangat pelan
        if (currentSpeed < 1.0f) {
            // Log.d(TAG, "Speed is too low ($currentSpeed m/s). Skipping inference to save battery.")
            return
        }

        // Catat waktu mulai preprocessing
        lastPipelineStartTime = System.nanoTime()

        // Siapkan Tensor shape [1, 7, 200]
        val tensorData = FloatArray(7 * WINDOW_SIZE)
        System.arraycopy(speedBuffer, 0, tensorData, 0, WINDOW_SIZE)
        System.arraycopy(axBuffer, 0, tensorData, 1 * WINDOW_SIZE, WINDOW_SIZE)
        System.arraycopy(ayBuffer, 0, tensorData, 2 * WINDOW_SIZE, WINDOW_SIZE)
        System.arraycopy(azBuffer, 0, tensorData, 3 * WINDOW_SIZE, WINDOW_SIZE)
        System.arraycopy(gxBuffer, 0, tensorData, 4 * WINDOW_SIZE, WINDOW_SIZE)
        System.arraycopy(gyBuffer, 0, tensorData, 5 * WINDOW_SIZE, WINDOW_SIZE)
        System.arraycopy(gzBuffer, 0, tensorData, 6 * WINDOW_SIZE, WINDOW_SIZE)

        // Z-Score Standardization is now handled internally by the ONNX Model (MobileInferenceWrapper).
        // No manual scaling is needed here.

        // Log.d(TAG, ">>> TRIGGER INFERENCE (SCALED) | aVertScaled[0..2]=[${tensorData[0]}, ${tensorData[1]}, ${tensorData[2]}] | speedScaled[0]=${tensorData[2 * WINDOW_SIZE]}")

        val emitted = _inferenceTrigger.tryEmit(tensorData)
        // Log.d(TAG, ">>> tryEmit result: $emitted (subscribers=${_inferenceTrigger.subscriptionCount.value})")
    }
}
