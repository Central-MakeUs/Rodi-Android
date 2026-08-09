package com.dororong.rodi.feature.home.detail.reviewactions

import com.dororong.rodi.core.domain.model.review.ReportForm
import com.dororong.rodi.core.domain.model.review.ReportFormOption
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.usecase.member.BlockMemberUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReportFormUseCase
import com.dororong.rodi.core.domain.usecase.review.DeleteReviewUseCase
import com.dororong.rodi.core.domain.usecase.review.ReportReviewUseCase
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewActionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getReportForm = mockk<GetReportFormUseCase>()
    private val reportReview = mockk<ReportReviewUseCase>()
    private val blockMember = mockk<BlockMemberUseCase>()
    private val deleteReview = mockk<DeleteReviewUseCase>()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `selected standard reason submits the report and opens receipt`() = runTest(dispatcher) {
        coEvery { getReportForm() } returns Result.success(reportForm())
        coEvery {
            reportReview(
                REVIEW_ID,
                ReportSubmission(reason = "ABUSE", detail = null, detailConsistent = true),
            )
        } returns Result.success(Unit)

        val viewModel = viewModel()
        viewModel.loadReportForm(REVIEW_ID)
        advanceUntilIdle()
        viewModel.selectReportOption(reportForm().options[1])
        viewModel.submitReport()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isReportSubmitted)
        coVerify(exactly = 1) {
            reportReview(
                REVIEW_ID,
                ReportSubmission(reason = "ABUSE", detail = null, detailConsistent = true),
            )
        }
    }

    @Test
    fun `other reason requires text before submitting`() = runTest(dispatcher) {
        coEvery { getReportForm() } returns Result.success(reportForm())
        coEvery {
            reportReview(
                REVIEW_ID,
                ReportSubmission(reason = "OTHER", detail = "추가 설명", detailConsistent = true),
            )
        } returns Result.success(Unit)

        val viewModel = viewModel()
        viewModel.loadReportForm(REVIEW_ID)
        advanceUntilIdle()
        viewModel.selectReportOption(reportForm().options[2])
        viewModel.submitReport()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isReportSubmitted)
        coVerify(exactly = 0) { reportReview(any(), any()) }

        viewModel.updateReportDetail("추가 설명")
        viewModel.submitReport()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isReportSubmitted)
    }

    @Test
    fun `block member reports a successful target`() = runTest(dispatcher) {
        coEvery { blockMember(MEMBER_ID) } returns Result.success(Unit)

        val viewModel = viewModel()
        viewModel.blockMember(MEMBER_ID)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.blockedMemberId == MEMBER_ID)
        coVerify(exactly = 1) { blockMember(MEMBER_ID) }
    }

    @Test
    fun `delete review exposes the deleted review id`() = runTest(dispatcher) {
        coEvery { deleteReview(REVIEW_ID) } returns Result.success(Unit)

        val viewModel = viewModel()
        viewModel.deleteReview(REVIEW_ID)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.deletedReviewId == REVIEW_ID)
        coVerify(exactly = 1) { deleteReview(REVIEW_ID) }
    }

    private fun viewModel() = ReviewActionsViewModel(getReportForm, reportReview, blockMember, deleteReview)

    private fun reportForm() = ReportForm(
        questionId = "review-report",
        title = "신고 사유",
        description = null,
        required = true,
        options = listOf(
            ReportFormOption("SPAM", "스팸/광고", 1, false, null, null),
            ReportFormOption("ABUSE", "욕설, 음란성, 혐오 표현", 2, false, null, null),
            ReportFormOption("OTHER", "기타", 3, true, "이유를 작성해주세요", 100),
        ),
    )

    private companion object {
        const val REVIEW_ID = 7L
        const val MEMBER_ID = 11L
    }
}
