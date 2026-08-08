package com.dororong.rodi.ui

import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.auth.GetGuestAccessUseCase
import com.dororong.rodi.core.domain.usecase.entry.GetEntryCompletedUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.SyncPendingOnboardingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RodiAppViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `combines entry guest and auth session into ready state`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        every { getEntryCompleted() } returns flowOf(false)
        every { getGuestAccess() } returns flowOf(true)
        coEvery { getAuthSession() } returns AuthSession(
            isLoggedIn = false,
            hasRecentKakaoLogin = true,
        )

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), sessionExpirationUseCase())
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isReady)
        assertEquals(false, viewModel.state.value.isEntryCompleted)
        assertEquals(true, viewModel.state.value.hasGuestAccess)
        assertEquals(true, viewModel.state.value.authSession.hasRecentKakaoLogin)
    }

    @Test
    fun `falls back to a ready guest state when session lookup fails`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        every { getEntryCompleted() } returns flowOf(false)
        every { getGuestAccess() } returns flowOf(true)
        coEvery { getAuthSession() } throws IllegalStateException("token store unavailable")

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), sessionExpirationUseCase())
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isReady)
        assertFalse(viewModel.state.value.isEntryCompleted)
        assertTrue(viewModel.state.value.hasGuestAccess)
        assertFalse(viewModel.state.value.authSession.isLoggedIn)
    }

    @Test
    fun `refreshes the auth session after login succeeds`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = false, hasRecentKakaoLogin = false)

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), sessionExpirationUseCase())
        advanceUntilIdle()

        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        viewModel.onLoginSucceeded()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.authSession.isLoggedIn)
        assertTrue(viewModel.state.value.authSession.hasRecentKakaoLogin)
        coVerify(atLeast = 2) { getAuthSession() }
    }

    @Test
    fun `recovers auth session collection when a later refresh succeeds`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } throws IllegalStateException("token store unavailable")

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), sessionExpirationUseCase())
        advanceUntilIdle()
        assertFalse(viewModel.state.value.authSession.isLoggedIn)

        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        viewModel.onLoginSucceeded()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.authSession.isLoggedIn)
        coVerify(atLeast = 2) { getAuthSession() }
    }

    @Test
    fun `retries an entry state collection failure with backoff`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        var attempts = 0
        every { getEntryCompleted() } answers {
            kotlinx.coroutines.flow.flow {
                if (attempts++ == 0) throw IllegalStateException("DataStore unavailable")
                emit(true)
            }
        }
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = false, hasRecentKakaoLogin = false)

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), sessionExpirationUseCase())
        runCurrent()

        assertEquals(1, attempts)
        assertTrue(viewModel.state.value.isReady)

        advanceTimeBy(999L)
        runCurrent()

        assertEquals(1, attempts)

        advanceTimeBy(1L)
        runCurrent()
        advanceUntilIdle()

        assertEquals(2, attempts)
        assertTrue(viewModel.state.value.isEntryCompleted)
    }

    @Test
    fun `keeps the logged out session when guest access changes after session ends`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val guestAccess = MutableStateFlow(false)
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns guestAccess
        coEvery { getAuthSession() } returns AuthSession(
            isLoggedIn = true,
            hasRecentKakaoLogin = true,
        )

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), sessionExpirationUseCase())
        advanceUntilIdle()

        viewModel.onSessionEnded()
        guestAccess.value = true
        advanceUntilIdle()

        assertFalse(viewModel.state.value.authSession.isLoggedIn)
        assertTrue(viewModel.state.value.authSession.hasRecentKakaoLogin)
    }

    @Test
    fun `session expiration emits one login navigation and clears the authenticated state`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val expirations = MutableStateFlow(false)
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        val viewModel = RodiAppViewModel(
            getEntryCompleted,
            getGuestAccess,
            getAuthSession,
            syncUseCase(),
            sessionExpirationUseCase(expirations),
        )
        val effects = mutableListOf<RodiAppEffect>()
        val effectCollection = launch { viewModel.effect.collect(effects::add) }
        advanceUntilIdle()

        expirations.value = true
        advanceUntilIdle()

        assertFalse(viewModel.state.value.authSession.isLoggedIn)
        assertEquals(listOf(RodiAppEffect.NavigateToLogin), effects)
        effectCollection.cancel()
    }

    @Test
    fun `expired session before ViewModel creation still navigates to login`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        val viewModel = RodiAppViewModel(
            getEntryCompleted,
            getGuestAccess,
            getAuthSession,
            syncUseCase(),
            sessionExpirationUseCase(MutableStateFlow(true)),
        )
        val effects = mutableListOf<RodiAppEffect>()
        val effectCollection = launch { viewModel.effect.collect(effects::add) }
        advanceUntilIdle()

        assertFalse(viewModel.state.value.authSession.isLoggedIn)
        assertEquals(listOf(RodiAppEffect.NavigateToLogin), effects)
        effectCollection.cancel()
    }

    @Test
    fun `retries pending onboarding sync when authenticated app starts`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val sync = syncUseCase()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(true, true)

        RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, sync, sessionExpirationUseCase())
        advanceUntilIdle()

        coVerify(exactly = 1) { sync() }
    }

    @Test
    fun `reissues an authenticated session before syncing on app resume`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val authRepository = sessionExpirationUseCase()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        coEvery { authRepository.reissueToken() } returns Unit

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), authRepository)
        viewModel.onAppResumed()
        advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.reissueToken() }
    }

    @Test
    fun `does not reissue a signed out session on app resume`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val authRepository = sessionExpirationUseCase()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = false, hasRecentKakaoLogin = true)

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), authRepository)
        viewModel.onAppResumed()
        advanceUntilIdle()

        coVerify(exactly = 0) { authRepository.reissueToken() }
    }

    @Test
    fun `completes session verification before syncing pending onboarding on app resume`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val sync = syncUseCase()
        val authRepository = sessionExpirationUseCase()
        val calls = mutableListOf<String>()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = false, hasRecentKakaoLogin = true)
        coEvery { sync() } answers {
            calls += "sync"
            null
        }
        coEvery { authRepository.reissueToken() } answers { calls += "reissue" }
        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, sync, authRepository)
        advanceUntilIdle()

        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        viewModel.onAppResumed()
        advanceUntilIdle()

        assertEquals(listOf("reissue", "sync"), calls)
    }

    @Test
    fun `does not sync pending onboarding when session reissue fails`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val sync = syncUseCase()
        val authRepository = sessionExpirationUseCase()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = false, hasRecentKakaoLogin = true)
        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, sync, authRepository)
        advanceUntilIdle()

        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        coEvery { authRepository.reissueToken() } throws AuthException.SessionRevoked("revoked")
        viewModel.onAppResumed()
        advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.reissueToken() }
        coVerify(exactly = 0) { sync() }
    }

    @Test
    fun `network failure during reissue on app resume does not crash or sync`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val sync = syncUseCase()
        val authRepository = sessionExpirationUseCase()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = false, hasRecentKakaoLogin = true)
        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, sync, authRepository)
        advanceUntilIdle()

        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        coEvery { authRepository.reissueToken() } throws java.io.IOException("network unavailable")
        viewModel.onAppResumed()
        advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.reissueToken() }
        coVerify(exactly = 0) { sync() }
    }

    @Test
    fun `does not run duplicate session verification while a reissue is active`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val authRepository = sessionExpirationUseCase()
        val reissueStarted = CompletableDeferred<Unit>()
        val allowReissueCompletion = CompletableDeferred<Unit>()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = false, hasRecentKakaoLogin = true)
        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), authRepository)
        advanceUntilIdle()

        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        coEvery { authRepository.reissueToken() } coAnswers {
            reissueStarted.complete(Unit)
            allowReissueCompletion.await()
        }
        viewModel.onAppResumed()
        runCurrent()
        reissueStarted.await()
        viewModel.onAppResumed()
        allowReissueCompletion.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.reissueToken() }
    }

    @Test
    fun `does not sync pending onboarding when signed out`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val sync = syncUseCase()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(true)
        coEvery { getAuthSession() } returns AuthSession(false, true)

        RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, sync, sessionExpirationUseCase())
        advanceUntilIdle()

        coVerify(exactly = 0) { sync() }
    }

    @Test
    fun `pending onboarding sync failure does not block app readiness`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val sync = syncUseCase()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(true, true)
        coEvery { sync() } throws IllegalStateException("offline")

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, sync, sessionExpirationUseCase())
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isReady)
        coVerify(exactly = 1) { sync() }
    }

    @Test
    fun `retry pending onboarding sync propagates cancellation`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val sync = syncUseCase()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(false, true)
        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, sync, sessionExpirationUseCase())
        advanceUntilIdle()
        coEvery { getAuthSession() } returns AuthSession(true, true)
        coEvery { sync() } throws CancellationException("cancelled")

        var completionCause: Throwable? = null
        val retryJob = viewModel.retryPendingOnboardingSync()
        retryJob.invokeOnCompletion { completionCause = it }
        advanceUntilIdle()

        coVerify(exactly = 1) { sync() }
        assertTrue(retryJob.isCancelled)
        assertTrue(completionCause is CancellationException)
    }

    private fun syncUseCase(): SyncPendingOnboardingUseCase = mockk {
        coEvery { this@mockk() } returns null
    }

    private fun sessionExpirationUseCase(
        expirations: kotlinx.coroutines.flow.Flow<Boolean> = flowOf(false),
    ): AuthRepository = mockk {
        every { observeSessionExpiration() } returns expirations
    }
}
