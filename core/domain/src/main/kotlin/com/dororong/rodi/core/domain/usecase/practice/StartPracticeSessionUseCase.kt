package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.practice.PracticeSession
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import java.time.Clock
import javax.inject.Inject

class StartPracticeSessionUseCase @Inject constructor(
    private val repository: PracticeSessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(placeId: Long, placeName: String) = runSuspendCatching {
        repository.save(PracticeSession(placeId, placeName, clock.instant()))
    }
}
