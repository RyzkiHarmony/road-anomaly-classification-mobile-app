package com.pemalang.roaddamage.domain

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
        const val WINDOW_SIZE = 200
        const val STRIDE = 50 // inferensi setiap 0.5 detik
    }

    // Buffer [3][200]: [0]=a_vertical, [1]=a_horizontal, [2]=speed
    private val buffer = Array(3) { FloatArray(WINDOW_SIZE) }
    private var bufferIndex = 0
    private var samplesSinceLastInference = 0

    // Latest states
    private var lastGravity = floatArrayOf(0f, 0f, 9.8f)
    private var lastSpeed = 0f

    // Filters to match Python's scipy.signal.butter 6.0Hz cutoff
    private val filterVertical = ButterworthFilter()
    private val filterHorizontal = ButterworthFilter()

    // Flow untuk mengirim data yang sudah matang ke OnnxModelRunner
    private val _inferenceTrigger = MutableSharedFlow<FloatArray>(extraBufferCapacity = 1)
    val inferenceTrigger = _inferenceTrigger.asSharedFlow()

    fun updateGravity(gx: Float, gy: Float, gz: Float) {
        lastGravity[0] = gx
        lastGravity[1] = gy
        lastGravity[2] = gz
    }

    fun updateSpeed(speed: Float) {
        lastSpeed = speed
    }

    fun processLinearAcceleration(linAx: Float, linAy: Float, linAz: Float) {
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
            buffer[0][bufferIndex] = aVerticalFiltered
            buffer[1][bufferIndex] = aHorizontalFiltered
            buffer[2][bufferIndex] = lastSpeed
            bufferIndex++
            samplesSinceLastInference++
        } else {
            // Shift left
            System.arraycopy(buffer[0], 1, buffer[0], 0, WINDOW_SIZE - 1)
            System.arraycopy(buffer[1], 1, buffer[1], 0, WINDOW_SIZE - 1)
            System.arraycopy(buffer[2], 1, buffer[2], 0, WINDOW_SIZE - 1)
            
            buffer[0][WINDOW_SIZE - 1] = aVerticalFiltered
            buffer[1][WINDOW_SIZE - 1] = aHorizontalFiltered
            buffer[2][WINDOW_SIZE - 1] = lastSpeed
            
            samplesSinceLastInference++
        }

        // 5. Trigger Inference
        if (bufferIndex == WINDOW_SIZE && samplesSinceLastInference >= STRIDE) {
            samplesSinceLastInference = 0
            
            // Siapkan Tensor shape [1, 3, 200] = 600 flat floats
            val tensorData = FloatArray(3 * WINDOW_SIZE)
            System.arraycopy(buffer[0], 0, tensorData, 0, WINDOW_SIZE)
            System.arraycopy(buffer[1], 0, tensorData, WINDOW_SIZE, WINDOW_SIZE)
            System.arraycopy(buffer[2], 0, tensorData, 2 * WINDOW_SIZE, WINDOW_SIZE)
            
            _inferenceTrigger.tryEmit(tensorData)
        }
    }
}
