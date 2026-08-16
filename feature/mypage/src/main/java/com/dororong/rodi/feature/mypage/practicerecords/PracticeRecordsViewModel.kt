package com.dororong.rodi.feature.mypage.practicerecords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
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
            loadPageChain(initial = true, cursor = null, existingRecords = emptyList())
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        val cursor = current.nextCursor ?: return
        if (!current.hasNextPage || current.isLoadingMore || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, nextPageError = null) }
            loadPageChain(initial = false, cursor = cursor, existingRecords = current.records)
        }
    }

    private suspend fun loadPageChain(
        initial: Boolean,
        cursor: String?,
        existingRecords: List<PracticeRecord>,
    ) {
        var requestCursor = cursor
        var records = existingRecords
        var pageHasNext = false
        var nextCursor: String? = null
        var totalCount: Long? = null
        var chainRequests = 0

        while (true) {
            val pageResult = getPracticeRecords(cursor = requestCursor, size = PAGE_SIZE)
            chainRequests++
            if (pageResult.isFailure) {
                val errorMessage = pageResult.exceptionOrNull()?.message
                    ?: if (initial) "연습기록을 불러오지 못했어요." else "다음 연습기록을 불러오지 못했어요."
                if (initial) {
                    _uiState.value = PracticeRecordsUiState(
                        isLoading = false,
                        initialError = errorMessage,
                    )
                } else {
                    _uiState.update {
                        it.copy(isLoadingMore = false, nextPageError = errorMessage)
                    }
                }
                return
            }

            val page = pageResult.getOrThrow()
            val visibleItems = page.items.filter { it.status == PracticeStatus.VISITED }
            records = (records + visibleItems).distinctBy(PracticeRecord::practiceId)
            totalCount = page.totalCount ?: totalCount
            nextCursor = page.nextCursor
            pageHasNext = page.hasNext && nextCursor != null && nextCursor != requestCursor

            // getPracticeRecords()는 내부적으로도 최대 페이지 수만큼만 스캔하고 멈춘다
            // (GetPracticeRecordsUseCase.MAX_PAGE_REQUESTS). 방문 기록이 뜨문뜨문 있는
            // 계정에서 그 내부 스캔이 매번 빈 결과 + hasNext=true로 끝나면, 이 바깥 루프가
            // 그걸 모르고 계속 재호출해 무제한으로 요청이 쌓일 수 있다 — 바깥도 자체 한도로 막는다.
            if (visibleItems.isNotEmpty() || !pageHasNext || chainRequests >= MAX_CHAIN_REQUESTS) break
            requestCursor = nextCursor
        }

        if (initial) {
            _uiState.value = PracticeRecordsUiState(
                records = records,
                isLoading = false,
                hasNextPage = pageHasNext,
                nextCursor = nextCursor,
                totalCount = totalCount,
            )
        } else {
            _uiState.update {
                it.copy(
                    records = records,
                    isLoadingMore = false,
                    hasNextPage = pageHasNext,
                    nextCursor = nextCursor,
                    totalCount = totalCount ?: it.totalCount,
                )
            }
        }
    }
}

private const val PAGE_SIZE = 20
private const val MAX_CHAIN_REQUESTS = 5
