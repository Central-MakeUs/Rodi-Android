package com.dororong.rodi.feature.home.detail.reviewactions

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.review.ReportForm
import com.dororong.rodi.core.domain.model.review.ReportFormOption
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.usecase.member.BlockMemberUseCase
import com.dororong.rodi.core.domain.usecase.review.DeleteReviewUseCase
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
    fun `일반 신고 사유를 고르면 신고를 제출하고 접수 화면을 연다`() = runTest(dispatcher) {
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

        assertTrue(viewModel.uiState.value.isReportSubmitted)
        coVerify(exactly = 1) {
            reportReview(
                REVIEW_ID,
                ReportSubmission(reason = "ABUSE", detail = null, detailConsistent = true),
            )
        }
    }

    @Test
    fun `기타 사유는 내용을 입력해야 제출할 수 있다`() = runTest(dispatcher) {
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

        assertFalse(viewModel.uiState.value.isReportSubmitted)
        coVerify(exactly = 0) { reportReview(any(), any()) }

        viewModel.updateReportDetail("추가 설명")
        viewModel.submitReport()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isReportSubmitted)
    }

    @Test
    fun `회원 차단이 성공하면 차단한 대상을 알린다`() = runTest(dispatcher) {
        coEvery { blockMember(MEMBER_ID) } returns Result.success(Unit)
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.blockMember(MEMBER_ID)
            advanceUntilIdle()

            assertEquals(ReviewActionsEffect.Blocked(MEMBER_ID), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isBlocking)
        coVerify(exactly = 1) { blockMember(MEMBER_ID) }
    }

    @Test
    fun `신고 폼을 불러오지 못하면 오류를 보여주고 로딩을 끝낸다`() = runTest(dispatcher) {
        coEvery { getReportForm() } returns Result.failure(IllegalStateException("신고 사유를 불러오지 못했어요."))

        val viewModel = viewModel()
        viewModel.loadReportForm(REVIEW_ID)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isReportFormLoading)
        assertEquals("신고 사유를 불러오지 못했어요.", viewModel.uiState.value.reportErrorMessage)
    }

    @Test
    fun `사용자 문구가 없는 신고 폼 오류는 예외 내용을 숨긴다`() = runTest(dispatcher) {
        val detail = "Field 'totalCount' is required for type with serial name 'ReportFormResponse'"
        coEvery { getReportForm() } returns Result.failure(IllegalStateException(detail))

        val viewModel = viewModel()
        viewModel.loadReportForm(REVIEW_ID)
        advanceUntilIdle()

        assertEquals("신고 사유를 불러오지 못했어요.", viewModel.uiState.value.reportErrorMessage)
        assertFalse(viewModel.uiState.value.reportErrorMessage.orEmpty().contains(detail))
    }

    @Test
    fun `신고 제출이 실패하면 제출 상태를 되돌린다`() = runTest(dispatcher) {
        coEvery { getReportForm() } returns Result.success(reportForm())
        coEvery { reportReview(any(), any()) } returns Result.failure(IllegalStateException("신고하지 못했어요."))

        val viewModel = viewModel()
        viewModel.loadReportForm(REVIEW_ID)
        advanceUntilIdle()
        viewModel.selectReportOption(reportForm().options[1])
        viewModel.submitReport()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isReportSubmitting)
        assertFalse(viewModel.uiState.value.isReportSubmitted)
        assertEquals("신고하지 못했어요.", viewModel.uiState.value.reportErrorMessage)
    }

    @Test
    fun `차단이 실패하면 차단한 회원 없이 오류를 알린다`() = runTest(dispatcher) {
        coEvery { blockMember(MEMBER_ID) } returns Result.failure(IllegalStateException("차단하지 못했어요."))
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.blockMember(MEMBER_ID)
            advanceUntilIdle()

            assertEquals(ReviewActionsEffect.BlockFailed("차단하지 못했어요."), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isBlocking)
    }

    @Test
    fun `후기를 삭제하면 삭제한 후기를 알린다`() = runTest(dispatcher) {
        coEvery { deleteReview(REVIEW_ID) } returns Result.success(Unit)
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.deleteReview(REVIEW_ID)
            advanceUntilIdle()

            assertEquals(ReviewActionsEffect.Deleted(REVIEW_ID), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `후기 삭제가 실패하면 사용자 문구를 알리고 삭제 상태를 끝낸다`() = runTest(dispatcher) {
        coEvery { deleteReview(REVIEW_ID) } returns Result.failure(IllegalStateException("raw server detail"))
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.deleteReview(REVIEW_ID)
            advanceUntilIdle()

            assertEquals(ReviewActionsEffect.DeleteFailed("후기를 삭제하지 못했어요."), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `더 짧은 입력 제한의 선택지를 고르면 이전 상세 내용을 자른다`() = runTest(dispatcher) {
        val form = reportForm()
        coEvery { getReportForm() } returns Result.success(form)

        val viewModel = viewModel()
        viewModel.loadReportForm(REVIEW_ID)
        advanceUntilIdle()
        viewModel.selectReportOption(form.options[2])
        viewModel.updateReportDetail("긴사유")
        viewModel.selectReportOption(form.options[3])

        assertEquals("긴사", viewModel.uiState.value.reportDetail)
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
            ReportFormOption("SHORT_OTHER", "짧은 기타", 4, true, "이유를 작성해주세요", 2),
        ),
    )

    private companion object {
        const val REVIEW_ID = 7L
        const val MEMBER_ID = 11L
    }
}
