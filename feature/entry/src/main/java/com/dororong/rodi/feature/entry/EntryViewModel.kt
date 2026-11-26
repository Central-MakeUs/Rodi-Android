package com.dororong.rodi.feature.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.NicknameGenerator
import com.dororong.rodi.core.domain.model.entry.EntryMode
import com.dororong.rodi.core.domain.model.entry.EntryProgress
import com.dororong.rodi.core.domain.model.entry.EntryProgressStep
import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.model.onboarding.PracticeSituation
import com.dororong.rodi.core.domain.model.onboarding.RecentDrivingFrequency
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.SoloDrivingRange
import com.dororong.rodi.core.domain.model.onboarding.SoloParkingLevel
import com.dororong.rodi.core.domain.model.onboarding.VehicleType
import com.dororong.rodi.core.domain.model.onboarding.answerCode
import com.dororong.rodi.core.domain.model.onboarding.answers
import com.dororong.rodi.core.domain.model.onboarding.withAnswers
import com.dororong.rodi.core.domain.model.onboarding.conformTo
import com.dororong.rodi.core.domain.model.onboarding.normalizeRoadExperiences
import com.dororong.rodi.core.domain.model.onboarding.OnboardingQuestionType
import com.dororong.rodi.core.domain.usecase.onboarding.GetOnboardingFormUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.isReadyForSubmission
import com.dororong.rodi.core.ui.text.takeGraphemes
import com.dororong.rodi.core.ui.text.graphemeLength
import com.dororong.rodi.core.domain.model.onboarding.calculateLevel
import com.dororong.rodi.core.domain.model.onboarding.analysisCopy
import com.dororong.rodi.core.domain.usecase.entry.GetEntryProgressUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.ApplyInitialFilterTagsUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.GetOnboardingProfileUseCase
import com.dororong.rodi.core.domain.usecase.entry.SaveEntryProgressUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.SaveOnboardingProfileUseCase
import com.dororong.rodi.core.domain.usecase.entry.SetEntryCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val setEntryCompletedUseCase: SetEntryCompletedUseCase,
    private val saveOnboardingProfileUseCase: SaveOnboardingProfileUseCase,
    private val getEntryProgressUseCase: GetEntryProgressUseCase,
    private val saveEntryProgressUseCase: SaveEntryProgressUseCase,
    private val getOnboardingProfileUseCase: GetOnboardingProfileUseCase,
    private val applyInitialFilterTagsUseCase: ApplyInitialFilterTagsUseCase,
    private val getOnboardingFormUseCase: GetOnboardingFormUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    private val _effect = Channel<EntryEffect>(Channel.BUFFERED)
    val effect: Flow<EntryEffect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                val progress = getEntryProgressUseCase().first()
                restoreProgress(progress)
                restoreOnboardingProfile(getOnboardingProfileUseCase().first())
                applyMode(progress.mode)
                if (uiState.value.step == EntryStep.NICKNAME) generateNicknameIfNeeded()
                if (progress.mode != EntryMode.GUEST_BROWSE) loadOnboardingForm()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
            } finally {
                _uiState.update { it.copy(isRestored = true) }
            }
        }
    }

    fun setAllTermsChecked(checked: Boolean) {
        _uiState.update {
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
        if (uiState.value.nickname.isBlank()) {
            _uiState.update { it.copy(nickname = NicknameGenerator.generate()) }
            persistOnboardingProfile()
        }
    }

    fun retryOnboardingForm() = loadOnboardingForm()

    private fun loadOnboardingForm() {
        if (uiState.value.isOnboardingFormLoading) return
        _uiState.update { it.copy(isOnboardingFormLoading = true, hasOnboardingFormError = false) }
        viewModelScope.launch {
            val result = getOnboardingFormUseCase()
            result.fold(
                onSuccess = { form ->
                    _uiState.update { state ->
                        if (form == null) state.copy(isOnboardingFormLoading = false)
                        else {
                            val normalized = state.profile.conformTo(form)
                            val maxGoalLength = form.steps.flatMap { it.questions }
                                .firstOrNull { it.code == "drivingGoal" }?.maxLength ?: MAX_GOAL_LENGTH
                            state.copy(onboardingForm = form, isOnboardingFormLoading = false, isServerFormLoaded = true)
                                .withProfile(normalized.copy(goal = normalized.goal.takeGraphemes(maxGoalLength)))
                        }
                    }
                    persistOnboardingProfile()
                },
                onFailure = {
                    _uiState.update { it.copy(isOnboardingFormLoading = false, hasOnboardingFormError = true) }
                },
            )
        }
    }

    fun selectDrivingPeriod(value: DrivingPeriod) = selectOption("drivingPeriod", value.answerCode())
    fun selectRecentFrequency(value: RecentDrivingFrequency) = selectOption("recentFrequency", value.answerCode())
    fun toggleRoadExperience(value: RoadExperience) = selectOption("roadExperiences", value.answerCode())
    fun selectSoloDrivingRange(value: SoloDrivingRange) = selectOption("soloDrivingRange", value.answerCode())
    fun selectSoloParkingLevel(value: SoloParkingLevel) = selectOption("soloParkingLevel", value.answerCode())
    fun togglePracticeSituation(value: PracticeSituation) = selectOption("practiceTypes", value.answerCode())
    fun selectVehicleType(value: VehicleType) = selectOption("carType", value.answerCode())

    fun selectOption(questionCode: String, optionCode: String) {
        _uiState.update { state ->
            val question = state.onboardingForm.steps.flatMap { it.questions }.firstOrNull { it.code == questionCode }
                ?: return@update state
            val option = question.options.firstOrNull { it.code == optionCode } ?: return@update state
            val selected = state.profile.answers(questionCode)
            val answers = when {
                question.type == OnboardingQuestionType.SINGLE_SELECT -> listOf(optionCode)
                optionCode in selected -> selected - optionCode
                option.exclusive || (questionCode == "roadExperiences" && optionCode == "NONE") -> listOf(optionCode)
                else -> {
                    val ordinary = selected.filter { code ->
                        question.options.none { it.code == code && it.exclusive } &&
                            (questionCode != "roadExperiences" || code != "NONE")
                    }
                    if (ordinary.size >= (question.maxSelect ?: Int.MAX_VALUE)) selected else ordinary + optionCode
                }
            }
            state.withProfile(state.profile.withAnswers(questionCode, answers).conformTo(state.onboardingForm))
        }
        persistOnboardingProfile()
    }

    fun updateGoal(value: String) = updateOnboardingProfile {
        val maxLength = it.onboardingForm.steps.flatMap { step -> step.questions }
            .firstOrNull { question -> question.code == "drivingGoal" }?.maxLength ?: MAX_GOAL_LENGTH
        it.copy(goal = value.takeGraphemes(maxLength))
    }

    fun next() {
        val previousNickname = uiState.value.nickname
        _uiState.update {
            val nextStep = when (it.step) {
                    EntryStep.TERMS -> if (it.mode == EntryMode.GUEST_BROWSE) {
                        EntryStep.PRECAUTIONS
                    } else {
                        EntryStep.NICKNAME
                    }
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
        if (previousNickname.isBlank() && uiState.value.step == EntryStep.NICKNAME) {
            persistOnboardingProfile()
        }
    }

    fun continueAfterCareer() {
        if (!uiState.value.isCareerStepValid) return
        if (uiState.value.onboardingForm.skipsFollowUps(uiState.value.onboardingForm.steps.first(), currentOnboardingProfile())) {
            startOnboardingAnalysis()
        } else {
            next()
        }
    }

    fun openWebView(url: String) {
        _uiState.update { it.copy(webViewUrl = url, step = EntryStep.TERMS_WEBVIEW) }
        persistEntryProgress()
    }

    fun back(): Boolean {
        val previousStep = when (uiState.value.step) {
            EntryStep.NICKNAME -> if (uiState.value.mode == EntryMode.GUEST_SIGN_UP) {
                return false
            } else {
                EntryStep.TERMS
            }
            EntryStep.CAREER -> EntryStep.NICKNAME
            EntryStep.PREFERENCE -> EntryStep.CAREER
            EntryStep.PRECAUTIONS -> return false
            EntryStep.LOCATION -> EntryStep.PRECAUTIONS
            EntryStep.TERMS_WEBVIEW -> EntryStep.TERMS
            EntryStep.TERMS -> return false
        }
        _uiState.update { it.copy(step = previousStep) }
        persistEntryProgress()
        return true
    }

    fun skipPreference() {
        val state = uiState.value
        if (!state.onboardingForm.steps.last().skippable) return
        val emptyPreference = state.onboardingForm.steps.last().questions.fold(state.profile) { profile, question ->
            profile.withAnswers(question.code, emptyList())
        }
        _uiState.update { it.withProfile(emptyPreference) }
        persistOnboardingProfile()
        startOnboardingAnalysis(skipPreference = true)
    }

    fun startOnboardingAnalysis() = startOnboardingAnalysis(skipPreference = false)

    private fun startOnboardingAnalysis(skipPreference: Boolean) {
        if (uiState.value.onboardingAnalysisState != null) return

        val state = uiState.value
        val profile = currentOnboardingProfile()
        val formValid = !state.isServerFormLoaded || state.onboardingForm.steps.all { step ->
            (step.skippable && (skipPreference || state.step != EntryStep.PREFERENCE)) || state.onboardingForm.isStepValid(step, profile)
        }
        val textValid = state.onboardingForm.steps.flatMap { it.questions }.all { question ->
            val maxLength = question.maxLength
            maxLength == null || profile.answers(question.code).all { it.graphemeLength() <= maxLength }
        }
        if (state.isOnboardingFormLoading || state.hasOnboardingFormError || !profile.isReadyForSubmission || !formValid || !textValid) {
            viewModelScope.launch { _effect.send(EntryEffect.ShowSubmissionError("입력 정보를 확인해주세요.", canRetry = false)) }
            return
        }
        val level = profile.calculateLevel()
        _uiState.update {
            it.copy(
                onboardingLevel = level,
                onboardingAnalysisCopy = profile.analysisCopy(level),
                onboardingAnalysisState = OnboardingAnalysisState.ANALYZING,
            )
        }
        viewModelScope.launch {
            val submissionResult = try {
                supervisorScope {
                    val submission = async {
                        saveOnboardingProfileUseCase.saveForSubmission(profile)
                        saveOnboardingProfileUseCase.submit(profile, level)
                    }
                    delay(ANALYSIS_DURATION_MILLIS.milliseconds)
                    submission.await()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.e(error, "Onboarding submission failed.")
                _uiState.update { it.copy(onboardingAnalysisState = null) }
                _effect.send(EntryEffect.ShowSubmissionError(DEFAULT_SUBMISSION_ERROR_MESSAGE, canRetry = true))
                return@launch
            }
            when (submissionResult) {
                OnboardingSubmissionResult.Submitted,
                OnboardingSubmissionResult.AlreadyCompleted,
                -> {
                    val filterResult = applyInitialFilterTagsUseCase(level)
                    if (filterResult.isFailure) {
                        _uiState.update { it.copy(onboardingAnalysisState = null) }
                        _effect.send(EntryEffect.ShowSubmissionError(DEFAULT_SUBMISSION_ERROR_MESSAGE, canRetry = true))
                        return@launch
                    }
                    _uiState.update { it.copy(step = EntryStep.PRECAUTIONS) }
                    persistEntryProgressNow()
                    _uiState.update { it.copy(onboardingAnalysisState = OnboardingAnalysisState.RESULT) }
                }

                else -> {
                    _uiState.update { it.copy(onboardingAnalysisState = null) }
                    _effect.send(submissionResult.toSubmissionError())
                }
            }
        }
    }

    fun continueAfterOnboardingAnalysis() {
        if (uiState.value.mode == EntryMode.GUEST_SIGN_UP) {
            finish()
            return
        }
        _uiState.update { it.copy(onboardingAnalysisState = null) }
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
        _uiState.update(transform)
        persistEntryProgress()
    }

    private fun updateOnboardingProfile(transform: (EntryUiState) -> EntryUiState) {
        _uiState.update(transform)
        persistOnboardingProfile()
    }

    private fun restoreProgress(progress: EntryProgress) {
        _uiState.update {
            it.copy(
                step = progress.step.toEntryStep(),
                didResumeProgress = progress.step != EntryProgressStep.TERMS,
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
        val normalized = profile.normalizeRoadExperiences()
        _uiState.update { it.withProfile(normalized) }
        if (normalized != profile) persistOnboardingProfile()
    }

    private suspend fun applyMode(mode: EntryMode) {
        val restoredStep = uiState.value.step
        val targetStep = when (mode) {
            EntryMode.AUTHENTICATED -> restoredStep
            EntryMode.GUEST_BROWSE -> when (restoredStep) {
                EntryStep.NICKNAME,
                EntryStep.CAREER,
                EntryStep.PREFERENCE,
                -> EntryStep.PRECAUTIONS

                else -> restoredStep
            }

            EntryMode.GUEST_SIGN_UP -> when (restoredStep) {
                EntryStep.TERMS,
                EntryStep.TERMS_WEBVIEW,
                -> EntryStep.NICKNAME

                EntryStep.PRECAUTIONS,
                EntryStep.LOCATION,
                -> EntryStep.CAREER

                else -> restoredStep
            }
        }
        _uiState.update { it.copy(mode = mode, step = targetStep) }
        if (targetStep != restoredStep) persistEntryProgressNow()
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

    private suspend fun persistEntryProgressNow() {
        val progress = currentEntryProgress()
        try {
            saveEntryProgressUseCase(progress)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
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
        uiState.value.let {
            EntryProgress(
                mode = it.mode,
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

    private fun currentOnboardingProfile(): OnboardingProfile = uiState.value.profile

}

private fun OnboardingSubmissionResult.toSubmissionError(): EntryEffect.ShowSubmissionError = when (this) {
    OnboardingSubmissionResult.InvalidProfile ->
        EntryEffect.ShowSubmissionError("입력 정보를 확인해주세요.", canRetry = false)

    OnboardingSubmissionResult.AuthenticationRequired ->
        EntryEffect.ShowSubmissionError("로그인 상태가 만료됐어요. 다시 로그인해주세요.", canRetry = false)

    OnboardingSubmissionResult.Forbidden ->
        EntryEffect.ShowSubmissionError("온보딩을 완료할 권한이 없어요. 로그인 상태를 확인해주세요.", canRetry = false)

    OnboardingSubmissionResult.RateLimited ->
        EntryEffect.ShowSubmissionError("요청이 많아요. 잠시 기다린 뒤 다시 시도해주세요.", canRetry = false)

    OnboardingSubmissionResult.RetryableFailure,
    OnboardingSubmissionResult.UnexpectedFailure,
    -> EntryEffect.ShowSubmissionError(DEFAULT_SUBMISSION_ERROR_MESSAGE, canRetry = true)

    OnboardingSubmissionResult.Submitted,
    OnboardingSubmissionResult.AlreadyCompleted,
    -> error("Successful onboarding submission cannot be shown as an error.")
}

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
private const val DEFAULT_SUBMISSION_ERROR_MESSAGE = "네트워크 연결이 원활하지 않아요.\n다시 시도해볼까요?"
