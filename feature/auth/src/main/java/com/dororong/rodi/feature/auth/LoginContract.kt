package com.dororong.rodi.feature.auth

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object LoggingIn : LoginUiState
}

sealed interface LoginEffect {
    data object NavigateNext : LoginEffect
    data class ShowSnackbar(val message: String) : LoginEffect
}
