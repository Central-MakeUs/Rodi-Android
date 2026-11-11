package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.course.CourseReportSubmission
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm

interface CourseReportRepository {
    suspend fun getReportForm(): ReviewIssueForm
    suspend fun reportCourse(courseId: Long, submission: CourseReportSubmission)
}
