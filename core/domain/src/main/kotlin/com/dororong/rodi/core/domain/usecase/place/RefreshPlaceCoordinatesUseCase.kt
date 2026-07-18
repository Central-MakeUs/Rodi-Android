package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject

class RefreshPlaceCoordinatesUseCase @Inject constructor(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke() = runSuspendCatching { repository.refreshCoordinates() }
}
