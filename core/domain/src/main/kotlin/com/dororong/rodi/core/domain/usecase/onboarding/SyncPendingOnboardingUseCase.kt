package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.model.onboarding.conformTo
import com.dororong.rodi.core.domain.model.onboarding.answers
import com.dororong.rodi.core.domain.model.onboarding.calculateLevel
import com.dororong.rodi.core.domain.model.onboarding.hasValidRoadExperiences
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
        val form = onboardingRepository.getForm()
        val currentProfile = form?.let { profile.conformTo(it) } ?: profile
        if (!currentProfile.isReadyForSubmission) return OnboardingSubmissionResult.InvalidProfile
        if (form != null && form.steps.any { step ->
                val skipped = step.skippable && step.questions.all { currentProfile.answers(it.code).isEmpty() }
                !skipped && !form.isStepValid(step, currentProfile)
            }) return OnboardingSubmissionResult.InvalidProfile
        if (currentProfile != profile) onboardingRepository.savePendingProfile(currentProfile)
        return onboardingRepository.submit(currentProfile, currentProfile.calculateLevel())
    }

    private companion object {
        val syncMutex = Mutex()
    }
}

val OnboardingProfile.isReadyForSubmission: Boolean
    get() = drivingPeriod != null && hasValidRoadExperiences
