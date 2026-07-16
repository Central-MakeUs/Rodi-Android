package com.dororong.rodi.feature.mypage

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.OnboardingSubmissionResult
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import com.dororong.rodi.core.domain.usecase.onboarding.GetOnboardingProfileUseCase
import com.dororong.rodi.core.domain.usecase.onboarding.SaveOnboardingProfileUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class DrivingGoalViewModelTest {

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
    fun `saves goal locally before navigating back when synchronization succeeds`() = runTest(testDispatcher) {
        val profile = OnboardingProfile(goal = "기존 목표")
        val repository = mockk<OnboardingRepository> {
            every { this@mockk.profile } returns flowOf(profile)
            coEvery { saveProfile(any()) } returns Unit
            coEvery { submit(any(), any()) } returns OnboardingSubmissionResult.Submitted
        }
        val viewModel = DrivingGoalViewModel(
            getOnboardingProfile = GetOnboardingProfileUseCase(repository),
            saveOnboardingProfile = SaveOnboardingProfileUseCase(repository),
        )

        viewModel.effect.test {
            viewModel.save("강남에서 운전하기")
            advanceUntilIdle()

            assertEquals(DrivingGoalEffect.NavigateBack, awaitItem())
        }
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            repository.saveProfile(match { it.goal == "강남에서 운전하기" })
            repository.submit(match { it.goal == "강남에서 운전하기" }, any())
        }
    }

    @Test
    fun `keeps local goal and shows error when synchronization fails`() = runTest(testDispatcher) {
        val repository = mockk<OnboardingRepository> {
            every { this@mockk.profile } returns flowOf(OnboardingProfile())
            coEvery { saveProfile(any()) } returns Unit
            coEvery { submit(any(), any()) } returns OnboardingSubmissionResult.RetryableFailure
        }
        val viewModel = DrivingGoalViewModel(
            getOnboardingProfile = GetOnboardingProfileUseCase(repository),
            saveOnboardingProfile = SaveOnboardingProfileUseCase(repository),
        )

        viewModel.effect.test {
            viewModel.save("주차 자신감 갖기")
            advanceUntilIdle()

            assertEquals(DrivingGoalEffect.ShowSyncError, awaitItem())
        }
        coVerify { repository.saveProfile(match { it.goal == "주차 자신감 갖기" }) }
    }
}
