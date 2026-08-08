package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.repository.ReviewRepository
import javax.inject.Inject

class ReportReviewUseCase @Inject constructor(
    private val repository: ReviewRepository,
) {
    suspend operator fun invoke(reviewId: Long, submission: ReportSubmission) =
        runSuspendCatching { repository.reportReview(reviewId, submission) }
}
