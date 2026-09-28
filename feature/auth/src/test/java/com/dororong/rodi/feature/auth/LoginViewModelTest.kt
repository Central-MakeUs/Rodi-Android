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
    fun `로그인에 성공하면 다음 화면으로 이동한다`() = runTest(testDispatcher) {
        val login = mockk<LoginWithKakaoUseCase>()
        coEvery { login("access-token") } returns Result.success(LoginResult.Success(isOnboarded = true, nickname = "로디"))
        val viewModel = viewModel(login = login)

        viewModel.effect.test {
            viewModel.onKakaoLoginResult("access-token")
            advanceUntilIdle()
            assertEquals(LoginEffect.NavigateNext(needsOnboarding = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `온보딩을 마치지 않은 회원이 로그인하면 온보딩으로 이동한다`() = runTest(testDispatcher) {
        val login = mockk<LoginWithKakaoUseCase>()
        coEvery { login("access-token") } returns Result.success(LoginResult.Success(isOnboarded = false, nickname = "로디"))
        val viewModel = viewModel(login = login)

        viewModel.effect.test {
            viewModel.onKakaoLoginResult("access-token")
            advanceUntilIdle()
            assertEquals(LoginEffect.NavigateNext(needsOnboarding = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `탈퇴 유예면 복구 다이얼로그를 열고 복구 후 다음 화면으로 이동한다`() = runTest(testDispatcher) {
        val login = mockk<LoginWithKakaoUseCase>()
        val restore = mockk<RestoreWithKakaoUseCase>()
        coEvery { login("access-token") } returns Result.success(
            LoginResult.WithdrawalPending(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
        )
        coEvery { restore("access-token") } returns Result.success(AccountRestoreResult.Restored(isOnboarded = true, nickname = "로디"))
        val viewModel = viewModel(login, restore)

        viewModel.onKakaoLoginResult("access-token")
        advanceUntilIdle()
        assertEquals(LoginUiState.RecoveryRequired(), viewModel.uiState.value)

        viewModel.effect.test {
            viewModel.onRecoveryConfirm()
            advanceUntilIdle()
            assertEquals(LoginEffect.NavigateNext(needsOnboarding = false), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `온보딩을 마치지 않은 회원을 복구하면 온보딩으로 이동한다`() = runTest(testDispatcher) {
        val login = mockk<LoginWithKakaoUseCase>()
        val restore = mockk<RestoreWithKakaoUseCase>()
        coEvery { login("access-token") } returns Result.success(
            LoginResult.WithdrawalPending(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
        )
        coEvery { restore("access-token") } returns Result.success(AccountRestoreResult.Restored(isOnboarded = false, nickname = "로디"))
        val viewModel = viewModel(login, restore)

        viewModel.onKakaoLoginResult("access-token")
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onRecoveryConfirm()
            advanceUntilIdle()
            assertEquals(LoginEffect.NavigateNext(needsOnboarding = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `로그인이 실패하면 스낵바를 보내고 대기 상태로 돌아간다`() = runTest(testDispatcher) {
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
    fun `둘러보기를 누르면 둘러보기 권한을 주고 이동한다`() = runTest(testDispatcher) {
        val grant = mockk<GrantGuestAccessUseCase>()
        coEvery { grant() } returns Unit
        val login = mockk<LoginWithKakaoUseCase>()
        val viewModel = viewModel(login = login, grant = grant)

        viewModel.effect.test {
            viewModel.onSkipClick()
            advanceUntilIdle()
            assertEquals(LoginEffect.NavigateNext(needsOnboarding = null), awaitItem())
            coVerify(exactly = 1) { grant() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `재가입 대기 계정은 복구 대신 재가입 가능 날짜를 보여준다`() = runTest(testDispatcher) {
        val login = mockk<LoginWithKakaoUseCase>()
        val restore = mockk<RestoreWithKakaoUseCase>()
        coEvery { login("access-token") } returns Result.success(LoginResult.WithdrawalLocked(reRegisterableAt = REJOIN_AT))
        val viewModel = viewModel(login, restore)

        viewModel.onKakaoLoginResult("access-token")
        advanceUntilIdle()

        assertEquals(LoginUiState.WithdrawalLocked(reRegisterableAt = REJOIN_AT), viewModel.uiState.value)
        viewModel.onRecoveryConfirm()
        advanceUntilIdle()
        coVerify(exactly = 0) { restore(any()) }

        viewModel.onWithdrawalLockedDismiss()
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `재가입 대기 계정의 날짜를 쓸 수 없으면 날짜를 불러오지 못했다는 문구를 보여준다`() = runTest(testDispatcher) {
        val login = mockk<LoginWithKakaoUseCase>()
        coEvery { login("access-token") } returns Result.success(LoginResult.WithdrawalLocked(reRegisterableAt = null))
        val viewModel = viewModel(login = login)

        viewModel.effect.test {
            viewModel.onKakaoLoginResult("access-token")
            advanceUntilIdle()
            assertEquals(LoginEffect.ShowSnackbar("재가입 가능 날짜를 불러오지 못했어요."), awaitItem())
            assertEquals(LoginUiState.Idle, viewModel.uiState.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `복구 중 유예 기간이 끝났으면 재가입 가능 날짜를 보여주고 인증 정보를 버린다`() =
        runTest(testDispatcher) {
            val login = mockk<LoginWithKakaoUseCase>()
            val restore = mockk<RestoreWithKakaoUseCase>()
            coEvery { login("access-token") } returns Result.success(
                LoginResult.WithdrawalPending(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
            )
            coEvery { restore("access-token") } returns
                Result.success(AccountRestoreResult.WithdrawalLocked(reRegisterableAt = REJOIN_AT))
            val viewModel = viewModel(login, restore)
            viewModel.onKakaoLoginResult("access-token")
            advanceUntilIdle()

            viewModel.onRecoveryConfirm()
            advanceUntilIdle()

            assertEquals(LoginUiState.WithdrawalLocked(reRegisterableAt = REJOIN_AT), viewModel.uiState.value)
            viewModel.onRecoveryConfirm()
            advanceUntilIdle()
            coVerify(exactly = 1) { restore(any()) }
        }

    private fun viewModel(
        login: LoginWithKakaoUseCase,
        restore: RestoreWithKakaoUseCase = mockk(),
        grant: GrantGuestAccessUseCase = mockk(relaxed = true),
    ) = LoginViewModel(login, restore, grant)

    private companion object {
        val REJOIN_AT: Instant = Instant.parse("2026-09-20T03:00:00Z")
    }
}
