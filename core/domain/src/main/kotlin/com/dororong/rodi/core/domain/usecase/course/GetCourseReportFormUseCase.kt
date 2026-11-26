package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.CourseReportRepository
import javax.inject.Inject

class GetCourseReportFormUseCase @Inject constructor(
    private val repository: CourseReportRepository,
) {
    suspend operator fun invoke() = runSuspendCatching { repository.getReportForm() }
}
