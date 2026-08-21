package com.dororong.rodi.feature.mypage.registeredcourses

import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.RegisteredCourse

enum class RegisteredCourseFilter(
    val label: String,
    val status: CourseApprovalStatus?,
) {
    ALL("전체", null),
    APPROVED("승인", CourseApprovalStatus.APPROVED),
    PENDING("검토중", CourseApprovalStatus.PENDING),
    REJECTED("반려", CourseApprovalStatus.REJECTED),
}

internal fun resolveRegisteredCourseFilterSelection(
    selectedFilter: RegisteredCourseFilter,
    tappedFilter: RegisteredCourseFilter,
): RegisteredCourseFilter = if (
    selectedFilter == tappedFilter && tappedFilter != RegisteredCourseFilter.ALL
) {
    RegisteredCourseFilter.ALL
} else {
    tappedFilter
}

internal fun registeredCourseFilterMenuItems(
    selectedFilter: RegisteredCourseFilter,
): List<RegisteredCourseFilter> = RegisteredCourseFilter.entries.filterNot { it == selectedFilter }

data class RegisteredCoursesUiState(
    val selectedFilter: RegisteredCourseFilter = RegisteredCourseFilter.ALL,
    val courses: List<RegisteredCourse> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNext: Boolean = false,
    val nextCursor: String? = null,
    val errorMessage: String? = null,
    val appendErrorMessage: String? = null,
    val deletingCourseId: Long? = null,
)
