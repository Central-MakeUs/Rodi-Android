package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel

fun MyPageResponse.toDomain() = MyPage(
    nickname = nickname,
    level = runCatching { OnboardingLevel.valueOf(level) }
        .getOrElse { throw IllegalArgumentException("Unsupported onboarding level: $level") },
    recommendationTags = recommendationTags,
    drivingGoal = drivingGoal,
    savedPlaceCount = savedPlaceCount,
)
