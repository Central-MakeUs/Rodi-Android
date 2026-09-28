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
    fun `전체 필터는 상태를 null로 보내고 중복을 뺀다`() = runTest(dispatcher) {
        coEvery { getCourses(status = null, cursor = null, size = any()) } returns Result.success(
            CursorPage(listOf(approved, approved, pending), hasNext = false, nextCursor = null, totalCount = 2),
        )
        val viewModel = RegisteredCoursesViewModel(getCourses, deleteCourse)
        advanceUntilIdle()

        assertEquals(listOf(approved, pending), viewModel.uiState.value.courses)
        coVerify(exactly = 1) { getCourses(status = null, cursor = null, size = any()) }
    }

    @Test
    fun `다음 페이지는 커서를 쓰고 항목을 중복 없이 유지한다`() = runTest(dispatcher) {
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
    fun `선택한 상태를 다시 누르면 전체로 돌아가고 저장된 전체 페이지를 복원한다`() = runTest(dispatcher) {
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
    fun `필터 메뉴로 네 가지 상태를 모두 고를 수 있다`() {
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
    fun `상태 필터를 고른 뒤에도 필터 메뉴에 전체가 있다`() {
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
    fun `첫 로드가 실패하면 첫 페이지를 다시 요청한다`() = runTest(dispatcher) {
        val failure = IllegalStateException("network")
        coEvery { getCourses(status = null, cursor = null, size = any()) } returnsMany listOf(
            Result.failure(failure),
            Result.success(CursorPage(listOf(approved), false, null, 1)),
        )
        val viewModel = RegisteredCoursesViewModel(getCourses, deleteCourse)
        advanceUntilIdle()
        assertEquals(
            "내 활동을 불러오지 못했어요. 잠시 후 다시 시도해주세요.",
            viewModel.uiState.value.errorMessage,
        )

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(approved), viewModel.uiState.value.courses)
        assertEquals(null, viewModel.uiState.value.errorMessage)
        coVerify(exactly = 2) { getCourses(status = null, cursor = null, size = any()) }
    }

    @Test
    fun `삭제에 성공하면 현재 목록에서 코스를 지운다`() = runTest(dispatcher) {
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
    fun `삭제가 실패해도 현재 목록을 유지하고 오류를 보여준다`() = runTest(dispatcher) {
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
        assertEquals(
            "코스를 삭제하지 못했어요. 잠시 후 다시 시도해주세요.",
            viewModel.uiState.value.errorMessage,
        )
        assertEquals(null, viewModel.uiState.value.deletingCourseId)
    }

    private fun course(id: Long, status: CourseApprovalStatus) = RegisteredCourse(
        courseId = id,
        name = "코스$id",
        approvalStatus = status,
        createdAt = Instant.EPOCH,
    )
}
