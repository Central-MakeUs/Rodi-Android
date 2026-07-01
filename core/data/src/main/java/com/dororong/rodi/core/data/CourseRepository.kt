package com.dororong.rodi.core.data

import com.dororong.rodi.core.data.directions.KakaoDirectionsClient
import com.dororong.rodi.core.domain.Course
import javax.inject.Inject

interface CourseRepository {
    fun getCourses(): List<Course>
    suspend fun getRoute(course: Course): KakaoDirectionsClient.RouteResult
}

class CourseRepositoryImpl @Inject constructor() : CourseRepository {
    override fun getCourses(): List<Course> = SampleCourses.RODI_COURSES
    override suspend fun getRoute(course: Course): KakaoDirectionsClient.RouteResult =
        KakaoDirectionsClient.getRoute(course)
}
