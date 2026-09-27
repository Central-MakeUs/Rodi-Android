package com.dororong.rodi.feature.mypage.savedcourses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.userMessage
import com.dororong.rodi.core.domain.model.place.CursorPage
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
            loadVisiblePage(cursor = null)
                .onSuccess { (page, hiddenCount) ->
                    _uiState.value = SavedCoursesUiState(
                        places = page.items.distinctBy { it.type to it.id },
                        totalCount = page.totalCount?.minus(hiddenCount)?.coerceAtLeast(0),
                        nextCursor = page.nextCursor,
                        hasNext = page.hasNext,
                        isLoading = false,
                    )
                }
                .onFailure { error ->
                    _uiState.value = SavedCoursesUiState(
                        isLoading = false,
                        initialError = error.userMessage("저장목록을 불러오지 못했어요."),
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
            loadVisiblePage(cursor = cursor)
                .onSuccess { (page, hiddenCount) ->
                    _uiState.update { latest ->
                        latest.copy(
                            places = (latest.places + page.items).distinctBy { it.type to it.id },
                            totalCount = latest.totalCount?.minus(hiddenCount)?.coerceAtLeast(0),
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
                            nextPageError = error.userMessage("다음 장소를 불러오지 못했어요."),
                        )
                    }
                }
        }
    }

    fun retry() {
        if (_uiState.value.places.isEmpty()) loadInitial() else loadNextPage()
    }

    // 저장 목록은 등록자가 삭제한 코스를 보여주지 않는다. 한 페이지가 전부 삭제된 코스면 빈 화면이 되지
    // 않게 다음 페이지를 이어서 받고, 숨긴 개수는 서버 총개수에서 뺀다.
    private suspend fun loadVisiblePage(cursor: String?): Result<Pair<CursorPage<PlaceSummary>, Int>> {
        var nextCursor = cursor
        var hiddenCount = 0
        var totalCount: Long? = null
        while (true) {
            val page = getSavedPlaces(cursor = nextCursor, size = PAGE_SIZE).getOrElse { return Result.failure(it) }
            totalCount = totalCount ?: page.totalCount
            val visible = page.items.filterNot(PlaceSummary::isDeleted)
            hiddenCount += page.items.size - visible.size
            val next = page.nextCursor
            if (visible.isNotEmpty() || !page.hasNext || next == null) {
                return Result.success(page.copy(items = visible, totalCount = totalCount) to hiddenCount)
            }
            nextCursor = next
        }
    }
}

private const val PAGE_SIZE = 20
