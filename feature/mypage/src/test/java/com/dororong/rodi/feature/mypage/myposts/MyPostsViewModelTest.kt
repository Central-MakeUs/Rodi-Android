package com.dororong.rodi.feature.mypage.myposts

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.usecase.review.DeleteReviewUseCase
import com.dororong.rodi.core.domain.usecase.member.GetMyReviewsUseCase
import com.dororong.rodi.core.domain.model.place.CursorPage
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
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
class MyPostsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val deleteReview = mockk<DeleteReviewUseCase>()
    private val getMyReviews = mockk<GetMyReviewsUseCase>()
    private val post = MyPost(1, "장소", Review(1, 1, "닉네임", OnboardingLevel.SEED, true, null, null, null, "내용", null, true, true, false, Instant.EPOCH))

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `successful deletion removes post`() = runTest(dispatcher) {
        coEvery { deleteReview(1) } returns Result.success(Unit)
        coEvery { getMyReviews(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        val viewModel = MyPostsViewModel(deleteReview, getMyReviews)
        viewModel.replacePosts(listOf(post))
        viewModel.delete(post)
        advanceUntilIdle()
        assertEquals(emptyList<MyPost>(), viewModel.uiState.value.posts)
    }

    @Test
    fun `failed deletion keeps post and exposes error`() = runTest(dispatcher) {
        coEvery { deleteReview(1) } returns Result.failure(IllegalStateException("실패"))
        coEvery { getMyReviews(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        val viewModel = MyPostsViewModel(deleteReview, getMyReviews)
        viewModel.replacePosts(listOf(post))
        viewModel.delete(post)
        advanceUntilIdle()
        assertEquals(listOf(post), viewModel.uiState.value.posts)
        assertEquals("실패", viewModel.uiState.value.errorMessage)
    }
}
