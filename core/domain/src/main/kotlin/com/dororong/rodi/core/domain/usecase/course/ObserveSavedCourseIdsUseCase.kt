package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.repository.CourseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSavedCourseIdsUseCase @Inject constructor(
    private val courseRepository: CourseRepository,
) {
    operator fun invoke(): Flow<Set<Int>> = courseRepository.observeSavedCourseIds()
}
