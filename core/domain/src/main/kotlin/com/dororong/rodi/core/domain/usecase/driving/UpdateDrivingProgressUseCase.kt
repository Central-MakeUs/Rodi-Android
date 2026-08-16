package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.repository.DrivingSessionRepository
import javax.inject.Inject

class UpdateDrivingProgressUseCase @Inject constructor(
    private val repository: DrivingSessionRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        traveledDistanceMeters: Double,
    ) = repository.updateProgress(sessionId, traveledDistanceMeters)
}
