package com.dororong.rodi.feature.entry

import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.entry.EntryMode
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingAnalysisCopy
import com.dororong.rodi.core.domain.model.onboarding.PracticeSituation
import com.dororong.rodi.core.domain.model.onboarding.RecentDrivingFrequency
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.SoloDrivingRange
import com.dororong.rodi.core.domain.model.onboarding.SoloParkingLevel
import com.dororong.rodi.core.domain.model.onboarding.VehicleType
import com.dororong.rodi.core.domain.model.onboarding.OnboardingForm
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.localOnboardingForm

enum class EntryStep { TERMS, NICKNAME, CAREER, PREFERENCE, PRECAUTIONS, LOCATION, TERMS_WEBVIEW }

enum class OnboardingAnalysisState { ANALYZING, RESULT }

data class EntryUiState(
    val isRestored: Boolean = false,
    val didResumeProgress: Boolean = false,
    val mode: EntryMode = EntryMode.AUTHENTICATED,
    val step: EntryStep = EntryStep.TERMS,
    val webViewUrl: String = "",
    val serviceTermsChecked: Boolean = false,
    val privacyTermsChecked: Boolean = false,
    val locationTermsChecked: Boolean = false,
    val licenseChecked: Boolean = false,
    val companionChecked: Boolean = false,
    val precautionAgreementChecked: Boolean = false,
    val nickname: String = "",
    val drivingPeriod: DrivingPeriod? = null,
    val recentFrequency: RecentDrivingFrequency? = null,
    val roadExperiences: List<RoadExperience> = emptyList(),
    val soloDrivingRange: SoloDrivingRange? = null,
    val soloParkingLevel: SoloParkingLevel? = null,
    val practiceSituations: List<PracticeSituation> = emptyList(),
    val vehicleType: VehicleType? = null,
    val goal: String = "",
    val onboardingForm: OnboardingForm = localOnboardingForm(),
    val isOnboardingFormLoading: Boolean = false,
    val hasOnboardingFormError: Boolean = false,
    val isServerFormLoaded: Boolean = false,
    val onboardingLevel: OnboardingLevel? = null,
    val onboardingAnalysisCopy: OnboardingAnalysisCopy? = null,
    val onboardingAnalysisState: OnboardingAnalysisState? = null,
) {
    val profile: OnboardingProfile
        get() = OnboardingProfile(nickname, drivingPeriod, recentFrequency, roadExperiences,
            soloDrivingRange, soloParkingLevel, practiceSituations, vehicleType, goal)

    val isCareerStepValid: Boolean
        get() = !isOnboardingFormLoading && !hasOnboardingFormError &&
            onboardingForm.isStepValid(onboardingForm.steps.first(), profile)

    val isPreferenceNextEnabled: Boolean
        get() = !isOnboardingFormLoading && !hasOnboardingFormError &&
            onboardingForm.isStepValid(onboardingForm.steps.last(), profile) &&
            (isServerFormLoaded || practiceSituations.isNotEmpty())
}

fun EntryUiState.withProfile(profile: OnboardingProfile): EntryUiState = copy(
    nickname = profile.nickname,
    drivingPeriod = profile.drivingPeriod,
    recentFrequency = profile.recentFrequency,
    roadExperiences = profile.roadExperiences,
    soloDrivingRange = profile.soloDrivingRange,
    soloParkingLevel = profile.soloParkingLevel,
    practiceSituations = profile.practiceSituations,
    vehicleType = profile.vehicleType,
    goal = profile.goal,
)

sealed interface EntryEffect {
    data object CompleteEntry : EntryEffect
    data class ShowSubmissionError(
        val message: String,
        val canRetry: Boolean,
    ) : EntryEffect
}
