package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.remote.directions.KakaoDirectionsClient
import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.source.local.sample.SampleCourses
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.repository.CourseRepository
import com.dororong.rodi.core.domain.model.course.RouteResult
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor(
    private val directionsClient: KakaoDirectionsClient,
) : CourseRepository {
    override fun getCourses(): List<Course> = SampleCourses.RODI_COURSES

    override suspend fun getRoute(course: Course): RouteResult {
        return directionsClient.getRoute(course).toDomain()
    }
}
