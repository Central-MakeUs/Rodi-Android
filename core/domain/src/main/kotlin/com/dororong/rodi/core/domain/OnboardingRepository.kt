package com.dororong.rodi.core.domain

import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val profile: Flow<OnboardingProfile>
    suspend fun saveProfile(profile: OnboardingProfile)
}
