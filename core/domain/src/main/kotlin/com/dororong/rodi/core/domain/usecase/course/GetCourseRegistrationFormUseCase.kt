package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.CourseRegistrationRepository
import javax.inject.Inject

class GetCourseRegistrationFormUseCase @Inject constructor(
    private val repository: CourseRegistrationRepository,
) {
    suspend operator fun invoke() = runSuspendCatching { repository.getRegistrationForm() }
}
