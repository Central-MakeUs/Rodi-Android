package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.review.ReviewLevelFilter
import com.dororong.rodi.core.domain.repository.ReviewRepository
import javax.inject.Inject

class GetReviewSummaryUseCase @Inject constructor(
    private val repository: ReviewRepository,
) {
    suspend operator fun invoke(placeId: Long, level: ReviewLevelFilter = ReviewLevelFilter.Mine) =
        runSuspendCatching { repository.getSummary(placeId, level) }
}
