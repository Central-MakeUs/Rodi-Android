package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOnboardingProfileUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): Flow<OnboardingProfile> = onboardingRepository.profile
}
