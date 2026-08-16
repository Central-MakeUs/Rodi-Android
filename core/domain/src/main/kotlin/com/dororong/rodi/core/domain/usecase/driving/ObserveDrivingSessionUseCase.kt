package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.repository.DrivingSessionRepository
import javax.inject.Inject

class ObserveDrivingSessionUseCase @Inject constructor(
    private val repository: DrivingSessionRepository,
) {
    operator fun invoke() = repository.session
}
