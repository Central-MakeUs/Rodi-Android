package com.dororong.rodi.feature.mypage.drivinggoal

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import com.dororong.rodi.core.domain.usecase.member.UpdateDrivingGoalUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class DrivingGoalViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `empty goal deletes an existing server goal`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val update = mockk<UpdateDrivingGoalUseCase>()
        coEvery { getMyPage() } returns Result.success(
            MyPage("로디", OnboardingLevel.SEED, emptyList(), "기존 목표", 0),
        )
        coEvery { update("") } returns Result.success(Unit)
        val viewModel = DrivingGoalViewModel(getMyPage, update)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.updateGoal("")
            viewModel.save()
            advanceUntilIdle()
            assertEquals(DrivingGoalEffect.NavigateBack, awaitItem())
            coVerify { update("") }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
