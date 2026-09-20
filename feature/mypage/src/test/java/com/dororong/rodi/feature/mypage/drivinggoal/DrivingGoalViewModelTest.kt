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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
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

        viewModel.updateGoal("")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveSucceeded)
        assertFalse(viewModel.uiState.value.isSaving)
        coVerify(exactly = 1) { update("") }
        viewModel.uiState.test {
            assertTrue(awaitItem().saveSucceeded)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.effect.test {
            expectNoEvents()
        }
    }

    @Test
    fun `failed save stops saving and retains the sync error effect`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val update = mockk<UpdateDrivingGoalUseCase>()
        coEvery { getMyPage() } returns Result.success(MyPage("로디", OnboardingLevel.SEED, emptyList(), "기존 목표", 0))
        coEvery { update("새 목표") } returns Result.failure(IllegalStateException("failed"))
        val viewModel = DrivingGoalViewModel(getMyPage, update)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.updateGoal("새 목표")
            viewModel.save()
            advanceUntilIdle()

            assertEquals(DrivingGoalEffect.ShowSyncError, awaitItem())
            assertFalse(viewModel.uiState.value.isSaving)
            assertFalse(viewModel.uiState.value.saveSucceeded)
        }
    }

    @Test
    fun `unchanged goal is not saved`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val update = mockk<UpdateDrivingGoalUseCase>()
        coEvery { getMyPage() } returns Result.success(MyPage("로디", OnboardingLevel.SEED, emptyList(), "기존 목표", 0))
        val viewModel = DrivingGoalViewModel(getMyPage, update)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 0) { update(any()) }
        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.saveSucceeded)
    }

    @Test
    fun `duplicate saves are ignored while saving and after success`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val update = mockk<UpdateDrivingGoalUseCase>()
        val response = CompletableDeferred<Result<Unit>>()
        coEvery { getMyPage() } returns Result.success(MyPage("로디", OnboardingLevel.SEED, emptyList(), "기존 목표", 0))
        coEvery { update("새 목표") } coAnswers { response.await() }
        val viewModel = DrivingGoalViewModel(getMyPage, update)
        advanceUntilIdle()
        viewModel.updateGoal("새 목표")

        viewModel.save()
        viewModel.save()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.saveSucceeded)
        coVerify(exactly = 1) { update("새 목표") }

        response.complete(Result.success(Unit))
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveSucceeded)
        assertFalse(viewModel.uiState.value.isSaving)
        coVerify(exactly = 1) { update("새 목표") }
    }

    @Test
    fun `successful result remains available to a restarted collector`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val update = mockk<UpdateDrivingGoalUseCase>()
        coEvery { getMyPage() } returns Result.success(MyPage("로디", OnboardingLevel.SEED, emptyList(), "기존 목표", 0))
        coEvery { update("새 목표") } returns Result.success(Unit)
        val viewModel = DrivingGoalViewModel(getMyPage, update)
        advanceUntilIdle()
        viewModel.updateGoal("새 목표")
        viewModel.save()
        advanceUntilIdle()

        repeat(2) {
            viewModel.uiState.test {
                assertTrue(awaitItem().saveSucceeded)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
