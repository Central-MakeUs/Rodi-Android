package com.dororong.rodi.core.domain

interface OnboardingRepository {
    suspend fun saveProfile(profile: OnboardingProfile)
}
