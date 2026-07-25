package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

enum class OnboardingDataCleanupTarget {
    ONBOARDING,
    ENTRY,
}

class OnboardingDataCleanupException(
    val failedTargets: Set<OnboardingDataCleanupTarget>,
    cause: Throwable,
) : IllegalStateException("온보딩 로컬 정보 초기화에 실패했습니다: $failedTargets", cause)

class ClearOnboardingDataUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke() {
        var onboardingFailure: Throwable? = null
        var entryFailure: Throwable? = null

        try {
            onboardingRepository.clear()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            onboardingFailure = exception
        }

        try {
            entryRepository.clear()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            entryFailure = exception
        }

        if (onboardingFailure != null || entryFailure != null) {
            val failedTargets = buildSet {
                if (onboardingFailure != null) add(OnboardingDataCleanupTarget.ONBOARDING)
                if (entryFailure != null) add(OnboardingDataCleanupTarget.ENTRY)
            }
            val cause = onboardingFailure ?: entryFailure!!
            entryFailure?.takeIf { it !== cause }?.let(cause::addSuppressed)
            throw OnboardingDataCleanupException(failedTargets, cause)
        }
    }
}
