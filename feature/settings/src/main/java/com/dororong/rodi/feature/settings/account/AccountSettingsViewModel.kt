package com.dororong.rodi.feature.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.usecase.auth.LogoutUseCase
import com.dororong.rodi.core.domain.usecase.member.WithdrawUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val logout: LogoutUseCase,
    private val withdraw: WithdrawUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountSettingsUiState())
    val uiState: StateFlow<AccountSettingsUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<AccountSettingsEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun confirm(action: AccountAction) {
        if (_uiState.value.isSubmitting) return
        _uiState.value = AccountSettingsUiState(isSubmitting = true)

        viewModelScope.launch {
            val result = when (action) {
                AccountAction.Logout -> logout()
                AccountAction.Withdraw -> withdraw()
            }
            _uiState.value = AccountSettingsUiState()
            result.fold(
                onSuccess = { effectChannel.send(AccountSettingsEffect.SessionEnded) },
                onFailure = { error ->
                    effectChannel.send(
                        AccountSettingsEffect.ShowError(
                            error.message ?: "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.",
                        ),
                    )
                },
            )
        }
    }
}
