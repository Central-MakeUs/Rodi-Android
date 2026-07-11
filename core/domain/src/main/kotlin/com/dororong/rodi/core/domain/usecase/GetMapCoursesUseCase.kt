package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseRepository
import com.dororong.rodi.core.domain.MapViewportQuery
import javax.inject.Inject

class GetMapCoursesUseCase @Inject constructor(
    private val courseRepository: CourseRepository,
) {
    suspend operator fun invoke(query: MapViewportQuery): Result<List<Course>> =
        runSuspendCatching { courseRepository.getCourses(query) }
}
