package com.dororong.rodi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.auth.GetGuestAccessUseCase
import com.dororong.rodi.core.domain.usecase.entry.GetEntryCompletedUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.SyncPendingOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
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

sealed interface RodiAppEffect {
    data object NavigateToLogin : RodiAppEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RodiAppViewModel @Inject constructor(
    getEntryCompletedUseCase: GetEntryCompletedUseCase,
    getGuestAccessUseCase: GetGuestAccessUseCase,
    private val getAuthSessionUseCase: GetAuthSessionUseCase,
    private val syncPendingOnboardingUseCase: SyncPendingOnboardingUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RodiAppUiState())
    private val authSessionRefresh = MutableStateFlow(0)
    private val sessionEnded = MutableStateFlow(false)
    private var sessionVerificationJob: Job? = null
    private val _effect = Channel<RodiAppEffect>(Channel.BUFFERED)
    val effect: Flow<RodiAppEffect> = _effect.receiveAsFlow()
    val state: StateFlow<RodiAppUiState> = _state.asStateFlow()

    init {
        retryPendingOnboardingSync()
        viewModelScope.launch {
            authRepository.observeSessionExpiration()
                .filter { it }
                .collect { onSessionEnded() }
        }
        viewModelScope.launch {
            combine(
                getEntryCompletedUseCase().filterNotNull(),
                getGuestAccessUseCase(),
                authSessionRefresh.flatMapLatest {
                    flow { emit(getAuthSessionUseCase()) }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            emit(AuthSession(isLoggedIn = false, hasRecentKakaoLogin = false))
                        }
                },
                sessionEnded,
            ) { isEntryCompleted, hasGuestAccess, authSession, hasSessionEnded ->
                RodiAppUiState(
                    isReady = true,
                    isEntryCompleted = isEntryCompleted,
                    hasGuestAccess = hasGuestAccess,
                    authSession = if (hasSessionEnded) {
                        AuthSession(
                            isLoggedIn = false,
                            hasRecentKakaoLogin = authSession.hasRecentKakaoLogin,
                        )
                    } else {
                        authSession
                    },
                )
            }.retryWhen { error, attempt ->
                if (error is CancellationException) throw error
                if (attempt >= MAX_AUTH_SESSION_RETRY_ATTEMPTS) return@retryWhen false
                _state.value = RodiAppUiState(isReady = true)
                delay(RETRY_DELAY_MILLIS * (attempt + 1))
                true
            }.collect(_state)
        }
    }

    fun onLoginSucceeded() {
        sessionEnded.value = false
        authSessionRefresh.update { it + 1 }
    }

    fun verifyAuthSession() {
        if (sessionVerificationJob?.isActive == true || sessionEnded.value) return
        sessionVerificationJob = viewModelScope.launch {
            try {
                if (!getAuthSessionUseCase().isLoggedIn) return@launch
                authRepository.reissueToken()
                authSessionRefresh.update { it + 1 }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
            }
        }
    }

    fun onSessionEnded() {
        if (sessionEnded.value) return
        sessionEnded.value = true
        _effect.trySend(RodiAppEffect.NavigateToLogin)
    }

    fun retryPendingOnboardingSync(): Job =
        viewModelScope.launch {
            try {
                syncPendingOnboardingIfAuthenticated()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
            }
        }

    internal suspend fun syncPendingOnboardingIfAuthenticated() {
        if (getAuthSessionUseCase().isLoggedIn) syncPendingOnboardingUseCase()
    }

    private companion object {
        const val RETRY_DELAY_MILLIS = 1_000L
        const val MAX_AUTH_SESSION_RETRY_ATTEMPTS = 3L
    }
}
