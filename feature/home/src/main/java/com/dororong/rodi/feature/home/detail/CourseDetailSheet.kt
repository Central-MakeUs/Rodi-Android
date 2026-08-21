package com.dororong.rodi.feature.home.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
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
                        .offset { IntOffset(0, topBarHeightPx()) }
                        .verticalScroll(scroll, enabled = isExpanded)
                        .drawWithContent {
                            val visibleHeight = (
                                containerHeightPx -
                                    sheetOffsetPx().toFloat() -
                                    topBarHeightPx().toFloat() -
                                    bottomBarHeightPx
                            ).coerceIn(0f, size.height)
                            clipRect(
                                left = 0f,
                                top = 0f,
                                right = size.width,
                                bottom = visibleHeight,
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        },
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
                    // TODO(미해결): 스크롤 컨테이너가 .offset { topBarHeightPx() }로 화면상으로만
                    // 아래로 밀려 있어, 스크롤 가능 범위(scroll.maxValue)는 여전히 containerHeightPx
                    // 전체를 뷰포트로 보고 계산된다. 실제 눈에 보이는 영역(위 drawWithContent의
                    // clipRect)은 상단바·하단바를 뺀 만큼 더 작아서, 리뷰 카드 등 콘텐츠 끝부분이
                    // 스크롤 최대치에 도달하기 전에 clip에 잘려 흰 배경으로 보인다(코스 상세 →
                    // 레벨 드롭다운에서 후기가 있는 레벨 선택 → 아래로 스크롤 시 재현).
                    //
                    // 아래 Spacer는 그 부족분을 topBarHeightPx+bottomBarHeightPx만큼 보정하려
                    // 시도했지만 실기기(에뮬레이터) 확인 결과 그 값만으로는 부족했고(여전히 잘림),
                    // +300dp를 더하면 오히려 콘텐츠를 다 지나쳐 빈 공간까지 스크롤됐다. +100dp와
                    // +200dp 사이 어딘가가 맞는 값으로 보이나 정확한 값을 못 찾았다 — 아마
                    // topBarHeightPx()가 드래그 중 보간되는 값이라 이 시점에 정확한 상수가 아니거나,
                    // 다른 누락된 항이 있을 수 있다.
                    //
                    // 근본적으로는 Spacer로 보정하는 대신, 스크롤 콘텐츠에 실제
                    // contentPadding(top/bottom)을 줘서 scroll.maxValue 자체가 처음부터 올바르게
                    // 계산되도록 구조를 바꾸는 게 안전하다(offset+drawWithContent clip 조합 대신).
                    Spacer(
                        Modifier.height(
                            with(density) { (expandedTopBarHeightPx + bottomBarHeightPx).toDp() } + 200.dp,
                        ),
                    )
                }
                SheetTopBar(
                    expansionProgress = expansionProgress,
                    expandedHeightPx = expandedTopBarHeightPx,
                    isExpanded = isExpanded,
                    onCollapse = { scope.launch { sheetState.animateTo(CourseSheetAnchor.Collapsed) } },
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

@Composable
private fun SheetTopBar(
    expansionProgress: () -> Float,
    expandedHeightPx: Int,
    isExpanded: Boolean,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            }
        }
    }
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
