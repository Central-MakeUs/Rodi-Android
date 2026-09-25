package com.dororong.rodi.feature.home.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dororong.rodi.core.ui.text.takeGraphemes
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
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewWriteViewModel @Inject constructor(
    private val createReview: CreateReviewUseCase,
    private val updateReview: UpdateReviewUseCase,
    private val getReview: GetReviewUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewWriteUiState())
    val uiState: StateFlow<ReviewWriteUiState> = _uiState.asStateFlow()

    // 시스템이 앱을 종료한 뒤 다시 만든 ViewModel에서만 의미가 있다. 첫 시작 대상이 같을 때 한 번만 쓴다.
    private var restorableDraft: SavedReviewDraft? = SavedReviewDraft.read(savedStateHandle)

    fun start(placeId: Long, placeName: String, review: Review? = null) {
        val restored = takeRestorableDraft(placeId, review?.reviewId)
        val initial = review?.toInitialValues()
        _uiState.value = ReviewWriteUiState(
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
        ).restoredWith(restored)
        saveDraft()
    }

    fun startForReviewId(placeId: Long, placeName: String, reviewId: Long) {
        val restored = takeRestorableDraft(placeId, reviewId)
        _uiState.value = ReviewWriteUiState(
            placeId = placeId,
            placeName = placeName,
            editingReviewId = reviewId,
            isInitializing = true,
        )
        viewModelScope.launch {
            getReview(reviewId)
                .onSuccess { review ->
                    val initial = review.toInitialValues()
                    _uiState.update {
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
                        ).restoredWith(restored)
                    }
                    saveDraft()
                }
                .onFailure { error ->
                    restorableDraft = restored
                    _uiState.update {
                        it.copy(
                            isInitializing = false,
                            initializationErrorMessage = error.userMessage("수정할 후기를 불러오지 못했어요."),
                        )
                    }
                }
        }
    }

    fun selectRecommend(value: Boolean) = edit { it.copy(isRecommended = value) }
    fun selectDifficulty(value: ReviewDifficulty) = edit { it.copy(difficulty = value) }
    fun selectCongestion(value: ReviewCongestion) = edit { it.copy(congestion = value) }
    fun selectPracticeMethod(value: PracticeMethod) = edit { it.copy(practiceMethod = value) }
    fun updateCaution(value: String) = edit { it.copy(caution = value.takeGraphemes(50)) }
    fun updateContent(value: String) = edit { it.copy(content = value.takeGraphemes(150)) }
    fun next() { if (_uiState.value.canGoNext) edit { it.copy(step = ReviewWriteStep.Detail) } }
    fun back() = edit { it.copy(step = ReviewWriteStep.Basics) }

    /** 사용자가 작성을 그만두고 닫았다. 다음에 다시 만들어진 화면이 이 입력을 되살리지 않게 지운다. */
    fun discardDraft() {
        restorableDraft = null
        SavedReviewDraft.clear(savedStateHandle)
    }
    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting || current.isSubmitted || current.isCompletionHandled) return
        val draft = current.draftOrNull() ?: return

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val result = current.editingReviewId?.let { reviewId ->
                updateReview(reviewId, draft).map { reviewId }
            } ?: createReview(current.placeId, draft)
            result.onSuccess { reviewId ->
                discardDraft()
                _uiState.update {
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
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.reviewErrorMessage(),
                    )
                }
            }
        }
    }
    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    fun consumeSubmittedResult(): ReviewSubmissionResult? {
        val current = _uiState.value
        val result = current.submittedResult ?: return null
        _uiState.value = current.copy(
            isSubmitted = false,
            submittedResult = null,
            isCompletionHandled = true,
        )
        return result
    }
    private fun edit(transform: (ReviewWriteUiState) -> ReviewWriteUiState) {
        _uiState.update(transform)
        saveDraft()
    }

    private fun takeRestorableDraft(placeId: Long, reviewId: Long?): SavedReviewDraft? {
        val draft = restorableDraft
        restorableDraft = null
        return draft?.takeIf { it.placeId == placeId && it.reviewId == reviewId }
    }

    private fun saveDraft() {
        val state = _uiState.value
        if (state.placeId == 0L || state.isInitializing || state.isSubmitted || state.isCompletionHandled) return
        if (!state.isDirty) {
            SavedReviewDraft.clear(savedStateHandle)
            return
        }
        SavedReviewDraft.from(state).write(savedStateHandle)
    }

    private fun ReviewWriteUiState.restoredWith(draft: SavedReviewDraft?): ReviewWriteUiState =
        if (draft == null) {
            this
        } else {
            copy(
                step = draft.step,
                isRecommended = draft.isRecommended,
                difficulty = draft.difficulty,
                congestion = draft.congestion,
                caution = draft.caution,
                practiceMethod = draft.practiceMethod,
                content = draft.content,
            )
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
