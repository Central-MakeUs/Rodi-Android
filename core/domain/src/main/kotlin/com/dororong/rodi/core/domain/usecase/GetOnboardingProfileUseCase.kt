package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.OnboardingProfile
import com.dororong.rodi.core.domain.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOnboardingProfileUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): Flow<OnboardingProfile> = onboardingRepository.profile
}
