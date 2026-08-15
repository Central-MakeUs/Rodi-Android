package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.course.CourseRegistrationRequest
import com.dororong.rodi.core.domain.model.course.validateForSubmission
import com.dororong.rodi.core.domain.repository.CourseRegistrationRepository
import javax.inject.Inject

class RegisterCourseUseCase @Inject constructor(
    private val repository: CourseRegistrationRepository,
) {
    suspend operator fun invoke(request: CourseRegistrationRequest) = runSuspendCatching {
        request.validateForSubmission()
        repository.registerCourse(request)
    }
}
