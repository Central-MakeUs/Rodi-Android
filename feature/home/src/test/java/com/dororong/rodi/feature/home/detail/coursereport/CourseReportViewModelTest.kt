package com.dororong.rodi.feature.home.detail.coursereport

import com.dororong.rodi.core.domain.model.course.CourseReportException
import com.dororong.rodi.core.domain.model.course.CourseReportSubmission
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.domain.model.review.ReviewIssueOption
import com.dororong.rodi.core.domain.usecase.course.GetCourseReportFormUseCase
import com.dororong.rodi.core.domain.usecase.course.ReportCourseUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class CourseReportViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getReportForm = mockk<GetCourseReportFormUseCase>()
    private val reportCourse = mockk<ReportCourseUseCase>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { getReportForm() } returns Result.success(form())
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `문제 항목을 고른 순서와 관계없이 폼 순서로 유지한다`() = runTest(dispatcher) {
        val viewModel = loaded()

        viewModel.toggle("OTHER")
        viewModel.toggle("VEHICLE_RESTRICTED")

        assertEquals(listOf("VEHICLE_RESTRICTED", "OTHER"), viewModel.uiState.value.selected)
    }

    @Test
    fun `기타를 고르면 직접 입력 전까지 제출할 수 없다`() = runTest(dispatcher) {
        val viewModel = loaded()

        viewModel.toggle("OTHER")
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.updateDetail("주말엔 입구가 잠겨 있어요")

        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `기타 입력은 폼의 최대 글자 수에서 자른다`() = runTest(dispatcher) {
        val viewModel = loaded()
        viewModel.toggle("OTHER")

        viewModel.updateDetail("가".repeat(40))

        assertEquals(30, viewModel.uiState.value.detail.length)
    }

    @Test
    fun `기타를 고르지 않으면 입력값을 보내지 않는다`() = runTest(dispatcher) {
        coEvery { reportCourse(any(), any()) } returns Result.success(Unit)
        val viewModel = loaded()
        viewModel.toggle("OTHER")
        viewModel.updateDetail("남은 입력")
        viewModel.toggle("OTHER")
        viewModel.toggle("CONSTRUCTION")

        viewModel.submit()
        advanceUntilIdle()

        coVerify(exactly = 1) { reportCourse(COURSE_ID, CourseReportSubmission(listOf("CONSTRUCTION"), null)) }
        assertTrue(viewModel.uiState.value.isSubmitted)
    }

    @Test
    fun `제출이 실패하면 서버 문구를 알리고 다시 제출할 수 있다`() = runTest(dispatcher) {
        coEvery { reportCourse(any(), any()) } returns
            Result.failure(CourseReportException.OwnCourse("본인이 등록한 코스는 신고할 수 없습니다."))
        val viewModel = loaded()
        viewModel.toggle("CONSTRUCTION")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("본인이 등록한 코스는 신고할 수 없습니다.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitted)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `폼 조회가 실패하면 다시 시도로 폼을 받는다`() = runTest(dispatcher) {
        coEvery { getReportForm() } returns Result.failure(CourseReportException.Network("네트워크 연결을 확인해주세요."))
        val viewModel = CourseReportViewModel(getReportForm, reportCourse)
        viewModel.load(COURSE_ID)
        advanceUntilIdle()
        assertEquals("네트워크 연결을 확인해주세요.", viewModel.uiState.value.loadErrorMessage)

        coEvery { getReportForm() } returns Result.success(form())
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadErrorMessage)
        assertEquals(form(), viewModel.uiState.value.form)
    }

    @Test
    fun `닫으면 다음 신고에 이전 선택이 남지 않는다`() = runTest(dispatcher) {
        val viewModel = loaded()
        viewModel.toggle("CONSTRUCTION")

        viewModel.close()
        viewModel.load(COURSE_ID)
        advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.uiState.value.selected)
    }

    @Test
    fun `다른 코스로 바뀐 뒤 도착한 이전 신고 결과는 새 코스 화면에 반영하지 않는다`() = runTest(dispatcher) {
        val pending = kotlinx.coroutines.CompletableDeferred<Result<Unit>>()
        coEvery { reportCourse(COURSE_ID, any()) } coAnswers { pending.await() }
        val viewModel = loaded()
        viewModel.toggle("CONSTRUCTION")
        viewModel.submit()
        runCurrent()

        viewModel.load(OTHER_COURSE_ID)
        advanceUntilIdle()
        pending.complete(Result.success(Unit))
        advanceUntilIdle()

        assertEquals(OTHER_COURSE_ID, viewModel.uiState.value.courseId)
        assertFalse(viewModel.uiState.value.isSubmitted)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    private fun kotlinx.coroutines.test.TestScope.loaded(): CourseReportViewModel {
        val viewModel = CourseReportViewModel(getReportForm, reportCourse)
        viewModel.load(COURSE_ID)
        advanceUntilIdle()
        return viewModel
    }

    private fun form() = ReviewIssueForm(
        title = "해당 코스에 어떤 문제가 있나요?",
        options = listOf(
            ReviewIssueOption("VEHICLE_RESTRICTED", "차량 진입이 제한된 장소예요", false, null, null),
            ReviewIssueOption("CONSTRUCTION", "공사·통제 등으로 현재 주행할 수 없어요", false, null, null),
            ReviewIssueOption("OTHER", "기타", true, "이유를 작성해주세요", 30),
        ),
    )

    private companion object {
        const val COURSE_ID = 9L
        const val OTHER_COURSE_ID = 10L
    }
}
