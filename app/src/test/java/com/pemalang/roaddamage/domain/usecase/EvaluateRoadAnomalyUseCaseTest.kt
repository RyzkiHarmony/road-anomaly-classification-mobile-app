package com.pemalang.roaddamage.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluateRoadAnomalyUseCaseTest {

    private lateinit var evaluateRoadAnomalyUseCase: EvaluateRoadAnomalyUseCase

    @Before
    fun setUp() {
        evaluateRoadAnomalyUseCase = EvaluateRoadAnomalyUseCase()
    }

    @Test
    fun testGForceCalculation() {
        // Magnitude equal to STANDARD_GRAVITY (9.80665f) should result in 1.0G
        val result = evaluateRoadAnomalyUseCase(
            magnitudeMps2 = 9.80665f,
            thresholdG = 1.4f,
            lastTriggerTimeMs = 0L,
            cooldownMs = 2000L,
            currentTimeMs = 5000L
        )
        assertEquals(1.0f, result.gForce, 0.0001f)
    }

    @Test
    fun testTriggerWhenGForceExceedsThresholdAndCooldownElapsed() {
        // Magnitude = 19.6133f => gForce = 2.0G
        // Threshold = 1.4G. Cooldown is 2000ms. Time difference is 3000ms.
        // Should trigger
        val result = evaluateRoadAnomalyUseCase(
            magnitudeMps2 = 19.6133f,
            thresholdG = 1.4f,
            lastTriggerTimeMs = 1000L,
            cooldownMs = 2000L,
            currentTimeMs = 4000L
        )
        assertTrue(result.shouldTrigger)
        assertEquals(2.0f, result.gForce, 0.0001f)
    }

    @Test
    fun testNoTriggerWhenGForceIsLessThanThreshold() {
        // Magnitude = 9.80665f => gForce = 1.0G
        // Threshold = 1.4G. Cooldown is 2000ms. Time difference is 3000ms.
        // Should not trigger because gForce is below threshold.
        val result = evaluateRoadAnomalyUseCase(
            magnitudeMps2 = 9.80665f,
            thresholdG = 1.4f,
            lastTriggerTimeMs = 1000L,
            cooldownMs = 2000L,
            currentTimeMs = 4000L
        )
        assertFalse(result.shouldTrigger)
    }

    @Test
    fun testNoTriggerWhenGForceIsExactlyThreshold() {
        // Magnitude = 1.4 * 9.80665f => gForce = 1.4G
        // Threshold = 1.4G. Cooldown is 2000ms. Time difference is 3000ms.
        // Should not trigger because gForce must be strictly greater than thresholdG.
        val result = evaluateRoadAnomalyUseCase(
            magnitudeMps2 = 1.4f * 9.80665f,
            thresholdG = 1.4f,
            lastTriggerTimeMs = 1000L,
            cooldownMs = 2000L,
            currentTimeMs = 4000L
        )
        assertFalse(result.shouldTrigger)
    }

    @Test
    fun testNoTriggerWhenCooldownNotElapsed() {
        // Magnitude = 19.6133f => gForce = 2.0G
        // Threshold = 1.4G. Cooldown = 2000ms. Time difference = 1500ms.
        // Should not trigger because cooldown hasn't elapsed.
        val result = evaluateRoadAnomalyUseCase(
            magnitudeMps2 = 19.6133f,
            thresholdG = 1.4f,
            lastTriggerTimeMs = 1000L,
            cooldownMs = 2000L,
            currentTimeMs = 2500L
        )
        assertFalse(result.shouldTrigger)
    }

    @Test
    fun testNoTriggerWhenTimeDeltaIsExactlyCooldown() {
        // Magnitude = 19.6133f => gForce = 2.0G
        // Threshold = 1.4G. Cooldown = 2000ms. Time difference = 2000ms (exactly equal).
        // Since the check is (currentTimeMs - lastTriggerTimeMs) > cooldownMs, it should be false.
        val result = evaluateRoadAnomalyUseCase(
            magnitudeMps2 = 19.6133f,
            thresholdG = 1.4f,
            lastTriggerTimeMs = 1000L,
            cooldownMs = 2000L,
            currentTimeMs = 3000L
        )
        assertFalse(result.shouldTrigger)
    }

    @Test
    fun testTriggerWhenTimeDeltaIsOneMsMoreThanCooldown() {
        // Magnitude = 19.6133f => gForce = 2.0G
        // Threshold = 1.4G. Cooldown = 2000ms. Time difference = 2001ms.
        // Should trigger because cooldownElapsed becomes true.
        val result = evaluateRoadAnomalyUseCase(
            magnitudeMps2 = 19.6133f,
            thresholdG = 1.4f,
            lastTriggerTimeMs = 1000L,
            cooldownMs = 2000L,
            currentTimeMs = 3001L
        )
        assertTrue(result.shouldTrigger)
    }

    @Test
    fun testNegativeMagnitude() {
        // Magnitude = -9.80665f => gForce = -1.0G
        // Threshold = 1.4G.
        // Should not trigger.
        val result = evaluateRoadAnomalyUseCase(
            magnitudeMps2 = -9.80665f,
            thresholdG = 1.4f,
            lastTriggerTimeMs = 1000L,
            cooldownMs = 2000L,
            currentTimeMs = 4000L
        )
        assertEquals(-1.0f, result.gForce, 0.0001f)
        assertFalse(result.shouldTrigger)
    }
}
