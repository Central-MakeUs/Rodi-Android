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
    fun `로그아웃이 성공하면 화면 이동은 앱 세션 담당에게 맡긴다`() = runTest(testDispatcher) {
        val logout = mockk<LogoutUseCase>()
        val withdraw = mockk<WithdrawUseCase>()
        coEvery { logout() } returns Result.success(Unit)
        val viewModel = AccountSettingsViewModel(logout, withdraw)

        viewModel.effect.test {
            viewModel.confirm(AccountAction.Logout)
            advanceUntilIdle()

            expectNoEvents()
        }
        assertEquals(false, viewModel.uiState.value.isSubmitting)
        coVerify(exactly = 1) { logout() }
        coVerify(exactly = 0) { withdraw() }
    }

    @Test
    fun `탈퇴가 실패하면 세션을 유지하고 오류를 보여준다`() = runTest(testDispatcher) {
        val logout = mockk<LogoutUseCase>()
        val withdraw = mockk<WithdrawUseCase>()
        coEvery { withdraw() } returns Result.failure(IllegalStateException("탈퇴에 실패했습니다."))
        val viewModel = AccountSettingsViewModel(logout, withdraw)

        viewModel.effect.test {
            viewModel.confirm(AccountAction.Withdraw)
            advanceUntilIdle()

            assertEquals(
                AccountSettingsEffect.ShowError("요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요."),
                awaitItem(),
            )
        }
        assertEquals(false, viewModel.uiState.value.isSubmitting)
        coVerify(exactly = 1) { withdraw() }
        coVerify(exactly = 0) { logout() }
    }
}
