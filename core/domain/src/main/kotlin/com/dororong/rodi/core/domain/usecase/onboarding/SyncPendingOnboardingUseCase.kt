package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.calculateLevel
import com.dororong.rodi.core.domain.model.onboarding.isNavigatorLevel
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncPendingOnboardingUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(): OnboardingSubmissionResult? = syncMutex.withLock {
        if (!onboardingRepository.isSyncPending.first() || !onboardingRepository.isSyncAuthorized.first()) return null
        val profile = onboardingRepository.profile.first()
        if (!profile.isReadyForSubmission) return OnboardingSubmissionResult.InvalidProfile
        return onboardingRepository.submit(profile, profile.calculateLevel())
    }

    private companion object {
        val syncMutex = Mutex()
    }
}

val OnboardingProfile.isReadyForSubmission: Boolean
    get() = drivingPeriod != null && (
        drivingPeriod.isNavigatorLevel || (
            recentFrequency != null &&
                roadExperiences.isNotEmpty() &&
                (RoadExperience.SOLO !in roadExperiences ||
                    (soloDrivingRange != null && soloParkingLevel != null))
            )
        )
