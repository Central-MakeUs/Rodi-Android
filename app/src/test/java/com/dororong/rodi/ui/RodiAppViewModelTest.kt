package com.dororong.rodi.ui

import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.auth.GetGuestAccessUseCase
import com.dororong.rodi.core.domain.usecase.entry.GetEntryCompletedUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.SyncPendingOnboardingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase())
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

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase())
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isReady)
        assertFalse(viewModel.state.value.isEntryCompleted)
        assertFalse(viewModel.state.value.hasGuestAccess)
        assertFalse(viewModel.state.value.authSession.isLoggedIn)
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

        val viewModel = RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, syncUseCase())
        advanceUntilIdle()

        viewModel.onSessionEnded()
        guestAccess.value = true
        advanceUntilIdle()

        assertFalse(viewModel.state.value.authSession.isLoggedIn)
        assertFalse(viewModel.state.value.authSession.hasRecentKakaoLogin)
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

        RodiAppViewModel(getEntryCompleted, getGuestAccess, getAuthSession, sync)
        advanceUntilIdle()

        coVerify(exactly = 1) { sync() }
    }

    private fun syncUseCase(): SyncPendingOnboardingUseCase = mockk {
        coEvery { this@mockk() } returns null
    }
}
