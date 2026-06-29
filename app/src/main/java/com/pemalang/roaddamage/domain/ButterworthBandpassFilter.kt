package com.pemalang.roaddamage.domain

/**
 * 2nd-order Butterworth Bandpass Filter (1Hz - 20Hz at 100Hz)
 * Matches Python's scipy.signal.butter(2, [1.0, 20.0]/(100/2), btype='bandpass', analog=False)
 */
class ButterworthBandpassFilter {
    private val b0 = 0.19061660009749753f
    private val b1 = 0.0f
    private val b2 = -0.38123320019499507f
    private val b3 = 0.0f
    private val b4 = 0.19061660009749753f

    private val a1 = -2.3350824020765124f
    private val a2 = 1.9509646897792021f
    private val a3 = -0.8192636853124013f
    private val a4 = 0.2066719851665643f

    private val xHist = FloatArray(4) // [n-1, n-2, n-3, n-4]
    private val yHist = FloatArray(4) // [n-1, n-2, n-3, n-4]

    fun filter(value: Float): Float {
        val yVal = (b0 * value + b1 * xHist[0] + b2 * xHist[1] + b3 * xHist[2] + b4 * xHist[3]
                - (a1 * yHist[0] + a2 * yHist[1] + a3 * yHist[2] + a4 * yHist[3]))

        // Shift history
        xHist[3] = xHist[2]
        xHist[2] = xHist[1]
        xHist[1] = xHist[0]
        xHist[0] = value

        yHist[3] = yHist[2]
        yHist[2] = yHist[1]
        yHist[1] = yHist[0]
        yHist[0] = yVal

        return yVal
    }
}
