package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.usecase.onboarding.ClearOnboardingDataUseCase
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val clearOnboardingData: ClearOnboardingDataUseCase,
) {
    suspend operator fun invoke(): Result<Unit> =
        runSuspendCatching {
            authRepository.logout()
            clearOnboardingData()
        }
}
