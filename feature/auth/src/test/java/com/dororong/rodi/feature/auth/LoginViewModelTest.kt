package com.dororong.rodi.feature.auth

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.auth.LoginResult
import com.dororong.rodi.core.domain.usecase.auth.GrantGuestAccessUseCase
import com.dororong.rodi.core.domain.usecase.auth.LoginWithKakaoUseCase
import com.dororong.rodi.core.domain.usecase.auth.RestoreWithKakaoUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
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
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `successful login navigates next`() = runTest(testDispatcher) {
        val login = mockk<LoginWithKakaoUseCase>()
        coEvery { login("access-token") } returns Result.success(LoginResult.Success(false, "로디"))
        val viewModel = viewModel(login = login)

        viewModel.effect.test {
            viewModel.onKakaoLoginResult("access-token")
            advanceUntilIdle()
            assertEquals(LoginEffect.NavigateNext(isNewMember = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `withdrawal pending opens recovery and confirmation navigates after restore`() = runTest(testDispatcher) {
        val login = mockk<LoginWithKakaoUseCase>()
        val restore = mockk<RestoreWithKakaoUseCase>()
        coEvery { login("access-token") } returns Result.success(
            LoginResult.WithdrawalPending(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
        )
        coEvery { restore("access-token") } returns Result.success(AccountRestoreResult.Restored(false, "로디"))
        val viewModel = viewModel(login, restore)

        viewModel.onKakaoLoginResult("access-token")
        advanceUntilIdle()
        assertEquals(LoginUiState.RecoveryRequired(), viewModel.uiState.value)

        viewModel.effect.test {
            viewModel.onRecoveryConfirm()
            advanceUntilIdle()
            assertEquals(LoginEffect.NavigateNext(isNewMember = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recovery preserves new member routing`() = runTest(testDispatcher) {
        val login = mockk<LoginWithKakaoUseCase>()
        val restore = mockk<RestoreWithKakaoUseCase>()
        coEvery { login("access-token") } returns Result.success(
            LoginResult.WithdrawalPending(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
        )
        coEvery { restore("access-token") } returns Result.success(AccountRestoreResult.Restored(true, "로디"))
        val viewModel = viewModel(login, restore)

        viewModel.onKakaoLoginResult("access-token")
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onRecoveryConfirm()
            advanceUntilIdle()
            assertEquals(LoginEffect.NavigateNext(isNewMember = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login failure emits snackbar and returns idle`() = runTest(testDispatcher) {
        val login = mockk<LoginWithKakaoUseCase>()
        coEvery { login("access-token") } returns Result.failure(
            AuthException.InvalidCredential("카카오 인증에 실패했습니다."),
        )
        val viewModel = viewModel(login = login)

        viewModel.effect.test {
            viewModel.onKakaoLoginResult("access-token")
            advanceUntilIdle()
            assertEquals(LoginEffect.ShowSnackbar("카카오 인증에 실패했습니다."), awaitItem())
            assertEquals(LoginUiState.Idle, viewModel.uiState.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skip grants guest access and navigates`() = runTest(testDispatcher) {
        val grant = mockk<GrantGuestAccessUseCase>()
        coEvery { grant() } returns Unit
        val login = mockk<LoginWithKakaoUseCase>()
        val viewModel = viewModel(login = login, grant = grant)

        viewModel.effect.test {
            viewModel.onSkipClick()
            advanceUntilIdle()
            assertEquals(LoginEffect.NavigateNext(isNewMember = null), awaitItem())
            coVerify(exactly = 1) { grant() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        login: LoginWithKakaoUseCase,
        restore: RestoreWithKakaoUseCase = mockk(),
        grant: GrantGuestAccessUseCase = mockk(relaxed = true),
    ) = LoginViewModel(login, restore, grant)
}
