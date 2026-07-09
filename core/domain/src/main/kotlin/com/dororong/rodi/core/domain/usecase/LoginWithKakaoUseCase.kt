package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.AuthRepository
import com.dororong.rodi.core.domain.OnboardingProfile
import com.dororong.rodi.core.domain.OnboardingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LoginWithKakaoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(kakaoAccessToken: String): Result<Boolean> =
        runSuspendCatching {
            authRepository.loginWithKakao(
                kakaoAccessToken = kakaoAccessToken,
                onboardingProfile = onboardingRepository.profile.first().takeIf { it.hasDraft },
            )
        }
}

private val OnboardingProfile.hasDraft: Boolean
    get() = nickname.isNotBlank() ||
        drivingPeriod != null ||
        recentFrequency != null ||
        roadExperiences.isNotEmpty() ||
        soloDrivingRange != null ||
        soloParkingLevel != null ||
        practiceSituations.isNotEmpty() ||
        vehicleType != null ||
        goal.isNotBlank()
