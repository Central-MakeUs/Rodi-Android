package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject

class ObserveSavedPlacesUseCase @Inject constructor(
    private val repository: PlaceRepository,
) {
    operator fun invoke() = repository.observeSavedPlaces()
}
