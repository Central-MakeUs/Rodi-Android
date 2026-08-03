package com.dororong.rodi.feature.home.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.search.PlaceSuggestion
import com.dororong.rodi.core.domain.model.search.RecentSearch
import com.dororong.rodi.core.domain.model.search.RecentSearchRegistration
import com.dororong.rodi.core.domain.model.search.SearchTargetType
import com.dororong.rodi.core.domain.usecase.place.GetRelatedSearchUseCase
import com.dororong.rodi.core.domain.usecase.place.SearchPlacesUseCase
import com.dororong.rodi.core.domain.usecase.search.DeleteAllRecentSearchesUseCase
import com.dororong.rodi.core.domain.usecase.search.DeleteRecentSearchUseCase
import com.dororong.rodi.core.domain.usecase.search.GetRecentSearchesUseCase
import com.dororong.rodi.core.domain.usecase.search.RegisterRecentSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_PAGE_SIZE = 20
private const val SEARCH_DEBOUNCE_MILLIS = 300L

enum class SearchResultState {
    Idle,
    Loading,
    Content,
    Empty,
    RegionEmpty,
}

data class SearchUiState(
    val query: String = "",
    val recentSearches: List<RecentSearch> = emptyList(),
    val isRecentSearchesLoading: Boolean = true,
    val isDeletingAllRecentSearches: Boolean = false,
    val deletingRecentSearchIds: Set<Long> = emptySet(),
    val resultState: SearchResultState = SearchResultState.Idle,
    val places: List<PlaceSuggestion> = emptyList(),
    val hasNextPage: Boolean = false,
    val nextCursor: String? = null,
    val isNextPageLoading: Boolean = false,
    val regionSuggestions: List<RegionOfficeLocation> = emptyList(),
)

sealed interface SearchIntent {
    data class OnQueryChange(val query: String) : SearchIntent
    data object OnImeSearch : SearchIntent
    data object OnLoadNextPage : SearchIntent
    data class OnRecentSearchClick(val search: RecentSearch) : SearchIntent
    data class OnRegionSuggestionClick(val region: RegionOfficeLocation) : SearchIntent
    data class OnPlaceSuggestionClick(val place: PlaceSuggestion) : SearchIntent
    data object OnDeleteAllRecentSearches : SearchIntent
    data class OnDeleteRecentSearch(val id: Long) : SearchIntent
}

sealed interface SearchEffect {
    data class ShowSnackbar(val message: String) : SearchEffect
    data class NavigateRegion(
        val region: RegionOfficeLocation,
        val initialPlaces: List<PlaceSummary>,
    ) : SearchEffect
    data class NavigatePlace(val placeId: Long) : SearchEffect
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val deleteAllRecentSearchesUseCase: DeleteAllRecentSearchesUseCase,
    private val deleteRecentSearchUseCase: DeleteRecentSearchUseCase,
    private val getRelatedSearchUseCase: GetRelatedSearchUseCase,
    private val searchPlacesUseCase: SearchPlacesUseCase,
    private val registerRecentSearchUseCase: RegisterRecentSearchUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _effect = Channel<SearchEffect>(Channel.BUFFERED)
    val effect: Flow<SearchEffect> = _effect.receiveAsFlow()

    private var searchJob: Job? = null
    private var nextPageJob: Job? = null
    private var searchGeneration = 0L
    private var origin: GeoPoint? = null

    init {
        loadRecentSearches()
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.OnQueryChange -> onQueryChange(intent.query)
            SearchIntent.OnImeSearch -> searchImmediately()
            SearchIntent.OnLoadNextPage -> loadNextPage()
            is SearchIntent.OnRecentSearchClick -> onRecentSearchClick(intent.search)
            is SearchIntent.OnRegionSuggestionClick -> {
                registerRecentSearch(
                    RecentSearchRegistration(SearchTargetType.REGION, intent.region.displayName),
                )
                selectRegion(intent.region)
            }
            is SearchIntent.OnPlaceSuggestionClick -> onPlaceSuggestionClick(intent.place)
            SearchIntent.OnDeleteAllRecentSearches -> deleteAllRecentSearches()
            is SearchIntent.OnDeleteRecentSearch -> deleteRecentSearch(intent.id)
        }
    }

    fun initialize(origin: GeoPoint) {
        this.origin = origin
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            getRecentSearchesUseCase()
                .onSuccess { searches ->
                    _state.update {
                        it.copy(
                            recentSearches = searches.take(MAX_RECENT_SEARCHES),
                            isRecentSearchesLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isRecentSearchesLoading = false) }
                    _effect.send(SearchEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun onQueryChange(query: String) {
        val limitedQuery = query.take(50)
        searchGeneration += 1
        searchJob?.cancel()
        nextPageJob?.cancel()
        val normalizedQuery = limitedQuery.trim()
        _state.update {
            it.copy(
                query = limitedQuery,
                resultState = if (normalizedQuery.isBlank()) SearchResultState.Idle else SearchResultState.Loading,
                regionSuggestions = emptyList(),
                places = emptyList(),
                hasNextPage = false,
                nextCursor = null,
                isNextPageLoading = false,
            )
        }
        if (normalizedQuery.isBlank()) return
        val generation = searchGeneration
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            searchFirstPage(normalizedQuery, generation)
        }
    }

    private fun searchImmediately() {
        val normalizedQuery = _state.value.query.trim()
        if (normalizedQuery.isBlank()) return
        registerRecentSearch(
            RecentSearchRegistration(
                type = SearchTargetType.REGION,
                keyword = normalizedQuery,
            ),
        )
        searchGeneration += 1
        val generation = searchGeneration
        searchJob?.cancel()
        nextPageJob?.cancel()
        _state.update {
            it.copy(
                resultState = SearchResultState.Loading,
                places = emptyList(),
                hasNextPage = false,
                nextCursor = null,
                isNextPageLoading = false,
            )
        }
        searchJob = viewModelScope.launch { searchFirstPage(normalizedQuery, generation) }
    }

    private suspend fun searchFirstPage(keyword: String, generation: Long) {
        getRelatedSearchUseCase(keyword, cursor = null, size = SEARCH_PAGE_SIZE)
            .onSuccess { relatedSearch ->
                if (generation != searchGeneration) return@onSuccess
                val regions = relatedSearch.regions.mapNotNull(RegionOfficeLocationResolver::find)
                _state.update {
                    it.copy(
                        resultState = if (relatedSearch.places.items.isEmpty() && regions.isEmpty()) {
                            SearchResultState.Empty
                        } else {
                            SearchResultState.Content
                        },
                        regionSuggestions = regions,
                        places = relatedSearch.places.items.distinctBy(PlaceSuggestion::placeId),
                        hasNextPage = relatedSearch.places.hasNext,
                        nextCursor = relatedSearch.places.nextCursor,
                    )
                }
            }
            .onFailure { error ->
                if (generation != searchGeneration) return@onFailure
                _state.update { it.copy(resultState = SearchResultState.Idle) }
                _effect.send(SearchEffect.ShowSnackbar(error.userMessage()))
            }
    }

    private fun onRecentSearchClick(search: RecentSearch) {
        val region = search.regionKey?.let(RegionOfficeLocationResolver::find)
            ?: RegionOfficeLocationResolver.find(search.keyword)
        val placeId = search.placeId
        when {
            search.type == SearchTargetType.PLACE && placeId != null -> {
                registerRecentSearch(
                    RecentSearchRegistration(SearchTargetType.PLACE, search.keyword, placeId),
                )
                viewModelScope.launch { _effect.send(SearchEffect.NavigatePlace(placeId)) }
            }
            search.type == SearchTargetType.REGION || region != null -> {
                region?.let {
                    registerRecentSearch(RecentSearchRegistration(SearchTargetType.REGION, search.keyword))
                    selectRegion(it)
                } ?: viewModelScope.launch {
                    _effect.send(SearchEffect.ShowSnackbar("지역 위치를 찾지 못했어요. 다시 검색해주세요."))
                }
            }
            else -> {
                onQueryChange(search.keyword)
                searchImmediately()
            }
        }
    }

    private fun onPlaceSuggestionClick(place: PlaceSuggestion) {
        registerRecentSearch(
            RecentSearchRegistration(SearchTargetType.PLACE, place.name, place.placeId),
        )
        viewModelScope.launch { _effect.send(SearchEffect.NavigatePlace(place.placeId)) }
    }

    private fun selectRegion(region: RegionOfficeLocation) {
        val origin = origin ?: return
        searchJob?.cancel()
        nextPageJob?.cancel()
        searchGeneration += 1
        val generation = searchGeneration
        _state.update {
            it.copy(
                query = region.displayName,
                regionSuggestions = emptyList(),
                resultState = SearchResultState.Loading,
                places = emptyList(),
                hasNextPage = false,
                nextCursor = null,
                isNextPageLoading = false,
            )
        }
        searchJob = viewModelScope.launch {
            searchPlacesUseCase(region.displayName, origin, cursor = null, size = SEARCH_PAGE_SIZE)
                .onSuccess { page ->
                    if (generation != searchGeneration) return@onSuccess
                    if (page.items.isEmpty()) {
                        _state.update { it.copy(resultState = SearchResultState.RegionEmpty) }
                    } else {
                        _effect.send(SearchEffect.NavigateRegion(region, page.items))
                    }
                }
                .onFailure { error ->
                    if (generation != searchGeneration) return@onFailure
                    _state.update { it.copy(resultState = SearchResultState.Idle) }
                    _effect.send(SearchEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun loadNextPage() {
        val current = _state.value
        val cursor = current.nextCursor ?: return
        if (!current.hasNextPage || current.isNextPageLoading || nextPageJob?.isActive == true) return
        val generation = searchGeneration
        val keyword = current.query.trim()
        if (keyword.isBlank()) return
        nextPageJob = viewModelScope.launch {
            _state.update { it.copy(isNextPageLoading = true) }
            getRelatedSearchUseCase(keyword, cursor = cursor, size = SEARCH_PAGE_SIZE)
                .onSuccess { relatedSearch ->
                    if (generation != searchGeneration) return@onSuccess
                    _state.update {
                        val places = (it.places + relatedSearch.places.items).distinctBy(PlaceSuggestion::placeId)
                        it.copy(
                            places = places,
                            hasNextPage = relatedSearch.places.hasNext,
                            nextCursor = relatedSearch.places.nextCursor,
                            isNextPageLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    if (generation != searchGeneration) return@onFailure
                    _state.update { it.copy(isNextPageLoading = false) }
                    _effect.send(SearchEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun registerRecentSearch(search: RecentSearchRegistration) {
        viewModelScope.launch {
            registerRecentSearchUseCase(search)
        }
    }

    private fun deleteAllRecentSearches() {
        if (_state.value.isDeletingAllRecentSearches) return
        viewModelScope.launch {
            _state.update { it.copy(isDeletingAllRecentSearches = true) }
            deleteAllRecentSearchesUseCase()
                .onSuccess {
                    _state.update {
                        it.copy(
                            recentSearches = emptyList(),
                            isDeletingAllRecentSearches = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isDeletingAllRecentSearches = false) }
                    _effect.send(SearchEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }

    private fun deleteRecentSearch(id: Long) {
        if (id in _state.value.deletingRecentSearchIds || _state.value.isDeletingAllRecentSearches) return
        viewModelScope.launch {
            _state.update { it.copy(deletingRecentSearchIds = it.deletingRecentSearchIds + id) }
            deleteRecentSearchUseCase(id)
                .onSuccess {
                    _state.update {
                        it.copy(
                            recentSearches = it.recentSearches.filterNot { search -> search.id == id },
                            deletingRecentSearchIds = it.deletingRecentSearchIds - id,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(deletingRecentSearchIds = it.deletingRecentSearchIds - id) }
                    _effect.send(SearchEffect.ShowSnackbar(error.userMessage()))
                }
        }
    }
}

private const val MAX_RECENT_SEARCHES = 15
private fun Throwable.userMessage(): String = message?.takeIf(String::isNotBlank)
    ?: "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요."
