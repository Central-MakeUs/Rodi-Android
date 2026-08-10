package com.dororong.rodi.feature.home.detail.components

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val ReviewDifficulty.label: String
    get() = when (this) {
        ReviewDifficulty.VERY_EASY -> "매우 쉬움"
        ReviewDifficulty.EASY -> "쉬움"
        ReviewDifficulty.NORMAL -> "보통"
        ReviewDifficulty.HARD -> "어려움"
        ReviewDifficulty.VERY_HARD -> "매우 어려움"
    }

internal val OnboardingLevel.displayName: String
    get() = when (this) {
        OnboardingLevel.SEED -> "Seed"
        OnboardingLevel.ROOKIE -> "Rookie"
        OnboardingLevel.OWNER -> "Owner"
        OnboardingLevel.EXPLORER -> "Explorer"
        OnboardingLevel.NAVIGATOR -> "Navigator"
    }

internal val PracticeMethod.label: String
    get() = when (this) {
        PracticeMethod.SOLO -> "혼자 연습"
        PracticeMethod.WITH_COMPANION -> "동승자와 연습"
    }

/**
 * 난이도 요약에 노출할 최빈 난이도. 동률이면 더 어려운 쪽을 우선한다.
 * [ReviewDifficulty.entries]가 쉬운 순이라 `last`가 곧 더 어려운 쪽이다.
 */
internal fun Map<ReviewDifficulty, Long>.topDifficulty(): ReviewDifficulty? {
    val max = values.maxOrNull() ?: return null
    if (max <= 0L) return null
    return ReviewDifficulty.entries.lastOrNull { this[it] == max }
}

private val ReviewDateFormatter = DateTimeFormatter.ofPattern("yy.MM.dd")

internal fun Instant.toReviewDateText(): String =
    atZone(ZoneId.systemDefault()).format(ReviewDateFormatter)
