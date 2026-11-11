package com.dororong.rodi.feature.home.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.feature.home.detail.components.BookmarkButton
import com.dororong.rodi.feature.home.detail.components.CourseDetailContent
import com.dororong.rodi.feature.home.layoutHeightPx
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal enum class CourseSheetAnchor { Dismissed, Collapsed, Expanded }

private val HandleHeight = 24.dp
private val TopBarHeight = 56.dp

/**
 * 코스 상세 바텀시트. 당겨 올리면 전체 화면으로 확장된다.
 *
 * 앵커 값은 **시트 상단의 y좌표**(컨테이너 최상단 기준 px)다. 내용은 컨테이너 높이로
 * 한 번 측정하고, 드래그 중에는 시트 위치와 보이는 본문 영역만 바꾼다.
 */
@Composable
fun CourseDetailSheet(
    place: PlaceDetail,
    isBookmarkUpdating: Boolean,
    onDismiss: () -> Unit,
    onBookmarkClick: () -> Unit,
    onNavigate: () -> Unit,
    onSheetHeightChanged: (Int) -> Unit,
    reviewContent: @Composable (ScrollState) -> Unit,
    modifier: Modifier = Modifier,
    onReportClick: () -> Unit = {},
) {
    if (place.course == null) {
        LaunchedEffect(place.id) { onDismiss() }
        return
    }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val sheetState = remember(place.id) { AnchoredDraggableState(CourseSheetAnchor.Collapsed) }
    var anchorsInitialized by remember(place.id) { mutableStateOf(false) }

    var containerHeightPx by remember { mutableIntStateOf(0) }
    var summaryHeightPx by remember { mutableIntStateOf(0) }
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }

    val handleHeightPx = with(density) { HandleHeight.roundToPx() }
    val expandedTopBarHeightPx = with(density) { TopBarHeight.roundToPx() } +
        WindowInsets.statusBars.getTop(density)
    val collapsedHeightPx = handleHeightPx + summaryHeightPx + bottomBarHeightPx
    val anchorsReady = containerHeightPx > 0 && summaryHeightPx > 0 && bottomBarHeightPx > 0

    LaunchedEffect(anchorsReady) {
        if (!anchorsReady || anchorsInitialized) return@LaunchedEffect
        sheetState.updateAnchors(
            DraggableAnchors {
                CourseSheetAnchor.Expanded at 0f
                CourseSheetAnchor.Collapsed at
                    (containerHeightPx - collapsedHeightPx).coerceAtLeast(0).toFloat()
                CourseSheetAnchor.Dismissed at containerHeightPx.toFloat()
            },
        )
        anchorsInitialized = true
    }

    // 지도 카메라 패딩은 LaunchedEffect 키로 쓰여 매 프레임 갱신하면 카메라가 튄다.
    // 드래그가 끝나 정착했을 때만 보고한다.
    LaunchedEffect(sheetState, containerHeightPx, collapsedHeightPx, anchorsReady) {
        if (!anchorsReady) return@LaunchedEffect
        snapshotFlow { sheetState.settledValue }.collect { settled ->
            val height = when (settled) {
                CourseSheetAnchor.Expanded -> containerHeightPx
                CourseSheetAnchor.Collapsed -> collapsedHeightPx
                CourseSheetAnchor.Dismissed -> 0
            }
            onSheetHeightChanged(height)
            if (settled == CourseSheetAnchor.Dismissed) onDismiss()
        }
    }

    val sheetOffsetPx: () -> Int = {
        val rawOffset = sheetState.offset
        if (!anchorsReady || rawOffset.isNaN()) {
            containerHeightPx
        } else {
            rawOffset.roundToInt().coerceIn(0, containerHeightPx)
        }
    }

    // 드래그 중 currentValue가 앵커를 통과해 바뀌므로, 정착 전까지 시트 레이아웃을 유지한다.
    val isExpanded = sheetState.settledValue == CourseSheetAnchor.Expanded
    val expansionProgress: () -> Float = {
        val collapsedOffset = (containerHeightPx - collapsedHeightPx).coerceAtLeast(0)
        val offset = sheetOffsetPx()
        if (!anchorsReady || collapsedOffset == 0) {
            if (offset == 0) 1f else 0f
        } else {
            ((collapsedOffset - offset).toFloat() / collapsedOffset).coerceIn(0f, 1f)
        }
    }
    val topBarHeightPx: () -> Int = {
        val progress = expansionProgress()
        (handleHeightPx + (expandedTopBarHeightPx - handleHeightPx) * progress).roundToInt()
    }
    val showBottomBar = anchorsReady &&
        sheetState.currentValue != CourseSheetAnchor.Dismissed &&
        sheetState.targetValue != CourseSheetAnchor.Dismissed
    BackHandler(enabled = isExpanded) {
        scope.launch { sheetState.animateTo(CourseSheetAnchor.Collapsed) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerHeightPx = it.height },
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
                .offset { IntOffset(0, sheetOffsetPx()) },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = RodiTheme.colors.white,
            shadowElevation = 8.dp,
        ) {
            Box(Modifier.fillMaxSize().clipToBounds()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .sheetContentViewport(
                            topBarHeightPx = topBarHeightPx,
                            bottomBarHeightPx = bottomBarHeightPx,
                        )
                        .verticalScroll(scroll, enabled = isExpanded),
                ) {
                    CourseDetailContent(
                        place = place,
                        onDismiss = onDismiss,
                        reviewContent = { reviewContent(scroll) },
                        showCloseButton = !isExpanded,
                        closeButtonAlpha = { 1f - expansionProgress() },
                        onSummaryHeightChanged = { height ->
                            if (!anchorsInitialized) summaryHeightPx = height
                        },
                    )
                }
                SheetTopBar(
                    expansionProgress = expansionProgress,
                    expandedHeightPx = expandedTopBarHeightPx,
                    isExpanded = isExpanded,
                    onCollapse = { scope.launch { sheetState.animateTo(CourseSheetAnchor.Collapsed) } },
                    onReportClick = onReportClick,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .anchoredDraggable(
                            state = sheetState,
                            orientation = Orientation.Vertical,
                        ),
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .offset {
                            if (showBottomBar) {
                                IntOffset(0, -sheetOffsetPx())
                            } else {
                                IntOffset(0, containerHeightPx)
                            }
                        }
                        .graphicsLayer { alpha = if (showBottomBar) 1f else 0f }
                        .onSizeChanged { bottomBarHeightPx = it.height },
                    color = RodiTheme.colors.white,
                    shadowElevation = 4.dp,
                ) {
                    Column {
                        HorizontalDivider(color = RodiTheme.colors.gray100)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BookmarkButton(
                                isBookmarked = place.isBookmarked,
                                onClick = onBookmarkClick,
                                enabled = showBottomBar && !isBookmarkUpdating,
                            )
                            RodiButton(
                                text = "연습하러 가기",
                                onClick = onNavigate,
                                enabled = showBottomBar,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.sheetContentViewport(
    topBarHeightPx: () -> Int,
    bottomBarHeightPx: Int,
): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    val topInset = topBarHeightPx().coerceIn(0, constraints.maxHeight)
    val bottomInset = bottomBarHeightPx.coerceIn(0, constraints.maxHeight - topInset)
    val viewportHeight = constraints.maxHeight - topInset - bottomInset
    val placeable = measurable.measure(
        constraints.copy(
            minHeight = viewportHeight,
            maxHeight = viewportHeight,
        ),
    )

    layout(placeable.width, constraints.maxHeight) {
        placeable.placeRelative(0, topInset)
    }
}

@Composable
private fun SheetTopBar(
    expansionProgress: () -> Float,
    expandedHeightPx: Int,
    isExpanded: Boolean,
    onCollapse: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(isExpanded) { if (!isExpanded) isMenuOpen = false }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .layoutHeightPx {
                val progress = expansionProgress()
                val collapsedHeight = HandleHeight.toPx()
                collapsedHeight + (expandedHeightPx - collapsedHeight) * progress
            }
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HandleHeight)
                .graphicsLayer { alpha = 1f - expansionProgress() },
            contentAlignment = Alignment.Center,
        ) {
            Spacer(
                Modifier
                    .size(width = 60.dp, height = 4.dp)
                    .background(RodiTheme.colors.handleBar, RoundedCornerShape(100.dp)),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .graphicsLayer { alpha = expansionProgress() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TopBarHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RodiIconButton(
                    painter = painterResource(com.dororong.rodi.core.ui.R.drawable.ic_chevron_left),
                    onClick = onCollapse,
                    contentDescription = "접기",
                    tint = RodiTheme.colors.black,
                    enabled = isExpanded,
                    modifier = Modifier.padding(start = 16.dp),
                )
                Spacer(Modifier.weight(1f))
                Box(Modifier.padding(end = 16.dp)) {
                    RodiIconButton(
                        painter = painterResource(R.drawable.ic_more_horizontal),
                        onClick = { isMenuOpen = true },
                        contentDescription = "코스 더보기",
                        tint = RodiTheme.colors.black,
                        enabled = isExpanded,
                    )
                    CourseMoreMenu(
                        expanded = isMenuOpen,
                        onReportClick = {
                            isMenuOpen = false
                            onReportClick()
                        },
                        onDismissRequest = { isMenuOpen = false },
                    )
                }
            }
        }
    }
}

/** 더보기 아이콘 바로 아래에, 오른쪽 끝을 아이콘보다 조금 바깥으로 맞춰 띄운다. */
@Composable
private fun CourseMoreMenu(
    expanded: Boolean,
    onReportClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (!expanded) return
    if (LocalInspectionMode.current) {
        CourseMoreMenuContent(onReportClick)
        return
    }
    val offset = with(LocalDensity.current) { IntOffset(MenuEndOverhang.roundToPx(), MoreIconSize.roundToPx()) }
    Popup(
        alignment = Alignment.TopEnd,
        offset = offset,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        CourseMoreMenuContent(onReportClick)
    }
}

@Composable
private fun CourseMoreMenuContent(onReportClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = Modifier
            .size(width = 94.dp, height = 42.dp)
            .clip(shape)
            .clickable(onClick = onReportClick),
        shape = shape,
        color = RodiTheme.colors.white,
        border = BorderStroke(1.dp, RodiTheme.colors.gray300),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "신고하기",
                style = RodiTheme.typography.button1,
                color = RodiTheme.colors.gray800,
            )
        }
    }
}

private val MoreIconSize = 24.dp
private val MenuEndOverhang = 4.dp

@Preview(name = "코스 상세 더보기 메뉴", showBackground = true, widthDp = 160)
@Composable
private fun CourseMoreMenuPreview() {
    RodiTheme { CourseMoreMenuContent(onReportClick = {}) }
}

@Preview(name = "코스 상세 시트 - collapsed", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseDetailSheetCollapsedPreview() {
    RodiTheme {
        CourseDetailSheet(
            place = HomePreviewData.courseDetail,
            isBookmarkUpdating = false,
            onDismiss = {},
            onBookmarkClick = {},
            onNavigate = {},
            onSheetHeightChanged = {},
            reviewContent = {},
        )
    }
}

@Preview(name = "코스 상세 시트 - 저장됨", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseDetailSheetBookmarkedPreview() {
    RodiTheme {
        CourseDetailSheet(
            place = HomePreviewData.courseDetail.copy(isBookmarked = true),
            isBookmarkUpdating = false,
            onDismiss = {},
            onBookmarkClick = {},
            onNavigate = {},
            onSheetHeightChanged = {},
            reviewContent = {},
        )
    }
}
