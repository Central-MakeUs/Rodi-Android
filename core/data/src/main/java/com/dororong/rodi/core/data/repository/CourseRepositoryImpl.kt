package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.source.local.datastore.SavedCourseLocalDataSource
import com.dororong.rodi.core.data.source.local.sample.SampleCourses
import com.dororong.rodi.core.data.source.remote.directions.KakaoDirectionsClient
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CoursePoint
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.repository.CourseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class CourseRepositoryImpl @Inject constructor(
    private val directionsClient: KakaoDirectionsClient,
    private val savedCourseLocalDataSource: SavedCourseLocalDataSource,
) : CourseRepository {
    override fun getCourses(): List<Course> = SampleCourses.RODI_COURSES

    override suspend fun getRoute(course: Course): RouteResult = directionsClient.getRoute(course).toDomain()

    override suspend fun getRoute(
        origin: CoursePoint,
        waypoints: List<CoursePoint>,
        destination: CoursePoint,
    ): RouteResult = directionsClient.getRoute(origin, waypoints, destination).toDomain()

    override fun observeSavedCourseIds(): Flow<Set<Int>> = savedCourseLocalDataSource.observeSavedCourseIds()

    override suspend fun toggleSavedCourse(courseId: Int) {
        savedCourseLocalDataSource.toggleSavedCourse(courseId)
    }
}
