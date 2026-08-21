package com.dororong.rodi.feature.mypage.drivinggoal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.ui.text.takeGraphemes
import com.dororong.rodi.core.ui.text.takeGraphemesWithinCodeUnits
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
        // 서버는 길이를 UTF-16 code unit으로 검증한다(maxLength=30). grapheme만 세면
        // 이모지를 넣었을 때 화면엔 30/30인데 서버에서 길이 초과로 거부당한다.
        val limited = goal
            .takeGraphemes(DRIVING_GOAL_MAX_LENGTH)
            .takeGraphemesWithinCodeUnits(DRIVING_GOAL_MAX_LENGTH)
        _uiState.update { it.copy(goal = limited) }
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
