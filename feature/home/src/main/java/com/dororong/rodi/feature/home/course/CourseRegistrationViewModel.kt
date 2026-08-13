package com.dororong.rodi.feature.home.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.userMessage
import com.dororong.rodi.core.domain.model.place.PlaceException
import com.dororong.rodi.core.domain.model.search.PlaceSuggestion
import com.dororong.rodi.core.domain.usecase.place.GetRelatedSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_PAGE_SIZE = 20
private const val SEARCH_DEBOUNCE_MILLIS = 300L

enum class CourseRegistrationSearchResultState {
    Idle,
    Loading,
    Content,
    Empty,
}

data class CourseRegistrationUiState(
    val query: String = "",
    val resultState: CourseRegistrationSearchResultState = CourseRegistrationSearchResultState.Idle,
    val regionSuggestions: List<String> = emptyList(),
    val placeSuggestions: List<PlaceSuggestion> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface CourseRegistrationIntent {
    data class OnQueryChange(val query: String) : CourseRegistrationIntent
    data object OnImeSearch : CourseRegistrationIntent
}

@HiltViewModel
class CourseRegistrationViewModel @Inject constructor(
    private val getRelatedSearchUseCase: GetRelatedSearchUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(CourseRegistrationUiState())
    val state: StateFlow<CourseRegistrationUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var searchGeneration = 0L

    fun onIntent(intent: CourseRegistrationIntent) {
        when (intent) {
            is CourseRegistrationIntent.OnQueryChange -> onQueryChange(intent.query)
            CourseRegistrationIntent.OnImeSearch -> searchImmediately()
        }
    }

    private fun onQueryChange(query: String) {
        val limitedQuery = query.take(50)
        searchJob?.cancel()
        searchGeneration += 1
        val normalizedQuery = limitedQuery.trim()
        _state.update {
            it.copy(
                query = limitedQuery,
                resultState = if (normalizedQuery.isBlank()) {
                    CourseRegistrationSearchResultState.Idle
                } else {
                    CourseRegistrationSearchResultState.Loading
                },
                regionSuggestions = emptyList(),
                placeSuggestions = emptyList(),
                errorMessage = null,
            )
        }
        if (normalizedQuery.isBlank()) return

        val generation = searchGeneration
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            search(normalizedQuery, generation)
        }
    }

    private fun searchImmediately() {
        val normalizedQuery = _state.value.query.trim()
        if (normalizedQuery.isBlank()) return
        searchJob?.cancel()
        searchGeneration += 1
        val generation = searchGeneration
        _state.update {
            it.copy(
                resultState = CourseRegistrationSearchResultState.Loading,
                regionSuggestions = emptyList(),
                placeSuggestions = emptyList(),
                errorMessage = null,
            )
        }
        searchJob = viewModelScope.launch { search(normalizedQuery, generation) }
    }

    private suspend fun search(keyword: String, generation: Long) {
        getRelatedSearchUseCase(keyword, cursor = null, size = SEARCH_PAGE_SIZE)
            .onSuccess { relatedSearch ->
                if (generation != searchGeneration) return@onSuccess
                val regions = relatedSearch.regions.distinct()
                val places = relatedSearch.places.items.distinctBy(PlaceSuggestion::placeId)
                _state.update {
                    it.copy(
                        resultState = if (regions.isEmpty() && places.isEmpty()) {
                            CourseRegistrationSearchResultState.Empty
                        } else {
                            CourseRegistrationSearchResultState.Content
                        },
                        regionSuggestions = regions,
                        placeSuggestions = places,
                    )
                }
            }
            .onFailure { error ->
                if (generation != searchGeneration) return@onFailure
                _state.update {
                    it.copy(
                        resultState = CourseRegistrationSearchResultState.Idle,
                        errorMessage = when (error) {
                            is PlaceException.AuthenticationRequired -> "로그인 후 장소 검색을 사용할 수 있어요."
                            else -> error.userMessage()
                        },
                    )
                }
            }
    }
}
