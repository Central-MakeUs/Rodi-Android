package com.dororong.rodi.ui

import com.dororong.rodi.core.domain.model.auth.AuthSession

data class RodiAppUiState(
    val isReady: Boolean = false,
    val isEntryCompleted: Boolean = false,
    val hasGuestAccess: Boolean = false,
    val authSession: AuthSession = AuthSession(
        isLoggedIn = false,
        hasRecentKakaoLogin = false,
    ),
)

sealed interface RodiAppEffect {
    data object NavigateToLogin : RodiAppEffect
}
