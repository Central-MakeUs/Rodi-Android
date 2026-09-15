package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.repository.CourseRegistrationRepository
import javax.inject.Inject

class GetMyRegisteredCoursesUseCase @Inject constructor(
    private val repository: CourseRegistrationRepository,
) {
    suspend operator fun invoke(
        status: CourseApprovalStatus? = null,
        cursor: String? = null,
        size: Int = 20,
    ) = runSuspendCatching { repository.getMyCourses(status, cursor, size) }
}
