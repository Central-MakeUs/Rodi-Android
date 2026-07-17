package com.dororong.rodi.feature.mypage.savedcourses

import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.repository.CourseRepository
import com.dororong.rodi.core.domain.usecase.course.GetCoursesUseCase
import com.dororong.rodi.core.domain.usecase.course.ObserveSavedCourseIdsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedCoursesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `only saved courses are displayed`() = runTest(testDispatcher) {
        val savedCourse = mockk<Course> { every { id } returns 1 }
        val unsavedCourse = mockk<Course> { every { id } returns 2 }
        val repository = mockk<CourseRepository> {
            every { observeSavedCourseIds() } returns flowOf(setOf(1))
            every { getCourses() } returns listOf(savedCourse, unsavedCourse)
        }

        val viewModel = SavedCoursesViewModel(
            getCourses = GetCoursesUseCase(repository),
            observeSavedCourseIds = ObserveSavedCourseIdsUseCase(repository),
        )
        advanceUntilIdle()

        assertEquals(listOf(savedCourse), viewModel.uiState.value.courses)
    }
}
