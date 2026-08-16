package com.dororong.rodi.feature.home.review

import com.dororong.rodi.core.common.graphemeLength
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDetail
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewException
import com.dororong.rodi.core.domain.model.review.ReviewSubmissionResult
import com.dororong.rodi.core.domain.usecase.review.CreateReviewUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReviewUseCase
import com.dororong.rodi.core.domain.usecase.review.UpdateReviewUseCase
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewWriteViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val createReview = mockk<CreateReviewUseCase>()
    private val updateReview = mockk<UpdateReviewUseCase>()
    private val getReview = mockk<GetReviewUseCase>()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `new review starts at basics with an untouched form`() {
        val viewModel = viewModel()

        viewModel.start(PLACE_ID, PLACE_NAME)

        assertEquals(ReviewWriteStep.Basics, viewModel.state.value.step)
        assertFalse(viewModel.state.value.isDirty)
        assertFalse(viewModel.state.value.canGoNext)
    }

    @Test
    fun `basics can advance only after all required selections`() {
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        viewModel.selectRecommend(true)
        viewModel.selectDifficulty(ReviewDifficulty.NORMAL)

        assertFalse(viewModel.state.value.canGoNext)

        viewModel.selectCongestion(ReviewCongestion.QUIET)
        viewModel.next()

        assertEquals(ReviewWriteStep.Detail, viewModel.state.value.step)
    }

    @Test
    fun `caution is optional and does not prevent continuing`() {
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        completeBasics(viewModel)

        assertTrue(viewModel.state.value.canGoNext)
        assertEquals("", viewModel.state.value.caution)
    }

    @Test
    fun `review fields limit emoji by grapheme count`() {
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)

        viewModel.updateCaution("😁".repeat(51))
        viewModel.updateContent("👨‍👩‍👧‍👦".repeat(151))

        assertEquals(50, viewModel.state.value.caution.graphemeLength())
        assertEquals(150, viewModel.state.value.content.graphemeLength())
    }

    @Test
    fun `new form becomes dirty on a partial selection`() {
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        viewModel.selectRecommend(true)

        assertTrue(viewModel.state.value.isDirty)
    }

    @Test
    fun `new review submits the completed draft`() = runTest(dispatcher) {
        val expected = draft()
        coEvery { createReview(PLACE_ID, expected) } returns Result.success(31L)
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        fillForSubmit(viewModel)

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSubmitted)
        assertEquals(
            ReviewSubmissionResult(
                placeId = PLACE_ID,
                reviewId = 31L,
                draft = expected,
                isEditing = false,
            ),
            viewModel.state.value.submittedResult,
        )
        coVerify(exactly = 1) { createReview(PLACE_ID, expected) }
    }

    @Test
    fun `submitted result is consumed only once`() = runTest(dispatcher) {
        coEvery { createReview(any(), any()) } returns Result.success(31L)
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        fillForSubmit(viewModel)

        viewModel.submit()
        advanceUntilIdle()

        assertNotNull(viewModel.consumeSubmittedResult())
        assertNull(viewModel.consumeSubmittedResult())
        assertFalse(viewModel.state.value.isSubmitted)
        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `submitting after success does not create a second review`() = runTest(dispatcher) {
        coEvery { createReview(any(), any()) } returns Result.success(31L)
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        fillForSubmit(viewModel)

        viewModel.submit()
        advanceUntilIdle()
        viewModel.submit()
        advanceUntilIdle()

        coVerify(exactly = 1) { createReview(any(), any()) }
    }

    @Test
    fun `selecting recommend sends true to the server`() = runTest(dispatcher) {
        val expected = draft().copy(caution = null)
        coEvery { createReview(PLACE_ID, expected) } returns Result.success(31L)
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        viewModel.selectRecommend(true)
        viewModel.selectDifficulty(ReviewDifficulty.NORMAL)
        viewModel.selectCongestion(ReviewCongestion.QUIET)
        viewModel.next()
        viewModel.selectPracticeMethod(PracticeMethod.SOLO)
        viewModel.updateContent("좋은 코스예요")

        viewModel.submit()
        advanceUntilIdle()

        coVerify(exactly = 1) { createReview(PLACE_ID, expected) }
    }

    @Test
    fun `blank caution is sent as null`() = runTest(dispatcher) {
        val expected = ReviewDraft(
            isRecommended = true,
            difficulty = ReviewDifficulty.NORMAL,
            congestion = ReviewCongestion.QUIET,
            practiceMethod = PracticeMethod.SOLO,
            content = "좋은 코스예요",
            caution = null,
        )
        coEvery { createReview(PLACE_ID, expected) } returns Result.success(31L)
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        completeBasics(viewModel)
        viewModel.next()
        viewModel.selectPracticeMethod(PracticeMethod.SOLO)
        viewModel.updateContent("좋은 코스예요")

        viewModel.submit()
        advanceUntilIdle()

        coVerify(exactly = 1) { createReview(PLACE_ID, expected) }
    }

    @Test
    fun `practice method is required before completing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        completeBasics(viewModel)
        viewModel.next()
        viewModel.submit()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSubmitted)
        coVerify(exactly = 0) { createReview(any(), any()) }

        viewModel.selectPracticeMethod(PracticeMethod.SOLO)
        assertTrue(viewModel.state.value.canSubmit)

        viewModel.selectPracticeMethod(PracticeMethod.WITH_COMPANION)
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `practice method can submit without review content`() = runTest(dispatcher) {
        val expected = draft().copy(
            practiceMethod = PracticeMethod.WITH_COMPANION,
            content = null,
            caution = null,
        )
        coEvery { createReview(PLACE_ID, expected) } returns Result.success(31L)
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        completeBasics(viewModel)
        viewModel.next()
        viewModel.selectPracticeMethod(PracticeMethod.WITH_COMPANION)

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSubmitted)
        coVerify(exactly = 1) { createReview(PLACE_ID, expected) }
    }

    @Test
    fun `editing pre-fills the review and is not dirty`() {
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME, review())

        assertEquals(ReviewDifficulty.NORMAL, viewModel.state.value.difficulty)
        assertEquals("자전거를 조심하세요", viewModel.state.value.caution)
        assertEquals("좋은 코스예요", viewModel.state.value.content)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun `review id restores the edit form from the review detail`() = runTest(dispatcher) {
        coEvery { getReview(REVIEW_ID) } returns Result.success(reviewDetail())
        val viewModel = viewModel()

        viewModel.startForReviewId(PLACE_ID, PLACE_NAME, REVIEW_ID)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isInitializing)
        assertEquals(REVIEW_ID, viewModel.state.value.editingReviewId)
        assertEquals("좋은 코스예요", viewModel.state.value.content)
        assertFalse(viewModel.state.value.isDirty)
        coVerify(exactly = 1) { getReview(REVIEW_ID) }
    }

    @Test
    fun `missing review id exposes initialization error`() = runTest(dispatcher) {
        coEvery { getReview(REVIEW_ID) } returns Result.failure(ReviewException.NotFound("404"))
        val viewModel = viewModel()

        viewModel.startForReviewId(PLACE_ID, PLACE_NAME, REVIEW_ID)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isInitializing)
        assertNotNull(viewModel.state.value.initializationErrorMessage)
        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `review lookup failure exposes initialization error`() = runTest(dispatcher) {
        coEvery { getReview(REVIEW_ID) } returns Result.failure(IllegalStateException("offline"))
        val viewModel = viewModel()

        viewModel.startForReviewId(PLACE_ID, PLACE_NAME, REVIEW_ID)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isInitializing)
        assertEquals("offline", viewModel.state.value.initializationErrorMessage)
        assertFalse(viewModel.state.value.canSubmit)
    }

    @Test
    fun `review lookup cancellation propagates after initialization stops`() = runTest(dispatcher) {
        coEvery { getReview(REVIEW_ID) } coAnswers { throw CancellationException("취소") }
        val viewModel = viewModel()

        viewModel.startForReviewId(PLACE_ID, PLACE_NAME, REVIEW_ID)
        try {
            advanceUntilIdle()
        } catch (_: CancellationException) {
        }

        assertFalse(viewModel.state.value.isInitializing)
        assertNotNull(viewModel.state.value.initializationErrorMessage)
    }

    @Test
    fun `editing keeps available fields when a nullable selection is absent`() {
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME, review().copy(congestion = null))

        assertTrue(viewModel.state.value.isRecommended == true)
        assertEquals(ReviewDifficulty.NORMAL, viewModel.state.value.difficulty)
        assertNull(viewModel.state.value.congestion)
        assertEquals("자전거를 조심하세요", viewModel.state.value.caution)
        assertEquals("좋은 코스예요", viewModel.state.value.content)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun `editing submits an update after a changed field`() = runTest(dispatcher) {
        val expected = draft().copy(content = "수정한 후기")
        coEvery { updateReview(REVIEW_ID, expected) } returns Result.success(Unit)
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME, review())
        viewModel.updateContent("수정한 후기")

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSubmitted)
        assertEquals(REVIEW_ID, viewModel.state.value.submittedResult?.reviewId)
        assertEquals(true, viewModel.state.value.submittedResult?.isEditing)
        coVerify(exactly = 1) { updateReview(REVIEW_ID, expected) }
    }

    @Test
    fun `submission failure keeps form open and exposes an error`() = runTest(dispatcher) {
        coEvery { createReview(any(), any()) } returns Result.failure(IllegalStateException("offline"))
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        fillForSubmit(viewModel)

        viewModel.submit()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSubmitted)
        assertEquals("요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.", viewModel.state.value.errorMessage)
        viewModel.consumeError()
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `level required uses its guided message`() = runTest(dispatcher) {
        coEvery { createReview(any(), any()) } returns Result.failure(ReviewException.LevelRequired("409"))
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME)
        fillForSubmit(viewModel)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("레벨 진단을 마쳐야 후기를 남길 수 있어요.", viewModel.state.value.errorMessage)
    }

    @Test
    fun `level changed uses its edit-specific message`() = runTest(dispatcher) {
        coEvery { updateReview(any(), any()) } returns Result.failure(ReviewException.LevelChanged("409"))
        val viewModel = viewModel()
        viewModel.start(PLACE_ID, PLACE_NAME, review())
        viewModel.updateContent("수정한 후기")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("레벨이 바뀌어서 이 후기는 수정할 수 없어요.", viewModel.state.value.errorMessage)
    }

    private fun viewModel() = ReviewWriteViewModel(createReview, updateReview, getReview)

    private fun completeBasics(viewModel: ReviewWriteViewModel) {
        viewModel.selectRecommend(true)
        viewModel.selectDifficulty(ReviewDifficulty.NORMAL)
        viewModel.selectCongestion(ReviewCongestion.QUIET)
    }

    private fun fillForSubmit(viewModel: ReviewWriteViewModel) {
        completeBasics(viewModel)
        viewModel.next()
        viewModel.selectPracticeMethod(PracticeMethod.SOLO)
        viewModel.updateContent("좋은 코스예요")
        viewModel.updateCaution("자전거를 조심하세요")
    }

    private fun draft() = ReviewDraft(
        isRecommended = true,
        difficulty = ReviewDifficulty.NORMAL,
        congestion = ReviewCongestion.QUIET,
        practiceMethod = PracticeMethod.SOLO,
        content = "좋은 코스예요",
        caution = "자전거를 조심하세요",
    )

    private fun review() = Review(
        reviewId = REVIEW_ID,
        memberId = 3L,
        nickname = "로디",
        memberLevel = OnboardingLevel.SEED,
        isRecommended = true,
        difficulty = ReviewDifficulty.NORMAL,
        congestion = ReviewCongestion.QUIET,
        practiceMethod = PracticeMethod.SOLO,
        content = "좋은 코스예요",
        caution = "자전거를 조심하세요",
        isMine = true,
        isEditable = true,
        isHidden = false,
        createdAt = Instant.EPOCH,
        isVerifiedVisit = true,
    )

    private fun reviewDetail() = ReviewDetail(
        reviewId = REVIEW_ID,
        placeId = PLACE_ID,
        placeName = PLACE_NAME,
        isRecommended = true,
        difficulty = ReviewDifficulty.NORMAL,
        congestion = ReviewCongestion.QUIET,
        practiceMethod = PracticeMethod.SOLO,
        content = "좋은 코스예요",
        caution = "자전거를 조심하세요",
        isEditable = true,
        isHidden = false,
        isVerifiedVisit = true,
        createdAt = Instant.EPOCH,
    )

    private companion object {
        const val PLACE_ID = 11L
        const val PLACE_NAME = "강남역 주변 코스"
        const val REVIEW_ID = 7L
    }
}
