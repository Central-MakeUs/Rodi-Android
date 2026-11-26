package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.PlaceFilterRepository
import javax.inject.Inject

class GetPlaceFilterUseCase @Inject constructor(
    private val repository: PlaceFilterRepository,
) {
    suspend operator fun invoke() = runSuspendCatching { repository.getPracticeTypes() }
}
