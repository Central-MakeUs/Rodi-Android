package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.PracticeRepository
import javax.inject.Inject

class SubmitSkipReasonUseCase @Inject constructor(
    private val repository: PracticeRepository,
) {
    suspend operator fun invoke(practiceId: Long, reason: String, detail: String? = null) =
        runSuspendCatching { repository.submitSkipReason(practiceId, reason, detail) }
}
