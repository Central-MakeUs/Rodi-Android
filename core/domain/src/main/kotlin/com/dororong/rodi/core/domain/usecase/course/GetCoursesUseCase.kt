package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.repository.CourseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetCoursesUseCase @Inject constructor(
    private val courseRepository: CourseRepository,
) {
    operator fun invoke(): List<Course> = courseRepository.getCourses()

    fun observeSavedCourseIds(): Flow<Set<Int>> = courseRepository.observeSavedCourseIds()

    suspend fun toggleSavedCourse(courseId: Int) = courseRepository.toggleSavedCourse(courseId)
}
