package com.dororong.rodi.feature.mypage.registeredcourses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.dororong.rodi.core.domain.model.course.CourseApprovalStatus
import com.dororong.rodi.core.domain.model.course.RegisteredCourse
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun RegisteredCourseRow(
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // 터치 영역을 48dp Box로 잡으면 그 높이가 그대로 행 높이가 돼서 제목-상태칩 간격과
            // 행 간격이 디자인보다 벌어진다. 레이아웃은 아이콘 크기(18dp)로 두고 터치만 넓힌다.
            Box(
                modifier = Modifier.size(18.dp),
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
                Box(
                    modifier = Modifier
                        .requiredSize(48.dp)
                        // 아이콘이 원형이라 리플도 원으로 잘라준다. 안 그러면 48dp 사각으로 번진다.
                        .clip(CircleShape)
                        .clearAndSetSemantics { contentDescription = "더보기" }
                        .clickable(enabled = !isDeleting, onClick = onMenuClick),
                )
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
internal fun RegisteredCourseStatusChip(status: CourseApprovalStatus) {
    val (label, background, foreground) = when (status) {
        CourseApprovalStatus.APPROVED -> Triple("승인", RodiTheme.colors.infoBgMint, RodiTheme.colors.infoApproval)
        CourseApprovalStatus.PENDING -> Triple("검토중", RodiTheme.colors.gray400, RodiTheme.colors.gray50)
        CourseApprovalStatus.REJECTED -> Triple("반려", RodiTheme.colors.infoBgPink, RodiTheme.colors.infoCancel)
    }
    Text(
        text = label,
        style = RodiTheme.typography.caption1Medium,
        color = foreground,
        modifier = Modifier.background(background, RoundedCornerShape(2.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
internal fun RegisteredCoursePopupMenu(
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
internal fun RegisteredCoursePopupSurface(onDelete: () -> Unit) {
    val shape = RoundedCornerShape(2.dp)
    // 디자인(3659:78807)은 흰 배경 + gray300 테두리에 글자만큼만 넓어지는 상자다.
    // 폭을 고정하면 본문 폰트에서 "삭제하/기"로 줄이 깨져서 nowrap으로 둔다.
    Text(
        text = "삭제하기",
        style = RodiTheme.typography.body2Medium,
        color = RodiTheme.colors.gray700,
        softWrap = false,
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(RodiTheme.colors.white, shape)
            .border(1.dp, RodiTheme.colors.gray300, shape)
            .clickable(onClick = onDelete)
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
