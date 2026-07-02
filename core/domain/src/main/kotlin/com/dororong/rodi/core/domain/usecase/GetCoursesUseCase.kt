package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseRepository
import javax.inject.Inject

class GetCoursesUseCase @Inject constructor(
    private val courseRepository: CourseRepository,
) {
    operator fun invoke(): List<Course> = courseRepository.getCourses()
}
