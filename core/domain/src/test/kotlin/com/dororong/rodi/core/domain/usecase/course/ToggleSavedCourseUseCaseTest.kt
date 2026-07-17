package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.repository.CourseRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ToggleSavedCourseUseCaseTest {

    @Test
    fun `invoke toggles saved course in repository`() = runTest {
        val repository = mockk<CourseRepository>(relaxed = true)

        ToggleSavedCourseUseCase(repository)(1)

        coVerify { repository.toggleSavedCourse(1) }
    }
}
