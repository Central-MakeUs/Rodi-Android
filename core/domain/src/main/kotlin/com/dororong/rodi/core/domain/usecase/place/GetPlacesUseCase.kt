package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject

class GetPlacesUseCase @Inject constructor(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(query: PlaceViewportQuery, cursor: String? = null, size: Int = 20) =
        runSuspendCatching { repository.getPlaces(query, cursor, size) }
}
