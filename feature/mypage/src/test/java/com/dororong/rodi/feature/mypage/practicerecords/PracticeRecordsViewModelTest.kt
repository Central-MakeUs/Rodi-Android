package com.dororong.rodi.feature.mypage.practicerecords

import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.usecase.member.GetPracticeRecordsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeRecordsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
    private val first = PracticeRecord(1, 1, "장소", listOf(PracticeType.PARKING), 1, Instant.EPOCH, true, false)

    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { getPracticeRecords(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
    }
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load starts empty and idle`() = runTest(dispatcher) {
        val viewModel = PracticeRecordsViewModel(getPracticeRecords)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.records.isEmpty())
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `paging accumulates records and removes duplicate ids`() {
        val viewModel = PracticeRecordsViewModel(getPracticeRecords)
        viewModel.appendPage(listOf(first), hasNextPage = true, initial = true)
        viewModel.appendPage(listOf(first, first.copy(practiceId = 2)), hasNextPage = false, initial = false)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.records.map { it.practiceId })
    }

    @Test
    fun `initial and next page errors are kept separate`() {
        val viewModel = PracticeRecordsViewModel(getPracticeRecords)
        viewModel.setInitialError("처음 오류")
        assertEquals("처음 오류", viewModel.uiState.value.initialError)
        viewModel.appendPage(listOf(first), true, true)
        viewModel.setNextPageError("추가 오류")
        assertEquals(null, viewModel.uiState.value.initialError)
        assertEquals("추가 오류", viewModel.uiState.value.nextPageError)
    }
}
