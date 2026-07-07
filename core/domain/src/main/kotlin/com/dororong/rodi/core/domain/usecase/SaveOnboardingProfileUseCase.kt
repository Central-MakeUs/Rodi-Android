package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.OnboardingProfile
import com.dororong.rodi.core.domain.OnboardingRepository
import javax.inject.Inject

class SaveOnboardingProfileUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(profile: OnboardingProfile) = onboardingRepository.saveProfile(profile)
}
