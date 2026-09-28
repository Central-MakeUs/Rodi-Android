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
    fun `대기 중인 완성된 프로필을 계산한 레벨로 제출한다`() = runTest {
        val profile = OnboardingProfile(drivingPeriod = DrivingPeriod.YEARS_3_9)
        val repository = repository(profile, isPending = true, isAuthorized = true)
        coEvery { repository.submit(profile, OnboardingLevel.NAVIGATOR) } returns
            OnboardingSubmissionResult.Submitted

        val result = SyncPendingOnboardingUseCase(repository)()

        assertEquals(OnboardingSubmissionResult.Submitted, result)
        coVerify { repository.submit(profile, OnboardingLevel.NAVIGATOR) }
    }

    @Test
    fun `대기 중이 아닌 프로필은 서버를 호출하지 않는다`() = runTest {
        val repository = repository(OnboardingProfile(), isPending = false, isAuthorized = false)

        assertNull(SyncPendingOnboardingUseCase(repository)())

        coVerify(exactly = 0) { repository.submit(any(), any()) }
    }

    @Test
    fun `둘러보기에서 저장한 프로필은 신규 회원 로그인이 동기화를 허용할 때까지 기다린다`() = runTest {
        val repository = repository(
            OnboardingProfile(drivingPeriod = DrivingPeriod.YEARS_3_9),
            isPending = true,
            isAuthorized = false,
        )

        assertNull(SyncPendingOnboardingUseCase(repository)())

        coVerify(exactly = 0) { repository.submit(any(), any()) }
    }

    @Test
    fun `완성되지 않은 대기 프로필은 요청 없이 대기 상태로 남긴다`() = runTest {
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
