package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import javax.inject.Inject

class ClearPracticeSessionUseCase @Inject constructor(
    private val repository: PracticeSessionRepository,
) {
    suspend operator fun invoke() = runSuspendCatching { repository.clear() }
}
