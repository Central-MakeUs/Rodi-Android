package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import javax.inject.Inject

class GetActivePracticeSessionUseCase @Inject constructor(
    private val repository: PracticeSessionRepository,
) {
    suspend operator fun invoke() = repository.read()
}
