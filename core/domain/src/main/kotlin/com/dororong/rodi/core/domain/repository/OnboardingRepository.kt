package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val profile: Flow<OnboardingProfile>
    suspend fun saveProfile(profile: OnboardingProfile)
    suspend fun submit(profile: OnboardingProfile, level: OnboardingLevel): OnboardingSubmissionResult
}
