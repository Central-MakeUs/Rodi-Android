package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.domain.repository.ReviewRepository
import javax.inject.Inject

class GetReportedReviewIdsUseCase @Inject constructor(
    private val repository: ReviewRepository,
) {
    suspend operator fun invoke(): Set<Long> = runCatching { repository.getReportedReviewIds() }
        .getOrDefault(emptySet())
}
