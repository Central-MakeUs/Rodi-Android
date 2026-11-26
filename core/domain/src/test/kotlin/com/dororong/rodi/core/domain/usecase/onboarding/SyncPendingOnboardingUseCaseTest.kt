package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.localOnboardingForm
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.RecentDrivingFrequency
import com.dororong.rodi.core.domain.model.onboarding.SoloDrivingRange
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

    @Test
    fun `없음과 다른 도로 경험이 함께 남아 있으면 서버 재전송을 막는다`() = runTest {
        val profile = OnboardingProfile(
            drivingPeriod = DrivingPeriod.MONTHS_1_2,
            recentFrequency = RecentDrivingFrequency.WEEKLY_1,
            roadExperiences = listOf(
                RoadExperience.NONE,
                RoadExperience.WITH_COMPANION,
            ),
        )
        val repository = repository(profile, isPending = true, isAuthorized = true)
        coEvery { repository.submit(any(), any()) } returns OnboardingSubmissionResult.Submitted

        assertEquals(OnboardingSubmissionResult.InvalidProfile, SyncPendingOnboardingUseCase(repository)())
        coVerify(exactly = 0) { repository.submit(any(), any()) }
    }

    @Test
    fun `로그인 후 현재 서버 폼에서 없어진 선택과 후속 답변은 빼고 점수를 다시 계산한다`() = runTest {
        val original = OnboardingProfile(
            drivingPeriod = DrivingPeriod.MONTHS_1_2,
            recentFrequency = RecentDrivingFrequency.WEEKLY_1,
            roadExperiences = listOf(RoadExperience.SOLO),
            soloDrivingRange = SoloDrivingRange.HIGHWAY_LONG_DISTANCE,
        )
        val local = localOnboardingForm()
        val form = local.copy(steps = local.steps.map { step -> step.copy(questions = step.questions.map { question ->
            question.copy(required = question.code == "drivingPeriod", options = question.options.filter {
                question.code != "roadExperiences" || it.code != "SOLO"
            })
        }) })
        val expected = original.copy(roadExperiences = emptyList(), soloDrivingRange = null)
        val repository = repository(original, isPending = true, isAuthorized = true)
        coEvery { repository.getForm() } returns form
        coEvery { repository.savePendingProfile(expected) } returns Unit
        coEvery { repository.submit(expected, OnboardingLevel.SEED) } returns OnboardingSubmissionResult.Submitted

        assertEquals(OnboardingSubmissionResult.Submitted, SyncPendingOnboardingUseCase(repository)())
        coVerify(exactly = 1) { repository.savePendingProfile(expected) }
        coVerify(exactly = 1) { repository.submit(expected, OnboardingLevel.SEED) }
    }

    @Test
    fun `로그인 후 서버가 필수로 바꾼 문항이 비었으면 자동 제출하지 않는다`() = runTest {
        val profile = OnboardingProfile(drivingPeriod = DrivingPeriod.MONTHS_1_2)
        val repository = repository(profile, isPending = true, isAuthorized = true)
        coEvery { repository.getForm() } returns localOnboardingForm()

        assertEquals(OnboardingSubmissionResult.InvalidProfile, SyncPendingOnboardingUseCase(repository)())
        coVerify(exactly = 0) { repository.submit(any(), any()) }
    }

    private fun repository(
        profile: OnboardingProfile,
        isPending: Boolean,
        isAuthorized: Boolean,
    ): OnboardingRepository = mockk {
        coEvery { this@mockk.getForm() } returns null
        coEvery { this@mockk.profile } returns flowOf(profile)
        coEvery { this@mockk.isSyncPending } returns flowOf(isPending)
        coEvery { this@mockk.isSyncAuthorized } returns flowOf(isAuthorized)
    }
}
