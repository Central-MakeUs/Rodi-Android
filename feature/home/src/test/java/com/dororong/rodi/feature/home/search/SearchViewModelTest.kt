package com.dororong.rodi.feature.home.search

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.search.PlaceSuggestion
import com.dororong.rodi.core.domain.model.search.RecentSearch
import com.dororong.rodi.core.domain.model.search.RecentSearchRegistration
import com.dororong.rodi.core.domain.model.search.RelatedSearch
import com.dororong.rodi.core.domain.model.search.SearchTargetType
import com.dororong.rodi.core.domain.repository.PlaceRepository
import com.dororong.rodi.core.domain.repository.RecentSearchRepository
import com.dororong.rodi.core.domain.usecase.place.GetRelatedSearchUseCase
import com.dororong.rodi.core.domain.usecase.place.SearchPlacesUseCase
import com.dororong.rodi.core.domain.usecase.search.DeleteAllRecentSearchesUseCase
import com.dororong.rodi.core.domain.usecase.search.DeleteRecentSearchUseCase
import com.dororong.rodi.core.domain.usecase.search.GetRecentSearchesUseCase
import com.dororong.rodi.core.domain.usecase.search.RegisterRecentSearchUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
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
    fun `query uses related search after debounce and loads the next place page`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        coEvery {
            dependencies.placeRepository.relatedSearch("강남", null, 20)
        } returns related(regions = listOf("서울 강남구"), places = listOf(suggestion(1)), hasNext = true, nextCursor = "next")
        coEvery {
            dependencies.placeRepository.relatedSearch("강남", "next", 20)
        } returns related(places = listOf(suggestion(2)))
        val viewModel = dependencies.viewModel()
        viewModel.initialize(GeoPoint(37.5, 126.9))
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.OnQueryChange(" 강남 "))
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(SearchResultState.Content, viewModel.state.value.resultState)
        assertEquals(listOf("서울 강남구"), viewModel.state.value.regionSuggestions.map { it.displayName })
        assertEquals(listOf(1L), viewModel.state.value.places.map { it.placeId })
        viewModel.onIntent(SearchIntent.OnLoadNextPage)
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.state.value.places.map { it.placeId })
        assertEquals(false, viewModel.state.value.hasNextPage)
    }

    @Test
    fun `typing and ime search do not register recent search`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        coEvery { dependencies.placeRepository.relatedSearch("강남", null, 20) } returns related()
        coEvery { dependencies.recentRepository.registerRecentSearch(any()) } returns Unit
        val viewModel = dependencies.viewModel()

        viewModel.onIntent(SearchIntent.OnQueryChange("강남"))
        advanceTimeBy(300)
        advanceUntilIdle()

        coVerify(exactly = 0) { dependencies.recentRepository.registerRecentSearch(any()) }
        viewModel.onIntent(SearchIntent.OnImeSearch)
        advanceUntilIdle()

        coVerify(exactly = 0) { dependencies.recentRepository.registerRecentSearch(any()) }
    }

    @Test
    fun `search failure shows error state and retry runs the same request`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        coEvery {
            dependencies.placeRepository.relatedSearch("강남", null, 20)
        } throws IllegalStateException("network")
        val viewModel = dependencies.viewModel()

        viewModel.onIntent(SearchIntent.OnQueryChange("강남"))
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(SearchResultState.Error, viewModel.state.value.resultState)

        coEvery {
            dependencies.placeRepository.relatedSearch("강남", null, 20)
        } returns related(places = listOf(suggestion(1)))
        viewModel.onIntent(SearchIntent.OnRetry)
        advanceUntilIdle()

        assertEquals(SearchResultState.Content, viewModel.state.value.resultState)
        assertEquals(listOf(1L), viewModel.state.value.places.map { it.placeId })
        coVerify(exactly = 2) { dependencies.placeRepository.relatedSearch("강남", null, 20) }
    }

    @Test
    fun `ime search cancellation does not emit error snackbar`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        coEvery { dependencies.placeRepository.relatedSearch("강남", null, 20) } coAnswers {
            awaitCancellation()
        }
        coEvery { dependencies.placeRepository.relatedSearch("서초", null, 20) } returns related()
        val viewModel = dependencies.viewModel()

        viewModel.effect.test {
            viewModel.onIntent(SearchIntent.OnQueryChange("강남"))
            advanceTimeBy(300)
            viewModel.onIntent(SearchIntent.OnQueryChange("서초"))
            viewModel.onIntent(SearchIntent.OnImeSearch)
            advanceUntilIdle()

            assertEquals(SearchResultState.Empty, viewModel.state.value.resultState)
            expectNoEvents()
        }
    }

    @Test
    fun `related search keeps the server region order instead of locally sorting it`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        coEvery { dependencies.placeRepository.relatedSearch("중구", null, 20) } returns related(
            regions = listOf("부산 중구", "서울 중구"),
        )
        val viewModel = dependencies.viewModel()

        viewModel.onIntent(SearchIntent.OnQueryChange("중구"))
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(
            listOf("부산 중구", "서울 중구"),
            viewModel.state.value.regionSuggestions.map { it.displayName },
        )
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
        coEvery { dependencies.recentRepository.registerRecentSearch(any()) } returns Unit
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
        coVerify {
            dependencies.recentRepository.registerRecentSearch(
                RecentSearchRegistration(SearchTargetType.REGION, "서울 중구"),
            )
        }
    }

    @Test
    fun `region suggestion without places shows region empty state`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        val origin = GeoPoint(37.5, 126.9)
        coEvery {
            dependencies.placeRepository.searchPlaces("서울 중구", origin, null, 20)
        } returns CursorPage(emptyList(), false, null, 0)
        coEvery { dependencies.recentRepository.registerRecentSearch(any()) } returns Unit
        coEvery { dependencies.recentRepository.getRecentSearches() } returnsMany listOf(
            emptyList(),
            listOf(RecentSearch(7, "서울 중구", SearchTargetType.REGION)),
        )
        val viewModel = dependencies.viewModel()
        viewModel.initialize(origin)
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.OnRegionSuggestionClick(
            requireNotNull(RegionOfficeLocationResolver.find("서울 중구")),
        ))
        advanceUntilIdle()

        assertEquals(SearchResultState.RegionEmpty, viewModel.state.value.resultState)
        assertEquals(listOf("서울 중구"), viewModel.state.value.recentSearches.map { it.keyword })
    }

    @Test
    fun `untyped recent search runs again without registering it as region`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        coEvery { dependencies.placeRepository.relatedSearch("알 수 없는 장소", null, 20) } returns related()
        coEvery { dependencies.recentRepository.registerRecentSearch(any()) } returns Unit
        val viewModel = dependencies.viewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            SearchIntent.OnRecentSearchClick(RecentSearch(1, "알 수 없는 장소")),
        )
        advanceUntilIdle()

        assertEquals(SearchResultState.Empty, viewModel.state.value.resultState)
        coVerify(exactly = 1) {
            dependencies.placeRepository.relatedSearch("알 수 없는 장소", null, 20)
        }
        coVerify(exactly = 0) { dependencies.recentRepository.registerRecentSearch(any()) }
    }

    @Test
    fun `place suggestion registers place before navigating`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        coEvery { dependencies.recentRepository.registerRecentSearch(any()) } returns Unit
        val viewModel = dependencies.viewModel()
        val effect = async { viewModel.effect.first() }

        viewModel.onIntent(SearchIntent.OnPlaceSuggestionClick(suggestion(7)))
        advanceUntilIdle()

        assertEquals(SearchEffect.NavigatePlace(7), effect.await())
        coVerify {
            dependencies.recentRepository.registerRecentSearch(
                RecentSearchRegistration(SearchTargetType.PLACE, "place-7", 7),
            )
        }
    }

    @Test
    fun `place navigation continues when recent search registration fails`() = runTest(dispatcher) {
        val dependencies = Dependencies()
        coEvery { dependencies.recentRepository.registerRecentSearch(any()) } throws IllegalStateException()
        val viewModel = dependencies.viewModel()
        val effect = async { viewModel.effect.first() }

        viewModel.onIntent(SearchIntent.OnPlaceSuggestionClick(suggestion(7)))
        advanceUntilIdle()

        assertEquals(SearchEffect.NavigatePlace(7), effect.await())
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
            getRelatedSearchUseCase = GetRelatedSearchUseCase(placeRepository),
            searchPlacesUseCase = SearchPlacesUseCase(placeRepository),
            registerRecentSearchUseCase = RegisterRecentSearchUseCase(recentRepository),
        )
    }
}

private fun related(
    regions: List<String> = emptyList(),
    places: List<PlaceSuggestion> = emptyList(),
    hasNext: Boolean = false,
    nextCursor: String? = null,
) = RelatedSearch(regions, CursorPage(places, hasNext, nextCursor, null))

private fun suggestion(id: Long) = PlaceSuggestion(id, "place-$id", "서울 중구")

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
