package com.dororong.rodi.feature.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.NicknameGenerator
import com.dororong.rodi.core.domain.DrivingPeriod
import com.dororong.rodi.core.domain.EntryProgress
import com.dororong.rodi.core.domain.EntryProgressStep
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
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val setEntryCompletedUseCase: SetEntryCompletedUseCase,
    private val saveOnboardingProfileUseCase: SaveOnboardingProfileUseCase,
    private val getEntryProgressUseCase: GetEntryProgressUseCase,
    private val saveEntryProgressUseCase: SaveEntryProgressUseCase,
    private val getOnboardingProfileUseCase: GetOnboardingProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EntryUiState())
    val state: StateFlow<EntryUiState> = _state.asStateFlow()

    private val _effect = Channel<EntryEffect>(Channel.BUFFERED)
    val effect: Flow<EntryEffect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                restoreProgress(getEntryProgressUseCase().first())
                restoreOnboardingProfile(getOnboardingProfileUseCase().first())
                if (state.value.step == EntryStep.NICKNAME) generateNicknameIfNeeded()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
            } finally {
                _state.update { it.copy(isRestored = true) }
            }
        }
    }

    fun setAllTermsChecked(checked: Boolean) {
        _state.update {
            it.copy(
                serviceTermsChecked = checked,
                privacyTermsChecked = checked,
                locationTermsChecked = checked,
            )
        }
        persistEntryProgress()
    }

    fun toggleServiceTerms() = updateEntryProgress { it.copy(serviceTermsChecked = !it.serviceTermsChecked) }

    fun togglePrivacyTerms() = updateEntryProgress { it.copy(privacyTermsChecked = !it.privacyTermsChecked) }

    fun toggleLocationTerms() = updateEntryProgress { it.copy(locationTermsChecked = !it.locationTermsChecked) }

    fun toggleLicense() = updateEntryProgress { it.copy(licenseChecked = !it.licenseChecked) }

    fun toggleCompanion() = updateEntryProgress { it.copy(companionChecked = !it.companionChecked) }

    fun togglePrecautionAgreement() =
        updateEntryProgress { it.copy(precautionAgreementChecked = !it.precautionAgreementChecked) }

    private fun generateNicknameIfNeeded() {
        if (state.value.nickname.isBlank()) {
            _state.update { it.copy(nickname = NicknameGenerator.generate()) }
            persistOnboardingProfile()
        }
    }

    fun selectDrivingPeriod(value: DrivingPeriod) {
        _state.update {
            if (value.allowsCareerStepSkip) {
                it.copy(
                    drivingPeriod = value,
                    recentFrequency = null,
                    roadExperiences = emptyList(),
                    soloDrivingRange = null,
                    soloParkingLevel = null,
                )
            } else {
                it.copy(drivingPeriod = value)
            }
        }
        persistOnboardingProfile()
    }

    fun selectRecentFrequency(value: RecentDrivingFrequency) =
        updateOnboardingProfile { it.copy(recentFrequency = value) }

    fun toggleRoadExperience(value: RoadExperience) {
        _state.update {
            val roadExperiences = if (value in it.roadExperiences) {
                it.roadExperiences - value
            } else {
                it.roadExperiences + value
            }
            it.copy(
                roadExperiences = roadExperiences,
                soloDrivingRange = it.soloDrivingRange.takeIf { RoadExperience.SOLO in roadExperiences },
                soloParkingLevel = it.soloParkingLevel.takeIf { RoadExperience.SOLO in roadExperiences },
            )
        }
        persistOnboardingProfile()
    }

    fun selectSoloDrivingRange(value: SoloDrivingRange) =
        updateOnboardingProfile { it.copy(soloDrivingRange = value) }

    fun selectSoloParkingLevel(value: SoloParkingLevel) =
        updateOnboardingProfile { it.copy(soloParkingLevel = value) }

    fun togglePracticeSituation(value: PracticeSituation) {
        _state.update {
            val practiceSituations = when {
                value in it.practiceSituations -> it.practiceSituations - value
                it.practiceSituations.size >= 3 -> it.practiceSituations
                else -> it.practiceSituations + value
            }
            it.copy(practiceSituations = practiceSituations)
        }
        persistOnboardingProfile()
    }

    fun selectVehicleType(value: VehicleType) =
        updateOnboardingProfile { it.copy(vehicleType = value) }

    fun updateGoal(value: String) =
        updateOnboardingProfile { it.copy(goal = value.take(MAX_GOAL_LENGTH)) }

    fun next() {
        val previousNickname = state.value.nickname
        _state.update {
            val nextStep = when (it.step) {
                    EntryStep.TERMS -> EntryStep.NICKNAME
                    EntryStep.NICKNAME -> EntryStep.CAREER
                    EntryStep.CAREER -> EntryStep.PREFERENCE
                    EntryStep.PREFERENCE -> EntryStep.PRECAUTIONS
                    EntryStep.PRECAUTIONS -> EntryStep.LOCATION
                    EntryStep.LOCATION -> EntryStep.LOCATION
                    EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
                }
            it.copy(
                step = nextStep,
                nickname = if (nextStep == EntryStep.NICKNAME && it.nickname.isBlank()) {
                    NicknameGenerator.generate()
                } else {
                    it.nickname
                },
            )
        }
        persistEntryProgress()
        if (previousNickname.isBlank() && state.value.step == EntryStep.NICKNAME) {
            persistOnboardingProfile()
        }
    }

    fun openWebView(url: String) {
        _state.update { it.copy(webViewUrl = url, step = EntryStep.TERMS_WEBVIEW) }
        persistEntryProgress()
    }

    fun back(): Boolean {
        val previousStep = when (state.value.step) {
            EntryStep.NICKNAME -> EntryStep.TERMS
            EntryStep.CAREER -> EntryStep.NICKNAME
            EntryStep.PREFERENCE -> EntryStep.CAREER
            EntryStep.PRECAUTIONS -> EntryStep.PREFERENCE
            EntryStep.LOCATION -> EntryStep.PRECAUTIONS
            EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
            EntryStep.TERMS -> return false
        }
        _state.update { it.copy(step = previousStep) }
        persistEntryProgress()
        return true
    }

    fun startOnboardingAnalysis() {
        viewModelScope.launch {
            val profile = currentOnboardingProfile()
            val level = profile.calculateLevel()
            _state.update {
                it.copy(
                    onboardingLevel = level,
                    onboardingAnalysisState = OnboardingAnalysisState.ANALYZING,
                )
            }
            try {
                saveOnboardingProfileUseCase(profile)
                coroutineScope {
                    val submission = async {
                        saveOnboardingProfileUseCase.submit(profile, level)
                    }
                    delay(ANALYSIS_DURATION_MILLIS.milliseconds)
                    submission.await()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                _state.update { it.copy(onboardingAnalysisState = null) }
                _effect.send(EntryEffect.ShowSubmissionError)
                return@launch
            }
            _state.update { it.copy(onboardingAnalysisState = OnboardingAnalysisState.RESULT) }
        }
    }

    fun continueAfterOnboardingAnalysis() {
        _state.update { it.copy(onboardingAnalysisState = null) }
        next()
    }

    fun finish() {
        viewModelScope.launch {
            try {
                setEntryCompletedUseCase()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                return@launch
            }
            _effect.send(EntryEffect.CompleteEntry)
        }
    }

    private fun updateEntryProgress(transform: (EntryUiState) -> EntryUiState) {
        _state.update(transform)
        persistEntryProgress()
    }

    private fun updateOnboardingProfile(transform: (EntryUiState) -> EntryUiState) {
        _state.update(transform)
        persistOnboardingProfile()
    }

    private fun restoreProgress(progress: EntryProgress) {
        _state.update {
            it.copy(
                step = progress.step.toEntryStep(),
                webViewUrl = progress.webViewUrl,
                serviceTermsChecked = progress.serviceTermsChecked,
                privacyTermsChecked = progress.privacyTermsChecked,
                locationTermsChecked = progress.locationTermsChecked,
                licenseChecked = progress.licenseChecked,
                companionChecked = progress.companionChecked,
                precautionAgreementChecked = progress.precautionAgreementChecked,
            )
        }
    }

    private fun restoreOnboardingProfile(profile: OnboardingProfile) {
        _state.update {
            it.copy(
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
        }
    }

    private fun persistEntryProgress() {
        val progress = currentEntryProgress()
        viewModelScope.launch {
            try {
                saveEntryProgressUseCase(progress)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
            }
        }
    }

    private fun persistOnboardingProfile() {
        val profile = currentOnboardingProfile()
        viewModelScope.launch {
            try {
                saveOnboardingProfileUseCase(profile)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
            }
        }
    }

    private fun currentEntryProgress(): EntryProgress =
        state.value.let {
            EntryProgress(
                step = it.step.toEntryProgressStep(),
                webViewUrl = it.webViewUrl,
                serviceTermsChecked = it.serviceTermsChecked,
                privacyTermsChecked = it.privacyTermsChecked,
                locationTermsChecked = it.locationTermsChecked,
                licenseChecked = it.licenseChecked,
                companionChecked = it.companionChecked,
                precautionAgreementChecked = it.precautionAgreementChecked,
            )
        }

    private fun currentOnboardingProfile(): OnboardingProfile =
        state.value.let {
            OnboardingProfile(
                nickname = it.nickname,
                drivingPeriod = it.drivingPeriod,
                recentFrequency = it.recentFrequency,
                roadExperiences = it.roadExperiences,
                soloDrivingRange = it.soloDrivingRange,
                soloParkingLevel = it.soloParkingLevel,
                practiceSituations = it.practiceSituations,
                vehicleType = it.vehicleType,
                goal = it.goal,
            )
        }
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
