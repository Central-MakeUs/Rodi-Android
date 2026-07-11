package com.dororong.rodi.feature.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.NicknameGenerator
import com.dororong.rodi.core.domain.DrivingPeriod
import com.dororong.rodi.core.domain.EntryProgress
import com.dororong.rodi.core.domain.EntryProgressStep
import com.dororong.rodi.core.domain.OnboardingLevel
import com.dororong.rodi.core.domain.OnboardingProfile
import com.dororong.rodi.core.domain.PracticeSituation
import com.dororong.rodi.core.domain.RecentDrivingFrequency
import com.dororong.rodi.core.domain.RoadExperience
import com.dororong.rodi.core.domain.SoloDrivingRange
import com.dororong.rodi.core.domain.SoloParkingLevel
import com.dororong.rodi.core.domain.VehicleType
import com.dororong.rodi.core.domain.calculateLevel
import com.dororong.rodi.core.domain.usecase.GetEntryProgressUseCase
import com.dororong.rodi.core.domain.usecase.GetOnboardingProfileUseCase
import com.dororong.rodi.core.domain.usecase.SaveEntryProgressUseCase
import com.dororong.rodi.core.domain.usecase.SaveOnboardingProfileUseCase
import com.dororong.rodi.core.domain.usecase.SetEntryCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import javax.inject.Inject

enum class EntryStep { TERMS, NICKNAME, CAREER, PREFERENCE, PRECAUTIONS, LOCATION, TERMS_WEBVIEW }

enum class OnboardingAnalysisState { ANALYZING, RESULT }

/**
 * 진입 게이트 단계 상태 머신. 마지막 단계 완료 시 DataStore에 완료를 저장하고 [onDone] 호출.
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val setEntryCompletedUseCase: SetEntryCompletedUseCase,
    private val saveOnboardingProfileUseCase: SaveOnboardingProfileUseCase,
    private val getEntryProgressUseCase: GetEntryProgressUseCase,
    private val saveEntryProgressUseCase: SaveEntryProgressUseCase,
    private val getOnboardingProfileUseCase: GetOnboardingProfileUseCase,
) : ViewModel() {

    var isRestored by mutableStateOf(false)
        private set

    var step by mutableStateOf(EntryStep.TERMS)
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

    var roadExperiences: List<RoadExperience> by mutableStateOf(emptyList())
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

    var onboardingLevel: OnboardingLevel? by mutableStateOf(null)
        private set

    var submissionFailed by mutableStateOf(false)
        private set

    var onboardingAnalysisState: OnboardingAnalysisState? by mutableStateOf(null)
        private set

    val isCareerStepValid: Boolean
        get() {
            val period = drivingPeriod ?: return false
            if (period.allowsCareerStepSkip) return true
            if (recentFrequency == null || roadExperiences.isEmpty()) return false
            return !roadExperiences.contains(RoadExperience.SOLO) ||
                (soloDrivingRange != null && soloParkingLevel != null)
        }

    val isPreferenceNextEnabled: Boolean
        get() = practiceSituations.isNotEmpty()

    init {
        viewModelScope.launch {
            try {
                val progress = getEntryProgressUseCase().first()
                val profile = getOnboardingProfileUseCase().first()
                restoreProgress(progress)
                restoreOnboardingProfile(profile)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
            } finally {
                isRestored = true
            }
        }
    }

    fun setAllTermsChecked(checked: Boolean) {
        serviceTermsChecked = checked
        privacyTermsChecked = checked
        locationTermsChecked = checked
        persistEntryProgress()
    }

    fun toggleServiceTerms() {
        serviceTermsChecked = !serviceTermsChecked
        persistEntryProgress()
    }

    fun togglePrivacyTerms() {
        privacyTermsChecked = !privacyTermsChecked
        persistEntryProgress()
    }

    fun toggleLocationTerms() {
        locationTermsChecked = !locationTermsChecked
        persistEntryProgress()
    }

    fun toggleLicense() {
        licenseChecked = !licenseChecked
        persistEntryProgress()
    }

    fun toggleCompanion() {
        companionChecked = !companionChecked
        persistEntryProgress()
    }

    fun togglePrecautionAgreement() {
        precautionAgreementChecked = !precautionAgreementChecked
        persistEntryProgress()
    }

    fun ensureNicknameGenerated() {
        if (nickname.isBlank()) {
            nickname = NicknameGenerator.generate()
            persistOnboardingProfile()
        }
    }

    fun selectDrivingPeriod(value: DrivingPeriod) {
        drivingPeriod = value
        if (value.allowsCareerStepSkip) {
            recentFrequency = null
            roadExperiences = emptyList()
            soloDrivingRange = null
            soloParkingLevel = null
        }
        persistOnboardingProfile()
    }

    fun selectRecentFrequency(value: RecentDrivingFrequency) {
        recentFrequency = value
        persistOnboardingProfile()
    }

    fun toggleRoadExperience(value: RoadExperience) {
        roadExperiences = if (roadExperiences.contains(value)) {
            roadExperiences - value
        } else {
            roadExperiences + value
        }
        if (!roadExperiences.contains(RoadExperience.SOLO)) {
            soloDrivingRange = null
            soloParkingLevel = null
        }
        persistOnboardingProfile()
    }

    fun selectSoloDrivingRange(value: SoloDrivingRange) {
        soloDrivingRange = value
        persistOnboardingProfile()
    }

    fun selectSoloParkingLevel(value: SoloParkingLevel) {
        soloParkingLevel = value
        persistOnboardingProfile()
    }

    fun togglePracticeSituation(value: PracticeSituation) {
        practiceSituations = when {
            practiceSituations.contains(value) -> practiceSituations - value
            practiceSituations.size >= 3 -> practiceSituations
            else -> practiceSituations + value
        }
        persistOnboardingProfile()
    }

    fun selectVehicleType(value: VehicleType) {
        vehicleType = value
        persistOnboardingProfile()
    }

    fun updateGoal(value: String) {
        goal = value.take(MAX_GOAL_LENGTH)
        persistOnboardingProfile()
    }

    fun next() {
        step = when (step) {
            EntryStep.TERMS -> EntryStep.NICKNAME
            EntryStep.NICKNAME -> EntryStep.CAREER
            EntryStep.CAREER -> EntryStep.PREFERENCE
            EntryStep.PREFERENCE -> EntryStep.PRECAUTIONS
            EntryStep.PRECAUTIONS -> EntryStep.LOCATION
            EntryStep.LOCATION -> EntryStep.LOCATION
            EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
        }
        persistEntryProgress()
    }

    fun openWebView(url: String) {
        webViewUrl = url
        step = EntryStep.TERMS_WEBVIEW
        persistEntryProgress()
    }

    /** 뒤로. 첫 단계면 false(처리할 것 없음). */
    fun back(): Boolean {
        step = when (step) {
            EntryStep.NICKNAME -> EntryStep.TERMS
            EntryStep.CAREER -> EntryStep.NICKNAME
            EntryStep.PREFERENCE -> EntryStep.CAREER
            EntryStep.PRECAUTIONS -> EntryStep.PREFERENCE
            EntryStep.LOCATION -> EntryStep.PRECAUTIONS
            EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
            EntryStep.TERMS -> return false
        }
        persistEntryProgress()
        return true
    }

    fun startOnboardingAnalysis() {
        viewModelScope.launch {
            val profile = currentOnboardingProfile()
            val level = profile.calculateLevel()
            onboardingLevel = level
            submissionFailed = false
            onboardingAnalysisState = OnboardingAnalysisState.ANALYZING
            try {
                saveOnboardingProfileUseCase(profile)
                coroutineScope {
                    val submission = async { saveOnboardingProfileUseCase.submit(profile, level) }
                    delay(ANALYSIS_DURATION_MILLIS)
                    submission.await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                submissionFailed = true
                return@launch
            }
            onboardingAnalysisState = OnboardingAnalysisState.RESULT
        }
    }

    fun continueAfterOnboardingAnalysis() {
        onboardingAnalysisState = null
        next()
    }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                setEntryCompletedUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                return@launch
            }
            onDone()
        }
    }

    private fun restoreProgress(progress: EntryProgress) {
        step = progress.step.toEntryStep()
        webViewUrl = progress.webViewUrl
        serviceTermsChecked = progress.serviceTermsChecked
        privacyTermsChecked = progress.privacyTermsChecked
        locationTermsChecked = progress.locationTermsChecked
        licenseChecked = progress.licenseChecked
        companionChecked = progress.companionChecked
        precautionAgreementChecked = progress.precautionAgreementChecked
    }

    private fun restoreOnboardingProfile(profile: OnboardingProfile) {
        nickname = profile.nickname
        drivingPeriod = profile.drivingPeriod
        recentFrequency = profile.recentFrequency
        roadExperiences = profile.roadExperiences
        soloDrivingRange = profile.soloDrivingRange
        soloParkingLevel = profile.soloParkingLevel
        practiceSituations = profile.practiceSituations
        vehicleType = profile.vehicleType
        goal = profile.goal
    }

    private fun persistEntryProgress() {
        viewModelScope.launch {
            try {
                saveEntryProgressUseCase(currentEntryProgress())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
    }

    private fun persistOnboardingProfile() {
        viewModelScope.launch {
            try {
                saveOnboardingProfileUseCase(currentOnboardingProfile())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
    }

    private fun currentEntryProgress(): EntryProgress =
        EntryProgress(
            step = step.toEntryProgressStep(),
            webViewUrl = webViewUrl,
            serviceTermsChecked = serviceTermsChecked,
            privacyTermsChecked = privacyTermsChecked,
            locationTermsChecked = locationTermsChecked,
            licenseChecked = licenseChecked,
            companionChecked = companionChecked,
            precautionAgreementChecked = precautionAgreementChecked,
        )

    private fun currentOnboardingProfile(): OnboardingProfile =
        OnboardingProfile(
            nickname = nickname,
            drivingPeriod = drivingPeriod,
            recentFrequency = recentFrequency,
            roadExperiences = roadExperiences,
            soloDrivingRange = soloDrivingRange,
            soloParkingLevel = soloParkingLevel,
            practiceSituations = practiceSituations,
            vehicleType = vehicleType,
            goal = goal,
        )
}

private val DrivingPeriod.allowsCareerStepSkip: Boolean
    get() = this == DrivingPeriod.YEAR_2_TO_10 || this == DrivingPeriod.OVER_YEAR_10

private fun EntryProgressStep.toEntryStep(): EntryStep =
    when (this) {
        EntryProgressStep.TERMS -> EntryStep.TERMS
        EntryProgressStep.NICKNAME -> EntryStep.NICKNAME
        EntryProgressStep.CAREER -> EntryStep.CAREER
        EntryProgressStep.PREFERENCE -> EntryStep.PREFERENCE
        EntryProgressStep.PRECAUTIONS -> EntryStep.PRECAUTIONS
        EntryProgressStep.LOCATION -> EntryStep.LOCATION
        EntryProgressStep.TERMS_WEBVIEW -> EntryStep.TERMS_WEBVIEW
    }

private fun EntryStep.toEntryProgressStep(): EntryProgressStep =
    when (this) {
        EntryStep.TERMS -> EntryProgressStep.TERMS
        EntryStep.NICKNAME -> EntryProgressStep.NICKNAME
        EntryStep.CAREER -> EntryProgressStep.CAREER
        EntryStep.PREFERENCE -> EntryProgressStep.PREFERENCE
        EntryStep.PRECAUTIONS -> EntryProgressStep.PRECAUTIONS
        EntryStep.LOCATION -> EntryProgressStep.LOCATION
        EntryStep.TERMS_WEBVIEW -> EntryProgressStep.TERMS_WEBVIEW
    }

private const val MAX_GOAL_LENGTH = 30
private const val ANALYSIS_DURATION_MILLIS = 3_000L
