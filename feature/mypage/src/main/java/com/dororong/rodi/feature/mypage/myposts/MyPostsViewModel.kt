package com.dororong.rodi.feature.mypage.myposts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.domain.model.member.MyReview
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.usecase.member.GetMyReviewsUseCase
import com.dororong.rodi.core.domain.usecase.member.HasPracticeRecordsUseCase
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
    private val hasPracticeRecordsUseCase: HasPracticeRecordsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPostsUiState())
    val uiState: StateFlow<MyPostsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private val loadedCursors = mutableSetOf<String?>()

    init { loadInitial() }

    fun loadInitial() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadedCursors.clear()
            _uiState.value = MyPostsUiState(isLoading = true)
            val reviewResult = getMyReviews(cursor = null, size = PAGE_SIZE)
            var cursor: String? = null
            var page: CursorPage<MyReview>? = null
            val posts = mutableListOf<MyPost>()
            val visitedCursors = mutableSetOf<String?>()
            while (true) {
                if (!visitedCursors.add(cursor)) break
                val result = if (cursor == null) reviewResult else getMyReviews(cursor, PAGE_SIZE)
                val loadedPage = result.getOrNull()
                if (loadedPage == null) {
                    _uiState.value = MyPostsUiState(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "후기를 불러오지 못했어요.",
                    )
                    return@launch
                }
                page = loadedPage
                posts += loadedPage.items.map(MyReview::toMyPost)
                val nextCursor = loadedPage.nextCursor
                val hasNext = loadedPage.hasNext &&
                    nextCursor != null &&
                    nextCursor != cursor &&
                    nextCursor !in visitedCursors
                if (posts.isNotEmpty() || !hasNext) break
                cursor = nextCursor
            }
            val loadedPage = requireNotNull(page)
            loadedCursors += visitedCursors
            _uiState.value = MyPostsUiState(
                posts = posts.distinctBy { it.review.reviewId },
                hasPracticeRecords = posts.isEmpty() && loadPracticeRecordPresence(),
                isLoading = false,
                nextCursor = loadedPage.nextCursor,
                hasNext = loadedPage.hasNext &&
                    loadedPage.nextCursor != null &&
                    loadedPage.nextCursor != cursor &&
                    loadedPage.nextCursor !in visitedCursors,
            )
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        val cursor = current.nextCursor ?: return
        if (!current.hasNext || current.isLoadingMore || loadJob?.isActive == true) return
        if (!loadedCursors.add(cursor)) {
            _uiState.update { it.copy(hasNext = false, nextCursor = null) }
            return
        }
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            getMyReviews(cursor = cursor, size = PAGE_SIZE)
                .onSuccess { page ->
                    val nextCursor = page.nextCursor
                    _uiState.update { latest ->
                        latest.copy(
                            posts = (latest.posts + page.items.map(MyReview::toMyPost)).distinctBy { it.review.reviewId },
                            nextCursor = nextCursor,
                            hasNext = page.hasNext &&
                                nextCursor != null &&
                                nextCursor != cursor &&
                                nextCursor !in loadedCursors,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure { error ->
                    loadedCursors.remove(cursor)
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            errorMessage = error.message ?: "다음 후기를 불러오지 못했어요.",
                        )
                    }
                }
        }
    }

    fun replacePosts(posts: List<MyPost>) {
        loadJob?.cancel()
        _uiState.update { it.copy(posts = posts.distinctBy { post -> post.review.reviewId }) }
    }

    fun delete(post: MyPost) {
        viewModelScope.launch {
            val deleteResult = deleteReview(post.review.reviewId)
            if (deleteResult.isSuccess) {
                val remainingPosts = _uiState.value.posts.filterNot { it.review.reviewId == post.review.reviewId }
                val shouldLoadNextPage = remainingPosts.isEmpty() && _uiState.value.hasNext
                _uiState.update { state -> state.copy(posts = remainingPosts) }
                if (remainingPosts.isEmpty()) {
                    if (shouldLoadNextPage) {
                        // 방금 보던 페이지가 통째로 비었을 뿐 아직 더 불러올 페이지가 있으므로,
                        // loadInitial()로 처음부터 다시 부르면 사용자가 있던 위치가 사라진다.
                        loadNextPage()
                    } else {
                        val hasPracticeRecords = loadPracticeRecordPresence()
                        _uiState.update { state -> state.copy(hasPracticeRecords = hasPracticeRecords) }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = deleteResult.exceptionOrNull()?.message ?: "후기를 삭제하지 못했어요.")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun loadPracticeRecordPresence(): Boolean =
        hasPracticeRecordsUseCase().getOrNull() == true
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
        isVerifiedVisit = isVerifiedVisit,
    ),
)

private const val PAGE_SIZE = 20
