package com.dororong.rodi.core.domain

interface CourseRepository {
    fun getCourses(): List<Course>
    suspend fun getRoute(course: Course): RouteResult
}
