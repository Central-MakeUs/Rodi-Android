package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.domain.model.practice.ActivePracticeSession
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import javax.inject.Inject

class SaveActivePracticeSessionUseCase @Inject constructor(
    private val repository: PracticeSessionRepository,
) {
    suspend operator fun invoke(session: ActivePracticeSession) = repository.save(session)
}
