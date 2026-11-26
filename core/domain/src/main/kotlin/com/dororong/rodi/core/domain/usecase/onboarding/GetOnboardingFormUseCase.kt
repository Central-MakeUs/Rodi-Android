package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.onboarding.OnboardingForm
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import javax.inject.Inject

class GetOnboardingFormUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(): Result<OnboardingForm?> = runSuspendCatching { onboardingRepository.getForm() }
}
