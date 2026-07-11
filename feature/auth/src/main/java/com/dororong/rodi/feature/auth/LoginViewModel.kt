package com.dororong.rodi.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.usecase.auth.GrantGuestAccessUseCase
import com.dororong.rodi.core.domain.usecase.auth.LoginWithKakaoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object LoggingIn : LoginUiState
}

sealed interface LoginEffect {
    data object NavigateNext : LoginEffect
    data class ShowSnackbar(val message: String) : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase,
    private val grantGuestAccessUseCase: GrantGuestAccessUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

    fun onKakaoLoginResult(accessToken: String) {
        _uiState.update { LoginUiState.LoggingIn }
        viewModelScope.launch {
            loginWithKakaoUseCase(accessToken)
                .onSuccess { _effect.send(LoginEffect.NavigateNext) }
                .onFailure { error ->
                    val message = (error as? AuthException)?.message
                        ?: "로그인에 실패했습니다. 잠시 후 다시 시도해주세요."
                    _effect.send(LoginEffect.ShowSnackbar(message))
                    _uiState.update { LoginUiState.Idle }
                }
        }
    }

    fun onKakaoLoginFailed(message: String) {
        viewModelScope.launch {
            _effect.send(LoginEffect.ShowSnackbar(message))
        }
    }

    fun onSkipClick() {
        viewModelScope.launch {
            try {
                grantGuestAccessUseCase()
                _effect.send(LoginEffect.NavigateNext)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                _effect.send(LoginEffect.ShowSnackbar("둘러보기를 시작할 수 없습니다. 다시 시도해주세요."))
            }
        }
    }
}
