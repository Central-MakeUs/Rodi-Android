package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.practice.PracticeSession
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

class GetCompletedPracticeSessionUseCase @Inject constructor(
    private val repository: PracticeSessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke() = runSuspendCatching {
        repository.get()?.takeIf { session ->
            Duration.between(session.startedAt, clock.instant()) >= PRACTICE_COMPLETE_THRESHOLD
        }
    }

    companion object {
        val PRACTICE_COMPLETE_THRESHOLD: Duration = Duration.ofMinutes(10)
    }
}
