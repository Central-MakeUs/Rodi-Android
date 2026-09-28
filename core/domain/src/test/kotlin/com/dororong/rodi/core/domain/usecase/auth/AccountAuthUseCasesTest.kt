package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.entry.EntryMode
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.flowOf
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AccountAuthUseCasesTest {
    @Test
    fun `저장소가 성공하면 재발급 결과가 성공이다`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.reissueToken() } returns Unit

        val result = ReissueAuthTokenUseCase(repository)()

        assertTrue(result.isSuccess)
        coVerify { repository.reissueToken() }
    }

    @Test
    fun `복구 실패를 Result로 감싼다`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.restoreWithKakao("credential") } throws AuthException.RecoveryExpired("기간 만료")

        val result = RestoreWithKakaoUseCase(repository, onboardingRepository(), entryRepository())("credential")

        assertTrue(result.isFailure)
        assertEquals("기간 만료", result.exceptionOrNull()?.message)
    }

    @Test
    fun `저장소가 세션을 끝내면 로그아웃이 성공한다`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.logout() } returns Unit

        val result = LogoutUseCase(repository)()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `로그아웃 중 취소를 그대로 전파한다`() = runTest {
        val repository = mockk<AuthRepository>()
        val cancellation = CancellationException("cancelled")
        coEvery { repository.logout() } throws cancellation

        try {
            LogoutUseCase(repository)()
        } catch (thrown: CancellationException) {
            assertSame(cancellation, thrown)
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }

    @Test
    fun `복구는 저장소의 도메인 결과를 그대로 반환한다`() = runTest {
        val repository = mockk<AuthRepository>()
        val expected = AccountRestoreResult.WithdrawalPending(
            withdrawalRequestedAt = Instant.parse("2026-07-13T00:00:00Z"),
            recoverableUntil = Instant.parse("2026-07-16T00:00:00Z"),
        )
        coEvery { repository.restoreWithKakao("credential") } returns expected

        val result = RestoreWithKakaoUseCase(repository, onboardingRepository(), entryRepository())("credential")

        assertEquals(expected, result.getOrThrow())
    }

    @Test
    fun `온보딩을 마친 회원을 복구하면 진입 완료를 저장한다`() = runTest {
        val repository = mockk<AuthRepository>()
        val entry = entryRepository()
        val restored = AccountRestoreResult.Restored(isOnboarded = true, nickname = "로디")
        coEvery { repository.restoreWithKakao("credential") } returns restored

        val result = RestoreWithKakaoUseCase(repository, onboardingRepository(), entry)("credential")

        assertEquals(restored, result.getOrThrow())
        coVerify { entry.setCompleted() }
        coVerify { entry.clearGuestAccess() }
    }

    @Test
    fun `온보딩을 마치지 않은 회원을 복구하면 온보딩으로 보낸다`() = runTest {
        val repository = mockk<AuthRepository>()
        val entry = entryRepository()
        val restored = AccountRestoreResult.Restored(isOnboarded = false, nickname = "로디")
        coEvery { repository.restoreWithKakao("credential") } returns restored

        RestoreWithKakaoUseCase(repository, onboardingRepository(), entry)("credential").getOrThrow()

        coVerify { entry.start(EntryMode.AUTHENTICATED) }
        coVerify(exactly = 0) { entry.setCompleted() }
        coVerify { entry.clearGuestAccess() }
    }

    @Test
    fun `온보딩을 마치지 않은 둘러보기 사용자를 복구하면 둘러보기 가입을 시작한다`() = runTest {
        val repository = mockk<AuthRepository>()
        val entry = entryRepository(hasGuestAccess = true)
        coEvery { repository.restoreWithKakao("credential") } returns
            AccountRestoreResult.Restored(isOnboarded = false, nickname = "로디")

        RestoreWithKakaoUseCase(repository, onboardingRepository(), entry)("credential").getOrThrow()

        coVerify { entry.start(EntryMode.GUEST_SIGN_UP) }
    }

    @Test
    fun `로컬 반영이 실패해도 복구 성공을 유지한다`() = runTest {
        val repository = mockk<AuthRepository>()
        val onboarding = onboardingRepository()
        val entry = entryRepository()
        val restored = AccountRestoreResult.Restored(isOnboarded = true, nickname = "로디")
        coEvery { repository.restoreWithKakao("credential") } returns restored
        coEvery { onboarding.saveProfile(any()) } throws IllegalStateException("local write failed")

        val result = RestoreWithKakaoUseCase(repository, onboarding, entry)("credential")

        assertEquals(restored, result.getOrThrow())
        coVerify { onboarding.clearSyncPending() }
        coVerify { entry.setCompleted() }
        coVerify { entry.clearGuestAccess() }
    }

    @Test
    fun `복구 후 로컬 반영 중 취소를 그대로 전파한다`() = runTest {
        val repository = mockk<AuthRepository>()
        val onboarding = onboardingRepository()
        val restored = AccountRestoreResult.Restored(isOnboarded = true, nickname = "로디")
        coEvery { repository.restoreWithKakao("credential") } returns restored
        coEvery { onboarding.saveProfile(any()) } throws CancellationException("cancelled")

        try {
            RestoreWithKakaoUseCase(repository, onboarding, entryRepository())("credential")
        } catch (_: CancellationException) {
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }

    private fun onboardingRepository(): OnboardingRepository = mockk {
        coEvery { profile } returns flowOf(OnboardingProfile())
        coEvery { saveProfile(any()) } returns Unit
        coEvery { clearSyncPending() } returns Unit
    }

    private fun entryRepository(hasGuestAccess: Boolean = false): EntryRepository = mockk {
        coEvery { this@mockk.hasGuestAccess } returns flowOf(hasGuestAccess)
        coEvery { start(any()) } returns Unit
        coEvery { setCompleted() } returns Unit
        coEvery { clearGuestAccess() } returns Unit
    }
}
