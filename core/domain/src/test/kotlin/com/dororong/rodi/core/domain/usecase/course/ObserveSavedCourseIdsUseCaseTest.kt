package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.repository.CourseRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObserveSavedCourseIdsUseCaseTest {

    @Test
    fun `invoke returns saved course ids from repository`() = runTest {
        val repository = mockk<CourseRepository> {
            every { observeSavedCourseIds() } returns flowOf(setOf(1, 2))
        }

        val result = ObserveSavedCourseIdsUseCase(repository)().first()

        assertEquals(setOf(1, 2), result)
    }
}
