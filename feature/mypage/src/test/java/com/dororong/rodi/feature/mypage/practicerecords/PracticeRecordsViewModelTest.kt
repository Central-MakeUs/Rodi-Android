package com.dororong.rodi.feature.mypage.practicerecords

import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.usecase.member.GetPracticeRecordsUseCase
import com.dororong.rodi.core.domain.usecase.practice.DeletePracticeUseCase
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeRecordsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
    private val deletePractice = mockk<DeletePracticeUseCase>()
    private val first = PracticeRecord(1, 1, "장소", listOf(PracticeType.PARKING), 1, Instant.EPOCH, true, false)

    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { getPracticeRecords(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        coEvery { deletePractice(any()) } returns Result.success(Unit)
    }
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load starts empty and idle`() = runTest(dispatcher) {
        val viewModel = PracticeRecordsViewModel(getPracticeRecords, deletePractice)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.records.isEmpty())
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `paging accumulates records and removes duplicate ids`() = runTest(dispatcher) {
        coEvery { getPracticeRecords(null, 20) } returns Result.success(
            CursorPage(listOf(first), true, "next", 2),
        )
        coEvery { getPracticeRecords("next", 20) } returns Result.success(
            CursorPage(listOf(first, first.copy(practiceId = 2)), false, null, 2),
        )
        val viewModel = PracticeRecordsViewModel(getPracticeRecords, deletePractice)
        advanceUntilIdle()
        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.records.map { it.practiceId })
    }

    @Test
    fun `initial load errors are kept in state`() = runTest(dispatcher) {
        coEvery { getPracticeRecords(null, 20) } returns Result.failure(IllegalStateException("처음 오류"))
        val viewModel = PracticeRecordsViewModel(getPracticeRecords, deletePractice)
        advanceUntilIdle()
        assertEquals("처음 오류", viewModel.uiState.value.initialError)
    }

    @Test
    fun `cancellation during initial load is not exposed as an error`() = runTest(dispatcher) {
        coEvery { getPracticeRecords(null, 20) } coAnswers { throw CancellationException("취소") }
        val viewModel = PracticeRecordsViewModel(getPracticeRecords, deletePractice)

        try {
            advanceUntilIdle()
        } catch (_: CancellationException) {
        }

        assertEquals(null, viewModel.uiState.value.initialError)
    }

    @Test
    fun `deleting a record removes it from state`() = runTest(dispatcher) {
        coEvery { getPracticeRecords(null, 20) } returns Result.success(CursorPage(listOf(first), false, null, 1))
        val viewModel = PracticeRecordsViewModel(getPracticeRecords, deletePractice)
        advanceUntilIdle()

        viewModel.delete(first)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.records.isEmpty())
    }

    @Test
    fun `failed deletion keeps the record and exposes the error`() = runTest(dispatcher) {
        coEvery { getPracticeRecords(null, 20) } returns Result.success(CursorPage(listOf(first), false, null, 1))
        coEvery { deletePractice(first.practiceId) } returns Result.failure(IllegalStateException("삭제 실패"))
        val viewModel = PracticeRecordsViewModel(getPracticeRecords, deletePractice)
        advanceUntilIdle()

        viewModel.delete(first)
        advanceUntilIdle()

        assertEquals(listOf(first), viewModel.uiState.value.records)
        assertEquals("삭제 실패", viewModel.uiState.value.deleteErrorMessage)
        assertNull(viewModel.uiState.value.deletingPracticeId)
    }

    @Test
    fun `cancelled deletion clears progress and allows a later delete`() = runTest(dispatcher) {
        coEvery { getPracticeRecords(null, 20) } returns Result.success(CursorPage(listOf(first), false, null, 1))
        var deleteCalls = 0
        coEvery { deletePractice(first.practiceId) } coAnswers {
            deleteCalls += 1
            if (deleteCalls == 1) throw CancellationException("취소")
            Result.success(Unit)
        }
        val viewModel = PracticeRecordsViewModel(getPracticeRecords, deletePractice)
        advanceUntilIdle()

        viewModel.delete(first)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deletingPracticeId)
        assertNull(viewModel.uiState.value.deleteErrorMessage)

        viewModel.delete(first)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.records.isEmpty())
        assertEquals(2, deleteCalls)
    }
}
