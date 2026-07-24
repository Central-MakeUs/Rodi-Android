package com.dororong.rodi.feature.mypage

import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import io.mockk.coEvery
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyPageViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `server my page is rendered with canonical recommendations`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        coEvery { getMyPage() } returns Result.success(
            MyPage("서버 닉네임", OnboardingLevel.ROOKIE, listOf("OUTDATED"), "골목길", 7),
        )

        val viewModel = MyPageViewModel(getMyPage)
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("서버 닉네임", state.profile.nickname)
        assertEquals(listOf("유턴", "교차로", "주차"), state.profile.practiceTypes)
        assertEquals(7, state.profile.savedPlaceCount)
    }

    @Test
    fun `refresh failure preserves loaded profile and exposes the error`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        coEvery { getMyPage() } returnsMany listOf(
            Result.success(MyPage("서버 닉네임", OnboardingLevel.SEED, emptyList(), null, 3)),
            Result.failure(IllegalStateException("새로고침 실패")),
        )
        val viewModel = MyPageViewModel(getMyPage)

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("서버 닉네임", state.profile.nickname)
        assertEquals("새로고침 실패", state.errorMessage)
    }
}
