package com.dororong.rodi.feature.home.detail.coursereport

import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.ui.text.graphemeLength

data class CourseReportUiState(
    val courseId: Long? = null,
    val form: ReviewIssueForm? = null,
    val selected: List<String> = emptyList(),
    val detail: String = "",
    val isLoading: Boolean = false,
    val loadErrorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null,
) {
    private val textOption get() = form?.options?.firstOrNull { it.requiresTextInput && it.code in selected }

    val canSubmit: Boolean
        get() {
            val form = form ?: return false
            if (isLoading || isSubmitting || isSubmitted || selected.isEmpty()) return false
            if (selected.any { code -> form.options.none { it.code == code } }) return false
            val option = textOption ?: return true
            return detail.isNotBlank() && detail.graphemeLength() <= (option.textInputMaxLength ?: 0)
        }

    val submittedDetail: String? get() = detail.takeIf { textOption != null }
}
