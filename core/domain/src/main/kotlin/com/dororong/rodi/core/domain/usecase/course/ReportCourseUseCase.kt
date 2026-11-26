package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.course.CourseReportSubmission
import com.dororong.rodi.core.domain.repository.CourseReportRepository
import javax.inject.Inject

class ReportCourseUseCase @Inject constructor(
    private val repository: CourseReportRepository,
) {
    suspend operator fun invoke(courseId: Long, submission: CourseReportSubmission) =
        runSuspendCatching { repository.reportCourse(courseId, submission) }
}
