package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.auth.LoginResult
import com.dororong.rodi.core.domain.model.entry.EntryMode
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import com.dororong.rodi.core.domain.usecase.onboarding.SyncPendingOnboardingUseCase
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
            if (result !is LoginResult.Success) return@runSuspendCatching result
            val hasGuestAccess = entryRepository.hasGuestAccess.first()
            val localDelivery = if (result.isOnboarded) null else deliverLocallyCompletedOnboarding()
            val isOnboarded = result.isOnboarded || localDelivery == true
            val profile = if (!isOnboarded && hasGuestAccess) {
                onboardingRepository.clear()
                OnboardingProfile(nickname = result.nickname)
            } else {
                onboardingRepository.profile.first().copy(nickname = result.nickname)
            }
            onboardingRepository.saveProfile(profile)
            if (isOnboarded) {
                entryRepository.setCompleted()
            } else {
                entryRepository.start(
                    if (hasGuestAccess) EntryMode.GUEST_SIGN_UP else EntryMode.AUTHENTICATED,
                )
            }
            entryRepository.clearGuestAccess()
            val canSyncPendingProfile = if (!isOnboarded) {
                onboardingRepository.authorizeSync()
                true
            } else {
                onboardingRepository.isSyncAuthorized.first()
            }
            when {
                !isOnboarded && hasGuestAccess -> onboardingRepository.clearSyncPending()
                // 방금 한 번 보냈다. 실패했으면 대기로 남겨 다음 동기화 때 다시 보낸다.
                localDelivery != null -> Unit
                canSyncPendingProfile -> attemptPendingSync()
                else -> onboardingRepository.clearSyncPending()
            }
            result.copy(isOnboarded = isOnboarded)
        }

    // 이 기기에서 온보딩을 끝냈지만 서버 제출만 실패한 채 다시 로그인하면 서버는 아직 미완료로 본다.
    // 온보딩을 처음부터 다시 시키지 않고 남은 제출을 먼저 보내, 서버가 받으면 완료로 본다.
    // 보낼 것이 없으면 null, 보냈으면 서버가 받았는지를 돌려준다.
    private suspend fun deliverLocallyCompletedOnboarding(): Boolean? {
        if (entryRepository.isCompleted.first() != true || !onboardingRepository.isSyncPending.first()) return null
        val submission = try {
            syncPendingOnboarding()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            null
        }
        return submission == OnboardingSubmissionResult.Submitted ||
            submission == OnboardingSubmissionResult.AlreadyCompleted
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
