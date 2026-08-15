package com.dororong.rodi.feature.mypage.myposts

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.member.MyReview
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.usecase.member.GetMyReviewsUseCase
import com.dororong.rodi.core.domain.usecase.member.GetPracticeRecordsUseCase
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.usecase.review.DeleteReviewUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
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
    private val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
    private val post = MyPost(1, "장소", Review(1, 1, "닉네임", OnboardingLevel.SEED, true, null, null, null, "내용", null, true, true, false, Instant.EPOCH))

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `successful deletion removes post`() = runTest(dispatcher) {
        coEvery { deleteReview(1) } returns Result.success(Unit)
        coEvery { getMyReviews(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        coEvery { getPracticeRecords.hasAny() } returns Result.success(false)
        val viewModel = MyPostsViewModel(deleteReview, getMyReviews, getPracticeRecords)
        viewModel.replacePosts(listOf(post))
        viewModel.delete(post)
        advanceUntilIdle()
        assertEquals(emptyList<MyPost>(), viewModel.uiState.value.posts)
    }

    @Test
    fun `failed deletion keeps post and exposes error`() = runTest(dispatcher) {
        coEvery { deleteReview(1) } returns Result.failure(IllegalStateException("실패"))
        coEvery { getMyReviews(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        coEvery { getPracticeRecords.hasAny() } returns Result.success(false)
        val viewModel = MyPostsViewModel(deleteReview, getMyReviews, getPracticeRecords)
        viewModel.replacePosts(listOf(post))
        viewModel.delete(post)
        advanceUntilIdle()
        assertEquals(listOf(post), viewModel.uiState.value.posts)
        assertEquals("실패", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `cancellation during deletion is not exposed as an error`() = runTest(dispatcher) {
        coEvery { deleteReview(1) } coAnswers { throw CancellationException("취소") }
        coEvery { getMyReviews(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        coEvery { getPracticeRecords.hasAny() } returns Result.success(false)
        val viewModel = MyPostsViewModel(deleteReview, getMyReviews, getPracticeRecords)
        viewModel.replacePosts(listOf(post))

        viewModel.delete(post)
        try {
            advanceUntilIdle()
        } catch (_: CancellationException) {
        }

        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `practice record CTA is enabled only when a practice record exists`() = runTest(dispatcher) {
        coEvery { getMyReviews(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        coEvery { getPracticeRecords.hasAny() } returns Result.success(true)

        val viewModel = MyPostsViewModel(deleteReview, getMyReviews, getPracticeRecords)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.hasPracticeRecords)
    }

    @Test
    fun `practice records are not fetched when a review exists`() = runTest(dispatcher) {
        coEvery { getMyReviews(any(), any()) } returns Result.success(
            CursorPage(
                listOf(MyReview(1, 1, "장소", "내용", true, false, false, Instant.EPOCH)),
                false,
                null,
                1,
            ),
        )

        val viewModel = MyPostsViewModel(deleteReview, getMyReviews, getPracticeRecords)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.hasPracticeRecords)
        coVerify(exactly = 0) { getPracticeRecords.hasAny() }
    }

    @Test
    fun `deleting the last review checks practice records for the empty state`() = runTest(dispatcher) {
        coEvery { getMyReviews(any(), any()) } returns Result.success(
            CursorPage(
                listOf(MyReview(1, 1, "장소", "내용", true, false, false, Instant.EPOCH)),
                false,
                null,
                1,
            ),
        )
        coEvery { deleteReview(1) } returns Result.success(Unit)
        coEvery { getPracticeRecords.hasAny() } returns Result.success(true)

        val viewModel = MyPostsViewModel(deleteReview, getMyReviews, getPracticeRecords)
        advanceUntilIdle()
        viewModel.delete(post)
        advanceUntilIdle()

        assertEquals(emptyList<MyPost>(), viewModel.uiState.value.posts)
        assertEquals(true, viewModel.uiState.value.hasPracticeRecords)
        coVerify(exactly = 1) { getPracticeRecords.hasAny() }
    }
}
