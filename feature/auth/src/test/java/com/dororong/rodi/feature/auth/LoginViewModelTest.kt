package com.dororong.rodi.feature.auth

import app.cash.turbine.test
import com.dororong.rodi.core.domain.AuthException
import com.dororong.rodi.core.domain.usecase.GrantGuestAccessUseCase
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
        coEvery { useCase("access-token") } returns Result.success(true)
        val grantGuestAccessUseCase = mockk<GrantGuestAccessUseCase>()
        coEvery { grantGuestAccessUseCase() } returns Unit
        val viewModel = LoginViewModel(useCase, grantGuestAccessUseCase)

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
        coEvery { useCase("access-token") } returns
            Result.failure(AuthException.InvalidCredential("카카오 인증에 실패했습니다."))
        val grantGuestAccessUseCase = mockk<GrantGuestAccessUseCase>()
        coEvery { grantGuestAccessUseCase() } returns Unit
        val viewModel = LoginViewModel(useCase, grantGuestAccessUseCase)

        viewModel.effect.test {
            viewModel.onKakaoLoginResult("access-token")
            advanceUntilIdle()

            assertEquals(
                LoginEffect.ShowSnackbar("카카오 인증에 실패했습니다."),
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
        val grantGuestAccessUseCase = mockk<GrantGuestAccessUseCase>()
        coEvery { grantGuestAccessUseCase() } returns Unit
        val viewModel = LoginViewModel(useCase, grantGuestAccessUseCase)

        viewModel.effect.test {
            viewModel.onSkipClick()
            advanceUntilIdle()

            assertEquals(LoginEffect.NavigateNext, awaitItem())
            coVerify(exactly = 0) { useCase(any()) }
            coVerify(exactly = 1) { grantGuestAccessUseCase() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSkipClick emits snackbar when guest access cannot be saved`() = runTest(testDispatcher) {
        val useCase = mockk<LoginWithKakaoUseCase>()
        val grantGuestAccessUseCase = mockk<GrantGuestAccessUseCase>()
        coEvery { grantGuestAccessUseCase() } throws IllegalStateException("failed")
        val viewModel = LoginViewModel(useCase, grantGuestAccessUseCase)

        viewModel.effect.test {
            viewModel.onSkipClick()
            advanceUntilIdle()

            assertEquals(
                LoginEffect.ShowSnackbar("둘러보기를 시작할 수 없습니다. 다시 시도해주세요."),
                awaitItem(),
            )
            coVerify(exactly = 0) { useCase(any()) }
            coVerify(exactly = 1) { grantGuestAccessUseCase() }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
