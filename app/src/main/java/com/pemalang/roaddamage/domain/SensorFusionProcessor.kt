package com.pemalang.roaddamage.domain

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Menerima data dari Linear Acceleration dan Gravity, melakukan fusi,
 * dan menyimpan dalam Ring Buffer berukuran [WINDOW_SIZE].
 * Jika buffer penuh, akan menembakkan event untuk inferensi model.
 */
class SensorFusionProcessor {

    companion object {
        private const val TAG = "SensorFusion"
        const val WINDOW_SIZE = 200
        const val STRIDE = 50 // inferensi setiap 0.5 detik
    }

    // Buffers for sliding window channels (size 200)
    private val aVertBuffer = FloatArray(WINDOW_SIZE)
    private val aHorizBuffer = FloatArray(WINDOW_SIZE)
    private val speedBuffer = FloatArray(WINDOW_SIZE)
    private val gxBuffer = FloatArray(WINDOW_SIZE)
    private val gyBuffer = FloatArray(WINDOW_SIZE)
    private val gzBuffer = FloatArray(WINDOW_SIZE)
    private val tsBuffer = LongArray(WINDOW_SIZE)

    private var bufferIndex = 0
    private var samplesSinceLastInference = 0

    // Latest states
    private var lastGravity = floatArrayOf(0f, 0f, 9.8f)
    private var lastGyro = floatArrayOf(0f, 0f, 0f)
    private var lastSpeed = 0f

    // Filters to match Python's scipy.signal.butter 6.0Hz cutoff
    private val filterVertical = ButterworthFilter()
    private val filterHorizontal = ButterworthFilter()

    // Flow untuk mengirim data yang sudah matang ke OnnxModelRunner
    private val _inferenceTrigger = MutableSharedFlow<FloatArray>(
        extraBufferCapacity = 10,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val inferenceTrigger = _inferenceTrigger.asSharedFlow()

    fun updateGravity(gx: Float, gy: Float, gz: Float) {
        lastGravity[0] = gx
        lastGravity[1] = gy
        lastGravity[2] = gz
        // Log hanya sekali setiap 200 sample agar tidak spam
        if (bufferIndex == 1) {
            Log.d(TAG, "Gravity updated: [$gx, $gy, $gz]")
        }
    }

    fun updateGyro(gx: Float, gy: Float, gz: Float) {
        lastGyro[0] = gx
        lastGyro[1] = gy
        lastGyro[2] = gz
    }

    fun updateSpeed(speed: Float) {
        if (speed != lastSpeed) {
            Log.d(TAG, "Speed updated: $speed m/s")
        }
        lastSpeed = speed
    }

    fun processLinearAcceleration(linAx: Float, linAy: Float, linAz: Float, timestamp: Long) {
        // Log setiap 50 sample agar tidak terlalu spam
        if (bufferIndex % 50 == 0 || samplesSinceLastInference % 50 == 0) {
            Log.d(TAG, "processLinAccel called | bufferIdx=$bufferIndex | samplesSinceInf=$samplesSinceLastInference | speed=$lastSpeed")
        }
        val (gx, gy, gz) = lastGravity
        
        // 1. Normalize Gravity
        var gMag = sqrt(gx * gx + gy * gy + gz * gz)
        if (gMag == 0f) gMag = 1f
        val gxNorm = gx / gMag
        val gyNorm = gy / gMag
        val gzNorm = gz / gMag

        // 2. Project linear acceleration onto gravity vector
        val aVertical = linAx * gxNorm + linAy * gyNorm + linAz * gzNorm

        // 3. Horizontal acceleration magnitude
        val linMagSq = linAx * linAx + linAy * linAy + linAz * linAz
        val aHorizSq = max(0f, linMagSq - aVertical * aVertical)
        val aHorizontal = sqrt(aHorizSq)

        // 4. Filter sinyal untuk menghilangkan high-frequency noise (Denoising)
        val aVerticalFiltered = filterVertical.filter(aVertical)
        val aHorizontalFiltered = filterHorizontal.filter(aHorizontal)

        // 5. Masukkan ke Ring Buffer
        if (bufferIndex < WINDOW_SIZE) {
            aVertBuffer[bufferIndex] = aVerticalFiltered
            aHorizBuffer[bufferIndex] = aHorizontalFiltered
            speedBuffer[bufferIndex] = lastSpeed
            gxBuffer[bufferIndex] = lastGyro[0]
            gyBuffer[bufferIndex] = lastGyro[1]
            gzBuffer[bufferIndex] = lastGyro[2]
            tsBuffer[bufferIndex] = timestamp
            bufferIndex++
            samplesSinceLastInference++
        } else {
            // Shift left
            System.arraycopy(aVertBuffer, 1, aVertBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(aHorizBuffer, 1, aHorizBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(speedBuffer, 1, speedBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(gxBuffer, 1, gxBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(gyBuffer, 1, gyBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(gzBuffer, 1, gzBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(tsBuffer, 1, tsBuffer, 0, WINDOW_SIZE - 1)
            
            aVertBuffer[WINDOW_SIZE - 1] = aVerticalFiltered
            aHorizBuffer[WINDOW_SIZE - 1] = aHorizontalFiltered
            speedBuffer[WINDOW_SIZE - 1] = lastSpeed
            gxBuffer[WINDOW_SIZE - 1] = lastGyro[0]
            gyBuffer[WINDOW_SIZE - 1] = lastGyro[1]
            gzBuffer[WINDOW_SIZE - 1] = lastGyro[2]
            tsBuffer[WINDOW_SIZE - 1] = timestamp
            
            samplesSinceLastInference++
        }

        // 5. Trigger Inference
        if (bufferIndex == WINDOW_SIZE && samplesSinceLastInference >= STRIDE) {
            samplesSinceLastInference = 0
            
            // Retrospective calculation of complex rolling features to match Python exactly:
            // 4. Crest Factor (rolling window size 10, center=True)
            val crestFactor = FloatArray(WINDOW_SIZE)
            for (i in 0 until WINDOW_SIZE) {
                val start = maxOf(0, i - 5)
                val end = minOf(WINDOW_SIZE - 1, i + 4)
                var maxVal = 0f
                var sumSq = 0f
                var count = 0
                for (k in start..end) {
                    val v = aVertBuffer[k]
                    val absV = if (v < 0f) -v else v
                    if (absV > maxVal) maxVal = absV
                    sumSq += v * v
                    count++
                }
                val rms = sqrt(sumSq / count)
                crestFactor[i] = maxVal / (rms + 1e-6f)
            }

            // 5. Jerk and 9-10. Gyro derivatives (Roll / Pitch acceleration)
            val jerk = FloatArray(WINDOW_SIZE)
            val rollAccel = FloatArray(WINDOW_SIZE)
            val pitchAccel = FloatArray(WINDOW_SIZE)
            
            jerk[0] = 0f
            rollAccel[0] = 0f
            pitchAccel[0] = 0f
            
            for (i in 1 until WINDOW_SIZE) {
                var dt = (tsBuffer[i] - tsBuffer[i - 1]) / 1000f
                if (dt <= 0f) dt = 0.01f
                
                jerk[i] = (aVertBuffer[i] - aVertBuffer[i - 1]) / dt
                rollAccel[i] = (gxBuffer[i] - gxBuffer[i - 1]) / dt
                pitchAccel[i] = (gyBuffer[i] - gyBuffer[i - 1]) / dt
            }

            // Siapkan Tensor shape [1, 10, 200] = 2000 flat floats
            val tensorData = FloatArray(10 * WINDOW_SIZE)
            System.arraycopy(aVertBuffer, 0, tensorData, 0, WINDOW_SIZE)
            System.arraycopy(aHorizBuffer, 0, tensorData, WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(speedBuffer, 0, tensorData, 2 * WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(crestFactor, 0, tensorData, 3 * WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(jerk, 0, tensorData, 4 * WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(gxBuffer, 0, tensorData, 5 * WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(gyBuffer, 0, tensorData, 6 * WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(gzBuffer, 0, tensorData, 7 * WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(rollAccel, 0, tensorData, 8 * WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(pitchAccel, 0, tensorData, 9 * WINDOW_SIZE, WINDOW_SIZE)
            
            // Log sample values sebelum emit
            Log.d(TAG, ">>> TRIGGER INFERENCE | aVert[0..2]=[${aVertBuffer[0]}, ${aVertBuffer[1]}, ${aVertBuffer[2]}] | speed[0]=${speedBuffer[0]}")
            
            // Trigger Inference
            val emitted = _inferenceTrigger.tryEmit(tensorData)
            Log.d(TAG, ">>> tryEmit result: $emitted (subscribers=${_inferenceTrigger.subscriptionCount.value})")
        }
    }
}
