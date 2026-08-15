package com.dororong.rodi.feature.home.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.common.takeGraphemes
import com.dororong.rodi.core.common.userMessage
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDetail
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewException
import com.dororong.rodi.core.domain.model.review.ReviewSubmissionResult
import com.dororong.rodi.core.domain.usecase.review.CreateReviewUseCase
import com.dororong.rodi.core.domain.usecase.review.GetReviewUseCase
import com.dororong.rodi.core.domain.usecase.review.UpdateReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewWriteViewModel @Inject constructor(
    private val createReview: CreateReviewUseCase,
    private val updateReview: UpdateReviewUseCase,
    private val getReview: GetReviewUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ReviewWriteUiState())
    val state: StateFlow<ReviewWriteUiState> = _state.asStateFlow()

    fun start(placeId: Long, placeName: String, review: Review? = null) {
        val initial = review?.toInitialValues()
        _state.value = ReviewWriteUiState(
            placeId = placeId,
            placeName = placeName,
            editingReviewId = review?.reviewId,
            original = initial,
            isRecommended = initial?.isRecommended,
            difficulty = initial?.difficulty,
            congestion = initial?.congestion,
            caution = initial?.caution.orEmpty(),
            practiceMethod = initial?.practiceMethod,
            content = initial?.content.orEmpty(),
        )
    }

    fun startForReviewId(placeId: Long, placeName: String, reviewId: Long) {
        _state.value = ReviewWriteUiState(
            placeId = placeId,
            placeName = placeName,
            editingReviewId = reviewId,
            isInitializing = true,
        )
        viewModelScope.launch {
            try {
                getReview(reviewId)
                    .onSuccess { review ->
                        val initial = review.toInitialValues()
                        _state.update {
                            it.copy(
                                original = initial,
                                isRecommended = initial.isRecommended,
                                difficulty = initial.difficulty,
                                congestion = initial.congestion,
                                caution = initial.caution.orEmpty(),
                                practiceMethod = initial.practiceMethod,
                                content = initial.content.orEmpty(),
                                isInitializing = false,
                                initializationErrorMessage = null,
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                isInitializing = false,
                                initializationErrorMessage = error.message ?: "수정할 후기를 불러오지 못했어요.",
                            )
                        }
                    }
            } catch (error: CancellationException) {
                _state.update {
                    it.copy(
                        isInitializing = false,
                        initializationErrorMessage = error.message ?: "수정할 후기를 불러오지 못했어요.",
                    )
                }
                throw error
            }
        }
    }

    fun selectRecommend(value: Boolean) = _state.update { it.copy(isRecommended = value) }
    fun selectDifficulty(value: ReviewDifficulty) = _state.update { it.copy(difficulty = value) }
    fun selectCongestion(value: ReviewCongestion) = _state.update { it.copy(congestion = value) }
    fun selectPracticeMethod(value: PracticeMethod) = _state.update { it.copy(practiceMethod = value) }
    fun updateCaution(value: String) = _state.update { it.copy(caution = value.takeGraphemes(50)) }
    fun updateContent(value: String) = _state.update { it.copy(content = value.takeGraphemes(150)) }
    fun next() { if (_state.value.canGoNext) _state.update { it.copy(step = ReviewWriteStep.Detail) } }
    fun back() = _state.update { it.copy(step = ReviewWriteStep.Basics) }
    fun submit() {
        val current = _state.value
        if (current.isSubmitting || current.isSubmitted || current.isCompletionHandled) return
        val draft = current.draftOrNull() ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = current.editingReviewId?.let { reviewId ->
                updateReview(reviewId, draft).map { reviewId }
            } ?: createReview(current.placeId, draft)
            result.onSuccess { reviewId ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        isSubmitted = true,
                        submittedResult = ReviewSubmissionResult(
                            placeId = current.placeId,
                            reviewId = reviewId,
                            draft = draft,
                            isEditing = current.editingReviewId != null,
                        ),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.reviewErrorMessage(),
                    )
                }
            }
        }
    }
    fun consumeError() = _state.update { it.copy(errorMessage = null) }

    fun consumeSubmittedResult(): ReviewSubmissionResult? {
        val current = _state.value
        val result = current.submittedResult ?: return null
        _state.value = current.copy(
            isSubmitted = false,
            submittedResult = null,
            isCompletionHandled = true,
        )
        return result
    }
    private fun Review.toInitialValues() = ReviewWriteInitialValues(
        isRecommended = isRecommended,
        difficulty = difficulty,
        congestion = congestion,
        caution = caution,
        practiceMethod = practiceMethod,
        content = content,
    )

    private fun ReviewDetail.toInitialValues() = ReviewWriteInitialValues(
        isRecommended = isRecommended,
        difficulty = difficulty,
        congestion = congestion,
        caution = caution,
        practiceMethod = practiceMethod,
        content = content,
    )

    private fun Throwable.reviewErrorMessage(): String = when (this) {
        is ReviewException.LevelRequired -> "레벨 진단을 마쳐야 후기를 남길 수 있어요."
        is ReviewException.LevelChanged -> "레벨이 바뀌어서 이 후기는 수정할 수 없어요."
        else -> userMessage()
    }
}
