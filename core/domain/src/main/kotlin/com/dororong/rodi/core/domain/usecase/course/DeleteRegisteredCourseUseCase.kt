package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.CourseRegistrationRepository
import javax.inject.Inject

class DeleteRegisteredCourseUseCase @Inject constructor(
    private val repository: CourseRegistrationRepository,
) {
    suspend operator fun invoke(courseId: Long) = runSuspendCatching { repository.deleteCourse(courseId) }
}
