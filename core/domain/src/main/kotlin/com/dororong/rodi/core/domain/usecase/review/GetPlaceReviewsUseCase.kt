package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.review.ReviewLevelFilter
import com.dororong.rodi.core.domain.repository.ReviewRepository
import javax.inject.Inject

class GetPlaceReviewsUseCase @Inject constructor(
    private val repository: ReviewRepository,
) {
    suspend operator fun invoke(
        placeId: Long,
        level: ReviewLevelFilter = ReviewLevelFilter.Mine,
        cursor: String? = null,
        size: Int = 10,
    ) = runSuspendCatching { repository.getReviews(placeId, level, cursor, size) }
}
