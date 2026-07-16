package com.dororong.rodi.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.model.onboarding.calculateLevel
import com.dororong.rodi.core.domain.usecase.onboarding.GetOnboardingProfileUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.SaveOnboardingProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DrivingGoalUiState(
    val initialGoal: String = "",
    val isSaving: Boolean = false,
)

sealed interface DrivingGoalEffect {
    data object NavigateBack : DrivingGoalEffect
    data object ShowSyncError : DrivingGoalEffect
}

@HiltViewModel
class DrivingGoalViewModel @Inject constructor(
    private val getOnboardingProfile: GetOnboardingProfileUseCase,
    private val saveOnboardingProfile: SaveOnboardingProfileUseCase,
) : ViewModel() {
    private val _effect = Channel<DrivingGoalEffect>(Channel.BUFFERED)
    private val isSaving = MutableStateFlow(false)
    val effect = _effect.receiveAsFlow()

    val uiState: StateFlow<DrivingGoalUiState> = combine(
        getOnboardingProfile(),
        isSaving,
    ) { profile, saving ->
        DrivingGoalUiState(initialGoal = profile.goal, isSaving = saving)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DrivingGoalUiState(),
        )

    fun save(goal: String) {
        if (goal.isBlank() || uiState.value.isSaving) return

        viewModelScope.launch {
            isSaving.value = true
            val result = try {
                val profile = getOnboardingProfile().first().copy(goal = goal.take(DRIVING_GOAL_MAX_LENGTH))
                saveOnboardingProfile(profile)
                saveOnboardingProfile.submit(profile, profile.calculateLevel())
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                OnboardingSubmissionResult.UnexpectedFailure
            } finally {
                isSaving.value = false
            }
            if (result.isSuccessful) {
                _effect.send(DrivingGoalEffect.NavigateBack)
            } else {
                _effect.send(DrivingGoalEffect.ShowSyncError)
            }
        }
    }
}

const val DRIVING_GOAL_MAX_LENGTH = 30

private val OnboardingSubmissionResult.isSuccessful: Boolean
    get() = this == OnboardingSubmissionResult.Submitted || this == OnboardingSubmissionResult.AlreadyCompleted
