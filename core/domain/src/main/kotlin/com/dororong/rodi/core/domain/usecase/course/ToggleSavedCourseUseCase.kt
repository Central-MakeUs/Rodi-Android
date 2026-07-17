package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.repository.CourseRepository
import javax.inject.Inject

class ToggleSavedCourseUseCase @Inject constructor(
    private val courseRepository: CourseRepository,
) {
    suspend operator fun invoke(courseId: Int) = courseRepository.toggleSavedCourse(courseId)
}
