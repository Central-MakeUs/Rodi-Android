package com.dororong.rodi.core.domain.usecase.driving

const val DRIVING_REENTRY_DELAY_MILLIS = 10 * 60 * 1_000L

object DrivingReentryPolicy {
    fun shouldShowUnmeasuredNavigationPrompt(
        launchedAtEpochMillis: Long,
        nowEpochMillis: Long,
    ): Boolean = nowEpochMillis - launchedAtEpochMillis >= DRIVING_REENTRY_DELAY_MILLIS
}
