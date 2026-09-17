package com.dororong.rodi.feature.mypage.registeredcourses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.RegisteredCourse
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.error.RodiInlineRetryError
import com.dororong.rodi.core.ui.components.error.RodiRetryError
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.registeredcourses.components.RegisteredCourseDeleteDialog
import com.dororong.rodi.feature.mypage.registeredcourses.components.RegisteredCourseFilters
import com.dororong.rodi.feature.mypage.registeredcourses.components.RegisteredCourseRow
import com.dororong.rodi.feature.mypage.registeredcourses.components.RegisteredCoursesEmpty
import com.dororong.rodi.feature.mypage.registeredcourses.components.RegisteredCoursesLoading
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun RegisteredCoursesScreen(
    onRegisterCourseClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisteredCoursesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RegisteredCoursesContent(
        state = state,
        onFilterSelected = viewModel::selectFilter,
        onRegisterCourseClick = onRegisterCourseClick,
        onLoadInitial = viewModel::loadInitial,
        onLoadNext = viewModel::loadNextPage,
        onRetry = viewModel::retry,
        onClearError = viewModel::clearError,
        onDelete = viewModel::delete,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun RegisteredCoursesContent(
    state: RegisteredCoursesUiState,
    onFilterSelected: (RegisteredCourseFilter) -> Unit,
    onRegisterCourseClick: () -> Unit,
    onLoadInitial: () -> Unit,
    onLoadNext: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
    onDelete: (RegisteredCourse) -> Unit,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState? = null,
    initiallyOpenCourseId: Long? = null,
    initiallyExpandFilterMenu: Boolean = false,
    applyNavigationBarsPadding: Boolean = true,
) {
    var menuCourseId by remember { mutableStateOf(initiallyOpenCourseId) }
    var deleteTarget by remember { mutableStateOf<RegisteredCourse?>(null) }
    val scrollState = listState ?: rememberLazyListState()
    val snackbarHostState = remember { RodiSnackbarHostState() }

    LaunchedEffect(state.courses, deleteTarget) {
        val target = deleteTarget
        if (target != null && state.courses.none { it.courseId == target.courseId }) {
            deleteTarget = null
            menuCourseId = null
        }
    }

    // 목록이 비어있을 때의 실패는 RodiRetryError 전체 화면으로만 보여준다 — 스낵바까지
    // 띄우면 같은 메시지가 두 번 뜬다. appendErrorMessage는 인라인 재시도 항목이 이미 같은
    // 액션을 제공하므로 스낵바를 따로 띄우지 않는다.
    LaunchedEffect(state.errorMessage, state.courses.isEmpty()) {
        if (state.courses.isNotEmpty()) {
            state.errorMessage?.let { message ->
                snackbarHostState.show(RodiSnackbarData(message = message))
            }
        }
    }

    LaunchedEffect(scrollState, state.courses.size, state.hasNext, state.isLoadingMore) {
        snapshotFlow { scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .map { index -> index != null && index >= state.courses.lastIndex - 2 }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadNext() }
    }

    Surface(modifier = modifier.fillMaxSize(), color = RodiTheme.colors.white) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().let {
                    if (applyNavigationBarsPadding) it.navigationBarsPadding() else it
                },
            ) {
                RegisteredCourseFilters(
                    selectedFilter = state.selectedFilter,
                    onFilterSelected = onFilterSelected,
                    initiallyExpanded = initiallyExpandFilterMenu,
                )
                when {
                    state.isLoading && state.courses.isEmpty() -> RegisteredCoursesLoading()
                    state.errorMessage != null && state.courses.isEmpty() -> RodiRetryError(
                        message = state.errorMessage,
                        onRetry = onRetry,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    state.courses.isEmpty() -> RegisteredCoursesEmpty(
                        filter = state.selectedFilter,
                        onRegisterCourseClick = onRegisterCourseClick,
                    )
                    else -> LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(state.courses, key = { it.courseId }) { course ->
                            RegisteredCourseRow(
                                course = course,
                                menuExpanded = menuCourseId == course.courseId,
                                onMenuClick = { menuCourseId = course.courseId },
                                onMenuDismiss = { menuCourseId = null },
                                onDelete = {
                                    deleteTarget = course
                                },
                                scrollState = scrollState,
                                isDeleting = state.deletingCourseId == course.courseId,
                            )
                        }
                        if (state.isLoadingMore) {
                            item(key = "append-loading") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    RodiSkeleton(
                                        modifier = Modifier.size(20.dp),
                                        shape = CircleShape,
                                        color = RodiTheme.colors.gray200,
                                    )
                                }
                            }
                        }
                        state.appendErrorMessage?.let { message ->
                            item(key = "append-error") {
                                RodiInlineRetryError(
                                    message = message,
                                    onRetry = { onClearError(); onRetry() },
                                )
                            }
                        }
                    }
                }
            }
        }
        RodiSnackbarHost(snackbarHostState)
    }

    deleteTarget?.let { target ->
        RegisteredCourseDeleteDialog(
            status = target.approvalStatus,
            enabled = state.deletingCourseId == null,
            onDelete = { onDelete(target) },
            onDismiss = { deleteTarget = null },
        )
    }
}

private val PreviewCourses = listOf(
    RegisteredCourse(1L, "망원한강공원 드라이브 코스", CourseApprovalStatus.APPROVED, Instant.parse("2026-05-10T00:00:00Z")),
    RegisteredCourse(2L, "서울숲 초보 연습 코스", CourseApprovalStatus.PENDING, Instant.parse("2026-05-11T00:00:00Z")),
    RegisteredCourse(3L, "남산서울타워 코스", CourseApprovalStatus.REJECTED, Instant.parse("2026-05-12T00:00:00Z")),
)

@Preview(name = "등록 코스 목록", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesListPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(courses = PreviewCourses, hasNext = true, nextCursor = "next"),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "등록 코스 로딩", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesLoadingPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(isLoading = true),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "등록 코스 빈 상태", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesEmptyPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "등록 코스 오류", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesErrorPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(errorMessage = "등록한 코스를 불러오지 못했어요."),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "등록 코스 추가 로딩", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesAppendPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(courses = PreviewCourses, isLoadingMore = true),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "등록 코스 추가 오류", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesAppendErrorPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(
            courses = PreviewCourses,
            appendErrorMessage = "다음 코스를 불러오지 못했어요.",
        ),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "등록 코스 삭제 오류", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesDeleteErrorPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(
            courses = PreviewCourses,
            errorMessage = "코스를 삭제하지 못했어요.",
        ),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "등록 코스 삭제 중", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesDeletingPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(
            courses = PreviewCourses,
            deletingCourseId = PreviewCourses.first().courseId,
        ),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "승인 코스 빈 상태", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesApprovedEmptyPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(selectedFilter = RegisteredCourseFilter.APPROVED),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "검토중 코스 빈 상태", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesPendingEmptyPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(selectedFilter = RegisteredCourseFilter.PENDING),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "반려 코스 빈 상태", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesRejectedEmptyPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(selectedFilter = RegisteredCourseFilter.REJECTED),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
    )
}

@Preview(name = "등록 코스 삭제 메뉴", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesMenuPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(courses = PreviewCourses),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
        initiallyOpenCourseId = PreviewCourses.first().courseId,
    )
}

@Preview(name = "등록 코스 필터 팝업", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesFilterPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(courses = PreviewCourses),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
        initiallyExpandFilterMenu = true,
    )
}

@Preview(name = "등록 코스 선택 필터 팝업", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesSelectedFilterPreview() = RodiTheme {
    RegisteredCoursesContent(
        state = RegisteredCoursesUiState(
            selectedFilter = RegisteredCourseFilter.APPROVED,
            courses = PreviewCourses,
        ),
        onFilterSelected = {},
        onRegisterCourseClick = {},
        onLoadInitial = {},
        onLoadNext = {},
        onRetry = {},
        onClearError = {},
        onDelete = {},
        initiallyExpandFilterMenu = true,
    )
}

@Preview(name = "등록 코스 삭제 확인 - 승인", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesDeleteApprovedPreview() = RodiTheme {
    Box(Modifier.fillMaxSize()) {
        RegisteredCoursesContent(
            state = RegisteredCoursesUiState(courses = PreviewCourses),
            onFilterSelected = {},
            onRegisterCourseClick = {},
            onLoadInitial = {},
            onLoadNext = {},
            onRetry = {},
            onClearError = {},
            onDelete = {},
            initiallyOpenCourseId = PreviewCourses.first().courseId,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            RegisteredCourseDeleteDialog(status = CourseApprovalStatus.APPROVED, onDelete = {}, onDismiss = {})
        }
    }
}

@Preview(name = "등록 코스 삭제 확인 - 검토중", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesDeletePendingPreview() = RodiTheme {
    Box(Modifier.fillMaxSize()) {
        RegisteredCoursesContent(
            state = RegisteredCoursesUiState(courses = PreviewCourses),
            onFilterSelected = {},
            onRegisterCourseClick = {},
            onLoadInitial = {},
            onLoadNext = {},
            onRetry = {},
            onClearError = {},
            onDelete = {},
            initiallyOpenCourseId = PreviewCourses[1].courseId,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            RegisteredCourseDeleteDialog(status = CourseApprovalStatus.PENDING, onDelete = {}, onDismiss = {})
        }
    }
}

@Preview(name = "등록 코스 삭제 확인 - 반려", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun RegisteredCoursesDeleteRejectedPreview() = RodiTheme {
    Box(Modifier.fillMaxSize()) {
        RegisteredCoursesContent(
            state = RegisteredCoursesUiState(courses = PreviewCourses),
            onFilterSelected = {},
            onRegisterCourseClick = {},
            onLoadInitial = {},
            onLoadNext = {},
            onRetry = {},
            onClearError = {},
            onDelete = {},
            initiallyOpenCourseId = PreviewCourses[2].courseId,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            RegisteredCourseDeleteDialog(status = CourseApprovalStatus.REJECTED, onDelete = {}, onDismiss = {})
        }
    }
}
