package com.dororong.rodi.feature.home.search

import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.search.PlaceSuggestion
import com.dororong.rodi.core.domain.model.search.RecentSearch

enum class SearchResultState {
    Idle,
    Loading,
    Content,
    Empty,
    RegionEmpty,
    Error,
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
    data object OnRetry : SearchIntent
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
