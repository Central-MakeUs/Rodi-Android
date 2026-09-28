package com.dororong.rodi.feature.mypage.savedcourses

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.usecase.place.GetSavedPlacesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
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
class SavedCoursesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `저장 목록은 커서 페이지를 이어 붙이고 종류와 id로 중복을 뺀다`() = runTest(dispatcher) {
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returns Result.success(
            CursorPage(listOf(place(1, PlaceType.COURSE)), true, "next", 2),
        )
        coEvery { getSaved("next", 20) } returns Result.success(
            CursorPage(
                listOf(place(1, PlaceType.COURSE), place(2, PlaceType.PARKING)),
                false,
                null,
                null,
            ),
        )
        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.places.map { it.id })
        assertEquals(2, viewModel.uiState.value.totalCount)
        assertFalse(viewModel.uiState.value.hasNext)
    }

    @Test
    fun `첫 로드 실패는 예외 내용을 숨기고 기본 문구를 보여준다`() = runTest(dispatcher) {
        val detail = "Field 'totalCount' is required for type with serial name 'SavedPlacesResponse'"
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returns Result.failure(IllegalStateException(detail))

        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()

        assertEquals("저장목록을 불러오지 못했어요.", viewModel.uiState.value.initialError)
        assertFalse(viewModel.uiState.value.initialError.orEmpty().contains(detail))
    }

    @Test
    fun `다음 페이지가 없으면 요청을 보내지 않는다`() = runTest(dispatcher) {
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returns Result.success(
            CursorPage(listOf(place(1, PlaceType.COURSE)), false, "next", 1),
        )
        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        coVerify(exactly = 0) { getSaved("next", any()) }
        assertEquals(listOf(1L), viewModel.uiState.value.places.map { it.id })
    }

    @Test
    fun `다음 커서가 없으면 다음 페이지를 요청하지 않는다`() = runTest(dispatcher) {
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returns Result.success(
            CursorPage(listOf(place(1, PlaceType.COURSE)), true, null, 1),
        )
        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        coVerify(exactly = 1) { getSaved(any(), any()) }
        assertFalse(viewModel.uiState.value.isNextPageLoading)
    }

    @Test
    fun `이전 요청이 진행 중이면 다음 페이지 요청을 중복으로 보내지 않는다`() = runTest(dispatcher) {
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returns Result.success(
            CursorPage(listOf(place(1, PlaceType.COURSE)), true, "next", 2),
        )
        coEvery { getSaved("next", 20) } returns Result.success(
            CursorPage(listOf(place(2, PlaceType.PARKING)), false, null, null),
        )
        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()

        viewModel.loadNextPage()
        viewModel.loadNextPage()
        advanceUntilIdle()

        coVerify(exactly = 1) { getSaved("next", 20) }
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.places.map { it.id })
    }

    @Test
    fun `다음 페이지가 실패하면 불러온 장소를 유지하고 다음 페이지 오류만 표시한다`() = runTest(dispatcher) {
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returns Result.success(
            CursorPage(listOf(place(1, PlaceType.COURSE)), true, "next", 2),
        )
        coEvery { getSaved("next", 20) } returns Result.failure(IllegalStateException("network"))
        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(1L), state.places.map { it.id })
        assertEquals("다음 장소를 불러오지 못했어요.", state.nextPageError)
        assertNull(state.initialError)
        assertFalse(state.isNextPageLoading)
        assertTrue(state.hasNext)
        assertEquals("next", state.nextCursor)
    }

    @Test
    fun `장소가 없으면 다시 시도는 첫 페이지를 다시 불러온다`() = runTest(dispatcher) {
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returnsMany listOf(
            Result.failure(IllegalStateException("network")),
            Result.success(CursorPage(listOf(place(1, PlaceType.COURSE)), false, null, 1)),
        )
        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        coVerify(exactly = 2) { getSaved(null, 20) }
        assertEquals(listOf(1L), viewModel.uiState.value.places.map { it.id })
        assertNull(viewModel.uiState.value.initialError)
    }

    @Test
    fun `장소가 있으면 다시 시도는 다음 페이지를 불러온다`() = runTest(dispatcher) {
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returns Result.success(
            CursorPage(listOf(place(1, PlaceType.COURSE)), true, "next", 2),
        )
        coEvery { getSaved("next", 20) } returnsMany listOf(
            Result.failure(IllegalStateException("network")),
            Result.success(CursorPage(listOf(place(2, PlaceType.PARKING)), false, null, null)),
        )
        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()
        viewModel.loadNextPage()
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        coVerify(exactly = 1) { getSaved(null, 20) }
        coVerify(exactly = 2) { getSaved("next", 20) }
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.places.map { it.id })
        assertNull(viewModel.uiState.value.nextPageError)
    }

    @Test
    fun `다시 불러오면 먼저 보낸 요청의 늦은 실패가 새 상태를 덮지 않는다`() = runTest(dispatcher) {
        val getSaved = mockk<GetSavedPlacesUseCase>()
        val staleResponse = CompletableDeferred<Result<CursorPage<PlaceSummary>>>()
        var calls = 0
        coEvery { getSaved(null, 20) } coAnswers {
            calls += 1
            if (calls == 1) {
                staleResponse.await()
            } else {
                Result.success(CursorPage(listOf(place(2, PlaceType.PARKING)), false, null, 1))
            }
        }
        val viewModel = SavedCoursesViewModel(getSaved)
        runCurrent()

        viewModel.loadInitial()
        advanceUntilIdle()
        staleResponse.complete(Result.failure(IllegalStateException("stale")))
        advanceUntilIdle()

        coVerify(exactly = 2) { getSaved(null, 20) }
        assertEquals(listOf(2L), viewModel.uiState.value.places.map { it.id })
        assertNull(viewModel.uiState.value.initialError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `등록자가 삭제한 코스는 저장 목록과 개수에서 뺀다`() = runTest(dispatcher) {
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returns Result.success(
            CursorPage(listOf(place(1, PlaceType.COURSE, isDeleted = true), place(2, PlaceType.PARKING)), false, null, 2),
        )

        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.places.map { it.id })
        assertEquals(1L, viewModel.uiState.value.totalCount)
    }

    @Test
    fun `첫 페이지가 모두 삭제된 코스면 빈 화면 대신 다음 페이지를 이어서 불러온다`() =
        runTest(dispatcher) {
            val getSaved = mockk<GetSavedPlacesUseCase>()
            coEvery { getSaved(null, 20) } returns Result.success(
                CursorPage(listOf(place(1, PlaceType.COURSE, isDeleted = true)), true, "next", 2),
            )
            coEvery { getSaved("next", 20) } returns Result.success(
                CursorPage(listOf(place(2, PlaceType.COURSE)), false, null, null),
            )

            val viewModel = SavedCoursesViewModel(getSaved)
            advanceUntilIdle()

            assertEquals(listOf(2L), viewModel.uiState.value.places.map { it.id })
            assertEquals(1L, viewModel.uiState.value.totalCount)
            assertFalse(viewModel.uiState.value.hasNext)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `다음 페이지에서 찾은 삭제된 코스도 개수에서 뺀다`() = runTest(dispatcher) {
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returns Result.success(
            CursorPage(listOf(place(1, PlaceType.COURSE)), true, "next", 3),
        )
        coEvery { getSaved("next", 20) } returns Result.success(
            CursorPage(listOf(place(2, PlaceType.COURSE, isDeleted = true), place(3, PlaceType.PARKING)), false, null, null),
        )
        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1L, 3L), viewModel.uiState.value.places.map { it.id })
        assertEquals(2L, viewModel.uiState.value.totalCount)
    }

    private fun place(id: Long, type: PlaceType, isDeleted: Boolean = false) = PlaceSummary(
        id = id,
        type = type,
        name = "장소 $id",
        address = "서울",
        point = GeoPoint(37.5, 126.9),
        distanceFromMeMeters = null,
        practiceTypes = emptyList(),
        description = null,
        distanceMeters = null,
        capacity = null,
        openTime = null,
        isDeleted = isDeleted,
    )
}
