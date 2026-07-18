package com.dororong.rodi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.auth.GetGuestAccessUseCase
import com.dororong.rodi.core.domain.usecase.entry.GetEntryCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

data class RodiAppUiState(
    val isReady: Boolean = false,
    val isEntryCompleted: Boolean = false,
    val hasGuestAccess: Boolean = false,
    val authSession: AuthSession = AuthSession(
        isLoggedIn = false,
        hasRecentKakaoLogin = false,
    ),
)

@HiltViewModel
class RodiAppViewModel @Inject constructor(
    getEntryCompletedUseCase: GetEntryCompletedUseCase,
    getGuestAccessUseCase: GetGuestAccessUseCase,
    getAuthSessionUseCase: GetAuthSessionUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(RodiAppUiState())
    private val sessionEnded = MutableStateFlow(false)
    val state: StateFlow<RodiAppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                combine(
                    getEntryCompletedUseCase().filterNotNull(),
                    getGuestAccessUseCase(),
                    flow { emit(getAuthSessionUseCase()) },
                    sessionEnded,
                ) { isEntryCompleted, hasGuestAccess, authSession, hasSessionEnded ->
                    RodiAppUiState(
                        isReady = true,
                        isEntryCompleted = isEntryCompleted,
                        hasGuestAccess = hasGuestAccess,
                        authSession = if (hasSessionEnded) LoggedOutSession else authSession,
                    )
                }.collect(_state)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                _state.value = RodiAppUiState(isReady = true)
            }
        }
    }

    fun onSessionEnded() {
        sessionEnded.value = true
    }
}

private val LoggedOutSession = AuthSession(
    isLoggedIn = false,
    hasRecentKakaoLogin = false,
)
