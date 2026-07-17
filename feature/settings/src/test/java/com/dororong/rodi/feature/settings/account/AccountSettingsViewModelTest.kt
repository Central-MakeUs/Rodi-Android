package com.dororong.rodi.feature.settings.account

import app.cash.turbine.test
import com.dororong.rodi.core.domain.usecase.auth.LogoutUseCase
import com.dororong.rodi.core.domain.usecase.member.WithdrawUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits session ended after logout succeeds`() = runTest(testDispatcher) {
        val logout = mockk<LogoutUseCase>()
        val withdraw = mockk<WithdrawUseCase>()
        coEvery { logout() } returns Result.success(Unit)
        val viewModel = AccountSettingsViewModel(logout, withdraw)

        viewModel.effects.test {
            viewModel.confirm(AccountAction.Logout)
            advanceUntilIdle()

            assertEquals(AccountSettingsEffect.SessionEnded, awaitItem())
        }
        coVerify(exactly = 1) { logout() }
        coVerify(exactly = 0) { withdraw() }
    }

    @Test
    fun `keeps session and shows error after withdrawal fails`() = runTest(testDispatcher) {
        val logout = mockk<LogoutUseCase>()
        val withdraw = mockk<WithdrawUseCase>()
        coEvery { withdraw() } returns Result.failure(IllegalStateException("탈퇴에 실패했습니다."))
        val viewModel = AccountSettingsViewModel(logout, withdraw)

        viewModel.effects.test {
            viewModel.confirm(AccountAction.Withdraw)
            advanceUntilIdle()

            assertEquals(
                AccountSettingsEffect.ShowError("탈퇴에 실패했습니다."),
                awaitItem(),
            )
        }
        assertEquals(false, viewModel.uiState.value.isSubmitting)
        coVerify(exactly = 1) { withdraw() }
        coVerify(exactly = 0) { logout() }
    }
}
