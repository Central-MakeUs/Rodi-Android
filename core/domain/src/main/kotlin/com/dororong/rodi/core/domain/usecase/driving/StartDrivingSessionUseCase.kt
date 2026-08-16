package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.domain.repository.DrivingSessionRepository
import javax.inject.Inject

class StartDrivingSessionUseCase @Inject constructor(
    private val repository: DrivingSessionRepository,
) {
    suspend operator fun invoke(session: DrivingSession) = repository.start(session)
}
