package com.dororong.rodi.feature.mypage.myposts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.member.MyReview
import com.dororong.rodi.core.domain.usecase.member.GetMyReviewsUseCase
import com.dororong.rodi.core.domain.usecase.review.DeleteReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

@HiltViewModel
class MyPostsViewModel @Inject constructor(
    private val deleteReview: DeleteReviewUseCase,
    private val getMyReviews: GetMyReviewsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPostsUiState())
    val uiState: StateFlow<MyPostsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { loadInitial() }

    fun loadInitial() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MyPostsUiState(isLoading = true)
            getMyReviews(cursor = null, size = PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.value = MyPostsUiState(
                        posts = page.items.map(MyReview::toMyPost),
                        isLoading = false,
                        nextCursor = page.nextCursor,
                        hasNext = page.hasNext,
                    )
                }
                .onFailure { error ->
                    _uiState.value = MyPostsUiState(isLoading = false, errorMessage = error.message ?: "후기를 불러오지 못했어요.")
                }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        val cursor = current.nextCursor ?: return
        if (!current.hasNext || current.isLoadingMore || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            getMyReviews(cursor = cursor, size = PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.update { latest ->
                        latest.copy(
                            posts = (latest.posts + page.items.map(MyReview::toMyPost)).distinctBy { it.review.reviewId },
                            nextCursor = page.nextCursor,
                            hasNext = page.hasNext,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(isLoadingMore = false, errorMessage = error.message ?: "다음 후기를 불러오지 못했어요.") } }
        }
    }

    fun replacePosts(posts: List<MyPost>) {
        loadJob?.cancel()
        _uiState.value = MyPostsUiState(posts = posts.distinctBy { it.review.reviewId })
    }

    fun delete(post: MyPost) {
        viewModelScope.launch {
            deleteReview(post.review.reviewId)
                .onSuccess {
                    _uiState.update { state -> state.copy(posts = state.posts.filterNot { it.review.reviewId == post.review.reviewId }) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "후기를 삭제하지 못했어요.") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

private fun MyReview.toMyPost() = MyPost(
    placeId = placeId,
    placeName = placeName,
    review = com.dororong.rodi.core.domain.model.review.Review(
        reviewId = reviewId,
        memberId = 0L,
        nickname = "",
        memberLevel = com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel.SEED,
        isRecommended = true,
        difficulty = com.dororong.rodi.core.domain.model.review.ReviewDifficulty.NORMAL,
        congestion = com.dororong.rodi.core.domain.model.review.ReviewCongestion.NORMAL,
        practiceMethod = com.dororong.rodi.core.domain.model.review.PracticeMethod.SOLO,
        content = content,
        caution = null,
        isMine = true,
        isEditable = isEditable,
        isHidden = isHidden,
        createdAt = createdAt,
    ),
)

private const val PAGE_SIZE = 20
