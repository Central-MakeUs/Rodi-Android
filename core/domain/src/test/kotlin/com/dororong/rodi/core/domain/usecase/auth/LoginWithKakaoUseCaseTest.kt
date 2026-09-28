package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.auth.LoginResult
import com.dororong.rodi.core.domain.model.entry.EntryMode
import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import com.dororong.rodi.core.domain.usecase.onboarding.SyncPendingOnboardingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class LoginWithKakaoUseCaseTest {
    @Test
    fun `로그인에 성공하면 서버 닉네임을 저장한다`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository(OnboardingProfile(nickname = "로컬"))
        val entry = entryRepository()
        val sync = syncUseCase()
        val login = LoginResult.Success(isOnboarded = true, nickname = "서버 닉네임")
        coEvery { auth.loginWithKakao("access-token") } returns login

        val result = LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token")

        assertEquals(login, result.getOrThrow())
        coVerify { onboarding.saveProfile(OnboardingProfile(nickname = "서버 닉네임")) }
        coVerify { entry.setCompleted() }
        coVerify { onboarding.clearSyncPending() }
        coVerify(exactly = 0) { sync() }
    }

    @Test
    fun `신규 둘러보기 회원은 이전 온보딩 정보를 지우고 둘러보기 가입을 시작한다`() = runTest {
        val auth = mockk<AuthRepository>()
        val profile = OnboardingProfile(
            nickname = "로컬",
            drivingPeriod = DrivingPeriod.YEARS_3_9,
        )
        val onboarding = onboardingRepository(profile)
        val entry = entryRepository(isCompleted = true, hasGuestAccess = true)
        val sync = syncUseCase()
        coEvery { auth.loginWithKakao("access-token") } returns LoginResult.Success(isOnboarded = false, nickname = "서버")

        LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token").getOrThrow()

        coVerify { onboarding.clear() }
        coVerify { onboarding.saveProfile(OnboardingProfile(nickname = "서버")) }
        coVerify { onboarding.authorizeSync() }
        coVerify { onboarding.clearSyncPending() }
        coVerify { entry.start(EntryMode.GUEST_SIGN_UP) }
        coVerify { entry.clearGuestAccess() }
        coVerify(exactly = 0) { onboarding.savePendingProfile(any()) }
        coVerify(exactly = 0) { sync() }
    }

    @Test
    fun `온보딩 동기화가 실패해도 로그인 성공을 실패로 바꾸지 않는다`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository(OnboardingProfile(drivingPeriod = DrivingPeriod.YEARS_3_9))
        val entry = entryRepository(isCompleted = true, hasGuestAccess = false)
        val sync = syncUseCase()
        val login = LoginResult.Success(isOnboarded = false, nickname = "서버")
        coEvery { auth.loginWithKakao("access-token") } returns login
        coEvery { sync() } throws IllegalStateException("offline")

        val result = LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token")

        assertEquals(login, result.getOrThrow())
        coVerify { entry.start(EntryMode.AUTHENTICATED) }
        coVerify(exactly = 0) { onboarding.clearSyncPending() }
    }

    @Test
    fun `온보딩을 마친 회원은 허용된 대기 동기화를 다시 시도한다`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository(
            profile = OnboardingProfile(drivingPeriod = DrivingPeriod.YEARS_3_9),
            isSyncAuthorized = true,
        )
        val entry = entryRepository()
        val sync = syncUseCase()
        coEvery { auth.loginWithKakao("access-token") } returns LoginResult.Success(isOnboarded = true, nickname = "서버")

        LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token").getOrThrow()

        coVerify { sync() }
        coVerify(exactly = 0) { onboarding.clearSyncPending() }
    }

    @Test
    fun `가입 후 온보딩을 이탈한 회원은 다시 온보딩으로 이동한다`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository()
        val entry = entryRepository(isCompleted = false, hasGuestAccess = false)
        val sync = syncUseCase()
        coEvery { auth.loginWithKakao("access-token") } returns LoginResult.Success(isOnboarded = false, nickname = "서버")

        val result = LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token").getOrThrow()

        assertEquals(LoginResult.Success(isOnboarded = false, nickname = "서버"), result)
        coVerify { entry.start(EntryMode.AUTHENTICATED) }
        coVerify { onboarding.authorizeSync() }
        coVerify(exactly = 0) { entry.setCompleted() }
    }

    @Test
    fun `이 기기에서 마친 온보딩은 처음부터 다시 하지 않고 제출한다`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository(isSyncPending = true, isSyncAuthorized = true)
        val entry = entryRepository(isCompleted = true)
        val sync = syncUseCase()
        coEvery { auth.loginWithKakao("access-token") } returns LoginResult.Success(isOnboarded = false, nickname = "서버")

        val result = LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token").getOrThrow()

        assertEquals(LoginResult.Success(isOnboarded = true, nickname = "서버"), result)
        coVerify { entry.setCompleted() }
        coVerify(exactly = 0) { entry.start(any()) }
        coVerify(exactly = 1) { sync() }
    }

    @Test
    fun `이 기기에서 마친 온보딩을 서버가 받지 않으면 다시 온보딩으로 이동한다`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository(isSyncPending = true, isSyncAuthorized = true)
        val entry = entryRepository(isCompleted = true)
        val sync = syncUseCase(OnboardingSubmissionResult.RetryableFailure)
        coEvery { auth.loginWithKakao("access-token") } returns LoginResult.Success(isOnboarded = false, nickname = "서버")

        val result = LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token").getOrThrow()

        assertEquals(LoginResult.Success(isOnboarded = false, nickname = "서버"), result)
        coVerify { entry.start(EntryMode.AUTHENTICATED) }
        coVerify(exactly = 0) { entry.setCompleted() }
        // 한 번 실패한 제출을 곧바로 다시 보내면, 두 번째가 성공해도 결과는 이미 온보딩으로 정해져 있다.
        coVerify(exactly = 1) { sync() }
    }

    @Test
    fun `탈퇴 유예 상태에서는 닉네임을 저장하지 않는다`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository()
        val entry = entryRepository()
        val sync = syncUseCase()
        val pending = LoginResult.WithdrawalPending(
            java.time.Instant.parse("2026-07-20T00:00:00Z"),
            java.time.Instant.parse("2026-07-23T00:00:00Z"),
        )
        coEvery { auth.loginWithKakao("access-token") } returns pending

        assertEquals(pending, LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token").getOrThrow())
        coVerify(exactly = 0) { onboarding.saveProfile(any()) }
    }

    @Test
    fun `실패는 Result로 감싸고 취소는 다시 던진다`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository()
        val entry = entryRepository()
        val sync = syncUseCase()
        coEvery { auth.loginWithKakao("bad") } throws AuthException.InvalidCredential("boom")
        assertTrue(LoginWithKakaoUseCase(auth, onboarding, entry, sync)("bad").isFailure)

        coEvery { auth.loginWithKakao("cancel") } throws CancellationException("cancelled")
        try {
            LoginWithKakaoUseCase(auth, onboarding, entry, sync)("cancel")
            fail("CancellationException should be rethrown")
        } catch (_: CancellationException) {
        }
    }

    private fun onboardingRepository(
        profile: OnboardingProfile = OnboardingProfile(),
        isSyncAuthorized: Boolean = false,
        isSyncPending: Boolean = false,
    ): OnboardingRepository = mockk {
        coEvery { this@mockk.profile } returns flowOf(profile)
        coEvery { saveProfile(any()) } returns Unit
        coEvery { savePendingProfile(any()) } returns Unit
        coEvery { authorizeSync() } returns Unit
        coEvery { clearSyncPending() } returns Unit
        coEvery { clear() } returns Unit
        coEvery { this@mockk.isSyncAuthorized } returns flowOf(isSyncAuthorized)
        coEvery { this@mockk.isSyncPending } returns flowOf(isSyncPending)
    }

    private fun entryRepository(
        isCompleted: Boolean = false,
        hasGuestAccess: Boolean = false,
    ): EntryRepository = mockk {
        coEvery { this@mockk.isCompleted } returns flowOf(isCompleted)
        coEvery { this@mockk.hasGuestAccess } returns flowOf(hasGuestAccess)
        coEvery { setCompleted() } returns Unit
        coEvery { start(any()) } returns Unit
        coEvery { clearGuestAccess() } returns Unit
    }

    private fun syncUseCase(
        result: OnboardingSubmissionResult = OnboardingSubmissionResult.Submitted,
    ): SyncPendingOnboardingUseCase = mockk {
        coEvery { this@mockk() } returns result
    }
}
