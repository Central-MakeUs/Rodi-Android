package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.ReviewRepository
import javax.inject.Inject

class GetReportFormUseCase @Inject constructor(
    private val repository: ReviewRepository,
) {
    suspend operator fun invoke() = runSuspendCatching { repository.getReportForm() }
}
