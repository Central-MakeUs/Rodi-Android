package com.dororong.rodi.feature.home.review

import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty

enum class ReviewWriteStep { Basics, Detail }

data class ReviewWriteInitialValues(
    val isRecommended: Boolean?,
    val difficulty: ReviewDifficulty?,
    val congestion: ReviewCongestion?,
    val caution: String?,
    val practiceMethod: PracticeMethod?,
    val content: String?,
)

data class ReviewWriteUiState(
    val placeId: Long = 0L,
    val placeName: String = "",
    val editingReviewId: Long? = null,
    val original: ReviewWriteInitialValues? = null,
    val step: ReviewWriteStep = ReviewWriteStep.Basics,
    val isRecommended: Boolean? = null,
    val difficulty: ReviewDifficulty? = null,
    val congestion: ReviewCongestion? = null,
    val caution: String = "",
    val practiceMethod: PracticeMethod? = null,
    val content: String = "",
    val isInitializing: Boolean = false,
    val initializationErrorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
) {
    val canGoNext get() = isRecommended != null && difficulty != null && congestion != null
    val canSubmit get() = initializationErrorMessage == null && canGoNext && practiceMethod != null && !isSubmitting
    val isDirty: Boolean
        get() {
            val initial = original
            if (initial == null) {
                return isRecommended != null ||
                    difficulty != null ||
                    congestion != null ||
                    caution.isNotBlank() ||
                    practiceMethod != null ||
                    content.isNotBlank()
            }

            return isRecommended != initial.isRecommended ||
                difficulty != initial.difficulty ||
                congestion != initial.congestion ||
                caution != initial.caution.orEmpty() ||
                practiceMethod != initial.practiceMethod ||
                content != initial.content.orEmpty()
        }
    fun draftOrNull(): com.dororong.rodi.core.domain.model.review.ReviewDraft? {
        val recommended = isRecommended ?: return null
        val selectedDifficulty = difficulty ?: return null
        val selectedCongestion = congestion ?: return null
        val selectedMethod = practiceMethod ?: return null
        return com.dororong.rodi.core.domain.model.review.ReviewDraft(
            isRecommended = recommended,
            difficulty = selectedDifficulty,
            congestion = selectedCongestion,
            practiceMethod = selectedMethod,
            content = content.takeIf(String::isNotBlank),
            caution = caution.takeIf(String::isNotBlank),
        )
    }
}
