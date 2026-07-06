package com.dororong.rodi.feature.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.NicknameGenerator
import com.dororong.rodi.core.domain.DrivingPeriod
import com.dororong.rodi.core.domain.OnboardingProfile
import com.dororong.rodi.core.domain.PracticeSituation
import com.dororong.rodi.core.domain.RecentDrivingFrequency
import com.dororong.rodi.core.domain.RoadExperience
import com.dororong.rodi.core.domain.SoloDrivingRange
import com.dororong.rodi.core.domain.SoloParkingLevel
import com.dororong.rodi.core.domain.VehicleType
import com.dororong.rodi.core.domain.usecase.SaveOnboardingProfileUseCase
import com.dororong.rodi.core.domain.usecase.SetEntryCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EntryStep { LOCATION, TERMS, PRECAUTIONS, NICKNAME, CAREER, PREFERENCE, TERMS_WEBVIEW }

/**
 * 진입 게이트 단계 상태 머신. 마지막 단계 완료 시 DataStore에 완료를 저장하고 [onDone] 호출.
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val setEntryCompletedUseCase: SetEntryCompletedUseCase,
    private val saveOnboardingProfileUseCase: SaveOnboardingProfileUseCase,
) : ViewModel() {

    var step by mutableStateOf(EntryStep.LOCATION)
        private set

    var webViewUrl by mutableStateOf("")
        private set

    var serviceTermsChecked by mutableStateOf(false)
        private set

    var privacyTermsChecked by mutableStateOf(false)
        private set

    var locationTermsChecked by mutableStateOf(false)
        private set

    var licenseChecked by mutableStateOf(false)
        private set

    var companionChecked by mutableStateOf(false)
        private set

    var precautionAgreementChecked by mutableStateOf(false)
        private set

    var nickname by mutableStateOf("")
        private set

    var drivingPeriod: DrivingPeriod? by mutableStateOf(null)
        private set

    var recentFrequency: RecentDrivingFrequency? by mutableStateOf(null)
        private set

    var roadExperience: RoadExperience? by mutableStateOf(null)
        private set

    var soloDrivingRange: SoloDrivingRange? by mutableStateOf(null)
        private set

    var soloParkingLevel: SoloParkingLevel? by mutableStateOf(null)
        private set

    var practiceSituations: List<PracticeSituation> by mutableStateOf(emptyList())
        private set

    var vehicleType: VehicleType? by mutableStateOf(null)
        private set

    var goal by mutableStateOf("")
        private set

    val isCareerStepValid: Boolean
        get() = drivingPeriod != null && recentFrequency != null && roadExperience != null &&
            (roadExperience != RoadExperience.SOLO || (soloDrivingRange != null && soloParkingLevel != null))

    val isPreferenceNextEnabled: Boolean
        get() = practiceSituations.isNotEmpty() && vehicleType != null

    fun setAllTermsChecked(checked: Boolean) {
        serviceTermsChecked = checked
        privacyTermsChecked = checked
        locationTermsChecked = checked
    }

    fun toggleServiceTerms() {
        serviceTermsChecked = !serviceTermsChecked
    }

    fun togglePrivacyTerms() {
        privacyTermsChecked = !privacyTermsChecked
    }

    fun toggleLocationTerms() {
        locationTermsChecked = !locationTermsChecked
    }

    fun toggleLicense() {
        licenseChecked = !licenseChecked
    }

    fun toggleCompanion() {
        companionChecked = !companionChecked
    }

    fun togglePrecautionAgreement() {
        precautionAgreementChecked = !precautionAgreementChecked
    }

    fun ensureNicknameGenerated() {
        if (nickname.isBlank()) nickname = NicknameGenerator.generate()
    }

    fun selectDrivingPeriod(value: DrivingPeriod) {
        drivingPeriod = value
    }

    fun selectRecentFrequency(value: RecentDrivingFrequency) {
        recentFrequency = value
    }

    fun selectRoadExperience(value: RoadExperience) {
        roadExperience = value
        if (value != RoadExperience.SOLO) {
            soloDrivingRange = null
            soloParkingLevel = null
        }
    }

    fun selectSoloDrivingRange(value: SoloDrivingRange) {
        soloDrivingRange = value
    }

    fun selectSoloParkingLevel(value: SoloParkingLevel) {
        soloParkingLevel = value
    }

    fun togglePracticeSituation(value: PracticeSituation) {
        practiceSituations = when {
            practiceSituations.contains(value) -> practiceSituations - value
            practiceSituations.size >= 3 -> practiceSituations
            else -> practiceSituations + value
        }
    }

    fun selectVehicleType(value: VehicleType) {
        vehicleType = value
    }

    fun updateGoal(value: String) {
        goal = value
    }

    fun next() {
        step = when (step) {
            EntryStep.LOCATION -> EntryStep.TERMS
            EntryStep.TERMS -> EntryStep.PRECAUTIONS
            EntryStep.PRECAUTIONS -> EntryStep.NICKNAME
            EntryStep.NICKNAME -> EntryStep.CAREER
            EntryStep.CAREER -> EntryStep.PREFERENCE
            EntryStep.PREFERENCE -> EntryStep.PREFERENCE
            EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
        }
    }

    fun openWebView(url: String) {
        webViewUrl = url
        step = EntryStep.TERMS_WEBVIEW
    }

    /** 뒤로. 첫 단계면 false(처리할 것 없음). */
    fun back(): Boolean {
        step = when (step) {
            EntryStep.PRECAUTIONS -> EntryStep.TERMS
            EntryStep.TERMS -> EntryStep.LOCATION
            EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
            EntryStep.NICKNAME -> EntryStep.PRECAUTIONS
            EntryStep.CAREER -> EntryStep.NICKNAME
            EntryStep.PREFERENCE -> EntryStep.CAREER
            EntryStep.LOCATION -> return false
        }
        return true
    }

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            val profile = OnboardingProfile(
                nickname = nickname,
                drivingPeriod = drivingPeriod,
                recentFrequency = recentFrequency,
                roadExperience = roadExperience,
                soloDrivingRange = soloDrivingRange,
                soloParkingLevel = soloParkingLevel,
                practiceSituations = practiceSituations,
                vehicleType = vehicleType,
                goal = goal,
            )
            try {
                saveOnboardingProfileUseCase(profile)
                setEntryCompletedUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                return@launch
            }
            onDone()
        }
    }
}
