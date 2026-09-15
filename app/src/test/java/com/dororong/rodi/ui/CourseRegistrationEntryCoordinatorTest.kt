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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseRegistrationEntryCoordinatorTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `missing or empty draft opens registration immediately`() {
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
    fun `meaningful draft shows home resume dialog`() {
        val draft = CourseDraft(description = "퇴근길 연습")

        assertEquals(
            CourseRegistrationPreflightDecision.ShowResumeDialog,
            draft.courseRegistrationPreflightDecision(),
        )
    }

    @Test
    fun `continue and fresh entry modes consume the flow resume dialog`() {
        assertEquals(
            CourseRegistrationIntent.ContinueDraft,
            courseRegistrationIntentForEntry(
                entryMode = CourseRegistrationEntryMode.ContinueDraft,
                dialog = CourseRegistrationDialog.ResumeDraft,
            ),
        )
        assertEquals(
            CourseRegistrationIntent.DiscardDraft,
            courseRegistrationIntentForEntry(
                entryMode = CourseRegistrationEntryMode.StartFresh,
                dialog = CourseRegistrationDialog.ResumeDraft,
            ),
        )
    }

    @Test
    fun `normal entry safely continues a stale flow draft without a second dialog`() {
        assertEquals(
            CourseRegistrationIntent.ContinueDraft,
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
    fun `non-resume dialogs do not consume an entry intent`() {
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
    fun `clear draft returns failure so the resume dialog can stay open`() = runTest(dispatcher) {
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
    fun `clear draft rethrows cancellation instead of converting it to a failure`() = runTest(dispatcher) {
        val observeDraft = mockk<ObserveCourseDraftUseCase>()
        val clearDraft = mockk<ClearCourseDraftUseCase>()
        every { observeDraft() } returns flowOf(null)
        coEvery { clearDraft() } throws CancellationException()
        val viewModel = CourseRegistrationEntryViewModel(observeDraft, clearDraft)
        advanceUntilIdle()

        try {
            viewModel.clearDraft()
            fail("CancellationException must be rethrown, not converted to Result.failure")
        } catch (_: CancellationException) {
        }
    }

    @Test
    fun `clear draft failure keeps the error for the snackbar`() {
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
