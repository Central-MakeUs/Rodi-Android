package com.dororong.rodi.feature.mypage.registeredcourses

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.RegisteredCourse
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.components.dialog.RodiDialog
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

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

    LaunchedEffect(state.errorMessage, state.appendErrorMessage, state.courses.isEmpty()) {
        state.errorMessage?.let { message ->
            snackbarHostState.show(RodiSnackbarData(message = message))
        }
        state.appendErrorMessage?.let { message ->
            snackbarHostState.show(
                RodiSnackbarData(
                    message = message,
                    actionLabel = "다시 시도",
                    onAction = onRetry,
                ),
            )
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
                    state.errorMessage != null && state.courses.isEmpty() -> RegisteredCoursesError(
                        message = state.errorMessage,
                        onRetry = onRetry,
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
                                RegisteredCoursesAppendError(
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

@Composable
private fun RegisteredCourseDeleteDialog(
    status: CourseApprovalStatus = CourseApprovalStatus.APPROVED,
    enabled: Boolean = true,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    RodiDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(280.dp)
            .height(226.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
        dismissible = false,
    ) {
        Text(
            text = "정말 삭제하시겠습니까?",
            modifier = Modifier.fillMaxWidth(),
            style = RodiTheme.typography.price1,
            color = RodiTheme.colors.black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when (status) {
                    CourseApprovalStatus.APPROVED ->
                        "이 코스는 다른 초보운전자에게도 도움이 되고\n있어요. 삭제하면 더 이상 공개되지 않아요."
                    CourseApprovalStatus.PENDING ->
                        "현재 검토 중인 코스예요. 삭제하면 코스 검토가\n중단돼요."
                    CourseApprovalStatus.REJECTED ->
                        "삭제하면 해당 코스를 내 활동에서 더 이상\n확인할 수 없어요."
                },
                modifier = Modifier.fillMaxWidth(),
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.black,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RodiButton(
                text = "삭제하기",
                onClick = onDelete,
                modifier = Modifier.width(116.dp),
                variant = RodiButtonVariant.Secondary,
                enabled = enabled,
                fillMaxWidth = false,
                height = 42.dp,
            )
            RodiButton(
                text = "취소",
                onClick = onDismiss,
                modifier = Modifier.width(116.dp),
                enabled = enabled,
                fillMaxWidth = false,
                height = 42.dp,
            )
        }
    }
}

@Composable
private fun RegisteredCourseFilters(
    selectedFilter: RegisteredCourseFilter,
    onFilterSelected: (RegisteredCourseFilter) -> Unit,
    initiallyExpanded: Boolean,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val density = LocalDensity.current
    val popupPositionProvider = remember(density) {
        RegisteredCourseFilterPopupPositionProvider(density)
    }
    Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(48.dp)
                .padding(end = 16.dp)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = if (expanded) "접기" else selectedFilter.label,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray700,
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down_16),
                contentDescription = if (expanded) "필터 닫기" else "필터 열기",
                tint = RodiTheme.colors.gray700,
                modifier = Modifier.size(16.dp).then(if (expanded) Modifier.rotate(180f) else Modifier),
            )
        }
        if (expanded) {
            val onSelected: (RegisteredCourseFilter) -> Unit = { filter ->
                expanded = false
                onFilterSelected(resolveRegisteredCourseFilterSelection(selectedFilter, filter))
            }
            if (LocalInspectionMode.current) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 28.dp, end = 16.dp),
                ) {
                    RegisteredCourseFilterMenuSurface(
                        selectedFilter = selectedFilter,
                        onSelected = onSelected,
                    )
                }
            } else {
                Popup(
                    popupPositionProvider = popupPositionProvider,
                    onDismissRequest = { expanded = false },
                ) {
                    RegisteredCourseFilterMenuSurface(
                        selectedFilter = selectedFilter,
                        onSelected = onSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisteredCourseFilterMenuSurface(
    selectedFilter: RegisteredCourseFilter,
    onSelected: (RegisteredCourseFilter) -> Unit,
) {
    val dividerColor = RodiTheme.colors.gray300
    Column(
        modifier = Modifier
            .width(75.dp)
            .background(RodiTheme.colors.white)
            .border(1.dp, RodiTheme.colors.gray300)
            .drawBehind {
                val dividerY = 35.dp.toPx()
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, dividerY),
                    end = Offset(size.width, dividerY),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, dividerY * 2),
                    end = Offset(size.width, dividerY * 2),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        listOf(
            RegisteredCourseFilter.APPROVED,
            RegisteredCourseFilter.PENDING,
            RegisteredCourseFilter.REJECTED,
        ).forEach { filter ->
            Text(
                text = filter.label,
                style = RodiTheme.typography.body2Medium,
                color = RodiTheme.colors.gray700,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .background(if (filter == selectedFilter) RodiTheme.colors.gray300 else RodiTheme.colors.white)
                    .clickable { onSelected(filter) }
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = 12.dp),
            )
        }
    }
}

private class RegisteredCourseFilterPopupPositionProvider(
    private val density: Density,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.right - with(density) { 16.dp.roundToPx() } - popupContentSize.width).coerceIn(
            0,
            (windowSize.width - popupContentSize.width).coerceAtLeast(0),
        )
        val y = (anchorBounds.top + with(density) { 28.dp.roundToPx() }).coerceIn(
            0,
            (windowSize.height - popupContentSize.height).coerceAtLeast(0),
        )
        return IntOffset(x, y)
    }
}

@Composable
private fun RegisteredCoursesLoading() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        repeat(5) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RodiSkeleton(modifier = Modifier.width(144.dp).height(18.dp))
                    Spacer(Modifier.weight(1f))
                    RodiSkeleton(modifier = Modifier.size(28.dp), shape = RoundedCornerShape(14.dp))
                }
                RodiSkeleton(modifier = Modifier.padding(top = 8.dp).width(72.dp).height(13.dp))
                RodiSkeleton(
                    modifier = Modifier.padding(top = 12.dp).width(60.dp).height(22.dp),
                    shape = RoundedCornerShape(12.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = RodiTheme.colors.gray100)
            }
        }
    }
}

@Composable
private fun RegisteredCoursesError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray700)
            RodiButton(
                text = "다시 시도",
                onClick = onRetry,
                fillMaxWidth = false,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun RegisteredCoursesAppendError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = RodiTheme.typography.caption1Medium, color = RodiTheme.colors.gray600)
        RodiButton(
            text = "다시 시도",
            onClick = onRetry,
            fillMaxWidth = false,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun RegisteredCoursesEmpty(
    filter: RegisteredCourseFilter,
    onRegisterCourseClick: () -> Unit,
) {
    val isAll = filter == RegisteredCourseFilter.ALL
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (isAll) 134.dp else 172.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.width(203.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.illust_registered_course_empty),
                contentDescription = null,
                modifier = Modifier.width(125.dp).height(50.dp),
            )
            Text(
                text = when (filter) {
                    RegisteredCourseFilter.ALL -> "아직 등록한 코스가 없어요!"
                    RegisteredCourseFilter.APPROVED -> "승인된 코스가 없어요!"
                    RegisteredCourseFilter.PENDING -> "검토중인 코스가 없어요!"
                    RegisteredCourseFilter.REJECTED -> "반려된 코스가 없어요!"
                },
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.gray600,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                textAlign = TextAlign.Center,
            )
            if (isAll) {
                Text(
                    text = "나만 알고 있는 운전 연습하기 좋은 코스를 공유해보세요.",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray600,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(
                    onClick = onRegisterCourseClick,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(147.dp)
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, RodiTheme.colors.primary600),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RodiTheme.colors.primary600,
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = "코스 등록하기",
                        style = RodiTheme.typography.body3Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisteredCourseRow(
    course: RegisteredCourse,
    menuExpanded: Boolean,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onDelete: () -> Unit,
    scrollState: ScrollableState,
    isDeleting: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = course.name,
                style = RodiTheme.typography.body2SemiBold,
                color = RodiTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clearAndSetSemantics { contentDescription = "더보기" }
                    .clickable(enabled = !isDeleting, onClick = onMenuClick),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(RodiTheme.colors.gray200, CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_horizontal),
                        contentDescription = null,
                        tint = RodiTheme.colors.gray600,
                        modifier = Modifier.size(18.dp),
                    )
                    RegisteredCoursePopupMenu(
                        expanded = menuExpanded,
                        onDelete = onDelete,
                        onDismissRequest = onMenuDismiss,
                        scrollState = scrollState,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RegisteredCourseStatusChip(course.approvalStatus)
            Text(
                text = "･",
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray600,
            )
            Text(
                text = RegisteredCourseDateFormatter.format(course.createdAt),
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.gray600,
            )
            if (isDeleting) {
                RodiSkeleton(
                    modifier = Modifier.size(16.dp),
                    shape = CircleShape,
                    color = RodiTheme.colors.gray200,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 24.dp), color = RodiTheme.colors.gray100)
    }
}

@Composable
private fun RegisteredCourseStatusChip(status: CourseApprovalStatus) {
    val (label, background, foreground) = when (status) {
        CourseApprovalStatus.APPROVED -> Triple("승인", RodiTheme.colors.primary50, RodiTheme.colors.primary600)
        CourseApprovalStatus.PENDING -> Triple("검토중", RodiTheme.colors.gray400, RodiTheme.colors.gray50)
        CourseApprovalStatus.REJECTED -> Triple("반려", RodiTheme.colors.secondary50, RodiTheme.colors.pointRed)
    }
    Text(
        text = label,
        style = RodiTheme.typography.caption1Medium,
        color = foreground,
        modifier = Modifier.background(background, RoundedCornerShape(2.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun RegisteredCoursePopupMenu(
    expanded: Boolean,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
    scrollState: ScrollableState,
) {
    if (!expanded) return
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { it }
            .collect { onDismissRequest() }
    }
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .offset(x = (-57).dp, y = 18.dp)
                .requiredWidth(75.dp),
        ) {
            RegisteredCoursePopupSurface(onDelete = onDelete)
        }
    } else {
        Popup(
            popupPositionProvider = RegisteredCoursePopupPositionProvider,
            onDismissRequest = onDismissRequest,
        ) {
            RegisteredCoursePopupSurface(onDelete = onDelete)
        }
    }
}

@Composable
private fun RegisteredCoursePopupSurface(onDelete: () -> Unit) {
    val shape = RoundedCornerShape(2.dp)
    Text(
        text = "삭제하기",
        style = RodiTheme.typography.body2Medium,
        color = RodiTheme.colors.gray700,
        modifier = Modifier
            .width(75.dp)
            .height(35.dp)
            .clip(shape)
            .background(RodiTheme.colors.white, shape)
            .border(1.dp, RodiTheme.colors.gray300, shape)
            .clickable(onClick = onDelete)
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = 12.dp),
    )
}

private object RegisteredCoursePopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.right - popupContentSize.width
        } else {
            anchorBounds.left
        }
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x.coerceIn(0, maxX), anchorBounds.bottom.coerceIn(0, maxY))
    }
}

private val RegisteredCourseDateFormatter = DateTimeFormatter.ofPattern("yy.MM.dd").withZone(ZoneId.systemDefault())

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
