package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SyncPendingOnboardingUseCaseTest {
    @Test
    fun `pending complete profile is submitted with calculated level`() = runTest {
        val profile = OnboardingProfile(drivingPeriod = DrivingPeriod.YEAR_2_TO_10)
        val repository = repository(profile, isPending = true, isAuthorized = true)
        coEvery { repository.submit(profile, OnboardingLevel.NAVIGATOR) } returns
            OnboardingSubmissionResult.Submitted

        val result = SyncPendingOnboardingUseCase(repository)()

        assertEquals(OnboardingSubmissionResult.Submitted, result)
        coVerify { repository.submit(profile, OnboardingLevel.NAVIGATOR) }
    }

    @Test
    fun `non pending profile does not call server`() = runTest {
        val repository = repository(OnboardingProfile(), isPending = false, isAuthorized = false)

        assertNull(SyncPendingOnboardingUseCase(repository)())

        coVerify(exactly = 0) { repository.submit(any(), any()) }
    }

    @Test
    fun `guest pending profile waits until a new member login authorizes sync`() = runTest {
        val repository = repository(
            OnboardingProfile(drivingPeriod = DrivingPeriod.YEAR_2_TO_10),
            isPending = true,
            isAuthorized = false,
        )

        assertNull(SyncPendingOnboardingUseCase(repository)())

        coVerify(exactly = 0) { repository.submit(any(), any()) }
    }

    @Test
    fun `incomplete pending profile remains pending without request`() = runTest {
        val repository = repository(OnboardingProfile(), isPending = true, isAuthorized = true)

        assertEquals(
            OnboardingSubmissionResult.InvalidProfile,
            SyncPendingOnboardingUseCase(repository)(),
        )

        coVerify(exactly = 0) { repository.submit(any(), any()) }
        coVerify(exactly = 0) { repository.clearSyncPending() }
    }

    private fun repository(
        profile: OnboardingProfile,
        isPending: Boolean,
        isAuthorized: Boolean,
    ): OnboardingRepository = mockk {
        coEvery { this@mockk.profile } returns flowOf(profile)
        coEvery { this@mockk.isSyncPending } returns flowOf(isPending)
        coEvery { this@mockk.isSyncAuthorized } returns flowOf(isAuthorized)
    }
}
