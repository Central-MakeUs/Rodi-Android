package com.dororong.rodi.feature.mypage.savedcourses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.usecase.place.GetSavedPlacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedCoursesUiState(
    val places: List<PlaceSummary> = emptyList(),
    val totalCount: Long? = null,
    val nextCursor: String? = null,
    val hasNext: Boolean = false,
    val isLoading: Boolean = true,
    val isNextPageLoading: Boolean = false,
    val initialError: String? = null,
    val nextPageError: String? = null,
)

@HiltViewModel
class SavedCoursesViewModel @Inject constructor(
    private val getSavedPlaces: GetSavedPlacesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavedCoursesUiState())
    val uiState: StateFlow<SavedCoursesUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadInitial()
    }

    fun loadInitial() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = SavedCoursesUiState(isLoading = true)
            getSavedPlaces(cursor = null, size = PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.value = SavedCoursesUiState(
                        places = page.items.distinctBy { it.type to it.id },
                        totalCount = page.totalCount,
                        nextCursor = page.nextCursor,
                        hasNext = page.hasNext,
                        isLoading = false,
                    )
                }
                .onFailure { error ->
                    _uiState.value = SavedCoursesUiState(
                        isLoading = false,
                        initialError = error.message ?: "저장한 장소를 불러오지 못했어요.",
                    )
                }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        val cursor = current.nextCursor ?: return
        if (!current.hasNext || current.isNextPageLoading || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isNextPageLoading = true, nextPageError = null) }
            getSavedPlaces(cursor = cursor, size = PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.update { latest ->
                        latest.copy(
                            places = (latest.places + page.items).distinctBy { it.type to it.id },
                            nextCursor = page.nextCursor,
                            hasNext = page.hasNext,
                            isNextPageLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isNextPageLoading = false,
                            nextPageError = error.message ?: "다음 장소를 불러오지 못했어요.",
                        )
                    }
                }
        }
    }

    fun retry() {
        if (_uiState.value.places.isEmpty()) loadInitial() else loadNextPage()
    }
}

private const val PAGE_SIZE = 20
