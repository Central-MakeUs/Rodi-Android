package com.dororong.rodi.feature.settings.account

enum class AccountAction {
    Logout,
    Withdraw,
}

data class AccountSettingsUiState(
    val isSubmitting: Boolean = false,
)

sealed interface AccountSettingsEffect {
    data object SessionEnded : AccountSettingsEffect
    data class ShowError(val message: String) : AccountSettingsEffect
}
