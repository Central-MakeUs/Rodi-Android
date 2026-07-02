package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseRepository
import com.dororong.rodi.core.domain.RouteResult
import javax.inject.Inject

class GetRouteUseCase @Inject constructor(
    private val courseRepository: CourseRepository,
) {
    suspend operator fun invoke(course: Course): Result<RouteResult> =
        runSuspendCatching { courseRepository.getRoute(course) }
}
