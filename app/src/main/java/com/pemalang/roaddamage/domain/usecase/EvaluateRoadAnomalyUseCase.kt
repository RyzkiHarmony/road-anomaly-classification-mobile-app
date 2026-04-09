package com.pemalang.roaddamage.domain.usecase

import javax.inject.Inject

/**
 * Result of evaluating a single accelerometer reading for road anomaly detection.
 *
 * @property gForce      The magnitude converted to G-Force scale.
 * @property shouldTrigger Whether this reading qualifies as a road anomaly
 *                         (exceeds threshold AND cooldown period has elapsed).
 */
data class AnomalyResult(
    val gForce: Float,
    val shouldTrigger: Boolean
)

/**
 * Pure domain-layer Use Case that decides if a sensor reading represents
 * a road surface anomaly (pothole / speed-bump).
 *
 * This class is **stateless** — cooldown tracking and timing are owned by
 * the caller (typically [RecordingService]).  Keeping it stateless makes it
 * trivially testable with plain JUnit.
 *
 * ### Conversion formula
 * `gForce = magnitudeMps2 / 9.80665`
 *
 * ### Trigger conditions (both must be true)
 * 1. `gForce > threshold`
 * 2. `currentTimeMs - lastTriggerTimeMs > cooldownMs`
 */
class EvaluateRoadAnomalyUseCase @Inject constructor() {

    companion object {
        /** Standard gravity constant in m/s². */
        const val STANDARD_GRAVITY = 9.80665f
    }

    /**
     * Evaluate a single accelerometer magnitude reading.
     *
     * @param magnitudeMps2    Raw magnitude in m/s² from the accelerometer.
     * @param thresholdG       User-configured sensitivity threshold in G.
     * @param lastTriggerTimeMs Timestamp (epoch ms) of the last accepted trigger.
     * @param cooldownMs       Minimum interval between two triggers (ms).
     * @param currentTimeMs    Current timestamp (epoch ms).
     * @return [AnomalyResult] containing the G-Force value and trigger decision.
     */
    operator fun invoke(
        magnitudeMps2: Float,
        thresholdG: Float,
        lastTriggerTimeMs: Long,
        cooldownMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): AnomalyResult {
        val gForce = magnitudeMps2 / STANDARD_GRAVITY
        val cooldownElapsed = (currentTimeMs - lastTriggerTimeMs) > cooldownMs
        return AnomalyResult(
            gForce = gForce,
            shouldTrigger = gForce > thresholdG && cooldownElapsed
        )
    }
}
