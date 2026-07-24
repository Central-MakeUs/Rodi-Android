package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import javax.inject.Inject

class SaveOnboardingProfileUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(profile: OnboardingProfile) = onboardingRepository.saveProfile(profile)

    suspend fun saveForSubmission(profile: OnboardingProfile) = onboardingRepository.savePendingProfile(profile)

    suspend fun submit(profile: OnboardingProfile, level: OnboardingLevel): OnboardingSubmissionResult =
        onboardingRepository.submit(profile, level)
}
