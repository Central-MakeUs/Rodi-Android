package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import javax.inject.Inject

class ConfirmPracticeArrivalUseCase @Inject constructor(
    private val repository: PracticeSessionRepository,
) {
    suspend operator fun invoke(placeId: Long) = repository.confirmArrival(placeId)
}
