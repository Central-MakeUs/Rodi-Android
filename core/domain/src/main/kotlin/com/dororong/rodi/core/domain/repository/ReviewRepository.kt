package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.review.ReportForm
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewLevelFilter
import com.dororong.rodi.core.domain.model.review.ReviewSummary

interface ReviewRepository {
    suspend fun getReviews(
        placeId: Long,
        level: ReviewLevelFilter = ReviewLevelFilter.Mine,
        cursor: String? = null,
        size: Int = 10,
    ): CursorPage<Review>

    suspend fun getSummary(
        placeId: Long,
        level: ReviewLevelFilter = ReviewLevelFilter.Mine,
    ): ReviewSummary

    suspend fun createReview(placeId: Long, draft: ReviewDraft): Long
    suspend fun updateReview(reviewId: Long, draft: ReviewDraft)
    suspend fun deleteReview(reviewId: Long)
    suspend fun reportReview(reviewId: Long, submission: ReportSubmission)
    suspend fun getReportForm(): ReportForm
}
