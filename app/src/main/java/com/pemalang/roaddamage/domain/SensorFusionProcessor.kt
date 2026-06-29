package com.pemalang.roaddamage.domain

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Menerima data dari Linear Acceleration dan Gravity, melakukan resampling berbasis waktu (100Hz),
 * menjalankan fusi sensor, dan menyimpan dalam Ring Buffer berukuran [WINDOW_SIZE].
 * Jika buffer penuh, akan menembakkan event untuk inferensi model.
 */
class SensorFusionProcessor {

    private class RawReading(
        val timestamp: Long,
        val linAx: Float,
        val linAy: Float,
        val linAz: Float,
        val gx: Float,
        val gy: Float,
        val gz: Float,
        val speed: Float
    )

    companion object {
        private const val TAG = "SensorFusion"
        const val WINDOW_SIZE = 200
        const val STRIDE = 50 // inferensi setiap 0.5 detik

        // Toggle Z-Score normalization mode
        // TRUE: Instance-level (window-level) scaling untuk mereduksi skew keragaman kendaraan/holder.
        // FALSE: Global scaling (default) menggunakan MEANS dan STDS hasil training offline.
        // PENTING: Jika diatur TRUE, model offline juga harus dilatih menggunakan normalisasi per jendela.
        const val USE_INSTANCE_NORMALIZATION = true

        // Computed scaler parameters dari Python training (cnn_1d_scaler_params.json)
        private val MEANS = floatArrayOf(
            0.0001622676f,  // a_vertical
            0.015716485f,   // a_horizontal
            7.7308968f,     // speed
            1.6351699f,     // a_vertical_crest_factor
            -0.02136093f,   // a_vertical_jerk
            0.01278548f,    // gx
            -0.00165105f,   // gy
            -0.00052315f,   // gz
            -0.01277581f,   // g_roll_accel
            0.00496474f,    // g_pitch_accel
            1.4430078f,     // a_vertical_rms
            0.11783215f,    // a_vertical_zcr
            4.0556123f,     // a_horizontal_rms
            0.43539162f     // energy_ratio_vh
        )

        private val STDS = floatArrayOf(
            1.7957241f,     // a_vertical
            4.9882046f,     // a_horizontal
            3.5719800f,     // speed
            0.2567671f,     // a_vertical_crest_factor
            64.977662f,     // a_vertical_jerk
            0.7785460f,     // gx
            0.3646522f,     // gy
            0.4208314f,     // gz
            105.08636f,     // g_roll_accel
            46.306300f,     // g_pitch_accel
            1.0675629f,     // a_vertical_rms
            0.0587104f,     // a_vertical_zcr
            2.9060467f,     // a_horizontal_rms
            0.3047168f      // energy_ratio_vh
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

    // Queue for raw samples waiting for time-based resampling (100Hz)
    private val rawQueue = ArrayList<RawReading>()
    private var nextResampleTimestamp = -1L

    // Filters to match Python's scipy.signal.butter 6.0Hz cutoff
    private val filterVertical = ButterworthFilter()
    private val filterHorizontal = ButterworthFilter()

    // Bandpass filters to match Python's filters.py (1Hz - 20Hz)
    private val bandpassVertical = ButterworthBandpassFilter()
    private val bandpassHorizontal = ButterworthBandpassFilter()

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

    /**
     * Entry point utama saat data sensor mentah dikumpulkan.
     * Fungsi ini menyimpan data ke antrean rawQueue dan memproses resampling
     * linier berkala (setiap 10ms) untuk mengeliminasi jitter sensor bawaan Android.
     */
    fun processLinearAcceleration(linAx: Float, linAy: Float, linAz: Float, timestamp: Long) {
        val currentGyro = lastGyro
        rawQueue.add(RawReading(timestamp, linAx, linAy, linAz, currentGyro[0], currentGyro[1], currentGyro[2], lastSpeed))

        // Batasi ukuran antrean mentah (prune data lebih lama dari 4 detik)
        val cutoffTime = timestamp - 4000
        while (rawQueue.isNotEmpty() && rawQueue[0].timestamp < cutoffTime) {
            rawQueue.removeAt(0)
        }

        if (nextResampleTimestamp == -1L && rawQueue.isNotEmpty()) {
            nextResampleTimestamp = rawQueue[0].timestamp
        }

        val lastTimestamp = rawQueue.last().timestamp

        // Lakukan interpolasi linier untuk setiap tick 10ms (100Hz)
        while (lastTimestamp >= nextResampleTimestamp) {
            var r1: RawReading? = null
            var r2: RawReading? = null

            for (i in 0 until rawQueue.size - 1) {
                if (rawQueue[i].timestamp <= nextResampleTimestamp && rawQueue[i + 1].timestamp >= nextResampleTimestamp) {
                    r1 = rawQueue[i]
                    r2 = rawQueue[i + 1]
                    break
                }
            }

            if (r1 != null && r2 != null) {
                val t1 = r1.timestamp
                val t2 = r2.timestamp
                val diff = t2 - t1
                val fraction = if (diff > 0) (nextResampleTimestamp - t1).toFloat() / diff else 0f

                val linAxInterp = r1.linAx + fraction * (r2.linAx - r1.linAx)
                val linAyInterp = r1.linAy + fraction * (r2.linAy - r1.linAy)
                val linAzInterp = r1.linAz + fraction * (r2.linAz - r1.linAz)

                val gxInterp = r1.gx + fraction * (r2.gx - r1.gx)
                val gyInterp = r1.gy + fraction * (r2.gy - r1.gy)
                val gzInterp = r1.gz + fraction * (r2.gz - r1.gz)

                val speedInterp = r1.speed + fraction * (r2.speed - r1.speed)

                // Jalankan sensor fusion dan ring buffer pada data yang sudah di-resample
                processResampledData(linAxInterp, linAyInterp, linAzInterp, gxInterp, gyInterp, gzInterp, speedInterp, nextResampleTimestamp)
            }

            nextResampleTimestamp += 10L // 100Hz = interval 10ms
        }
    }

    private fun processResampledData(
        linAx: Float, linAy: Float, linAz: Float,
        gx: Float, gy: Float, gz: Float,
        speed: Float, timestamp: Long
    ) {
        val (grx, gry, grz) = lastGravity

        // 1. Normalize Gravity
        var gMag = sqrt(grx * grx + gry * gry + grz * grz)
        if (gMag == 0f) gMag = 1f
        val gxNorm = grx / gMag
        val gyNorm = gry / gMag
        val gzNorm = grz / gMag

        // 2. Project linear acceleration onto gravity vector to get vertical acceleration
        val aVertical = linAx * gxNorm + linAy * gyNorm + linAz * gzNorm

        // 3. Horizontal acceleration magnitude
        val linMagSq = linAx * linAx + linAy * linAy + linAz * linAz
        val aHorizSq = max(0f, linMagSq - aVertical * aVertical)
        val aHorizontal = sqrt(aHorizSq)

        // 4. Denoising (LPF 6Hz) & Bandpass (1Hz - 20Hz) IIR
        val aVerticalDenoised = filterVertical.filter(aVertical)
        val aHorizontalDenoised = filterHorizontal.filter(aHorizontal)

        val aVerticalFiltered = bandpassVertical.filter(aVerticalDenoised)
        val aHorizontalFiltered = bandpassHorizontal.filter(aHorizontalDenoised)

        // 5. Masukkan ke Ring Buffer
        if (bufferIndex < WINDOW_SIZE) {
            aVertBuffer[bufferIndex] = aVerticalFiltered
            aHorizBuffer[bufferIndex] = aHorizontalFiltered
            speedBuffer[bufferIndex] = speed
            gxBuffer[bufferIndex] = gx
            gyBuffer[bufferIndex] = gy
            gzBuffer[bufferIndex] = gz
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
            speedBuffer[WINDOW_SIZE - 1] = speed
            gxBuffer[WINDOW_SIZE - 1] = gx
            gyBuffer[WINDOW_SIZE - 1] = gy
            gzBuffer[WINDOW_SIZE - 1] = gz
            tsBuffer[WINDOW_SIZE - 1] = timestamp

            samplesSinceLastInference++
        }

        // 6. Trigger Inference
        if (bufferIndex == WINDOW_SIZE && samplesSinceLastInference >= STRIDE) {
            samplesSinceLastInference = 0
            runInferencePipeline()
        }
    }

    private fun runInferencePipeline() {
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

        // Detect if the device is stationary/static (e.g. sitting on a desk or stable holder)
        var maxVertRms = 0f
        for (v in aVertRms) {
            if (v > maxVertRms) maxVertRms = v
        }
        if (maxVertRms < 0.35f) {
            Log.d(TAG, ">>> INFERENCE SKIPPED: Device is static (maxVertRms = $maxVertRms < 0.35)")
            return
        }

        // Handle stationary or manual shaking test override
        val effectiveSpeedBuffer = FloatArray(WINDOW_SIZE)
        val currentSpeed = speedBuffer[WINDOW_SIZE - 1]
        if (currentSpeed < 1.0f) {
            for (i in 0 until WINDOW_SIZE) {
                effectiveSpeedBuffer[i] = 8.0f // Cruising speed override
            }
        } else {
            System.arraycopy(speedBuffer, 0, effectiveSpeedBuffer, 0, WINDOW_SIZE)
        }

        // Siapkan Tensor shape [1, 14, 200] = 2800 flat floats
        val tensorData = FloatArray(14 * WINDOW_SIZE)
        System.arraycopy(aVertBuffer, 0, tensorData, 0, WINDOW_SIZE)
        System.arraycopy(aHorizBuffer, 0, tensorData, WINDOW_SIZE, WINDOW_SIZE)
        System.arraycopy(effectiveSpeedBuffer, 0, tensorData, 2 * WINDOW_SIZE, WINDOW_SIZE)
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
            val offset = c * WINDOW_SIZE

            if (USE_INSTANCE_NORMALIZATION) {
                // Langkah 2: Instance-Level (Window-Level) Normalization
                var sum = 0f
                for (i in 0 until WINDOW_SIZE) {
                    sum += tensorData[offset + i]
                }
                val mean = sum / WINDOW_SIZE

                var sumSqDiff = 0f
                for (i in 0 until WINDOW_SIZE) {
                    val diff = tensorData[offset + i] - mean
                    sumSqDiff += diff * diff
                }
                val variance = sumSqDiff / WINDOW_SIZE
                val std = sqrt(variance)

                val eps = 1e-6f
                val divisor = if (std < eps) eps else std
                for (i in 0 until WINDOW_SIZE) {
                    tensorData[offset + i] = (tensorData[offset + i] - mean) / divisor
                }
            } else {
                // Global Scaling (Default)
                val mean = MEANS[c]
                val std = STDS[c]
                for (i in 0 until WINDOW_SIZE) {
                    tensorData[offset + i] = (tensorData[offset + i] - mean) / std
                }
            }
        }

        Log.d(TAG, ">>> TRIGGER INFERENCE (SCALED) | aVertScaled[0..2]=[${tensorData[0]}, ${tensorData[1]}, ${tensorData[2]}] | speedScaled[0]=${tensorData[2 * WINDOW_SIZE]}")

        val emitted = _inferenceTrigger.tryEmit(tensorData)
        Log.d(TAG, ">>> tryEmit result: $emitted (subscribers=${_inferenceTrigger.subscriptionCount.value})")
    }
}
