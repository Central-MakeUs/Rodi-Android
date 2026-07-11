package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.RouteResult

interface CourseRepository {
    fun getCourses(): List<Course>
    suspend fun getRoute(course: Course): RouteResult
}
