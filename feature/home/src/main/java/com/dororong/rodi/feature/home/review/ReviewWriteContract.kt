package com.dororong.rodi.feature.home.review

import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.domain.model.review.ReviewSubmissionResult
import com.dororong.rodi.core.ui.text.graphemeLength

enum class ReviewWriteStep { Basics, Detail }

data class ReviewWriteInitialValues(
    val isRecommended: Boolean?,
    val difficulty: ReviewDifficulty?,
    val congestion: ReviewCongestion?,
    val caution: String?,
    val practiceMethod: PracticeMethod?,
    val content: String?,
    val courseIssues: List<String> = emptyList(),
    val courseIssueDetail: String? = null,
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
    val issueForm: ReviewIssueForm? = null,
    val courseIssues: List<String> = emptyList(),
    val courseIssueDetail: String = "",
    val isInitializing: Boolean = false,
    val initializationErrorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val submittedResult: ReviewSubmissionResult? = null,
    val isCompletionHandled: Boolean = false,
    val errorMessage: String? = null,
) {
    val canGoNext: Boolean
        get() {
            if (isInitializing || initializationErrorMessage != null) return false
            return when (isRecommended) {
                null -> false
                false -> if (issueForm == null) {
                    difficulty != null && congestion != null
                } else {
                    val options = issueForm.options.filter { it.code in courseIssues }
                    val textOption = options.firstOrNull { it.requiresTextInput }
                    options.isNotEmpty() && options.size == courseIssues.distinct().size &&
                        (textOption == null || (courseIssueDetail.isNotBlank() &&
                            courseIssueDetail.graphemeLength() <= (textOption.textInputMaxLength ?: 0)))
                }
                true -> difficulty != null && congestion != null
            }
        }
    val canSubmit get() = initializationErrorMessage == null &&
        canGoNext &&
        practiceMethod != null &&
        !isSubmitting &&
        !isSubmitted &&
        !isCompletionHandled
    val isDirty: Boolean
        get() {
            val initial = original
            if (initial == null) {
                return isRecommended != null ||
                    difficulty != null ||
                    congestion != null ||
                    caution.isNotBlank() ||
                    practiceMethod != null ||
                    content.isNotBlank() ||
                    courseIssues.isNotEmpty() ||
                    courseIssueDetail.isNotBlank()
            }

            return isRecommended != initial.isRecommended ||
                difficulty != initial.difficulty ||
                congestion != initial.congestion ||
                caution != initial.caution.orEmpty() ||
                practiceMethod != initial.practiceMethod ||
                content != initial.content.orEmpty() ||
                courseIssues != initial.courseIssues ||
                courseIssueDetail != initial.courseIssueDetail.orEmpty()
        }
    fun draftOrNull(): ReviewDraft? {
        val recommended = isRecommended ?: return null
        if (!canGoNext) return null
        val selectedMethod = practiceMethod ?: return null
        val reportingIssues = !recommended && issueForm != null
        return ReviewDraft(
            isRecommended = recommended,
            difficulty = difficulty.takeUnless { reportingIssues },
            congestion = congestion.takeUnless { reportingIssues },
            practiceMethod = selectedMethod,
            content = content.takeIf(String::isNotBlank),
            caution = caution.takeIf { !reportingIssues && it.isNotBlank() },
            courseIssues = courseIssues.takeIf { reportingIssues },
            courseIssueDetail = courseIssueDetail.takeIf {
                reportingIssues && issueForm?.options?.any { option -> option.requiresTextInput && option.code in courseIssues } == true
            },
        )
    }
}
