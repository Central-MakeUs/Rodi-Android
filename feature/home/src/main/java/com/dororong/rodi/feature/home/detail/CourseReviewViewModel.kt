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
    private var loadJob: Job? = null

    fun load(placeId: Long, force: Boolean = false) {
        if (!force && _state.value.placeId == placeId) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = CourseReviewUiState(placeId = placeId, isLoading = true)
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
        }
    }

    fun selectLevel(level: OnboardingLevel) {
        val placeId = _state.value.placeId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, selectedLevel = level, errorMessage = null) }
            val summary = getReviewSummary(placeId, ReviewLevelFilter.Of(level))
            val latest = getPlaceReviews(placeId, ReviewLevelFilter.Of(level), size = 1)
            _state.update {
                it.copy(
                    isLoading = false,
                    difficultyCounts = summary.getOrNull()?.difficultyCounts.orEmpty(),
                    latestReviews = latest.getOrNull()?.items.orEmpty(),
                    errorMessage = summary.exceptionOrNull()?.message ?: latest.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun loadInitialReviews() {
        val placeId = _state.value.placeId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val result = getPlaceReviews(placeId, ReviewLevelFilter.Of(_state.value.selectedLevel), size = PAGE_SIZE)
            result.onSuccess { page -> _state.update { it.copy(reviews = page.items.distinctBy { review -> review.reviewId }, nextCursor = page.nextCursor, hasNext = page.hasNext) } }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun loadNextPage() {
        val current = _state.value
        val placeId = current.placeId ?: return
        val cursor = current.nextCursor ?: return
        if (!current.hasNext || current.isNextPageLoading || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isNextPageLoading = true) }
            getPlaceReviews(placeId, ReviewLevelFilter.Of(current.selectedLevel), cursor, PAGE_SIZE)
                .onSuccess { page -> _state.update { it.copy(reviews = (it.reviews + page.items).distinctBy { review -> review.reviewId }, nextCursor = page.nextCursor, hasNext = page.hasNext, isNextPageLoading = false) } }
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

    fun removeReview(reviewId: Long) {
        _state.update {
            it.copy(
                latestReviews = it.latestReviews.filterNot { review -> review.reviewId == reviewId },
                reviews = it.reviews.filterNot { review -> review.reviewId == reviewId },
            )
        }
    }

    fun refresh() {
        _state.value.placeId?.let { load(it, force = true) }
    }

    companion object { private const val PAGE_SIZE = 10 }
}
