package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.auth.LoginResult
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
    fun `successful login persists server nickname`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository(OnboardingProfile(nickname = "로컬"))
        val entry = entryRepository()
        val sync = syncUseCase()
        val login = LoginResult.Success(isNewMember = false, nickname = "서버 닉네임")
        coEvery { auth.loginWithKakao("access-token") } returns login

        val result = LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token")

        assertEquals(login, result.getOrThrow())
        coVerify { onboarding.saveProfile(OnboardingProfile(nickname = "서버 닉네임")) }
        coVerify { entry.setCompleted() }
        coVerify { onboarding.clearSyncPending() }
        coVerify(exactly = 0) { sync() }
    }

    @Test
    fun `complete guest onboarding is submitted after new member login`() = runTest {
        val auth = mockk<AuthRepository>()
        val profile = OnboardingProfile(
            nickname = "로컬",
            drivingPeriod = DrivingPeriod.YEARS_3_9,
        )
        val onboarding = onboardingRepository(profile)
        val entry = entryRepository(isCompleted = true, hasGuestAccess = true)
        val sync = syncUseCase()
        coEvery { auth.loginWithKakao("access-token") } returns LoginResult.Success(true, "서버")

        LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token").getOrThrow()

        coVerify { onboarding.savePendingProfile(profile.copy(nickname = "서버")) }
        coVerify { onboarding.authorizeSync() }
        coVerify { entry.clearGuestAccess() }
        coVerify(exactly = 0) { entry.setCompleted() }
        coVerify { sync() }
    }

    @Test
    fun `onboarding sync failure does not turn successful login into failure`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository(OnboardingProfile(drivingPeriod = DrivingPeriod.YEARS_3_9))
        val entry = entryRepository(isCompleted = true, hasGuestAccess = true)
        val sync = syncUseCase()
        val login = LoginResult.Success(true, "서버")
        coEvery { auth.loginWithKakao("access-token") } returns login
        coEvery { sync() } throws IllegalStateException("offline")

        val result = LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token")

        assertEquals(login, result.getOrThrow())
        coVerify(exactly = 0) { onboarding.clearSyncPending() }
    }

    @Test
    fun `authorized pending sync retries even when a later login is no longer new`() = runTest {
        val auth = mockk<AuthRepository>()
        val onboarding = onboardingRepository(
            profile = OnboardingProfile(drivingPeriod = DrivingPeriod.YEARS_3_9),
            isSyncAuthorized = true,
        )
        val entry = entryRepository()
        val sync = syncUseCase()
        coEvery { auth.loginWithKakao("access-token") } returns LoginResult.Success(false, "서버")

        LoginWithKakaoUseCase(auth, onboarding, entry, sync)("access-token").getOrThrow()

        coVerify { sync() }
        coVerify(exactly = 0) { onboarding.clearSyncPending() }
    }

    @Test
    fun `withdrawal pending does not persist a nickname`() = runTest {
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
    fun `invoke wraps failure and rethrows cancellation`() = runTest {
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
    ): OnboardingRepository = mockk {
        coEvery { this@mockk.profile } returns flowOf(profile)
        coEvery { saveProfile(any()) } returns Unit
        coEvery { savePendingProfile(any()) } returns Unit
        coEvery { authorizeSync() } returns Unit
        coEvery { clearSyncPending() } returns Unit
        coEvery { this@mockk.isSyncAuthorized } returns flowOf(isSyncAuthorized)
    }

    private fun entryRepository(
        isCompleted: Boolean = false,
        hasGuestAccess: Boolean = false,
    ): EntryRepository = mockk {
        coEvery { this@mockk.isCompleted } returns flowOf(isCompleted)
        coEvery { this@mockk.hasGuestAccess } returns flowOf(hasGuestAccess)
        coEvery { setCompleted() } returns Unit
        coEvery { clearGuestAccess() } returns Unit
    }

    private fun syncUseCase(): SyncPendingOnboardingUseCase = mockk {
        coEvery { this@mockk() } returns OnboardingSubmissionResult.Submitted
    }
}
