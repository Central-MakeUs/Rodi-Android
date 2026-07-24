package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import javax.inject.Inject

class ClearOnboardingDataUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke() {
        onboardingRepository.clear()
        entryRepository.clear()
    }
}
