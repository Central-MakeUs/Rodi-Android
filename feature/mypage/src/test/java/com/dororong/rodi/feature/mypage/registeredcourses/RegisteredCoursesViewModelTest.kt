package com.dororong.rodi.feature.mypage.registeredcourses

import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.RegisteredCourse
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.usecase.course.DeleteRegisteredCourseUseCase
import com.dororong.rodi.core.domain.usecase.course.GetMyRegisteredCoursesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class RegisteredCoursesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getCourses = mockk<GetMyRegisteredCoursesUseCase>()
    private val deleteCourse = mockk<DeleteRegisteredCourseUseCase>()
    private val approved = course(1, CourseApprovalStatus.APPROVED)
    private val pending = course(2, CourseApprovalStatus.PENDING)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `all filter sends null status and removes duplicates`() = runTest(dispatcher) {
        coEvery { getCourses(status = null, cursor = null, size = any()) } returns Result.success(
            CursorPage(listOf(approved, approved, pending), hasNext = false, nextCursor = null, totalCount = 2),
        )
        val viewModel = RegisteredCoursesViewModel(getCourses, deleteCourse)
        advanceUntilIdle()

        assertEquals(listOf(approved, pending), viewModel.uiState.value.courses)
        coVerify(exactly = 1) { getCourses(status = null, cursor = null, size = any()) }
    }

    @Test
    fun `append uses cursor and keeps page items unique`() = runTest(dispatcher) {
        val next = course(3, CourseApprovalStatus.APPROVED)
        coEvery { getCourses(status = null, cursor = null, size = any()) } returns Result.success(
            CursorPage(listOf(approved), hasNext = true, nextCursor = "cursor-1", totalCount = 2),
        )
        coEvery { getCourses(status = null, cursor = "cursor-1", size = any()) } returns Result.success(
            CursorPage(listOf(approved, next), hasNext = false, nextCursor = null, totalCount = 2),
        )
        val viewModel = RegisteredCoursesViewModel(getCourses, deleteCourse)
        advanceUntilIdle()
        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(approved, next), viewModel.uiState.value.courses)
        coVerify(exactly = 1) { getCourses(status = null, cursor = "cursor-1", size = any()) }
    }

    @Test
    fun `selected status retap returns all null status and restores its cached page`() = runTest(dispatcher) {
        coEvery { getCourses(status = null, cursor = null, size = any()) } returns Result.success(
            CursorPage(listOf(approved), false, null, 1),
        )
        coEvery { getCourses(status = CourseApprovalStatus.PENDING, cursor = null, size = any()) } returns Result.success(
            CursorPage(listOf(pending), false, null, 1),
        )
        val viewModel = RegisteredCoursesViewModel(getCourses, deleteCourse)
        advanceUntilIdle()
        viewModel.selectFilter(RegisteredCourseFilter.PENDING)
        advanceUntilIdle()
        viewModel.selectFilter(
            resolveRegisteredCourseFilterSelection(
                selectedFilter = RegisteredCourseFilter.PENDING,
                tappedFilter = RegisteredCourseFilter.PENDING,
            ),
        )
        advanceUntilIdle()

        assertEquals(RegisteredCourseFilter.ALL, viewModel.uiState.value.selectedFilter)
        assertEquals(listOf(approved), viewModel.uiState.value.courses)
        coVerify(exactly = 1) {
            getCourses(status = null, cursor = null, size = any())
        }
        coVerify(exactly = 1) {
            getCourses(status = CourseApprovalStatus.PENDING, cursor = null, size = any())
        }
    }

    @Test
    fun `filter menu can reach all four status selections`() {
        assertEquals(
            RegisteredCourseFilter.ALL,
            resolveRegisteredCourseFilterSelection(
                RegisteredCourseFilter.PENDING,
                RegisteredCourseFilter.PENDING,
            ),
        )
        assertEquals(
            RegisteredCourseFilter.APPROVED,
            resolveRegisteredCourseFilterSelection(
                RegisteredCourseFilter.ALL,
                RegisteredCourseFilter.APPROVED,
            ),
        )
        assertEquals(
            RegisteredCourseFilter.PENDING,
            resolveRegisteredCourseFilterSelection(
                RegisteredCourseFilter.ALL,
                RegisteredCourseFilter.PENDING,
            ),
        )
        assertEquals(
            RegisteredCourseFilter.REJECTED,
            resolveRegisteredCourseFilterSelection(
                RegisteredCourseFilter.ALL,
                RegisteredCourseFilter.REJECTED,
            ),
        )
    }

    @Test
    fun `filter menu includes all when a status filter is selected`() {
        assertEquals(
            listOf(
                RegisteredCourseFilter.ALL,
                RegisteredCourseFilter.APPROVED,
                RegisteredCourseFilter.REJECTED,
            ),
            registeredCourseFilterMenuItems(RegisteredCourseFilter.PENDING),
        )
    }

    @Test
    fun `initial failure retries the first page`() = runTest(dispatcher) {
        val failure = IllegalStateException("network")
        coEvery { getCourses(status = null, cursor = null, size = any()) } returnsMany listOf(
            Result.failure(failure),
            Result.success(CursorPage(listOf(approved), false, null, 1)),
        )
        val viewModel = RegisteredCoursesViewModel(getCourses, deleteCourse)
        advanceUntilIdle()
        assertEquals(failure.message, viewModel.uiState.value.errorMessage)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(approved), viewModel.uiState.value.courses)
        assertEquals(null, viewModel.uiState.value.errorMessage)
        coVerify(exactly = 2) { getCourses(status = null, cursor = null, size = any()) }
    }

    @Test
    fun `successful delete removes course from current list`() = runTest(dispatcher) {
        coEvery { getCourses(status = null, cursor = null, size = any()) } returns Result.success(
            CursorPage(listOf(approved, pending), false, null, 2),
        )
        coEvery { deleteCourse(approved.courseId) } returns Result.success(Unit)
        val viewModel = RegisteredCoursesViewModel(getCourses, deleteCourse)
        advanceUntilIdle()
        viewModel.delete(approved)
        advanceUntilIdle()

        assertEquals(listOf(pending), viewModel.uiState.value.courses)
    }

    @Test
    fun `delete failure is surfaced without dropping the current list`() = runTest(dispatcher) {
        val failure = IllegalStateException("delete failed")
        coEvery { getCourses(status = null, cursor = null, size = any()) } returns Result.success(
            CursorPage(listOf(approved, pending), false, null, 2),
        )
        coEvery { deleteCourse(approved.courseId) } returns Result.failure(failure)
        val viewModel = RegisteredCoursesViewModel(getCourses, deleteCourse)
        advanceUntilIdle()
        viewModel.delete(approved)
        advanceUntilIdle()

        assertEquals(listOf(approved, pending), viewModel.uiState.value.courses)
        assertEquals(failure.message, viewModel.uiState.value.errorMessage)
        assertEquals(null, viewModel.uiState.value.deletingCourseId)
    }

    private fun course(id: Long, status: CourseApprovalStatus) = RegisteredCourse(
        courseId = id,
        name = "코스$id",
        approvalStatus = status,
        createdAt = Instant.EPOCH,
    )
}
