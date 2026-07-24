package com.dororong.rodi.core.domain.model.member

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel

data class MyPage(
    val nickname: String,
    val level: OnboardingLevel,
    val recommendationTags: List<String>,
    val drivingGoal: String?,
    val savedPlaceCount: Long,
)
