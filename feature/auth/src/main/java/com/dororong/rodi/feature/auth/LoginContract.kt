package com.dororong.rodi.feature.auth

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object LoggingIn : LoginUiState
    data class RecoveryRequired(val isRestoring: Boolean = false) : LoginUiState
}

sealed interface LoginEffect {
    data class NavigateNext(val isNewMember: Boolean?) : LoginEffect
    data class ShowSnackbar(val message: String) : LoginEffect
}
