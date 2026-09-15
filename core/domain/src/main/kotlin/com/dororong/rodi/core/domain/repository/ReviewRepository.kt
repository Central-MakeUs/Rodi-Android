package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.review.ReportForm
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewDetail
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

    suspend fun getReview(reviewId: Long): ReviewDetail
    suspend fun createReview(placeId: Long, draft: ReviewDraft): Long
    suspend fun updateReview(reviewId: Long, draft: ReviewDraft)
    suspend fun deleteReview(reviewId: Long)
    suspend fun reportReview(reviewId: Long, submission: ReportSubmission)
    suspend fun getReportForm(): ReportForm

    /**
     * 내가 신고한 후기 id. 서버는 서로 다른 5명이 신고해야 후기를 감추고 신고자가 누구인지도
     * 내려주지 않아서, 신고 직후 내 화면에서만 가리려면 기기 로컬 기록에 기대야 한다.
     */
    suspend fun getReportedReviewIds(): Set<Long>
}
