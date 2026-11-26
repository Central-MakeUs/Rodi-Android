package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject

class RefreshPlaceClustersUseCase @Inject constructor(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(query: PlaceViewportQuery) =
        runSuspendCatching { repository.refreshClusters(query) }
}
