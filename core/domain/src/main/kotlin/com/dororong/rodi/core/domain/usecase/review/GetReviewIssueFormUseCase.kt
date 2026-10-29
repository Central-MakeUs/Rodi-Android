package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.ReviewRepository
import com.dororong.rodi.core.domain.repository.PlaceRepository
import com.dororong.rodi.core.domain.model.place.PlaceType
import javax.inject.Inject

class GetReviewIssueFormUseCase @Inject constructor(
    private val repository: ReviewRepository,
    private val placeRepository: PlaceRepository,
) {
    suspend operator fun invoke(placeId: Long) =
        runSuspendCatching {
            val form = repository.getReviewIssueForm(placeId) ?: return@runSuspendCatching null
            form.takeIf { placeRepository.getPlaceDetail(placeId).type == PlaceType.COURSE }
        }
}
