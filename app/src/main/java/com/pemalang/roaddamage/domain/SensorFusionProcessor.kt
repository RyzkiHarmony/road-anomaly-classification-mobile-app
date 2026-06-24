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

        // Computed scaler parameters from Python training (scaler_params.json)
        private val MEANS = floatArrayOf(
            -0.60148597f,   // a_vertical
            17.245502f,     // a_horizontal
            7.730897f,      // speed
            1.6059954f,     // a_vertical_crest_factor
            -0.0037311495f, // a_vertical_jerk
            0.012785485f,   // gx
            -0.0016510552f, // gy
            -5.2315627e-4f, // gz
            -0.012775812f,  // g_roll_accel
            0.0049647455f,  // g_pitch_accel
            1.7363335f,     // a_vertical_rms
            0.09711277f,    // a_vertical_zcr
            17.617577f,     // a_horizontal_rms
            0.10290575f     // energy_ratio_vh
        )

        private val STDS = floatArrayOf(
            2.0046096f,
            9.085543f,
            3.5719802f,
            0.26944867f,
            67.18712f,
            0.77854604f,
            0.36465223f,
            0.42083147f,
            105.086365f,
            46.3063f,
            1.1659465f,
            0.06588201f,
            8.334107f,
            0.055498246f
        )
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

            // 11. a_vertical_rms & 13. a_horizontal_rms (rolling window size 20, center=True)
            val aVertRms = FloatArray(WINDOW_SIZE)
            val aHorizRms = FloatArray(WINDOW_SIZE)
            for (i in 0 until WINDOW_SIZE) {
                val start = maxOf(0, i - 10)
                val end = minOf(WINDOW_SIZE - 1, i + 9)
                var sumSqVert = 0f
                var sumSqHoriz = 0f
                var count = 0
                for (k in start..end) {
                    sumSqVert += aVertBuffer[k] * aVertBuffer[k]
                    sumSqHoriz += aHorizBuffer[k] * aHorizBuffer[k]
                    count++
                }
                aVertRms[i] = sqrt(sumSqVert / count)
                aHorizRms[i] = sqrt(sumSqHoriz / count)
            }

            // 12. a_vertical_zcr (rolling window size 20, center=True)
            val signChanges = FloatArray(WINDOW_SIZE)
            signChanges[0] = 0f
            for (i in 1 until WINDOW_SIZE) {
                val s1 = if (aVertBuffer[i] > 0f) 1 else if (aVertBuffer[i] < 0f) -1 else 0
                val s0 = if (aVertBuffer[i - 1] > 0f) 1 else if (aVertBuffer[i - 1] < 0f) -1 else 0
                signChanges[i] = if (s1 != s0) 1f else 0f
            }
            val aVertZcr = FloatArray(WINDOW_SIZE)
            for (i in 0 until WINDOW_SIZE) {
                val start = maxOf(0, i - 10)
                val end = minOf(WINDOW_SIZE - 1, i + 9)
                var sumVal = 0f
                var count = 0
                for (k in start..end) {
                    sumVal += signChanges[k]
                    count++
                }
                aVertZcr[i] = sumVal / count
            }

            // 14. energy_ratio_vh
            val energyRatioVh = FloatArray(WINDOW_SIZE)
            for (i in 0 until WINDOW_SIZE) {
                energyRatioVh[i] = aVertRms[i] / (aHorizRms[i] + 1e-6f)
            }

            // Siapkan Tensor shape [1, 14, 200] = 2800 flat floats
            val tensorData = FloatArray(14 * WINDOW_SIZE)
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
            System.arraycopy(aVertRms, 0, tensorData, 10 * WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(aVertZcr, 0, tensorData, 11 * WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(aHorizRms, 0, tensorData, 12 * WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(energyRatioVh, 0, tensorData, 13 * WINDOW_SIZE, WINDOW_SIZE)

            // Z-Score Standardize per channel before passing to model
            for (c in 0 until 14) {
                val mean = MEANS[c]
                val std = STDS[c]
                val offset = c * WINDOW_SIZE
                for (i in 0 until WINDOW_SIZE) {
                    tensorData[offset + i] = (tensorData[offset + i] - mean) / std
                }
            }
            
            // Log sample values sebelum emit
            Log.d(TAG, ">>> TRIGGER INFERENCE (Z-SCALED) | aVertScaled[0..2]=[${tensorData[0]}, ${tensorData[1]}, ${tensorData[2]}] | speedScaled[0]=${tensorData[2*WINDOW_SIZE]}")
            
            // Trigger Inference
            val emitted = _inferenceTrigger.tryEmit(tensorData)
            Log.d(TAG, ">>> tryEmit result: $emitted (subscribers=${_inferenceTrigger.subscriptionCount.value})")
        }
    }
}
