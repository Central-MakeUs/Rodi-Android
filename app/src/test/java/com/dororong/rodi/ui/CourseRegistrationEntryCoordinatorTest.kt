package com.dororong.rodi.ui

import com.dororong.rodi.core.domain.model.course.CourseDraft
import com.dororong.rodi.core.domain.usecase.course.ClearCourseDraftUseCase
import com.dororong.rodi.core.domain.usecase.course.ObserveCourseDraftUseCase
import com.dororong.rodi.feature.course.registration.CourseRegistrationDialog
import com.dororong.rodi.feature.course.registration.CourseRegistrationIntent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseRegistrationEntryCoordinatorTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `임시 저장이 없거나 비어 있으면 코스 등록을 바로 연다`() {
        val noDraft: CourseDraft? = null

        assertEquals(
            CourseRegistrationPreflightDecision.OpenImmediately,
            noDraft.courseRegistrationPreflightDecision(),
        )
        assertEquals(
            CourseRegistrationPreflightDecision.OpenImmediately,
            CourseDraft().courseRegistrationPreflightDecision(),
        )
    }

    @Test
    fun `내용이 있는 임시 저장이 있으면 홈에서 이어하기 다이얼로그를 띄운다`() {
        val draft = CourseDraft(description = "퇴근길 연습")

        assertEquals(
            CourseRegistrationPreflightDecision.ShowResumeDialog,
            draft.courseRegistrationPreflightDecision(),
        )
    }

    @Test
    fun `이어하기와 새로 시작 진입은 등록 흐름의 이어하기 다이얼로그를 소비한다`() {
        assertEquals(
            CourseRegistrationIntent.DraftContinueClicked,
            courseRegistrationIntentForEntry(
                entryMode = CourseRegistrationEntryMode.ContinueDraft,
                dialog = CourseRegistrationDialog.ResumeDraft,
            ),
        )
        assertEquals(
            CourseRegistrationIntent.DraftDiscardClicked,
            courseRegistrationIntentForEntry(
                entryMode = CourseRegistrationEntryMode.StartFresh,
                dialog = CourseRegistrationDialog.ResumeDraft,
            ),
        )
    }

    @Test
    fun `일반 진입은 남아 있던 등록 흐름 임시 저장을 다이얼로그를 다시 띄우지 않고 이어간다`() {
        assertEquals(
            CourseRegistrationIntent.DraftContinueClicked,
            courseRegistrationIntentForEntry(
                entryMode = CourseRegistrationEntryMode.Normal,
                dialog = CourseRegistrationDialog.ResumeDraft,
            ),
        )
        assertEquals(
            null,
            courseRegistrationIntentForEntry(
                entryMode = CourseRegistrationEntryMode.Normal,
                dialog = CourseRegistrationDialog.Exit,
            ),
        )
    }

    @Test
    fun `이어하기가 아닌 다이얼로그는 진입 의도를 소비하지 않는다`() {
        assertEquals(
            null,
            courseRegistrationIntentForEntry(
                entryMode = CourseRegistrationEntryMode.ContinueDraft,
                dialog = CourseRegistrationDialog.Exit,
            ),
        )
        assertTrue(
            CourseRegistrationPreflightDecision.ShowResumeDialog !=
                CourseDraft().courseRegistrationPreflightDecision(),
        )
    }

    @Test
    fun `임시 저장 삭제가 실패하면 이어하기 다이얼로그를 유지할 수 있게 실패를 반환한다`() = runTest(dispatcher) {
        val observeDraft = mockk<ObserveCourseDraftUseCase>()
        val clearDraft = mockk<ClearCourseDraftUseCase>()
        every { observeDraft() } returns flowOf(null)
        coEvery { clearDraft() } throws IllegalStateException("disk")
        val viewModel = CourseRegistrationEntryViewModel(observeDraft, clearDraft)
        advanceUntilIdle()

        val result = viewModel.clearDraft()

        assertTrue(result.isFailure)
        assertEquals("disk", result.exceptionOrNull()?.message)
    }

    @Test
    fun `임시 저장 삭제 중 취소는 실패로 바꾸지 않고 그대로 전파한다`() = runTest(dispatcher) {
        val observeDraft = mockk<ObserveCourseDraftUseCase>()
        val clearDraft = mockk<ClearCourseDraftUseCase>()
        every { observeDraft() } returns flowOf(null)
        coEvery { clearDraft() } throws CancellationException()
        val viewModel = CourseRegistrationEntryViewModel(observeDraft, clearDraft)
        advanceUntilIdle()

        try {
            viewModel.clearDraft()
            fail<Nothing>("CancellationException must be rethrown, not converted to Result.failure")
        } catch (_: CancellationException) {
        }
    }

    @Test
    fun `임시 저장 삭제가 실패하면 스낵바에 보여줄 오류를 남긴다`() {
        assertEquals(
            "disk",
            courseRegistrationClearDraftFailureMessage(IllegalStateException("disk")),
        )
        assertEquals(
            "작성 중인 코스를 삭제하지 못했어요. 다시 시도해주세요.",
            courseRegistrationClearDraftFailureMessage(IllegalStateException("")),
        )
    }
}
