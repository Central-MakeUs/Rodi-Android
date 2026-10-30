package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.ReviewRepository
import javax.inject.Inject

class GetReviewIssueFormUseCase @Inject constructor(
    private val repository: ReviewRepository,
) {
    suspend operator fun invoke(placeId: Long) =
        runSuspendCatching { repository.getReviewIssueForm(placeId) }
}
