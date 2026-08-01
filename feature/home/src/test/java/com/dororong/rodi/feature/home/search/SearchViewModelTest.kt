package com.dororong.rodi.feature.home.search

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.search.RecentSearch
import com.dororong.rodi.core.domain.model.search.SearchTargetType
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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

    @Test
    fun `recent searches are capped at fifteen`() = runTest(dispatcher) {
        val dependencies = Dependencies(
            recentSearches = List(16) { RecentSearch(it.toLong(), "검색어$it") },
        )
        val viewModel = dependencies.viewModel()

        advanceUntilIdle()

        assertEquals(15, viewModel.state.value.recentSearches.size)
    }

    @Test
    fun `region suggestion with places navigates to its map result`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        val origin = GeoPoint(37.5, 126.9)
        coEvery {
            dependencies.placeRepository.searchPlaces("서울 중구", origin, null, 20)
        } returns CursorPage(listOf(place(1)), false, null, 1)
        val viewModel = dependencies.viewModel()
        viewModel.initialize(origin)
        advanceUntilIdle()
        val region = requireNotNull(RegionOfficeLocationResolver.find("서울 중구"))
        val effect = async { viewModel.effect.first() }

        viewModel.onIntent(SearchIntent.OnRecentSearchClick(
            RecentSearch(1, "서울 중구", SearchTargetType.REGION),
        ))
        advanceUntilIdle()

        assertEquals(SearchEffect.NavigateRegion(region, listOf(place(1))), effect.await())
    }

    @Test
    fun `region suggestion without places shows region empty state`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        val origin = GeoPoint(37.5, 126.9)
        coEvery {
            dependencies.placeRepository.searchPlaces("서울 중구", origin, null, 20)
        } returns CursorPage(emptyList(), false, null, 0)
        val viewModel = dependencies.viewModel()
        viewModel.initialize(origin)
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.OnRegionSuggestionClick(
            requireNotNull(RegionOfficeLocationResolver.find("서울 중구")),
        ))
        advanceUntilIdle()

        assertEquals(SearchResultState.RegionEmpty, viewModel.state.value.resultState)
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
