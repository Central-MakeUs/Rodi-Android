package com.dororong.rodi.feature.auth

import app.cash.turbine.test
import com.dororong.rodi.core.domain.usecase.LoginWithKakaoUseCase
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
class LoginViewModelTest {

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
    fun `onKakaoLoginResult emits NavigateNext when login succeeds`() = runTest(testDispatcher) {
        val useCase = mockk<LoginWithKakaoUseCase>()
        coEvery { useCase("access-token") } returns Result.success(Unit)
        val viewModel = LoginViewModel(useCase)

        viewModel.effect.test {
            viewModel.onKakaoLoginResult("access-token")
            advanceUntilIdle()

            assertEquals(LoginEffect.NavigateNext, awaitItem())
            coVerify(exactly = 1) { useCase("access-token") }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onKakaoLoginResult emits snackbar and returns Idle when login fails`() = runTest(testDispatcher) {
        val useCase = mockk<LoginWithKakaoUseCase>()
        coEvery { useCase("access-token") } returns Result.failure(RuntimeException("boom"))
        val viewModel = LoginViewModel(useCase)

        viewModel.effect.test {
            viewModel.onKakaoLoginResult("access-token")
            advanceUntilIdle()

            assertEquals(
                LoginEffect.ShowSnackbar("로그인에 실패했습니다. 잠시 후 다시 시도해주세요."),
                awaitItem(),
            )
            assertEquals(LoginUiState.Idle, viewModel.uiState.value)
            coVerify(exactly = 1) { useCase("access-token") }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSkipClick emits NavigateNext without invoking use case`() = runTest(testDispatcher) {
        val useCase = mockk<LoginWithKakaoUseCase>()
        val viewModel = LoginViewModel(useCase)

        viewModel.effect.test {
            viewModel.onSkipClick()
            advanceUntilIdle()

            assertEquals(LoginEffect.NavigateNext, awaitItem())
            coVerify(exactly = 0) { useCase(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
