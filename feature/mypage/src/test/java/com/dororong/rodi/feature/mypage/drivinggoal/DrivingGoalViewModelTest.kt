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
    fun `빈 목표는 기존 서버 목표를 지운다`() = runTest(dispatcher) {
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
    fun `저장이 실패하면 저장 상태를 끝내고 동기화 오류 효과를 남긴다`() = runTest(dispatcher) {
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
    fun `목표가 바뀌지 않았으면 저장하지 않는다`() = runTest(dispatcher) {
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
    fun `저장 중이거나 저장에 성공한 뒤의 중복 저장은 무시한다`() = runTest(dispatcher) {
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
    fun `저장 성공 결과는 다시 시작한 수집에서도 받을 수 있다`() = runTest(dispatcher) {
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
