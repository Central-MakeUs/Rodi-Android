package com.dororong.rodi.feature.home.detail.reviewactions

import com.dororong.rodi.core.domain.model.review.ReportForm
import com.dororong.rodi.core.domain.model.review.ReportFormOption
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.usecase.member.BlockMemberUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReportFormUseCase
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewActionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getReportForm = mockk<GetReportFormUseCase>()
    private val reportReview = mockk<ReportReviewUseCase>()
    private val blockMember = mockk<BlockMemberUseCase>()

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
    fun `report form failure shows an error and finishes loading`() = runTest(dispatcher) {
        coEvery { getReportForm() } returns Result.failure(IllegalStateException("신고 사유를 불러오지 못했어요."))

        val viewModel = viewModel()
        viewModel.loadReportForm(REVIEW_ID)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isReportFormLoading)
        assertEquals("신고 사유를 불러오지 못했어요.", viewModel.state.value.reportErrorMessage)
    }

    @Test
    fun `report submission failure restores the submit state`() = runTest(dispatcher) {
        coEvery { getReportForm() } returns Result.success(reportForm())
        coEvery { reportReview(any(), any()) } returns Result.failure(IllegalStateException("신고하지 못했어요."))

        val viewModel = viewModel()
        viewModel.loadReportForm(REVIEW_ID)
        advanceUntilIdle()
        viewModel.selectReportOption(reportForm().options[1])
        viewModel.submitReport()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isReportSubmitting)
        assertFalse(viewModel.state.value.isReportSubmitted)
        assertEquals("신고하지 못했어요.", viewModel.state.value.reportErrorMessage)
    }

    @Test
    fun `block failure leaves no blocked member and exposes an error`() = runTest(dispatcher) {
        coEvery { blockMember(MEMBER_ID) } returns Result.failure(IllegalStateException("차단하지 못했어요."))

        val viewModel = viewModel()
        viewModel.blockMember(MEMBER_ID)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isBlocking)
        assertNull(viewModel.state.value.blockedMemberId)
        assertEquals("차단하지 못했어요.", viewModel.state.value.blockErrorMessage)
    }

    @Test
    fun `selecting a shorter text option truncates the previous detail`() = runTest(dispatcher) {
        val form = reportForm()
        coEvery { getReportForm() } returns Result.success(form)

        val viewModel = viewModel()
        viewModel.loadReportForm(REVIEW_ID)
        advanceUntilIdle()
        viewModel.selectReportOption(form.options[2])
        viewModel.updateReportDetail("긴사유")
        viewModel.selectReportOption(form.options[3])

        assertEquals("긴사", viewModel.state.value.reportDetail)
    }

    private fun viewModel() = ReviewActionsViewModel(getReportForm, reportReview, blockMember)

    private fun reportForm() = ReportForm(
        questionId = "review-report",
        title = "신고 사유",
        description = null,
        required = true,
        options = listOf(
            ReportFormOption("SPAM", "스팸/광고", 1, false, null, null),
            ReportFormOption("ABUSE", "욕설, 음란성, 혐오 표현", 2, false, null, null),
            ReportFormOption("OTHER", "기타", 3, true, "이유를 작성해주세요", 100),
            ReportFormOption("SHORT_OTHER", "짧은 기타", 4, true, "이유를 작성해주세요", 2),
        ),
    )

    private companion object {
        const val REVIEW_ID = 7L
        const val MEMBER_ID = 11L
    }
}
