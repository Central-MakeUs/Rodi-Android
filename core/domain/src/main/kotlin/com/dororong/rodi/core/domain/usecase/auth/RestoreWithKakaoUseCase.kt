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
    suspend operator fun invoke(credential: String): Result<AccountRestoreResult> {
        val restoreResult = runSuspendCatching { authRepository.restoreWithKakao(credential) }
        val restoredAccount = restoreResult.getOrNull() as? AccountRestoreResult.Restored
        if (restoredAccount != null) synchronizeLocalState(restoredAccount)
        return restoreResult
    }

    private suspend fun synchronizeLocalState(result: AccountRestoreResult.Restored) {
        attemptLocalUpdate {
            onboardingRepository.saveProfile(
                onboardingRepository.profile.first().copy(nickname = result.nickname),
            )
        }
        attemptLocalUpdate { onboardingRepository.clearSyncPending() }
        if (!result.isNewMember) {
            attemptLocalUpdate { entryRepository.setCompleted() }
        }
        attemptLocalUpdate { entryRepository.clearGuestAccess() }
    }

    private suspend fun attemptLocalUpdate(update: suspend () -> Unit) {
        try {
            update()
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Throwable) {
        }
    }
}
