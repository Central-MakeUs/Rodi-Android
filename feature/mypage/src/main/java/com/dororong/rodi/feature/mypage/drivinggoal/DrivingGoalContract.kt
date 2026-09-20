package com.dororong.rodi.feature.mypage.drivinggoal

data class DrivingGoalUiState(
    val initialGoal: String = "",
    val goal: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSucceeded: Boolean = false,
)

sealed interface DrivingGoalEffect {
    data object ShowSyncError : DrivingGoalEffect
}
