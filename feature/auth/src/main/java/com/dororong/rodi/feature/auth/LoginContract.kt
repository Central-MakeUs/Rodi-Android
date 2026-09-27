package com.dororong.rodi.feature.auth

import java.time.Instant

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object LoggingIn : LoginUiState
    data class RecoveryRequired(val isRestoring: Boolean = false) : LoginUiState

    /** 복구 기간이 지나 [reRegisterableAt] 전까지 다시 가입할 수 없다. 복구와 달리 할 수 있는 행동이 없다. */
    data class WithdrawalLocked(val reRegisterableAt: Instant) : LoginUiState
}

sealed interface LoginEffect {
    data class NavigateNext(val needsOnboarding: Boolean?) : LoginEffect
    data class ShowSnackbar(val message: String) : LoginEffect
}
