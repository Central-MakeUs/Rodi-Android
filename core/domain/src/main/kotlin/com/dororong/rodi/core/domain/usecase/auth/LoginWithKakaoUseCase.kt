package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.auth.LoginResult
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import com.dororong.rodi.core.domain.usecase.onboarding.SyncPendingOnboardingUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.isReadyForSubmission
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class LoginWithKakaoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val onboardingRepository: OnboardingRepository,
    private val entryRepository: EntryRepository,
    private val syncPendingOnboarding: SyncPendingOnboardingUseCase,
) {
    suspend operator fun invoke(kakaoAccessToken: String): Result<LoginResult> =
        runSuspendCatching {
            val result = authRepository.loginWithKakao(kakaoAccessToken)
            if (result is LoginResult.Success) {
                val profile = onboardingRepository.profile.first().copy(nickname = result.nickname)
                val hasCompletedGuestOnboarding = entryRepository.isCompleted.first() == true &&
                    entryRepository.hasGuestAccess.first()
                if (hasCompletedGuestOnboarding && profile.isReadyForSubmission) {
                    onboardingRepository.savePendingProfile(profile)
                } else {
                    onboardingRepository.saveProfile(profile)
                }
                if (!result.isNewMember) entryRepository.setCompleted()
                entryRepository.clearGuestAccess()
                val canSyncPendingProfile = if (result.isNewMember) {
                    onboardingRepository.authorizeSync()
                    true
                } else {
                    onboardingRepository.isSyncAuthorized.first()
                }
                if (canSyncPendingProfile) attemptPendingSync() else onboardingRepository.clearSyncPending()
            }
            result
        }

    private suspend fun attemptPendingSync() {
        try {
            syncPendingOnboarding()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
        }
    }
}
