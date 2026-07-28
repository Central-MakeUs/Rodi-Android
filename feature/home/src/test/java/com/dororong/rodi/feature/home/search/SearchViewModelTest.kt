package com.dororong.rodi.feature.home.search

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.search.RecentSearch
import com.dororong.rodi.core.domain.repository.PlaceRepository
import com.dororong.rodi.core.domain.repository.RecentSearchRepository
import com.dororong.rodi.core.domain.usecase.place.SearchPlacesUseCase
import com.dororong.rodi.core.domain.usecase.search.DeleteAllRecentSearchesUseCase
import com.dororong.rodi.core.domain.usecase.search.DeleteRecentSearchUseCase
import com.dororong.rodi.core.domain.usecase.search.GetRecentSearchesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `query searches after debounce and loads the next page`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        coEvery {
            dependencies.placeRepository.searchPlaces("강남", GeoPoint(37.5, 126.9), null, 20)
        } returns CursorPage(listOf(place(1)), true, "next", 2)
        coEvery {
            dependencies.placeRepository.searchPlaces("강남", GeoPoint(37.5, 126.9), "next", 20)
        } returns CursorPage(listOf(place(2)), false, null, null)
        val viewModel = dependencies.viewModel()
        viewModel.initialize(GeoPoint(37.5, 126.9))
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.OnQueryChange(" 강남 "))
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(SearchResultState.Content, viewModel.state.value.resultState)
        assertEquals(listOf(1L), viewModel.state.value.places.map { it.id })
        viewModel.onIntent(SearchIntent.OnLoadNextPage)
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.state.value.places.map { it.id })
        assertEquals(false, viewModel.state.value.hasNextPage)
    }

    @Test
    fun `successful recent search delete removes only that row`() = runTest(dispatcher) {
        val dependencies = Dependencies(
            recentSearches = listOf(RecentSearch(1, "서울 중구"), RecentSearch(2, "부산 중구")),
        )
        coEvery { dependencies.recentRepository.deleteRecentSearch(1) } returns Unit
        val viewModel = dependencies.viewModel()
        viewModel.initialize(GeoPoint(37.5, 126.9))
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.OnDeleteRecentSearch(1))
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.state.value.recentSearches.map { it.id })
        coVerify { dependencies.recentRepository.deleteRecentSearch(1) }
    }

    private class Dependencies(
        recentSearches: List<RecentSearch> = emptyList(),
    ) {
        val recentRepository = mockk<RecentSearchRepository>()
        val placeRepository = mockk<PlaceRepository>()

        init {
            coEvery { recentRepository.getRecentSearches() } returns recentSearches
        }

        fun viewModel() = SearchViewModel(
            getRecentSearchesUseCase = GetRecentSearchesUseCase(recentRepository),
            deleteAllRecentSearchesUseCase = DeleteAllRecentSearchesUseCase(recentRepository),
            deleteRecentSearchUseCase = DeleteRecentSearchUseCase(recentRepository),
            searchPlacesUseCase = SearchPlacesUseCase(placeRepository),
        )
    }
}

private fun place(id: Long) = PlaceSummary(
    id = id,
    type = PlaceType.COURSE,
    name = "place-$id",
    address = "서울",
    point = GeoPoint(37.5, 126.9),
    distanceFromMeMeters = 10,
    practiceTypes = emptyList(),
    description = null,
    distanceMeters = null,
    capacity = null,
    openTime = null,
)
