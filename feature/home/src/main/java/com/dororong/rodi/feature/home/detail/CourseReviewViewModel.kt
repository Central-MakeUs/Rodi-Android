package com.dororong.rodi.feature.home.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.review.ReviewLevelFilter
import com.dororong.rodi.core.domain.usecase.auth.GetAuthSessionUseCase
import com.dororong.rodi.core.domain.usecase.review.GetPlaceReviewsUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReviewSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CourseReviewViewModel @Inject constructor(
    private val getReviewSummary: GetReviewSummaryUseCase,
    private val getPlaceReviews: GetPlaceReviewsUseCase,
    private val getAuthSession: GetAuthSessionUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(CourseReviewUiState())
    val state: StateFlow<CourseReviewUiState> = _state.asStateFlow()
    private var summaryJob: Job? = null
    private var initialReviewsJob: Job? = null
    private var nextPageJob: Job? = null

    fun load(placeId: Long) {
        val current = _state.value
        if (current.placeId == placeId && !current.isLoading && current.errorMessage == null) return

        summaryJob?.cancel()
        cancelReviewPageLoads()
        summaryJob = viewModelScope.launch {
            _state.value = CourseReviewUiState(placeId = placeId, isLoading = true)
            try {
                if (!getAuthSession().isLoggedIn) {
                    _state.value = CourseReviewUiState(placeId = placeId, isGuest = true)
                    return@launch
                }
                val all = async { getReviewSummary(placeId, ReviewLevelFilter.All) }
                val mine = async { getReviewSummary(placeId, ReviewLevelFilter.Mine) }
                val mineSummary = mine.await()
                val selectedLevel = mineSummary.getOrNull()?.level ?: OnboardingLevel.SEED
                val allSummary = all.await()
                val latest = getPlaceReviews(placeId, ReviewLevelFilter.Mine, size = 1)
                val error = allSummary.exceptionOrNull() ?: mineSummary.exceptionOrNull() ?: latest.exceptionOrNull()
                _state.value = CourseReviewUiState(
                    placeId = placeId,
                    selectedLevel = selectedLevel,
                    totalCount = allSummary.getOrNull()?.totalCount ?: 0,
                    recommendCount = allSummary.getOrNull()?.recommendCount ?: 0,
                    difficultyCounts = mineSummary.getOrNull()?.difficultyCounts.orEmpty(),
                    latestReviews = latest.getOrNull()?.items.orEmpty(),
                    errorMessage = error?.message,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (_state.value.placeId == placeId) {
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            }
        }
    }

    fun selectLevel(level: OnboardingLevel) {
        selectLevel(level, loadReviews = false)
    }

    fun selectLevelAndLoadReviews(level: OnboardingLevel) {
        selectLevel(level, loadReviews = true)
    }

    private fun selectLevel(level: OnboardingLevel, loadReviews: Boolean) {
        val placeId = _state.value.placeId ?: return
        summaryJob?.cancel()
        cancelReviewPageLoads()
        _state.update {
            it.copy(
                isLoading = true,
                selectedLevel = level,
                difficultyCounts = emptyMap(),
                latestReviews = emptyList(),
                reviews = emptyList(),
                nextCursor = null,
                hasNext = false,
                isNextPageLoading = false,
                errorMessage = null,
            )
        }
        summaryJob = viewModelScope.launch {
            val summary = getReviewSummary(placeId, ReviewLevelFilter.Of(level))
            val latest = getPlaceReviews(placeId, ReviewLevelFilter.Of(level), size = 1)
            if (_state.value.placeId != placeId || _state.value.selectedLevel != level) return@launch
            _state.update {
                it.copy(
                    isLoading = false,
                    difficultyCounts = summary.getOrNull()?.difficultyCounts.orEmpty(),
                    latestReviews = latest.getOrNull()?.items.orEmpty(),
                    errorMessage = summary.exceptionOrNull()?.message ?: latest.exceptionOrNull()?.message,
                )
            }
        }
        if (loadReviews) loadInitialReviews(placeId, level)
    }

    fun loadInitialReviews() {
        val placeId = _state.value.placeId ?: return
        loadInitialReviews(placeId, _state.value.selectedLevel)
    }

    private fun loadInitialReviews(placeId: Long, level: OnboardingLevel) {
        initialReviewsJob?.cancel()
        nextPageJob?.cancel()
        _state.update { it.copy(isNextPageLoading = false) }
        initialReviewsJob = viewModelScope.launch {
            val result = getPlaceReviews(placeId, ReviewLevelFilter.Of(level), size = PAGE_SIZE)
            if (_state.value.placeId != placeId || _state.value.selectedLevel != level) return@launch
            result
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            reviews = page.items.distinctBy { review -> review.reviewId },
                            nextCursor = page.nextCursor,
                            hasNext = page.hasNext,
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun loadNextPage() {
        val current = _state.value
        val placeId = current.placeId ?: return
        val cursor = current.nextCursor ?: return
        if (!current.hasNext || current.isNextPageLoading || initialReviewsJob?.isActive == true || nextPageJob?.isActive == true) return
        nextPageJob = viewModelScope.launch {
            _state.update { it.copy(isNextPageLoading = true) }
            getPlaceReviews(placeId, ReviewLevelFilter.Of(current.selectedLevel), cursor, PAGE_SIZE)
                .onSuccess { page ->
                    if (_state.value.placeId == placeId && _state.value.selectedLevel == current.selectedLevel) {
                        _state.update {
                            it.copy(
                                reviews = (it.reviews + page.items).distinctBy { review -> review.reviewId },
                                nextCursor = page.nextCursor,
                                hasNext = page.hasNext,
                                isNextPageLoading = false,
                            )
                        }
                    }
                }
                .onFailure { error -> _state.update { it.copy(isNextPageLoading = false, errorMessage = error.message) } }
        }
    }

    fun excludeMemberReviews(memberId: Long) {
        _state.update {
            it.copy(
                latestReviews = it.latestReviews.filterNot { review -> review.memberId == memberId },
                reviews = it.reviews.filterNot { review -> review.memberId == memberId },
            )
        }
    }

    private fun cancelReviewPageLoads() {
        initialReviewsJob?.cancel()
        nextPageJob?.cancel()
    }

    companion object { private const val PAGE_SIZE = 10 }
}
