package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.repository.ReviewRepository
import javax.inject.Inject

class CreateReviewUseCase @Inject constructor(
    private val repository: ReviewRepository,
) {
    suspend operator fun invoke(placeId: Long, draft: ReviewDraft) =
        runSuspendCatching { repository.createReview(placeId, draft) }
}
