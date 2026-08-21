package com.dororong.rodi.feature.mypage.drivinggoal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.ui.text.takeGraphemes
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import com.dororong.rodi.core.domain.usecase.member.UpdateDrivingGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DrivingGoalUiState(
    val initialGoal: String = "",
    val goal: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
)

sealed interface DrivingGoalEffect {
    data object NavigateBack : DrivingGoalEffect
    data object ShowSyncError : DrivingGoalEffect
}

@HiltViewModel
class DrivingGoalViewModel @Inject constructor(
    private val getMyPage: GetMyPageUseCase,
    private val updateDrivingGoal: UpdateDrivingGoalUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DrivingGoalUiState())
    val uiState: StateFlow<DrivingGoalUiState> = _uiState.asStateFlow()
    private val _effect = Channel<DrivingGoalEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            getMyPage()
                .onSuccess { page ->
                    val goal = page.drivingGoal.orEmpty()
                    _uiState.value = DrivingGoalUiState(initialGoal = goal, goal = goal, isLoading = false)
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(DrivingGoalEffect.ShowSyncError)
                }
        }
    }

    fun updateGoal(goal: String) {
        _uiState.update { it.copy(goal = goal.takeGraphemes(DRIVING_GOAL_MAX_LENGTH)) }
    }

    fun save() {
        val state = _uiState.value
        if (state.goal == state.initialGoal || state.isSaving || state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            updateDrivingGoal(state.goal)
                .onSuccess { _effect.send(DrivingGoalEffect.NavigateBack) }
                .onFailure {
                    _uiState.update { it.copy(isSaving = false) }
                    _effect.send(DrivingGoalEffect.ShowSyncError)
                }
        }
    }
}

const val DRIVING_GOAL_MAX_LENGTH = 30
