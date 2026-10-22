package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.repository.PlaceFilterRepository
import javax.inject.Inject

class SetPlaceFilterUseCase @Inject constructor(
    private val repository: PlaceFilterRepository,
) {
    suspend operator fun invoke(practiceTypes: Set<PracticeType>) =
        runSuspendCatching { repository.setPracticeTypes(practiceTypes) }
}
