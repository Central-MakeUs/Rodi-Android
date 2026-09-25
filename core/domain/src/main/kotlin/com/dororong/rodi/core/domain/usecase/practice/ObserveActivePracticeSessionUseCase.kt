package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.domain.model.practice.ActivePracticeSession
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveActivePracticeSessionUseCase @Inject constructor(
    private val repository: PracticeSessionRepository,
) {
    operator fun invoke(): Flow<ActivePracticeSession?> = repository.observe()
}
