package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class RestoreWithKakaoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val onboardingRepository: OnboardingRepository,
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke(credential: String): Result<AccountRestoreResult> =
        runSuspendCatching {
            val result = authRepository.restoreWithKakao(credential)
            if (result is AccountRestoreResult.Restored) {
                onboardingRepository.saveProfile(
                    onboardingRepository.profile.first().copy(nickname = result.nickname),
                )
                onboardingRepository.clearSyncPending()
                if (!result.isNewMember) entryRepository.setCompleted()
                entryRepository.clearGuestAccess()
            }
            result
        }
}
