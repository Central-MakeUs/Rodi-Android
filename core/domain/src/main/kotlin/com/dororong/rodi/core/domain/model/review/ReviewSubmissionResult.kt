package com.dororong.rodi.core.domain.model.review

data class ReviewSubmissionResult(
    val placeId: Long,
    val reviewId: Long,
    val draft: ReviewDraft,
    val isEditing: Boolean,
)
