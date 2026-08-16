package com.dororong.rodi.core.domain.usecase.driving

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DrivingReentryPolicyTest {
    @Test
    fun `does not show prompt before ten minutes`() {
        assertFalse(
            DrivingReentryPolicy.shouldShowUnmeasuredNavigationPrompt(
                launchedAtEpochMillis = 1_000L,
                nowEpochMillis = 1_000L + DRIVING_REENTRY_DELAY_MILLIS - 1,
            ),
        )
    }

    @Test
    fun `shows prompt at ten minutes`() {
        assertTrue(
            DrivingReentryPolicy.shouldShowUnmeasuredNavigationPrompt(
                launchedAtEpochMillis = 1_000L,
                nowEpochMillis = 1_000L + DRIVING_REENTRY_DELAY_MILLIS,
            ),
        )
    }
}
