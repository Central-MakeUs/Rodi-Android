package com.dororong.rodi.core.data.map

import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.MapViewportQuery

interface MapCourseDataSource {
    suspend fun getCourses(query: MapViewportQuery): List<Course>
}
