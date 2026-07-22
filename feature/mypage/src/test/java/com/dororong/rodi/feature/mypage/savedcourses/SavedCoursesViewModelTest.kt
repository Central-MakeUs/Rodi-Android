package com.dororong.rodi.feature.mypage.savedcourses

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.usecase.place.GetSavedPlacesUseCase
import io.mockk.coEvery
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
