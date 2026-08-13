package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.PracticeRepository
import javax.inject.Inject

class GetSkipReasonFormUseCase @Inject constructor(
    private val repository: PracticeRepository,
) {
    suspend operator fun invoke() = runSuspendCatching { repository.getSkipReasonForm() }
}
