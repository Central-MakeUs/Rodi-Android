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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.dororong.rodi.feature.home.detail.components.BookmarkButton
import com.dororong.rodi.feature.home.detail.components.CourseDetailContent
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal enum class CourseSheetAnchor { Dismissed, Collapsed, Expanded }

private val HandleHeight = 24.dp
private val TopBarHeight = 56.dp

/**
 * 코스 상세 바텀시트. 당겨 올리면 전체 화면으로 확장된다.
 *
 * 앵커 값은 **시트 상단의 y좌표**(컨테이너 최상단 기준 px)이고, 시트 높이를
 * `컨테이너 높이 - 앵커값`으로 계산해 아래에 붙인다. offset으로 밀지 않는 이유는
 * collapsed에서도 하단 버튼 바가 화면 안에 남아야 하기 때문이다.
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

    var containerHeightPx by remember { mutableIntStateOf(0) }
    var summaryHeightPx by remember { mutableIntStateOf(0) }
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }

    val handleHeightPx = with(density) { HandleHeight.roundToPx() }
    val collapsedHeightPx = handleHeightPx + summaryHeightPx + bottomBarHeightPx
    val anchorsReady = containerHeightPx > 0 && summaryHeightPx > 0 && bottomBarHeightPx > 0

    LaunchedEffect(containerHeightPx, collapsedHeightPx, anchorsReady) {
        if (!anchorsReady) return@LaunchedEffect
        sheetState.updateAnchors(
            DraggableAnchors {
                CourseSheetAnchor.Expanded at 0f
                CourseSheetAnchor.Collapsed at
                    (containerHeightPx - collapsedHeightPx).coerceAtLeast(0).toFloat()
                CourseSheetAnchor.Dismissed at containerHeightPx.toFloat()
            },
        )
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

    val isExpanded = sheetState.currentValue == CourseSheetAnchor.Expanded
    BackHandler(enabled = isExpanded) {
        scope.launch { sheetState.animateTo(CourseSheetAnchor.Collapsed) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerHeightPx = it.height },
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .sheetHeight(sheetState, containerHeightPx, collapsedHeightPx, anchorsReady)
                .anchoredDraggable(sheetState, Orientation.Vertical, enabled = !isExpanded),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = RodiTheme.colors.white,
            shadowElevation = 8.dp,
        ) {
            Column {
                SheetTopBar(
                    isExpanded = isExpanded,
                    onCollapse = { scope.launch { sheetState.animateTo(CourseSheetAnchor.Collapsed) } },
                    modifier = Modifier.anchoredDraggable(
                        state = sheetState,
                        orientation = Orientation.Vertical,
                        enabled = !isExpanded,
                    ),
                )
                Column(
                    modifier = Modifier
                        // 앵커가 잡히기 전에는 내용 높이대로 재야 첫 프레임이 전체화면으로 번쩍이지 않는다.
                        .then(if (anchorsReady) Modifier.weight(1f) else Modifier)
                        .verticalScroll(scroll, enabled = isExpanded),
                ) {
                    CourseDetailContent(
                        place = place,
                        onDismiss = onDismiss,
                        reviewContent = { reviewContent(scroll) },
                        showCloseButton = !isExpanded,
                        onSummaryHeightChanged = { summaryHeightPx = it },
                    )
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { bottomBarHeightPx = it.height },
                    color = RodiTheme.colors.white,
                    shadowElevation = 4.dp,
                ) {
                    Column {
                        HorizontalDivider(color = RodiTheme.colors.gray200)
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
                                enabled = !isBookmarkUpdating,
                            )
                            RodiButton(
                                text = "연습하러 가기",
                                onClick = onNavigate,
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
    isExpanded: Boolean,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isExpanded) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TopBarHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RodiIconButton(
                    painter = painterResource(com.dororong.rodi.core.ui.R.drawable.ic_chevron_left),
                    onClick = onCollapse,
                    iconSize = 20.dp,
                    contentDescription = "접기",
                    tint = RodiTheme.colors.black,
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(HandleHeight),
            contentAlignment = Alignment.Center,
        ) {
            Spacer(
                Modifier
                    .size(width = 60.dp, height = 4.dp)
                    .background(RodiTheme.colors.handleBar, RoundedCornerShape(100.dp)),
            )
        }
    }
}

/**
 * 시트 높이를 앵커 offset에서 계산한다. `layout` 안에서 offset을 읽어 드래그 중에는
 * 레이아웃만 다시 돌고 컴포지션은 발생하지 않는다.
 */
private fun Modifier.sheetHeight(
    sheetState: AnchoredDraggableState<CourseSheetAnchor>,
    containerHeightPx: Int,
    collapsedHeightPx: Int,
    anchorsReady: Boolean,
): Modifier = layout { measurable, constraints ->
    // updateAnchors는 LaunchedEffect에서 돈다. 측정이 그보다 먼저 올 수 있어 offset이 아직 NaN일 수
    // 있으므로 requireOffset()을 바로 부르면 안 된다(크래시). 확정 전에는 내용 높이 그대로 잰다.
    val rawOffset = sheetState.offset
    if (!anchorsReady || rawOffset.isNaN()) {
        val natural = measurable.measure(constraints.copy(minHeight = 0))
        return@layout layout(natural.width, natural.height) { natural.place(0, 0) }
    }
    val top = rawOffset.roundToInt().coerceIn(0, containerHeightPx)
    val height = (containerHeightPx - top).coerceAtLeast(0)
    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
    layout(placeable.width, height) { placeable.place(0, 0) }
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
