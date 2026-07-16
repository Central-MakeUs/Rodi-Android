package com.dororong.rodi.core.domain.model.onboarding

sealed interface OnboardingSubmissionResult {
    data object Submitted : OnboardingSubmissionResult
    data object AlreadyCompleted : OnboardingSubmissionResult
    data object InvalidProfile : OnboardingSubmissionResult
    data object AuthenticationRequired : OnboardingSubmissionResult
    data object Forbidden : OnboardingSubmissionResult
    data object RateLimited : OnboardingSubmissionResult
    data object RetryableFailure : OnboardingSubmissionResult
    data object UnexpectedFailure : OnboardingSubmissionResult
}
