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

        // Toggle Z-Score normalization mode
        // TRUE: Instance-level (window-level) scaling untuk mereduksi skew keragaman kendaraan/holder.
        // FALSE: Global scaling (default) menggunakan MEANS dan STDS hasil training offline.
        // PENTING: Jika diatur TRUE, model offline juga harus dilatih menggunakan normalisasi per jendela.
        const val USE_INSTANCE_NORMALIZATION = true

        // Computed scaler parameters dari Python training (cnn_1d_scaler_params.json)
        private val MEANS = floatArrayOf(
            0.0001622676f,  // 0: a_vertical
            0.015716485f,   // 1: a_horizontal
            7.7308968f,     // 2: speed
            1.6351699f,     // 3: a_vertical_crest_factor
            -0.02136093f,   // 4: a_vertical_jerk
            0.01278548f,    // 5: gx
            -0.00165105f,   // 6: gy
            -0.00052315f,   // 7: gz
            -0.01277581f,   // 8: g_roll_accel
            0.00496474f,    // 9: g_pitch_accel
            1.4430078f,     // 10: a_vertical_rms
            0.11783215f,    // 11: a_vertical_zcr
            4.0556123f,     // 12: a_horizontal_rms
            0.43539162f,    // 13: energy_ratio_vh
            0.0f,           // 14: lin_ax
            0.0f,           // 15: lin_ay
            0.0f,           // 16: lin_az
            0.0f            // 17: magnitude_deviation
        )

        private val STDS = floatArrayOf(
            1.7957241f,     // 0: a_vertical
            4.9882046f,     // 1: a_horizontal
            3.5719800f,     // 2: speed
            0.2567671f,     // 3: a_vertical_crest_factor
            64.977662f,     // 4: a_vertical_jerk
            0.7785460f,     // 5: gx
            0.3646522f,     // 6: gy
            0.4208314f,     // 7: gz
            105.08636f,     // 8: g_roll_accel
            46.306300f,     // 9: g_pitch_accel
            1.0675629f,     // 10: a_vertical_rms
            0.0587104f,     // 11: a_vertical_zcr
            2.9060467f,     // 12: a_horizontal_rms
            0.3047168f,     // 13: energy_ratio_vh
            1.0f,           // 14: lin_ax
            1.0f,           // 15: lin_ay
            1.0f,           // 16: lin_az
            1.0f            // 17: magnitude_deviation
        )
    }

    // Buffers for sliding window channels (size 200)
    private val aVertBuffer = FloatArray(WINDOW_SIZE)
    private val aHorizBuffer = FloatArray(WINDOW_SIZE)
    private val speedBuffer = FloatArray(WINDOW_SIZE)
    private val gxBuffer = FloatArray(WINDOW_SIZE)
    private val gyBuffer = FloatArray(WINDOW_SIZE)
    private val gzBuffer = FloatArray(WINDOW_SIZE)
    private val linAxBuffer = FloatArray(WINDOW_SIZE) 
    private val linAyBuffer = FloatArray(WINDOW_SIZE) 
    private val linAzBuffer = FloatArray(WINDOW_SIZE) 
    private val magDevBuffer = FloatArray(WINDOW_SIZE) 
    private val tsBuffer = LongArray(WINDOW_SIZE)

    private var bufferIndex = 0
    private var samplesSinceLastInference = 0

    // Queue for raw samples waiting for time-based resampling (100Hz)
    private val linAccelQueue = ArrayList<SensorEventData>()
    private val gyroQueue = ArrayList<SensorEventData>()
    private val gravityQueue = ArrayList<SensorEventData>()
    private var nextResampleTimestampNs = -1L

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
        if (linAccelQueue.isEmpty() || gyroQueue.isEmpty() || gravityQueue.isEmpty()) return

        if (nextResampleTimestampNs == -1L) {
            val tLin = linAccelQueue[0].timestampNs
            val tGyr = gyroQueue[0].timestampNs
            val tGra = gravityQueue[0].timestampNs
            nextResampleTimestampNs = maxOf(tLin, maxOf(tGyr, tGra))
        }

        while (true) {
            if (linAccelQueue.isEmpty() || gyroQueue.isEmpty() || gravityQueue.isEmpty()) break

            val lastLin = linAccelQueue.last().timestampNs
            val lastGyr = gyroQueue.last().timestampNs
            val lastGra = gravityQueue.last().timestampNs

            if (lastLin < nextResampleTimestampNs || lastGyr < nextResampleTimestampNs || lastGra < nextResampleTimestampNs) {
                break
            }

            val linInterp = interpolate(linAccelQueue, nextResampleTimestampNs)
            val gyrInterp = interpolate(gyroQueue, nextResampleTimestampNs)
            val graInterp = interpolate(gravityQueue, nextResampleTimestampNs)

            if (linInterp != null && gyrInterp != null && graInterp != null) {
                val cutoff = nextResampleTimestampNs - 4_000_000_000L
                pruneQueue(linAccelQueue, cutoff)
                pruneQueue(gyroQueue, cutoff)
                pruneQueue(gravityQueue, cutoff)

                val timestampMs = nextResampleTimestampNs / 1_000_000L

                val reading = SensorReading(
                    timestamp = timestampMs,
                    accelX = linInterp[0] + graInterp[0],
                    accelY = linInterp[1] + graInterp[1],
                    accelZ = linInterp[2] + graInterp[2],
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
                    linInterp[0], linInterp[1], linInterp[2],
                    gyrInterp[0], gyrInterp[1], gyrInterp[2],
                    graInterp[0], graInterp[1], graInterp[2],
                    lastSpeed, timestampMs
                )
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
        val fraction = if (diff > 0) (targetNs - t1).toFloat() / diff else 0f

        val res = FloatArray(r1.values.size)
        for (i in res.indices) {
            res[i] = r1.values[i] + fraction * (r2.values[i] - r1.values[i])
        }
        return res
    }

    private fun processResampledData(
        linAx: Float, linAy: Float, linAz: Float,
        gx: Float, gy: Float, gz: Float,
        grx: Float, gry: Float, grz: Float,
        speed: Float, timestamp: Long
    ) {

        // 1. Normalize Gravity
        var gMag = sqrt(grx * grx + gry * gry + grz * grz)
        if (gMag == 0f) gMag = 1f
        val gxNorm = grx / gMag
        val gyNorm = gry / gMag
        val gzNorm = grz / gMag

        // 2. Project linear acceleration onto gravity vector to get vertical acceleration
        val aVertical = linAx * gxNorm + linAy * gyNorm + linAz * gzNorm

        // 2. Magnitude Deviation dari 1G
        // ax = linAx + grx (total accelerometer = linear + gravity)
        val totalAx = linAx + grx
        val totalAy = linAy + gry
        val totalAz = linAz + grz
        val totalMag = sqrt(totalAx * totalAx + totalAy * totalAy + totalAz * totalAz)
        val magnitudeDeviation = totalMag - 9.81f

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
            linAxBuffer[bufferIndex] = linAx
            linAyBuffer[bufferIndex] = linAy
            linAzBuffer[bufferIndex] = linAz
            magDevBuffer[bufferIndex] = magnitudeDeviation
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
            System.arraycopy(linAxBuffer, 1, linAxBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(linAyBuffer, 1, linAyBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(linAzBuffer, 1, linAzBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(magDevBuffer, 1, magDevBuffer, 0, WINDOW_SIZE - 1)
            System.arraycopy(tsBuffer, 1, tsBuffer, 0, WINDOW_SIZE - 1)

            aVertBuffer[WINDOW_SIZE - 1] = aVerticalFiltered
            aHorizBuffer[WINDOW_SIZE - 1] = aHorizontalFiltered
            speedBuffer[WINDOW_SIZE - 1] = speed
            gxBuffer[WINDOW_SIZE - 1] = gx
            gyBuffer[WINDOW_SIZE - 1] = gy
            gzBuffer[WINDOW_SIZE - 1] = gz
            linAxBuffer[WINDOW_SIZE - 1] = linAx
            linAyBuffer[WINDOW_SIZE - 1] = linAy
            linAzBuffer[WINDOW_SIZE - 1] = linAz
            magDevBuffer[WINDOW_SIZE - 1] = magnitudeDeviation
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
        // Catat waktu mulai preprocessing
        lastPipelineStartTime = System.nanoTime()

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

        // Siapkan Tensor shape [1, 18, 200] = 3600 flat floats
        // Channel order HARUS identik dengan CHANNELS di build_cnn_data.py:
        // [0]=a_vertical, [1]=a_horizontal, [2]=speed, [3]=crest_factor, [4]=jerk,
        // [5]=gx, [6]=gy, [7]=gz, [8]=roll_accel, [9]=pitch_accel,
        // [10]=a_vert_rms, [11]=a_vert_zcr, [12]=a_horiz_rms, [13]=energy_ratio_vh,
        // [14]=lin_ax, [15]=lin_ay, [16]=lin_az, [17]=magnitude_deviation
        val tensorData = FloatArray(18 * WINDOW_SIZE)
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
        System.arraycopy(linAxBuffer, 0, tensorData, 14 * WINDOW_SIZE, WINDOW_SIZE)
        System.arraycopy(linAyBuffer, 0, tensorData, 15 * WINDOW_SIZE, WINDOW_SIZE)
        System.arraycopy(linAzBuffer, 0, tensorData, 16 * WINDOW_SIZE, WINDOW_SIZE)
        System.arraycopy(magDevBuffer, 0, tensorData, 17 * WINDOW_SIZE, WINDOW_SIZE)

        // Z-Score Standardize per channel before passing to model
        for (c in 0 until 18) {
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

        // Log.d(TAG, ">>> TRIGGER INFERENCE (SCALED) | aVertScaled[0..2]=[${tensorData[0]}, ${tensorData[1]}, ${tensorData[2]}] | speedScaled[0]=${tensorData[2 * WINDOW_SIZE]}")

        val emitted = _inferenceTrigger.tryEmit(tensorData)
        // Log.d(TAG, ">>> tryEmit result: $emitted (subscribers=${_inferenceTrigger.subscriptionCount.value})")
    }
}
