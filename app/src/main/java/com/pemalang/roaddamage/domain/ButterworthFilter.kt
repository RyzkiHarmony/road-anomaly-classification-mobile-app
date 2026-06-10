package com.pemalang.roaddamage.domain

/**
 * Butterworth Low-Pass Filter (2nd Order)
 * Matches Python's scipy.signal.butter(2, 6.0/(100/2), btype='low', analog=False)
 * 
 * Coefficients for 100Hz sampling rate and 6.0Hz cutoff:
 * b = [0.027859766, 0.055719532, 0.027859766]
 * a = [1.0, -1.475480443, 0.586919508]
 * 
 * Equation:
 * y[n] = b[0]*x[n] + b[1]*x[n-1] + b[2]*x[n-2] - a[1]*y[n-1] - a[2]*y[n-2]
 */
class ButterworthFilter {
    private val b0 = 0.027859766f
    private val b1 = 0.055719532f
    private val b2 = 0.027859766f
    
    private val a1 = -1.475480443f
    private val a2 = 0.586919508f

    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f
    private var isInitialized = false

    fun filter(value: Float): Float {
        // Handle start-up transients by initializing state to the first value
        if (!isInitialized) {
            x1 = value
            x2 = value
            y1 = value
            y2 = value
            isInitialized = true
        }

        val y = b0 * value + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2

        // Shift state
        x2 = x1
        x1 = value
        y2 = y1
        y1 = y

        return y
    }
}
