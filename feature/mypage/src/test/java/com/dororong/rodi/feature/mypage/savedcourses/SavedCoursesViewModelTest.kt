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
    fun `saved places append cursor pages and deduplicate by type and id`() = runTest(dispatcher) {
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
    fun `unapproved initial failure hides the exception detail`() = runTest(dispatcher) {
        val detail = "Field 'totalCount' is required for type with serial name 'SavedPlacesResponse'"
        val getSaved = mockk<GetSavedPlacesUseCase>()
        coEvery { getSaved(null, 20) } returns Result.failure(IllegalStateException(detail))

        val viewModel = SavedCoursesViewModel(getSaved)
        advanceUntilIdle()

        assertEquals("저장목록을 불러오지 못했어요.", viewModel.uiState.value.initialError)
        assertFalse(viewModel.uiState.value.initialError.orEmpty().contains(detail))
    }

    @Test
    fun `loadNextPage sends no request when hasNext is false`() = runTest(dispatcher) {
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
    fun `loadNextPage returns early when nextCursor is null`() = runTest(dispatcher) {
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
    fun `loadNextPage does not send a duplicate request while the previous one is active`() = runTest(dispatcher) {
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
    fun `next page failure keeps loaded places and sets only nextPageError`() = runTest(dispatcher) {
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
    fun `retry reloads the first page when places are empty`() = runTest(dispatcher) {
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
    fun `retry loads the next page when places already exist`() = runTest(dispatcher) {
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
    fun `reloading cancels the in-flight request so its late failure does not overwrite the new state`() = runTest(dispatcher) {
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

    private fun place(id: Long, type: PlaceType) = PlaceSummary(
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
    )
}
