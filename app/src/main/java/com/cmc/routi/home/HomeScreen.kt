package com.cmc.routi.home

import android.Manifest
import android.content.Intent
import com.kakao.vectormap.MapGravity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmc.routi.R
import com.cmc.routi.data.SampleCourses
import com.cmc.routi.directions.KakaoDirectionsClient.RouteResult
import com.cmc.routi.location.awaitCurrentLocation
import com.cmc.routi.location.hasLocationPermission
import com.cmc.routi.map.clearCourse
import com.cmc.routi.map.rememberMapViewWithLifecycle
import com.cmc.routi.map.renderCourse
import com.cmc.routi.model.Course
import com.cmc.routi.model.Difficulty
import com.cmc.routi.navi.KakaoMapLauncher
import com.cmc.routi.navi.KakaoNaviLauncher
import com.cmc.routi.navi.NaviApp
import com.cmc.routi.navi.NaviPreference
import com.cmc.routi.ui.theme.RoutiTheme
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import kotlinx.coroutines.launch

private const val DEFAULT_ZOOM = 13
private val SEOUL = LatLng.from(37.5563, 126.9220)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val mapView = rememberMapViewWithLifecycle()
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var naviCourse by remember { mutableStateOf<Course?>(null) }
    var isAtCurrentLocation by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> permissionGranted = result.values.any { it } }

    LaunchedEffect(Unit) {
        if (context.hasLocationPermission()) {
            currentLocation = context.awaitCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) currentLocation = context.awaitCurrentLocation()
    }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val peekHeight = maxOf(380.dp, screenHeightDp.dp * 0.468f)
    val density = LocalDensity.current
    val peekHeightPx = with(density) { peekHeight.roundToPx() }
    val logoMarginPx = with(density) { 8.dp.toPx() }
    val logoBottomPx = peekHeightPx + with(density) { 16.dp.toPx() }

    LaunchedEffect(kakaoMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        map.setOnCameraMoveStartListener { _, gestureType ->
            if (gestureType != GestureType.Unknown) {
                isAtCurrentLocation = false
            }
        }
        map.setOnCameraMoveEndListener { _, _, _ -> }
    }

    LaunchedEffect(kakaoMap, currentLocation) {
        val map = kakaoMap ?: return@LaunchedEffect
        map.setPadding(0, 0, 0, peekHeightPx)
        map.logo?.setPosition(
            MapGravity.BOTTOM or MapGravity.LEFT,
            logoMarginPx,
            logoMarginPx,
        )
        if (state.selectedCourseId == null) {
            map.moveCamera(CameraUpdateFactory.newCenterPosition(currentLocation ?: SEOUL, DEFAULT_ZOOM))
        }
    }

    LaunchedEffect(kakaoMap, state.selectedCourseId, state.selectedRoute) {
        val map = kakaoMap ?: return@LaunchedEffect
        val course = state.selectedCourse
        if (course == null) {
            map.clearCourse()
        } else {
            val route = state.selectedRoute
            map.renderCourse(context, course, route?.points, route?.snappedPoints ?: emptyList())
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true,
        ),
    )

    // 코스 선택 시 시트 펼침
    LaunchedEffect(state.selectedCourseId) {
        if (state.selectedCourseId != null) {
            scaffoldState.bottomSheetState.expand()
        }
    }

    // 바텀시트 펼쳐진 상태에서 뒤로가기 → 기본 형태(PartiallyExpanded)로 복귀
    BackHandler(enabled = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
        if (state.selectedCourseId != null) vm.onDismissDetail()
        coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
    }

    val sheetOffsetPx by remember {
        derivedStateOf {
            try {
                scaffoldState.bottomSheetState.requireOffset()
            } catch (_: IllegalStateException) {
                Float.MAX_VALUE
            }
        }
    }

    var scaffoldHeightPx by remember { mutableIntStateOf(0) }

    val expandFraction by remember {
        derivedStateOf {
            if (sheetOffsetPx == Float.MAX_VALUE || scaffoldHeightPx == 0) return@derivedStateOf 0f
            val partialOffsetPx = (scaffoldHeightPx - peekHeightPx).toFloat()
            if (partialOffsetPx <= 0f) return@derivedStateOf 0f
            1f - (sheetOffsetPx / partialOffsetPx).coerceIn(0f, 1f)
        }
    }

    // 리스트 모드 풀스크린 시에만 코너 0dp로 자연스럽게 전환 (리컴포지션 없이 Shape 내부에서 계산)
    val sheetShape = remember(density) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val offset = sheetOffsetPx
                val radiusPx = if (
                    state.selectedCourse == null && 
                    offset != Float.MAX_VALUE && 
                    scaffoldHeightPx > 0
                ) {
                    // 최상단 150px 구간에서 20dp -> 0dp로 부드럽게 펴짐
                    val fraction = (offset / 150f).coerceIn(0f, 1f)
                    with(density) { (20f * fraction).dp.toPx() }
                } else {
                    with(density) { 20.dp.toPx() }
                }
                return Outline.Rounded(
                    RoundRect(
                        rect = Rect(0f, 0f, size.width, size.height),
                        topLeft = CornerRadius(radiusPx, radiusPx),
                        topRight = CornerRadius(radiusPx, radiusPx)
                    )
                )
            }
        }
    }

    BottomSheetScaffold(
        modifier = Modifier.onGloballyPositioned { scaffoldHeightPx = it.size.height },
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetContainerColor = RoutiTheme.colors.white,
        sheetShadowElevation = 8.dp,
        sheetShape = sheetShape,
        sheetSwipeEnabled = false,
        sheetDragHandle = null,
        sheetContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 커스텀 드래그 핸들
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                        .draggable(
                            state = rememberDraggableState { },
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity ->
                                if (velocity < -200f) scaffoldState.bottomSheetState.expand()
                                else if (velocity > 200f) scaffoldState.bottomSheetState.partialExpand()
                            },
                        )
                        .graphicsLayer {
                            val offset = sheetOffsetPx
                            alpha = if (offset != Float.MAX_VALUE && scaffoldHeightPx > 0) {
                                (offset / 150f).coerceIn(0f, 1f)
                            } else {
                                1f
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(RoutiTheme.colors.handleBar),
                    )
                }
            val handleHeightDp = 24.dp
            val handleHeightPx = with(density) { handleHeightDp.toPx() }
            val scaffoldHeightDp = with(density) { scaffoldHeightPx.toDp() }
            val boxHeightDp = if (scaffoldHeightDp > 0.dp) scaffoldHeightDp - handleHeightDp else Dp.Unspecified

            val visibleHeightDp = with(density) {
                if (sheetOffsetPx != Float.MAX_VALUE && scaffoldHeightPx > 0) {
                    maxOf(0f, scaffoldHeightPx - sheetOffsetPx - handleHeightPx).toDp()
                } else {
                    Dp.Unspecified
                }
            }

            val selectedCourse = state.selectedCourse
            if (selectedCourse == null) {
                val listState = rememberLazyListState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (boxHeightDp != Dp.Unspecified) boxHeightDp else Dp.Unspecified)
                ) {
                    CourseListContent(
                        courses = state.courses,
                        onCourseClick = { id ->
                            vm.onCourseClick(id)
                            isAtCurrentLocation = false
                        },
                        expandFraction = expandFraction,
                        onCollapse = { coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() } },
                        listState = listState,
                        modifier = if (visibleHeightDp != Dp.Unspecified) Modifier.height(visibleHeightDp) else Modifier
                    )
                }
            } else {
                CourseDetailContent(
                            course = selectedCourse,
                            route = state.selectedRoute,
                            isRouting = state.isRouting,
                            onDismiss = {
                                vm.onDismissDetail()
                                coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
                            },
                        onNavigate = {
                            val saved = NaviPreference.getAlways(context)
                            val kakaoMapInstalled = runCatching {
                                context.packageManager.getPackageInfo("net.daum.android.map", 0); true
                            }.getOrDefault(false)
                            val kakaoNaviInstalled = runCatching {
                                context.packageManager.getPackageInfo("com.locnall.KimGiSa", 0); true
                            }.getOrDefault(false)
                            when {
                                saved == NaviApp.KAKAOMAP && kakaoMapInstalled ->
                                    KakaoMapLauncher.launch(context, selectedCourse)

                                saved == NaviApp.KAKAONAVI && kakaoNaviInstalled ->
                                    KakaoNaviLauncher.launch(context, selectedCourse)

                                kakaoMapInstalled && kakaoNaviInstalled ->
                                    naviCourse = selectedCourse

                                kakaoMapInstalled ->
                                    KakaoMapLauncher.launch(context, selectedCourse)

                                kakaoNaviInstalled ->
                                    KakaoNaviLauncher.launch(context, selectedCourse)

                                else -> context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "market://details?id=net.daum.android.map".toUri(),
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                    )
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {}
                            override fun onMapError(error: Exception?) {}
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(map: KakaoMap) {
                                kakaoMap = map
                            }

                            override fun getPosition(): LatLng = SEOUL
                            override fun getZoomLevel(): Int = DEFAULT_ZOOM
                        },
                    )
                    mapView
                },
            )

            // 현위치 버튼 — 시트 우상단 위 12dp에 부유
            if (sheetOffsetPx != Float.MAX_VALUE) {
                val buttonTopDp = with(density) { sheetOffsetPx.toDp() } - 40.dp - 12.dp
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp)
                        .absoluteOffset(y = buttonTopDp)
                        .size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MyLocationButton(
                        isActive = isAtCurrentLocation,
                        onClick = {
                            val loc = currentLocation ?: SEOUL
                            kakaoMap?.moveCamera(
                                CameraUpdateFactory.newCenterPosition(loc, DEFAULT_ZOOM),
                            )
                            isAtCurrentLocation = true
                        },
                    )
                }
            }
        }
    }

    naviCourse?.let { course ->
        NaviPickerSheet(
            onDismiss = { naviCourse = null },
            onSelect = { app, always ->
                if (always) NaviPreference.setAlways(context, app)
                when (app) {
                    NaviApp.KAKAOMAP -> KakaoMapLauncher.launch(context, course)
                    NaviApp.KAKAONAVI -> KakaoNaviLauncher.launch(context, course)
                }
                naviCourse = null
            },
        )
    }
}

@Composable
private fun MyLocationButton(isActive: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = RoutiTheme.colors.white,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_crosshair),
                contentDescription = "현재 위치",
                tint = if (isActive) RoutiTheme.colors.primary600 else RoutiTheme.colors.gray900,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun CourseListContent(
    courses: List<Course>,
    onCourseClick: (String) -> Unit,
    expandFraction: Float = 0f,
    onCollapse: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 피크: TopStart에 고정, 아래 20dp만 패딩
            Text(
                "연습코스",
                style = RoutiTheme.typography.headline1,
                color = RoutiTheme.colors.black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, bottom = 20.dp)
                    .graphicsLayer { alpha = 1f - expandFraction },
            )
            // 전체화면: 56dp 앱바 (expandFraction > 0 일 때만 레이아웃에 포함)
            if (expandFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer { alpha = expandFraction },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "연습코스",
                        style = RoutiTheme.typography.headline1,
                        color = RoutiTheme.colors.black,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .graphicsLayer { alpha = expandFraction },
                    ) {
                        IconButton(onClick = onCollapse, enabled = expandFraction > 0.5f) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_left),
                                contentDescription = "접기",
                                tint = RoutiTheme.colors.black,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 0.dp),
        ) {
            item {
                Spacer(Modifier.height(20.dp * expandFraction))
            }
            items(courses, key = { it.id }) { course ->
                CourseCard(course = course, onClick = { onCourseClick(course.id) })
                if (course != courses.last()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        thickness = 1.dp,
                        color = RoutiTheme.colors.primary100,
                    )
                }
            }
            item {
                Spacer(Modifier.navigationBarsPadding().height(8.dp))
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, onClick: () -> Unit) {
    var addressExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            course.title,
            style = RoutiTheme.typography.body1SemiBold,
            color = RoutiTheme.colors.black,
            maxLines = 1,
        )
        RatingRegionRow(
            rating = course.rating,
            region = course.region,
            onChevronClick = { addressExpanded = !addressExpanded },
        )
        if (addressExpanded) {
            ExpandableAddressCard(
                roadAddress = course.roadAddress,
                jibunAddress = course.jibunAddress,
            )
            Spacer(modifier = Modifier.height(0.5.dp))
        } else {
            TagRow(difficulty = course.difficulty, tags = course.tags)
            Spacer(modifier = Modifier.height(8.dp))
            SummaryBox(text = course.summary, bgColor = RoutiTheme.colors.gray50)
        }
    }
}

@Composable
private fun CourseDetailContent(
    course: Course,
    route: RouteResult?,
    isRouting: Boolean,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
) {
    var addressExpanded by rememberSaveable(course.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        // 제목 + ✕
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                course.title,
                style = RoutiTheme.typography.headline1,
                color = RoutiTheme.colors.black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = "닫기",
                    tint = RoutiTheme.colors.black,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RatingRegionRow(
                rating = course.rating,
                region = course.region,
                onChevronClick = { addressExpanded = !addressExpanded },
            )
            if (addressExpanded) {
                Spacer(modifier = Modifier.height(2.dp))
                ExpandableAddressCard(
                    roadAddress = course.roadAddress,
                    jibunAddress = course.jibunAddress,
                )
            } else {
                Text(
                    distanceText(route, isRouting),
                    style = RoutiTheme.typography.body3Medium,
                    color = RoutiTheme.colors.gray800,
                )
                TagRow(difficulty = course.difficulty, tags = course.tags)

                Spacer(Modifier.height(8.dp))

                SummaryBox(
                    text = course.summary,
                    bgColor = RoutiTheme.colors.gray100,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        VerticalStepList(
            course = course,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onNavigate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RoutiTheme.colors.primary600,
                contentColor = RoutiTheme.colors.white,
            ),
        ) {
            Text("경로 안내", style = RoutiTheme.typography.button1)
        }
    }
}

@Composable
private fun RatingRegionRow(
    rating: Double,
    region: String,
    onChevronClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_star),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "%.1f".format(rating),
            style = RoutiTheme.typography.body3Medium,
            color = RoutiTheme.colors.primary600,
        )
        Text(" ･ ", style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray800)
        Row(
            modifier = Modifier.clickable(onClick = onChevronClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(region, style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray800)
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = "주소 보기",
                tint = RoutiTheme.colors.gray800,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ExpandableAddressCard(
    roadAddress: String,
    jibunAddress: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(width = 1.dp, color = RoutiTheme.colors.primary200),
        color = RoutiTheme.colors.primary50,
    ) {
        Column(
            modifier = Modifier.padding(top = 10.dp, bottom = 11.dp, start = 10.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("도로명", style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray600)
                Text(roadAddress, style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.black)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("지번", style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray600)
                Text(jibunAddress, style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray700)
            }
        }
    }
}

@Composable
private fun TagRow(difficulty: Difficulty, tags: Set<Any>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DifficultyTag(difficulty)
        tags.take(2).filterIsInstance<com.cmc.routi.model.PracticeTag>().forEach { tag ->
            PracticeTagChip(tag.label)
        }
    }
}

@Composable
private fun DifficultyTag(difficulty: Difficulty) {
    // TODO: 색상 확정 후 변경
    val bgColor = when (difficulty) {
        Difficulty.LV1 -> Color(0xFFD6F5D6)
        Difficulty.LV2 -> Color(0xFFD6EAF5)
        Difficulty.LV3 -> Color(0xFFFFF3D6)
        Difficulty.LV4 -> Color(0xFFFFE5D6)
        Difficulty.LV5 -> Color(0xFFFFD6D6)
    }
    Surface(shape = RoundedCornerShape(2.dp), color = bgColor) {
        Text(
            difficulty.label,
            style = RoutiTheme.typography.caption3Medium,
            color = RoutiTheme.colors.gray800,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PracticeTagChip(label: String) {
    Surface(shape = RoundedCornerShape(2.dp), color = RoutiTheme.colors.gray200) {
        Text(
            label,
            style = RoutiTheme.typography.caption3Medium,
            color = RoutiTheme.colors.gray700,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SummaryBox(
    text: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
    ) {
        Text(
            text,
            style = RoutiTheme.typography.caption1Regular,
            color = RoutiTheme.colors.gray700,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Composable
private fun VerticalStepList(course: Course, modifier: Modifier = Modifier) {
    val points = course.allPoints
    Column(modifier = modifier) {
        points.forEachIndexed { i, point ->
            val isStart = i == 0
            val isEnd = i == points.lastIndex
            val dotColor = when {
                isStart -> RoutiTheme.colors.pinStart
                isEnd -> RoutiTheme.colors.pinArrival
                else -> RoutiTheme.colors.gray400
            }
            val roleLabel = when {
                isStart -> "출발지"
                isEnd -> "도착지"
                else -> "경유지 $i"
            }
            val roleLabelColor = when {
                isStart -> RoutiTheme.colors.pinStart
                isEnd -> RoutiTheme.colors.pinArrival
                else -> RoutiTheme.colors.gray800
            }
            val roleLabelWeight = if (isStart || isEnd) FontWeight.SemiBold else FontWeight.Medium

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = roleLabel,
                    style = RoutiTheme.typography.caption1Medium.copy(fontWeight = roleLabelWeight),
                    color = roleLabelColor,
                    modifier = Modifier.width(54.dp)
                )
                Text(
                    text = point.name,
                    style = RoutiTheme.typography.caption1Medium,
                    color = RoutiTheme.colors.gray800,
                    maxLines = 1,
                )
            }
            if (!isEnd) {
                Box(
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .width(1.dp)
                        .height(12.dp)
                        .background(RoutiTheme.colors.gray400, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

private fun distanceText(route: RouteResult?, isRouting: Boolean): String = when {
    isRouting || route == null -> "주행거리 · 측정 중…"
    route.totalDistanceMeters >= 1000 -> "주행거리 · ${"%.1f".format(route.totalDistanceMeters / 1000.0)}km"
    else -> "주행거리 · ${route.totalDistanceMeters}m"
}

@Composable
private fun BottomSheetPreviewWrapper(content: @Composable () -> Unit) {
    RoutiTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray)
                .padding(top = 40.dp), // 뒷배경 맵 공간 시뮬레이션
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = RoutiTheme.colors.white,
                shadowElevation = 8.dp,
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(RoutiTheme.colors.handleBar),
                        )
                    }
                    // 콘텐츠
                    content()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CourseListContentPreview() {
    BottomSheetPreviewWrapper {
        CourseListContent(courses = SampleCourses.ALL, onCourseClick = {}, expandFraction = 0f)
    }
}

@Preview(showBackground = true)
@Composable
fun CourseDetailContentPreview() {
    BottomSheetPreviewWrapper {
        CourseDetailContent(
            course = SampleCourses.ALL.first(),
            route = null,
            isRouting = false,
            onDismiss = {},
            onNavigate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CourseCardPreview() {
    RoutiTheme {
        CourseCard(
            course = SampleCourses.ALL.first(),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VerticalStepListPreview() {
    RoutiTheme {
        VerticalStepList(
            course = SampleCourses.ALL.first(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MyLocationButtonPreview() {
    RoutiTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            MyLocationButton(isActive = false, onClick = {})
            MyLocationButton(isActive = true, onClick = {})
        }
    }
}
