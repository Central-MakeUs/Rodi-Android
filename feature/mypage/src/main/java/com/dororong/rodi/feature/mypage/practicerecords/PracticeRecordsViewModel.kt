package com.dororong.rodi.feature.mypage.practicerecords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.usecase.member.GetPracticeRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PracticeRecordsUiState(
    val records: List<PracticeRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val initialError: String? = null,
    val nextPageError: String? = null,
    val hasNextPage: Boolean = false,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@HiltViewModel
class PracticeRecordsViewModel @Inject constructor(
    private val getPracticeRecords: GetPracticeRecordsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PracticeRecordsUiState())
    val uiState: StateFlow<PracticeRecordsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadInitial()
    }

    fun refresh() {
        loadInitial()
    }

    fun loadInitial() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = PracticeRecordsUiState(isLoading = true)
            getPracticeRecords(cursor = null, size = PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.value = PracticeRecordsUiState(
                        records = page.items,
                        isLoading = false,
                        hasNextPage = page.hasNext,
                        nextCursor = page.nextCursor,
                        totalCount = page.totalCount,
                    )
                }
                .onFailure { error ->
                    _uiState.value = PracticeRecordsUiState(
                        isLoading = false,
                        initialError = error.message ?: "연습기록을 불러오지 못했어요.",
                    )
                }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        val cursor = current.nextCursor ?: return
        if (!current.hasNextPage || current.isLoadingMore || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, nextPageError = null) }
            getPracticeRecords(cursor = cursor, size = PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.update { latest ->
                        latest.copy(
                            records = (latest.records + page.items).distinctBy(PracticeRecord::practiceId),
                            isLoadingMore = false,
                            hasNextPage = page.hasNext,
                            nextCursor = page.nextCursor,
                            totalCount = page.totalCount ?: latest.totalCount,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            nextPageError = error.message ?: "다음 연습기록을 불러오지 못했어요.",
                        )
                    }
                }
        }
    }

    private fun setInitialLoading() {
        _uiState.update { it.copy(isLoading = true, initialError = null) }
    }

    private fun setInitialError(message: String) {
        _uiState.update { it.copy(isLoading = false, initialError = message) }
    }

    private fun setNextPageError(message: String) {
        _uiState.update { it.copy(isLoadingMore = false, nextPageError = message) }
    }

    private fun appendPage(page: List<PracticeRecord>, hasNextPage: Boolean, initial: Boolean) {
        if (initial) loadJob?.cancel()
        _uiState.update { current ->
            val records = if (initial) page.distinctBy(PracticeRecord::practiceId)
            else (current.records + page).distinctBy(PracticeRecord::practiceId)
            current.copy(
                records = records,
                isLoading = false,
                isLoadingMore = false,
                initialError = null,
                nextPageError = null,
                hasNextPage = hasNextPage,
            )
        }
    }
}

private const val PAGE_SIZE = 20
