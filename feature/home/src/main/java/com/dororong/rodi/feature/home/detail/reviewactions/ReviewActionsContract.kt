package com.dororong.rodi.feature.home.detail.reviewactions

import com.dororong.rodi.core.domain.model.review.ReportForm

data class ReviewActionsUiState(
    val reportReviewId: Long? = null,
    val reportForm: ReportForm? = null,
    val selectedOptionCode: String? = null,
    val reportDetail: String = "",
    val isReportFormLoading: Boolean = false,
    val isReportSubmitting: Boolean = false,
    val isReportSubmitted: Boolean = false,
    val reportErrorMessage: String? = null,
    val isBlocking: Boolean = false,
    val isDeleting: Boolean = false,
)

sealed interface ReviewActionsEffect {
    data class Blocked(val memberId: Long) : ReviewActionsEffect
    data class BlockFailed(val message: String) : ReviewActionsEffect
    data class Deleted(val reviewId: Long) : ReviewActionsEffect
    data class DeleteFailed(val message: String) : ReviewActionsEffect
}
