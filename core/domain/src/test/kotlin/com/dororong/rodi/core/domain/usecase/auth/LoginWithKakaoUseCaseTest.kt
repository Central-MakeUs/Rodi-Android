package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.repository.OnboardingRepository
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
    fun `invoke returns success when repository login succeeds`() = runTest {
        val repository = mockk<AuthRepository>()
        val onboardingRepository = onboardingRepository()
        coEvery {
            repository.loginWithKakao(
                kakaoAccessToken = "access-token",
                onboardingProfile = null,
            )
        } returns true
        val useCase = LoginWithKakaoUseCase(repository, onboardingRepository)

        val result = useCase("access-token")

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow())
        coVerify(exactly = 1) {
            repository.loginWithKakao(
                kakaoAccessToken = "access-token",
                onboardingProfile = null,
            )
        }
    }

    @Test
    fun `invoke sends onboarding profile when local draft exists`() = runTest {
        val repository = mockk<AuthRepository>()
        val profile = OnboardingProfile(
            nickname = "로디",
            drivingPeriod = DrivingPeriod.MONTH_1_TO_3,
        )
        val onboardingRepository = onboardingRepository(profile)
        coEvery {
            repository.loginWithKakao(
                kakaoAccessToken = "access-token",
                onboardingProfile = profile,
            )
        } returns true
        val useCase = LoginWithKakaoUseCase(repository, onboardingRepository)

        val result = useCase("access-token")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            repository.loginWithKakao(
                kakaoAccessToken = "access-token",
                onboardingProfile = profile,
            )
        }
    }

    @Test
    fun `invoke wraps repository failure as Result failure`() = runTest {
        val repository = mockk<AuthRepository>()
        val onboardingRepository = onboardingRepository()
        coEvery {
            repository.loginWithKakao(
                kakaoAccessToken = "access-token",
                onboardingProfile = null,
            )
        } throws AuthException.InvalidCredential("boom")
        val useCase = LoginWithKakaoUseCase(repository, onboardingRepository)

        val result = useCase("access-token")

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) {
            repository.loginWithKakao(
                kakaoAccessToken = "access-token",
                onboardingProfile = null,
            )
        }
    }

    @Test
    fun `invoke rethrows CancellationException instead of wrapping it`() = runTest {
        val repository = mockk<AuthRepository>()
        val onboardingRepository = onboardingRepository()
        coEvery {
            repository.loginWithKakao(
                kakaoAccessToken = "access-token",
                onboardingProfile = null,
            )
        } throws CancellationException("cancelled")
        val useCase = LoginWithKakaoUseCase(repository, onboardingRepository)

        try {
            useCase("access-token")
            fail("CancellationException should be rethrown")
        } catch (_: CancellationException) {
            coVerify(exactly = 1) {
                repository.loginWithKakao(
                    kakaoAccessToken = "access-token",
                    onboardingProfile = null,
                )
            }
        }
    }

    private fun onboardingRepository(
        profile: OnboardingProfile = OnboardingProfile(),
    ): OnboardingRepository =
        mockk {
            coEvery { saveProfile(any()) } returns Unit
            coEvery { this@mockk.profile } returns flowOf(profile)
        }
}
