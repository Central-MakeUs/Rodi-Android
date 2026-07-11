package com.dororong.rodi.feature.entry

import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.PracticeSituation
import com.dororong.rodi.core.domain.model.onboarding.RecentDrivingFrequency
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.SoloDrivingRange
import com.dororong.rodi.core.domain.model.onboarding.SoloParkingLevel
import com.dororong.rodi.core.domain.model.onboarding.VehicleType

enum class EntryStep { TERMS, NICKNAME, CAREER, PREFERENCE, PRECAUTIONS, LOCATION, TERMS_WEBVIEW }

enum class OnboardingAnalysisState { ANALYZING, RESULT }

data class EntryUiState(
    val isRestored: Boolean = false,
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
    val onboardingLevel: OnboardingLevel? = null,
    val onboardingAnalysisState: OnboardingAnalysisState? = null,
) {
    val isCareerStepValid: Boolean
        get() {
            val period = drivingPeriod ?: return false
            if (period == DrivingPeriod.YEAR_2_TO_10 || period == DrivingPeriod.OVER_YEAR_10) return true
            if (recentFrequency == null || roadExperiences.isEmpty()) return false
            return !roadExperiences.contains(RoadExperience.SOLO) ||
                (soloDrivingRange != null && soloParkingLevel != null)
        }

    val isPreferenceNextEnabled: Boolean
        get() = practiceSituations.isNotEmpty()
}

sealed interface EntryEffect {
    data object CompleteEntry : EntryEffect
    data object ShowSubmissionError : EntryEffect
}
