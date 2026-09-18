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
    data class QueryChanged(val query: String) : SearchIntent
    data object ImeSearchSubmitted : SearchIntent
    data object RetryClicked : SearchIntent
    data object ListEndReached : SearchIntent
    data class RecentSearchClicked(val search: RecentSearch) : SearchIntent
    data class RegionSuggestionClicked(val region: RegionOfficeLocation) : SearchIntent
    data class PlaceSuggestionClicked(val place: PlaceSuggestion) : SearchIntent
    data object DeleteAllRecentSearchesClicked : SearchIntent
    data class DeleteRecentSearchClicked(val id: Long) : SearchIntent
}

sealed interface SearchEffect {
    data class ShowSnackbar(val message: String) : SearchEffect
    data class NavigateRegion(
        val region: RegionOfficeLocation,
        val initialPlaces: List<PlaceSummary>,
    ) : SearchEffect
    data class NavigatePlace(val placeId: Long) : SearchEffect
}
