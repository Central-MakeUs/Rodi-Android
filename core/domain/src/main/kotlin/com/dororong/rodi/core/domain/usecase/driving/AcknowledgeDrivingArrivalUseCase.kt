package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.repository.DrivingSessionRepository
import javax.inject.Inject

class AcknowledgeDrivingArrivalUseCase @Inject constructor(
    private val repository: DrivingSessionRepository,
) {
    suspend operator fun invoke(sessionId: String) = repository.acknowledgeArrival(sessionId)
}
