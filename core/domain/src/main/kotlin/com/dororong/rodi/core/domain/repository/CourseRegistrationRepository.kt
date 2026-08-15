package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.CourseRegistrationForm
import com.dororong.rodi.core.domain.model.course.CourseRegistrationRequest
import com.dororong.rodi.core.domain.model.course.CourseRegistrationResult
import com.dororong.rodi.core.domain.model.course.RegisteredCourse
import com.dororong.rodi.core.domain.model.place.CursorPage

interface CourseRegistrationRepository {
    suspend fun getRegistrationForm(): CourseRegistrationForm
    suspend fun registerCourse(request: CourseRegistrationRequest): CourseRegistrationResult
    suspend fun getMyCourses(
        status: CourseApprovalStatus?,
        cursor: String?,
        size: Int,
    ): CursorPage<RegisteredCourse>
    suspend fun deleteCourse(courseId: Long)
}
