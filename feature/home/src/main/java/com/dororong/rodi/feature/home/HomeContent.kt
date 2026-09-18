package com.dororong.rodi.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.components.HomeSearchBar
import com.dororong.rodi.feature.home.components.MapListButton
import com.dororong.rodi.core.ui.components.map.MapLoadingScreen
import com.dororong.rodi.core.ui.components.map.MapNetworkErrorScreen
import com.dororong.rodi.feature.home.components.MapResearchButton
import com.dororong.rodi.feature.home.components.MyLocationButton
import com.dororong.rodi.feature.home.detail.CourseDetailSheet
import com.dororong.rodi.feature.home.detail.CourseReviewUiState
import com.dororong.rodi.feature.home.detail.components.LevelReviewSection
import com.dororong.rodi.feature.home.detail.components.ParkingDetailContent
import com.dororong.rodi.feature.home.detail.components.PlaceDetailLoading
import com.dororong.rodi.feature.home.list.components.PlaceEmptyContent
import com.dororong.rodi.feature.home.list.components.PlaceListContent
import com.dororong.rodi.feature.home.map.MapScreenState
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt
import com.dororong.rodi.core.ui.R as CoreUiR

private const val RESEARCH_BUTTON_FADE_IN_MILLIS = 150
private const val RESEARCH_BUTTON_FADE_OUT_MILLIS = 100
private const val LIST_BUTTON_FADE_OUT_MILLIS = 100
private const val LIST_BUTTON_FADE_IN_DELAY_MILLIS = 100
private const val LIST_BUTTON_FADE_IN_MILLIS = 180
private val LIST_BUTTON_VISUAL_OFFSET = 7.dp
private val LIST_SHEET_CORNER_RADIUS = 20.dp
private val LIST_SHEET_SHADOW_ELEVATION = 8.dp
private val FULL_LIST_CONTENT_TOP_PADDING = 20.dp
private val PARTIAL_LIST_TITLE_TOP_PADDING = 24.dp
private val FULL_LIST_TITLE_TOP_PADDING = 40.dp
private val LIST_HEADER_HORIZONTAL_PADDING = 16.dp
private const val LIST_TITLE_CENTERING_START = 0.5f
// 주차장 상세는 내용 길이와 무관하게 코스 상세와 같은 높이로 고정한다.
private val PARKING_DETAIL_SHEET_HEIGHT = 400.dp
private val FILTER_HEADER_ICON_TOUCH_SIZE = 48.dp
internal val PARTIAL_LIST_HEADER_HEIGHT = 48.dp
internal val FULL_LIST_HEADER_HEIGHT = 64.dp

/**
 * 홈 화면의 상태·콜백만 받아 그리는 부분. 카카오맵 SDK·ViewModel·권한 런처는 [HomeScreen]이 쥐고,
 * 지도 뷰는 [mapView] 슬롯으로 받는다. 테스트에서 지도 없이 시트·뒤로가기 동작을 렌더링하기 위한 경계다.
 */
@Composable
internal fun HomeContent(
    state: HomeUiState,
    reviewState: CourseReviewUiState,
    overlay: HomeOverlayState,
    mapScreenState: MapScreenState,
    isAtCurrentLocation: Boolean,
    snackbarHostState: RodiSnackbarHostState,
    sheet: HomeContentSheetLayout,
    actions: HomeContentActions,
    onIntent: (HomeIntent) -> Unit,
    mapView: @Composable () -> Unit,
    bottomNavigation: @Composable () -> Unit,
) {
    val isEmptySheet = state.showEmpty || state.showInitialError
    val isContainerMeasured = sheet.isContainerMeasured
    val listSheetDrag = sheet.listSheetDrag
    val listSheetOffsetPx = sheet.listSheetOffsetPx
    val listSheetProgress = sheet.listSheetProgress
    val listHeaderHeightPx = sheet.listHeaderHeightPx
    val listViewportHeightPx = sheet.listViewportHeightPx
    val bottomControlOffsetPx = sheet.bottomControlOffsetPx
    val selectedDetailPlaceId = state.selectedPlace?.id
    val shouldShowResearch = state.surfaceState != HomeSurfaceState.Detail && state.isMapSearchDirty
    val showSearchBackButton = state.searchKeyword != null || state.detailOrigin == HomeDetailOrigin.List

    val handleSystemBack: () -> Unit = {
        if (state.isFilterSheetVisible) {
            if (!state.isFilterSaving) onIntent(HomeIntent.FilterDismissed)
        } else {
            when (state.surfaceState) {
                HomeSurfaceState.Detail -> actions.onDismissDetail()
                else -> onIntent(HomeIntent.ListCollapseRequested)
            }
        }
    }
    BackHandler(enabled = state.isFilterSheetVisible || state.surfaceState != HomeSurfaceState.Navigation) {
        handleSystemBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .onSizeChanged { actions.onContainerSizeChanged(it) },
            ) {
                Box(Modifier.fillMaxSize()) {
                        mapView()

                        HomeSearchBar(
                            onClick = {
                                if (showSearchBackButton) {
                                    handleSystemBack()
                                } else {
                                    actions.onSearchClick()
                                }
                            },
                            searchKeyword = state.searchKeyword,
                            showBackButton = showSearchBackButton,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 5.dp),
                        )

                        AnimatedVisibility(
                            visible = shouldShowResearch,
                            enter = fadeIn(tween(durationMillis = RESEARCH_BUTTON_FADE_IN_MILLIS)),
                            exit = fadeOut(tween(durationMillis = RESEARCH_BUTTON_FADE_OUT_MILLIS)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 63.dp),
                        ) {
                            MapResearchButton(onClick = actions.onResearchClick)
                        }

                        AnimatedVisibility(
                            visible = state.surfaceState == HomeSurfaceState.Navigation,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = LIST_BUTTON_FADE_IN_MILLIS,
                                    delayMillis = LIST_BUTTON_FADE_IN_DELAY_MILLIS,
                                ),
                            ),
                            exit = fadeOut(tween(durationMillis = LIST_BUTTON_FADE_OUT_MILLIS)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .offset { IntOffset(0, -bottomControlOffsetPx().roundToInt()) },
                        ) {
                            MapListButton(
                                onClick = { onIntent(HomeIntent.ListOpenClicked) },
                                modifier = Modifier.offset(y = LIST_BUTTON_VISUAL_OFFSET),
                            )
                        }

                        AnimatedVisibility(
                            visible = state.surfaceState != HomeSurfaceState.FullList &&
                                    (state.surfaceState != HomeSurfaceState.Detail ||
                                            state.selectedPlace?.type == PlaceType.PARKING),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(end = 12.dp)
                                .offset { IntOffset(0, -bottomControlOffsetPx().roundToInt()) },
                        ) {
                            MyLocationButton(
                                isActive = isAtCurrentLocation,
                                onClick = actions.onMyLocationClick,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .onSizeChanged { actions.onBottomNavigationHeightChanged(it.height) },
                        ) {
                            bottomNavigation()
                        }
                }

                // 앵커가 잡히기 전에는 offset이 NaN이라 시트가 펼쳐진 위치에 그려진다. 첫 측정 전까지 미룬다.
                if (isContainerMeasured) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(0, listSheetOffsetPx().roundToInt()) }
                            .graphicsLayer {
                                val radius = LIST_SHEET_CORNER_RADIUS.toPx() * (1f - listSheetProgress())
                                shape = RoundedCornerShape(topStart = radius, topEnd = radius)
                                clip = true
                                shadowElevation = LIST_SHEET_SHADOW_ELEVATION.toPx()
                            }
                            .background(RodiTheme.colors.white),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (!isEmptySheet) {
                                ListSheetHeader(
                                    expansionProgress = listSheetProgress,
                                    isExpanded = state.surfaceState == HomeSurfaceState.FullList,
                                    onBack = { onIntent(HomeIntent.ListCollapseRequested) },
                                    onFilterClick = { onIntent(HomeIntent.FilterOpened) },
                                    modifier = Modifier
                                        .layoutHeightPx { listHeaderHeightPx() }
                                        .then(listSheetDrag),
                                )
                            }
                            when {
                                state.listState == HomeListState.Loading ||
                                    state.listState == HomeListState.Idle -> PlaceListLoadingContent(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .layoutHeightPx { listViewportHeightPx() },
                                )

                                isEmptySheet -> PlaceEmptyContent(
                                    isInitialError = state.showInitialError,
                                    onRetry = actions.onRetryPlaces,
                                    dragHandleModifier = listSheetDrag,
                                )

                                else -> key(state.placeListGeneration) {
                                    PlaceListContent(
                                        places = state.places,
                                        onPlaceClick = {
                                            onIntent(HomeIntent.PlaceClicked(it, HomeDetailOrigin.List))
                                        },
                                        onLoadNextPage = { onIntent(HomeIntent.ListEndReached) },
                                        isNextPageLoading = state.isNextPageLoading,
                                        topContentPadding = {
                                            lerp(0.dp, FULL_LIST_CONTENT_TOP_PADDING, listSheetProgress())
                                        },
                                        modifier = Modifier.layoutHeightPx { listViewportHeightPx() },
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.surfaceState == HomeSurfaceState.Detail) {
                    val selectedPlace = state.selectedPlace
                    if (!state.isDetailLoading && selectedPlace?.type == PlaceType.COURSE) {
                        // 전체화면까지 확장되려면 화면 높이를 써야 해서 wrap-height Surface 밖에서 직접 그린다.
                        CourseDetailSheet(
                            place = selectedPlace,
                            isBookmarkUpdating = state.isBookmarkUpdating,
                            onDismiss = actions.onDismissDetail,
                            onBookmarkClick = { onIntent(HomeIntent.BookmarkClicked) },
                            onNavigate = actions.onNavigate,
                            onSheetHeightChanged = actions.onCourseDetailSheetHeightChanged,
                            reviewContent = { sheetScrollState ->
                                LaunchedEffect(selectedPlace.id) { actions.onLoadReviews(selectedPlace.id) }
                                if (!reviewState.isGuest) {
                                    LevelReviewSection(
                                        totalCount = reviewState.totalCount,
                                        recommendCount = reviewState.recommendCount,
                                        selectedLevel = reviewState.selectedLevel,
                                        difficultyCounts = reviewState.difficultyCounts,
                                        review = reviewState.latestReviews.firstOrNull(),
                                        onSelectLevel = actions.onSelectReviewLevel,
                                        onAllClick = { onIntent(HomeIntent.LevelReviewsOpened) },
                                        onWriteReviewClick = { overlay.reviewToWrite = ReviewWriteTarget(selectedPlace.id, selectedPlace.name, null) },
                                        onEditReviewClick = { overlay.reviewToWrite = ReviewWriteTarget(selectedPlace.id, selectedPlace.name, it) },
                                        onDeleteReviewClick = { overlay.reviewToDelete = it },
                                        onReportReviewClick = overlay::requestReport,
                                        onBlockMemberClick = overlay::requestBlock,
                                        scrollState = sheetScrollState,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        // 상세가 바뀔 때마다 새 상태를 만들어 항상 Visible(offset 0)에서 시작하게 한다.
                        // 하나를 재사용하면 드래그로 닫은 뒤 다음 상세가 화면 밖 오프셋에서 열린다.
                        val detailSheetState = remember(selectedDetailPlaceId) {
                            AnchoredDraggableState(DetailSheetValue.Visible)
                        }
                        val detailSheetDrag = Modifier.anchoredDraggable(
                            state = detailSheetState,
                            orientation = Orientation.Vertical,
                            flingBehavior = AnchoredDraggableDefaults.flingBehavior(detailSheetState),
                        )
                        LaunchedEffect(detailSheetState) {
                            snapshotFlow { detailSheetState.settledValue }
                                .drop(1)
                                .collect { if (it == DetailSheetValue.Dismissed) actions.onDragDismissDetail() }
                        }
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .offset {
                                    IntOffset(0, detailSheetState.offset.takeIf { !it.isNaN() }?.roundToInt() ?: 0)
                                }
                                .onSizeChanged { detailSheetState.updateAnchors(detailSheetAnchors(it.height)) }
                                .then(
                                    if (selectedPlace?.type == PlaceType.PARKING) {
                                        Modifier
                                            .height(PARKING_DETAIL_SHEET_HEIGHT)
                                            .onSizeChanged { size ->
                                                selectedDetailPlaceId?.let { placeId ->
                                                    actions.onParkingSheetMeasured(placeId, size.height)
                                                }
                                            }
                                    } else {
                                        Modifier
                                    },
                                ),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            color = RodiTheme.colors.white,
                            shadowElevation = 8.dp,
                        ) {
                            when {
                                state.isDetailLoading -> PlaceDetailLoading(
                                    dragHandleModifier = detailSheetDrag,
                                )

                                selectedPlace?.type == PlaceType.PARKING -> ParkingDetailContent(
                                    place = selectedPlace,
                                    isBookmarkUpdating = state.isBookmarkUpdating,
                                    onDismiss = actions.onDismissDetail,
                                    dragHandleModifier = detailSheetDrag,
                                    onBookmarkClick = { onIntent(HomeIntent.BookmarkClicked) },
                                    onNavigate = actions.onNavigate,
                                )
                            }
                        }
                    }
                }

                when (mapScreenState) {
                    MapScreenState.Loading -> MapLoadingScreen()
                    MapScreenState.NetworkError -> MapNetworkErrorScreen()
                    MapScreenState.Error -> HomeMapErrorOverlay(
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = actions.onRetryMap,
                    )
                    MapScreenState.Ready -> Unit
                }

                RodiSnackbarHost(
                    state = snackbarHostState,
                    bottomPadding = if (mapScreenState == MapScreenState.NetworkError) 20.dp else 114.dp,
                )
            }
        },
    )
}

/** 시트 앵커·오프셋 계산은 [HomeScreen]의 지도 이펙트와 공유하므로 그쪽에서 만들어 넘긴다. */
internal class HomeContentSheetLayout(
    val isContainerMeasured: Boolean,
    val listSheetDrag: Modifier,
    val listSheetOffsetPx: () -> Float,
    val listSheetProgress: () -> Float,
    val listHeaderHeightPx: Density.() -> Float,
    val listViewportHeightPx: Density.() -> Float,
    val bottomControlOffsetPx: Density.() -> Float,
)

internal class HomeContentActions(
    val onSearchClick: () -> Unit,
    val onResearchClick: () -> Unit,
    val onMyLocationClick: () -> Unit,
    val onRetryPlaces: () -> Unit,
    val onRetryMap: () -> Unit,
    val onDismissDetail: () -> Unit,
    val onDragDismissDetail: () -> Unit,
    val onNavigate: () -> Unit,
    val onLoadReviews: (placeId: Long) -> Unit,
    val onSelectReviewLevel: (OnboardingLevel) -> Unit,
    val onContainerSizeChanged: (IntSize) -> Unit,
    val onBottomNavigationHeightChanged: (Int) -> Unit,
    val onCourseDetailSheetHeightChanged: (Int) -> Unit,
    val onParkingSheetMeasured: (placeId: Long, heightPx: Int) -> Unit,
)

@Composable
private fun PlaceListLoadingContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PlaceListCourseLoadingItem()
        PlaceListCourseLoadingItem()
        PlaceListParkingLoadingItem()
    }
}

@Composable
private fun PlaceListCourseLoadingItem() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RodiSkeleton(modifier = Modifier.width(170.dp).height(20.dp))
            Spacer(Modifier.weight(1f))
            RodiSkeleton(modifier = Modifier.width(46.dp).height(16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RodiSkeleton(modifier = Modifier.width(42.dp).height(20.dp))
            RodiSkeleton(modifier = Modifier.width(48.dp).height(20.dp))
            RodiSkeleton(modifier = Modifier.width(40.dp).height(20.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(37.dp)
                .background(RodiTheme.colors.gray50, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            RodiSkeleton(modifier = Modifier.fillMaxWidth(0.76f).height(14.dp))
        }
    }
}

@Composable
private fun PlaceListParkingLoadingItem() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RodiSkeleton(modifier = Modifier.width(188.dp).height(20.dp))
        RodiSkeleton(modifier = Modifier.width(132.dp).height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RodiSkeleton(modifier = Modifier.width(42.dp).height(20.dp))
            RodiSkeleton(modifier = Modifier.width(116.dp).height(16.dp))
        }
        RodiSkeleton(modifier = Modifier.width(144.dp).height(16.dp))
    }
}

private fun titleCenteringProgress(expansionProgress: Float): Float =
    ((expansionProgress - LIST_TITLE_CENTERING_START) / (1f - LIST_TITLE_CENTERING_START))
        .coerceIn(0f, 1f)

@Composable
private fun ListSheetHeader(
    expansionProgress: () -> Float,
    isExpanded: Boolean,
    onBack: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(width = 60.dp, height = 4.dp)
                .graphicsLayer {
                    alpha = (1f - expansionProgress() / LIST_TITLE_CENTERING_START).coerceIn(0f, 1f)
                }
                .background(RodiTheme.colors.handleBar, RoundedCornerShape(2.dp)),
        )
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            contentDescription = "뒤로가기",
            tint = RodiTheme.colors.black,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = LIST_HEADER_HORIZONTAL_PADDING, top = FULL_LIST_TITLE_TOP_PADDING)
                .size(24.dp)
                .clip(CircleShape)
                .graphicsLayer { alpha = titleCenteringProgress(expansionProgress()) }
                .clickable(enabled = isExpanded, onClick = onBack),
        )
        FilterHeaderIconButton(
            painter = painterResource(R.drawable.ic_filter_top),
            iconSize = 24.dp,
            visualSize = 24.dp,
            visualBottomInset = 0.dp,
            backgroundColor = null,
            onClick = onFilterClick,
            enabled = isExpanded,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp, top = 16.dp)
                .graphicsLayer { alpha = titleCenteringProgress(expansionProgress()) }
        )
        FilterHeaderIconButton(
            painter = painterResource(R.drawable.ic_filter),
            iconSize = 16.dp,
            visualSize = 23.dp,
            visualBottomInset = 1.dp,
            backgroundColor = RodiTheme.colors.gray100,
            onClick = onFilterClick,
            enabled = !isExpanded,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp)
                .graphicsLayer { alpha = 1f - titleCenteringProgress(expansionProgress()) }
        )
        Text(
            text = "추천 목록",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = LIST_HEADER_HORIZONTAL_PADDING, top = PARTIAL_LIST_TITLE_TOP_PADDING)
                .graphicsLayer {
                    translationY =
                        (FULL_LIST_TITLE_TOP_PADDING - PARTIAL_LIST_TITLE_TOP_PADDING).toPx() * expansionProgress()
                }
                .layout { measurable, constraints ->
                    // 부모 폭은 여기서 직접 알 수 없지만, start 패딩만큼 줄어든 제약에서 되돌릴 수 있다.
                    val startPadding = LIST_HEADER_HORIZONTAL_PADDING.roundToPx()
                    val placeable = measurable.measure(constraints)
                    val centeredStart = (constraints.maxWidth + startPadding - placeable.width) / 2
                    val shift = (centeredStart - startPadding) *
                        titleCenteringProgress(expansionProgress())
                    layout(placeable.width, placeable.height) {
                        placeable.place(shift.roundToInt(), 0)
                    }
                },
        )
    }
}

@Composable
private fun FilterHeaderIconButton(
    painter: Painter,
    iconSize: Dp,
    visualSize: Dp,
    visualBottomInset: Dp,
    backgroundColor: Color?,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(FILTER_HEADER_ICON_TOUCH_SIZE)
            .semantics { contentDescription = "필터" }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -visualBottomInset)
                .size(visualSize)
                .clip(CircleShape)
                .then(backgroundColor?.let { Modifier.background(it, CircleShape) } ?: Modifier)
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, radius = visualSize / 2),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = RodiTheme.colors.black,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Preview(name = "List header - partial", showBackground = true, widthDp = 375, heightDp = 64)
@Composable
private fun PartialListHeaderPreview() {
    RodiTheme {
        Surface(color = RodiTheme.colors.white) {
            ListSheetHeader(
                expansionProgress = { 0f },
                isExpanded = false,
                onBack = {},
                onFilterClick = {},
                modifier = Modifier.height(PARTIAL_LIST_HEADER_HEIGHT),
            )
        }
    }
}

@Preview(name = "List header - full with filter", showBackground = true, widthDp = 375, heightDp = 80)
@Composable
private fun FullListHeaderPreview() {
    RodiTheme {
        Surface(color = RodiTheme.colors.white) {
            ListSheetHeader(
                expansionProgress = { 1f },
                isExpanded = true,
                onBack = {},
                onFilterClick = {},
                modifier = Modifier.height(FULL_LIST_HEADER_HEIGHT),
            )
        }
    }
}

@Composable
private fun HomeMapErrorOverlay(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(
        modifier = modifier
            .background(RodiTheme.colors.white, RoundedCornerShape(RodiRadius.md))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("지도를 불러오지 못했어요", style = RodiTheme.typography.body3SemiBold, color = RodiTheme.colors.black)
        Spacer(Modifier.height(12.dp))
        RodiButton(
            text = "다시 시도",
            onClick = onRetry,
            fillMaxWidth = false,
            modifier = Modifier.width(120.dp),
            height = 42.dp,
        )
    }
}
