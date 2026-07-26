package com.dororong.rodi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.auth.GetGuestAccessUseCase
import com.dororong.rodi.core.domain.usecase.entry.GetEntryCompletedUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.SyncPendingOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RodiAppViewModel @Inject constructor(
    getEntryCompletedUseCase: GetEntryCompletedUseCase,
    getGuestAccessUseCase: GetGuestAccessUseCase,
    private val getAuthSessionUseCase: GetAuthSessionUseCase,
    private val syncPendingOnboardingUseCase: SyncPendingOnboardingUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(RodiAppUiState())
    private val authSessionRefresh = MutableStateFlow(0)
    private val sessionEnded = MutableStateFlow(false)
    val state: StateFlow<RodiAppUiState> = _state.asStateFlow()

    init {
        retryPendingOnboardingSync()
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
            }.retryWhen { error, _ ->
                if (error is CancellationException) throw error
                _state.value = RodiAppUiState(isReady = true)
                delay(RETRY_DELAY_MILLIS)
                true
            }.collect(_state)
        }
    }

    fun onLoginSucceeded() {
        sessionEnded.value = false
        authSessionRefresh.value += 1
    }

    fun onSessionEnded() {
        sessionEnded.value = true
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
    }
}
