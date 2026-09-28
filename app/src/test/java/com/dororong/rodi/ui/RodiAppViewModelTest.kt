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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RodiAppViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `진입 상태와 둘러보기 여부와 로그인 세션을 합쳐 준비 상태를 만든다`() = runTest(dispatcher) {
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

        assertTrue(viewModel.uiState.value.isReady)
        assertEquals(false, viewModel.uiState.value.isEntryCompleted)
        assertEquals(true, viewModel.uiState.value.hasGuestAccess)
        assertEquals(true, viewModel.uiState.value.authSession.hasRecentKakaoLogin)
    }

    @Test
    fun `세션 조회가 실패하면 비로그인 상태로 준비를 마친다`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        every { getEntryCompleted() } returns flowOf(false)
        every { getGuestAccess() } returns flowOf(true)
        coEvery { getAuthSession() } throws IllegalStateException("token store unavailable")

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), sessionExpirationUseCase())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isReady)
        assertFalse(viewModel.uiState.value.isEntryCompleted)
        assertTrue(viewModel.uiState.value.hasGuestAccess)
        assertFalse(viewModel.uiState.value.authSession.isLoggedIn)
    }

    @Test
    fun `로그인에 성공하면 로그인 세션을 다시 읽는다`() = runTest(dispatcher) {
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

        assertTrue(viewModel.uiState.value.authSession.isLoggedIn)
        assertTrue(viewModel.uiState.value.authSession.hasRecentKakaoLogin)
        coVerify(atLeast = 2) { getAuthSession() }
    }

    @Test
    fun `세션 조회가 실패해도 이후 갱신이 성공하면 로그인 세션을 복구한다`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } throws IllegalStateException("token store unavailable")

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase(), sessionExpirationUseCase())
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.authSession.isLoggedIn)

        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        viewModel.onLoginSucceeded()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.authSession.isLoggedIn)
        coVerify(atLeast = 2) { getAuthSession() }
    }

    @Test
    fun `진입 상태 수집이 실패하면 간격을 두고 다시 시도한다`() = runTest(dispatcher) {
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
        assertTrue(viewModel.uiState.value.isReady)

        advanceTimeBy(999L)
        runCurrent()

        assertEquals(1, attempts)

        advanceTimeBy(1L)
        runCurrent()
        advanceUntilIdle()

        assertEquals(2, attempts)
        assertTrue(viewModel.uiState.value.isEntryCompleted)
    }

    @Test
    fun `세션이 끝난 뒤 둘러보기 여부가 바뀌어도 로그아웃 상태를 유지한다`() = runTest(dispatcher) {
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

        val signOuts = MutableSharedFlow<Unit>()
        val viewModel = RodiAppViewModel(
            getEntryCompleted,
            getGuestAccess,
            getAuthSession,
            syncUseCase(),
            sessionExpirationUseCase(signOuts = signOuts),
        )
        advanceUntilIdle()

        signOuts.emit(Unit)
        guestAccess.value = true
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.authSession.isLoggedIn)
        assertTrue(viewModel.uiState.value.authSession.hasRecentKakaoLogin)
    }

    @Test
    fun `세션이 만료되면 로그인 화면 이동을 한 번 보내고 로그인 상태를 지운다`() = runTest(dispatcher) {
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

        assertFalse(viewModel.uiState.value.authSession.isLoggedIn)
        assertEquals(listOf(RodiAppEffect.NavigateToLogin(showSessionExpiredMessage = true)), effects)
        effectCollection.cancel()
    }

    @Test
    fun `사용자가 로그아웃하면 만료 안내 없이 로그인 화면으로 한 번 이동한다`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val signOuts = MutableSharedFlow<Unit>()
        val expirations = MutableStateFlow(false)
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        val viewModel = RodiAppViewModel(
            getEntryCompleted,
            getGuestAccess,
            getAuthSession,
            syncUseCase(),
            sessionExpirationUseCase(expirations, signOuts),
        )
        val effects = mutableListOf<RodiAppEffect>()
        val effectCollection = launch { viewModel.effect.collect(effects::add) }
        advanceUntilIdle()

        signOuts.emit(Unit)
        expirations.value = true
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.authSession.isLoggedIn)
        assertEquals(listOf(RodiAppEffect.NavigateToLogin(showSessionExpiredMessage = false)), effects)
        effectCollection.cancel()
    }

    @Test
    fun `새로 로그인하면 다음 로그아웃 때 다시 로그인 화면으로 이동한다`() = runTest(dispatcher) {
        val getEntryCompleted = mockk<GetEntryCompletedUseCase>()
        val getGuestAccess = mockk<GetGuestAccessUseCase>()
        val getAuthSession = mockk<GetAuthSessionUseCase>()
        val signOuts = MutableSharedFlow<Unit>()
        every { getEntryCompleted() } returns flowOf(true)
        every { getGuestAccess() } returns flowOf(false)
        coEvery { getAuthSession() } returns AuthSession(isLoggedIn = true, hasRecentKakaoLogin = true)
        val viewModel = RodiAppViewModel(
            getEntryCompleted,
            getGuestAccess,
            getAuthSession,
            syncUseCase(),
            sessionExpirationUseCase(signOuts = signOuts),
        )
        val effects = mutableListOf<RodiAppEffect>()
        val effectCollection = launch { viewModel.effect.collect(effects::add) }
        advanceUntilIdle()

        signOuts.emit(Unit)
        advanceUntilIdle()
        viewModel.onLoginSucceeded()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.authSession.isLoggedIn)
        signOuts.emit(Unit)
        advanceUntilIdle()

        assertEquals(2, effects.size)
        effectCollection.cancel()
    }

    @Test
    fun `ViewModel 생성 전에 만료된 세션도 로그인 화면으로 이동한다`() = runTest(dispatcher) {
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

        assertFalse(viewModel.uiState.value.authSession.isLoggedIn)
        assertEquals(listOf(RodiAppEffect.NavigateToLogin(showSessionExpiredMessage = true)), effects)
        effectCollection.cancel()
    }

    @Test
    fun `로그인 상태로 앱이 시작되면 대기 중인 온보딩 동기화를 다시 시도한다`() = runTest(dispatcher) {
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
    fun `앱으로 돌아오면 로그인 세션을 재발급한다`() = runTest(dispatcher) {
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
    fun `앱으로 돌아와도 로그아웃 상태면 재발급하지 않는다`() = runTest(dispatcher) {
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
    fun `앱으로 돌아오면 세션 확인을 마친 뒤 대기 중인 온보딩을 동기화한다`() = runTest(dispatcher) {
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
    fun `세션 재발급이 실패하면 대기 중인 온보딩을 동기화하지 않는다`() = runTest(dispatcher) {
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
    fun `앱 복귀 중 재발급이 네트워크 오류로 실패해도 멈추지 않고 동기화하지 않는다`() = runTest(dispatcher) {
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
    fun `재발급이 진행 중이면 세션 확인을 중복으로 실행하지 않는다`() = runTest(dispatcher) {
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
    fun `로그아웃 상태에서는 대기 중인 온보딩을 동기화하지 않는다`() = runTest(dispatcher) {
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
    fun `대기 중인 온보딩 동기화가 실패해도 앱 준비를 막지 않는다`() = runTest(dispatcher) {
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

        assertTrue(viewModel.uiState.value.isReady)
        coVerify(exactly = 1) { sync() }
    }

    @Test
    fun `대기 중인 온보딩 동기화 재시도 중 취소를 그대로 전파한다`() = runTest(dispatcher) {
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
        signOuts: kotlinx.coroutines.flow.Flow<Unit> = emptyFlow(),
    ): AuthRepository = mockk {
        every { observeSessionExpiration() } returns expirations
        every { observeSignOut() } returns signOuts
    }
}
