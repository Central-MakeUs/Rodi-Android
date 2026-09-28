package com.dororong.rodi.feature.mypage.practicerecords

import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import com.dororong.rodi.core.domain.usecase.member.GetPracticeRecordsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeRecordsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
    private val first = PracticeRecord(1, 1, "장소", listOf(PracticeType.ROUNDABOUT), 1, Instant.EPOCH, false, PracticeStatus.VISITED)

    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { getPracticeRecords(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
    }
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `처음에는 비어 있고 로딩 중이 아니다`() = runTest(dispatcher) {
        val viewModel = PracticeRecordsViewModel(getPracticeRecords)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.records.isEmpty())
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `페이지를 이어 붙이고 중복 id를 뺀다`() = runTest(dispatcher) {
        coEvery { getPracticeRecords(null, 20) } returns Result.success(
            CursorPage(listOf(first), true, "next", 2),
        )
        coEvery { getPracticeRecords("next", 20) } returns Result.success(
            CursorPage(listOf(first, first.copy(practiceId = 2)), false, null, 2),
        )
        val viewModel = PracticeRecordsViewModel(getPracticeRecords)
        advanceUntilIdle()
        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.records.map { it.practiceId })
    }

    @Test
    fun `첫 로드는 방문 기록이 나올 때까지 보이지 않는 상태를 건너뛴다`() = runTest(dispatcher) {
        coEvery { getPracticeRecords(null, 20) } returns Result.success(
            CursorPage(listOf(first.copy(status = PracticeStatus.PLANNED)), true, "next", 2),
        )
        coEvery { getPracticeRecords("next", 20) } returns Result.success(
            CursorPage(listOf(first.copy(practiceId = 2)), false, null, 2),
        )

        val viewModel = PracticeRecordsViewModel(getPracticeRecords)
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.records.map { it.practiceId })
        coVerify(exactly = 1) { getPracticeRecords(null, 20) }
        coVerify(exactly = 1) { getPracticeRecords("next", 20) }
    }

    @Test
    fun `첫 로드 오류를 상태에 남긴다`() = runTest(dispatcher) {
        coEvery { getPracticeRecords(null, 20) } returns Result.failure(IllegalStateException("처음 오류"))
        val viewModel = PracticeRecordsViewModel(getPracticeRecords)
        advanceUntilIdle()
        assertEquals("연습기록을 불러오지 못했어요.", viewModel.uiState.value.initialError)
    }

    @Test
    fun `첫 로드 중 취소는 오류로 보여주지 않는다`() = runTest(dispatcher) {
        coEvery { getPracticeRecords(null, 20) } coAnswers { throw CancellationException("취소") }
        val viewModel = PracticeRecordsViewModel(getPracticeRecords)

        try {
            advanceUntilIdle()
        } catch (_: CancellationException) {
        }

        assertEquals(null, viewModel.uiState.value.initialError)
    }
}
