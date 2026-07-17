package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject

class GetPlaceDetailUseCase @Inject constructor(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(placeId: Long) = runSuspendCatching { repository.getPlaceDetail(placeId) }
}
