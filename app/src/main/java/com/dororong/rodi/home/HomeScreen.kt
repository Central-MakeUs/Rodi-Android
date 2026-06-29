package com.dororong.rodi.home

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dororong.rodi.R
import com.dororong.rodi.data.SampleCourses
import com.dororong.rodi.directions.KakaoDirectionsClient.RouteResult
import com.dororong.rodi.location.awaitCurrentLocation
import com.dororong.rodi.location.hasLocationPermission
import com.dororong.rodi.map.rememberMapViewWithLifecycle
import com.dororong.rodi.map.renderCourse
import com.dororong.rodi.map.renderCourseChips
import com.dororong.rodi.model.Course
import com.dororong.rodi.model.Difficulty
import com.dororong.rodi.model.ParkingDetail
import com.dororong.rodi.model.PracticeTag
import com.dororong.rodi.navi.KakaoMapLauncher
import com.dororong.rodi.navi.KakaoNaviLauncher
import com.dororong.rodi.navi.NaviApp
import com.dororong.rodi.navi.NaviPreference
import com.dororong.rodi.ui.theme.RoutiTheme
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapGravity
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.edit

private const val DEFAULT_ZOOM = 13
private const val HOME_PREFS = "routi_home_prefs"
private const val KEY_HAS_LOADED_MAP = "has_loaded_map"
private val SEOUL = LatLng.from(37.5563, 126.9220)
private var hasLoadedMapInSession = false

private enum class MapScreenState {
    Loading,
    Ready,
    NetworkError,
}

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
    var installNaviCourse by remember { mutableStateOf<Course?>(null) }
    var isAtCurrentLocation by remember { mutableStateOf(false) }
    val hasLoadedMapBefore = remember { hasLoadedMapInSession || context.hasLoadedMapBefore() }
    var mapScreenState by remember {
        mutableStateOf(if (hasLoadedMapBefore) MapScreenState.Ready else MapScreenState.Loading)
    }
    var mapRetryKey by remember { mutableIntStateOf(0) }

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

    // 위치 정보를 ViewModel에 전달 (거리 필터링용)
    LaunchedEffect(currentLocation) {
        val loc = currentLocation ?: return@LaunchedEffect
        vm.onLocationUpdate(loc.latitude, loc.longitude)
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
        map.setOnCameraMoveEndListener { movedMap, _, _ ->
            if (movedMap === kakaoMap && mapScreenState == MapScreenState.Loading) {
                coroutineScope.launch {
                    delay(1_500)
                    if (kakaoMap === movedMap && mapScreenState == MapScreenState.Loading) {
                        hasLoadedMapInSession = true
                        context.markMapLoaded()
                        mapScreenState = MapScreenState.Ready
                    }
                }
            }
        }
        // 장소 칩 탭 → 코스 상세 진입
        map.setOnLabelClickListener { _, _, label ->
            val courseId = label.tag as? Int ?: return@setOnLabelClickListener true
            vm.onCourseClick(courseId)
            isAtCurrentLocation = false
            true
        }
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

    LaunchedEffect(kakaoMap, mapScreenState) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (mapScreenState != MapScreenState.Loading) return@LaunchedEffect
        delay(5_000)
        if (kakaoMap === map && mapScreenState == MapScreenState.Loading) {
            hasLoadedMapInSession = true
            context.markMapLoaded()
            mapScreenState = MapScreenState.Ready
        }
    }

    // 코스 선택 여부 + 필터된 코스 목록에 따라 지도 레이어 업데이트
    LaunchedEffect(kakaoMap, state.selectedCourseId, state.selectedRoute, state.filteredCourses) {
        val map = kakaoMap ?: return@LaunchedEffect
        val course = state.selectedCourse
        if (course == null) {
            map.renderCourseChips(context, state.filteredCourses)
        } else if (course.isParking) {
            map.renderCourseChips(context, listOf(course))
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

    val sheetShape = remember(density) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Outline {
                val offset = sheetOffsetPx
                val radiusPx = if (
                    state.selectedCourse == null &&
                    offset != Float.MAX_VALUE &&
                    scaffoldHeightPx > 0
                ) {
                    val fraction = (offset / 150f).coerceIn(0f, 1f)
                    with(density) { (20f * fraction).dp.toPx() }
                } else {
                    with(density) { 20.dp.toPx() }
                }
                return Outline.Rounded(
                    RoundRect(
                        rect = Rect(0f, 0f, size.width, size.height),
                        topLeft = CornerRadius(radiusPx, radiusPx),
                        topRight = CornerRadius(radiusPx, radiusPx),
                    ),
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
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

                    val listState = rememberLazyListState()
                    val selectedCourse = state.selectedCourse
                    if (selectedCourse == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (boxHeightDp != Dp.Unspecified) boxHeightDp else Dp.Unspecified),
                        ) {
                            if (state.filteredCourses.isEmpty()) {
                                CourseEmptyContent()
                            } else {
                                CourseListContent(
                                    courses = state.filteredCourses,
                                    onCourseClick = { id ->
                                        vm.onCourseClick(id)
                                        isAtCurrentLocation = false
                                    },
                                    expandFraction = expandFraction,
                                    onCollapse = { coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() } },
                                    listState = listState,
                                    modifier = if (visibleHeightDp != Dp.Unspecified) Modifier.height(
                                        visibleHeightDp,
                                    ) else Modifier,
                                )
                            }
                        }
                    } else {
                        val dismissDetail: () -> Unit = {
                            vm.onDismissDetail()
                            coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
                        }
                        val navigate: () -> Unit = {
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

                                else -> installNaviCourse = selectedCourse
                            }
                        }
                        FixedInitialHeightDetailSheet(
                            itemKey = selectedCourse.id,
                            maxHeight = boxHeightDp,
                        ) { modifier, isHeightFixed ->
                            if (selectedCourse.isParking) {
                                ParkingDetailContent(
                                    course = selectedCourse,
                                    onDismiss = dismissDetail,
                                    onNavigate = navigate,
                                    modifier = modifier,
                                    isHeightFixed = isHeightFixed,
                                )
                            } else {
                                CourseDetailContent(
                                    course = selectedCourse,
                                    route = state.selectedRoute,
                                    isRouting = state.isRouting,
                                    onDismiss = dismissDetail,
                                    onNavigate = navigate,
                                    modifier = modifier,
                                )
                            }
                        }
                    }
                }
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                key(mapRetryKey) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            mapView.start(
                                object : MapLifeCycleCallback() {
                                    override fun onMapDestroy() {}
                                    override fun onMapError(error: Exception?) {
                                        kakaoMap = null
                                        mapScreenState = MapScreenState.NetworkError
                                    }
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
                }

                // 거리 필터 바 — 코스 리스트 바텀시트 상태에서만 지도 상단 중앙에 부유
                AnimatedVisibility(
                    visible = state.selectedCourse == null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp),
                ) {
                    DistanceFilterBar(
                        selectedKm = state.distanceFilterKm,
                        onSelect = { vm.onDistanceFilterChange(it) },
                    )
                }

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
                                    CameraAnimation.from(250),
                                )
                                isAtCurrentLocation = true
                            },
                        )
                    }
                }
            }
        }

        when (mapScreenState) {
            MapScreenState.Loading -> MapLoadingScreen()
            MapScreenState.NetworkError -> MapNetworkErrorScreen(
                onRetry = {
                    mapScreenState = MapScreenState.Loading
                    kakaoMap = null
                    mapRetryKey += 1
                },
            )

            MapScreenState.Ready -> Unit
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

    installNaviCourse?.let {
        NaviPickerSheet(
            mode = NaviPickerMode.INSTALL,
            onDismiss = { installNaviCourse = null },
            onSelect = { app, _ ->
                when (app) {
                    NaviApp.KAKAOMAP -> KakaoMapLauncher.openInstallPage(context)
                    NaviApp.KAKAONAVI -> KakaoNaviLauncher.openInstallPage(context)
                }
                installNaviCourse = null
            },
        )
    }
}

@Composable
private fun DistanceFilterBar(
    selectedKm: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options: List<Pair<String, Int?>> = listOf(
        "전체" to null,
        "3km" to 3,
        "5km" to 5,
        "10km" to 10,
    )
    Surface(
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(50),
        color = RoutiTheme.colors.white,
        shadowElevation = 4.dp,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            options.forEach { (label, km) ->
                val selected = selectedKm == km
                Surface(
                    onClick = { onSelect(km) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) RoutiTheme.colors.primary600 else Color.Transparent,
                ) {
                    Text(
                        text = label,
                        style = RoutiTheme.typography.body1Medium,
                        color = if (selected) RoutiTheme.colors.white else RoutiTheme.colors.gray600,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MapLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoutiTheme.colors.white),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .absoluteOffset(y = (-33).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RoutiLoadingIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "지도를 불러오고 있어요",
                style = RoutiTheme.typography.body1SemiBold,
                color = RoutiTheme.colors.gray800,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "잠시만 기다려 주세요.",
                style = RoutiTheme.typography.body3Medium,
                color = RoutiTheme.colors.gray800,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MapNetworkErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoutiTheme.colors.white),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .absoluteOffset(y = (-30).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.illust_network_disconntected),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "지도를 불러올 수 없어요",
                style = RoutiTheme.typography.body1SemiBold,
                color = RoutiTheme.colors.gray800,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "현재 위치 정보를 확인하기 위해\n네트워크 연결 상태를 확인해 주세요.",
                style = RoutiTheme.typography.body3Medium,
                color = RoutiTheme.colors.gray800,
                textAlign = TextAlign.Center,
            )
        }

        RoutiNetworkSnackbar(
            onRetry = onRetry,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 106.dp),
        )
    }
}

@Composable
private fun RoutiLoadingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "map_loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
        ),
        label = "map_loading_rotation",
    )
    val colors = listOf(
        RoutiTheme.colors.primary600,
        Color(0xFFF4F4FF),
        RoutiTheme.colors.primary50,
        Color(0xFFDBD9FF),
        RoutiTheme.colors.primary200,
        RoutiTheme.colors.primary300,
        RoutiTheme.colors.primary400,
        RoutiTheme.colors.primary500,
    )

    Canvas(modifier = modifier.size(39.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = 3.dp.toPx()
        val lineLength = 10.dp.toPx()
        colors.forEachIndexed { index, color ->
            rotate(degrees = rotation + index * 45f, pivot = center) {
                drawLine(
                    color = color,
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, lineLength),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun RoutiNetworkSnackbar(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(8.dp),
        color = RoutiTheme.colors.gray800,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SnackbarAlertIcon()
            Text(
                text = "네트워크 연결이 원활하지 않아요.\n다시 시도해볼까요?",
                style = RoutiTheme.typography.body3Medium,
                color = RoutiTheme.colors.white,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                color = RoutiTheme.colors.primary600,
            ) {
                Text(
                    text = "새로고침",
                    style = RoutiTheme.typography.caption2SemiBold,
                    color = RoutiTheme.colors.white,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SnackbarAlertIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val color = Color.White
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color,
            radius = 9.5.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawLine(
            color = color,
            start = Offset(center.x, center.y - 5.dp.toPx()),
            end = Offset(center.x, center.y + 2.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = 1.3.dp.toPx(), center = Offset(center.x, center.y + 6.dp.toPx()))
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
    onCourseClick: (Int) -> Unit,
    expandFraction: Float = 0f,
    onCollapse: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                "연습코스",
                style = RoutiTheme.typography.headline1,
                color = RoutiTheme.colors.black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, bottom = 20.dp)
                    .graphicsLayer { alpha = 1f - expandFraction },
            )
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
                Spacer(
                    Modifier
                        .navigationBarsPadding()
                        .height(8.dp),
                )
            }
        }
    }
}

@Composable
private fun CourseEmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "추천할 수 있는 연습 코스를 찾지 못했어요.",
            style = RoutiTheme.typography.headline1,
            color = RoutiTheme.colors.gray800,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "지도를 축소시켜, 전체 지역의\n연습 코스를 둘러보세요.",
            style = RoutiTheme.typography.body3Medium,
            color = RoutiTheme.colors.gray800,
            textAlign = TextAlign.Center,
        )
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
            region = course.regionDisplay,
            onChevronClick = { addressExpanded = !addressExpanded },
        )
        if (addressExpanded) {
            ExpandableAddressCard(
                roadAddress = course.roadAddress.shortenRoadAddress(),
                jibunAddress = course.jibunAddress.shortenJibunAddress(),
            )
            Spacer(modifier = Modifier.height(0.5.dp))
        } else {
            TagRow(difficulty = course.difficultyEnum, tags = course.tags)
            Spacer(modifier = Modifier.height(8.dp))
            SummaryBox(text = course.summary, bgColor = RoutiTheme.colors.gray50)
        }
    }
}

@Composable
private fun FixedInitialHeightDetailSheet(
    itemKey: Int,
    maxHeight: Dp,
    content: @Composable (modifier: Modifier, isHeightFixed: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    var fixedHeightPx by rememberSaveable(itemKey) { mutableStateOf<Float?>(null) }
    val fixedHeightDp = fixedHeightPx?.let { with(density) { it.toDp() } }

    val measuringModifier = Modifier.onGloballyPositioned { coordinates ->
        if (fixedHeightPx != null) return@onGloballyPositioned

        val measuredHeightPx = coordinates.size.height.toFloat()
        if (measuredHeightPx <= 0f) return@onGloballyPositioned

        val maxHeightPx = with(density) {
            if (maxHeight.isSpecified && maxHeight > 0.dp) {
                maxHeight.toPx()
            } else {
                Float.POSITIVE_INFINITY
            }
        }
        fixedHeightPx = measuredHeightPx.coerceAtMost(maxHeightPx)
    }

    if (fixedHeightDp == null) {
        content(measuringModifier, false)
    } else {
        content(Modifier.height(fixedHeightDp), true)
    }
}

@Composable
private fun CourseDetailContent(
    course: Course,
    route: RouteResult?,
    isRouting: Boolean,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var addressExpanded by rememberSaveable(course.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
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
                region = course.regionDisplay,
                onChevronClick = { addressExpanded = !addressExpanded },
            )
            if (addressExpanded) {
                Spacer(modifier = Modifier.height(2.dp))
                ExpandableAddressCard(
                    roadAddress = course.roadAddress.shortenRoadAddress(),
                    jibunAddress = course.jibunAddress.shortenJibunAddress(),
                )
            } else {
                Text(
                    distanceText(route, isRouting),
                    style = RoutiTheme.typography.body3Medium,
                    color = RoutiTheme.colors.gray800,
                )
                TagRow(difficulty = course.difficultyEnum, tags = course.tags)

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
private fun ParkingDetailContent(
    course: Course,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    isHeightFixed: Boolean = false,
) {
    val parking = course.parkingDetail
    var addressExpanded by rememberSaveable(course.id) { mutableStateOf(false) }
    var hoursExpanded by rememberSaveable(course.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
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
                overflow = TextOverflow.Ellipsis,
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
                .then(if (isHeightFixed) Modifier.weight(1f) else Modifier)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RatingRegionRow(
                    rating = course.rating,
                    region = course.regionDisplay,
                    onChevronClick = { addressExpanded = !addressExpanded },
                )
                if (addressExpanded) {
                    ExpandableAddressCard(
                        roadAddress = course.roadAddress.shortenRoadAddress(),
                        jibunAddress = course.jibunAddress.shortenJibunAddress(),
                    )
                } else {
                    ParkingMetaRow(
                        parking = parking,
                        hoursExpanded = hoursExpanded,
                        onHoursClick = { hoursExpanded = !hoursExpanded },
                    )
                    if (hoursExpanded) {
                        ParkingHoursRows(parking = parking)
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    ParkingCapacityRow(capacity = parking?.capacity)
                    DifficultyTag(course.difficultyEnum)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = RoutiTheme.colors.primary100)
            Spacer(Modifier.height(13.dp))

            ParkingFeeSection(
                parking = parking,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onNavigate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoutiTheme.colors.primary600,
                    contentColor = RoutiTheme.colors.white,
                ),
            ) {
                Text("경로 안내", style = RoutiTheme.typography.button1)
            }
        }
    }
}

@Composable
private fun ParkingMetaRow(
    parking: ParkingDetail?,
    hoursExpanded: Boolean,
    onHoursClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = parking.parkingTypeDisplay(),
            style = RoutiTheme.typography.body3Medium,
            color = RoutiTheme.colors.gray800,
        )
        Text("･", style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray800)
        Row(
            modifier = Modifier.clickable(onClick = onHoursClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = parking.operatingSummary(),
                style = RoutiTheme.typography.body3Medium,
                color = RoutiTheme.colors.gray800,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = if (hoursExpanded) "영업시간 접기" else "영업시간 보기",
                tint = RoutiTheme.colors.gray800,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = if (hoursExpanded) 180f else 0f },
            )
        }
    }
}

@Composable
private fun ParkingCapacityRow(capacity: Int?) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("총 주차 면수", style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray800)
        Text("･", style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray800)
        Text(
            text = capacity?.let { "${it}대" } ?: "해당항목없음",
            style = RoutiTheme.typography.body3Medium,
            color = RoutiTheme.colors.gray800,
        )
    }
}

@Composable
private fun ParkingHoursRows(parking: ParkingDetail?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val hours = parking?.operatingHours
        ParkingInfoRow("평일", hours?.weekday.toDisplayHours())
        ParkingInfoRow("토요일", hours?.saturday.toDisplayHours())
        ParkingInfoRow("일요일", hours?.holiday.toDisplayHours())
        ParkingInfoRow("공휴일", hours?.holiday.toDisplayHours())
    }
}

@Composable
private fun ParkingFeeSection(
    parking: ParkingDetail?,
    modifier: Modifier = Modifier,
) {
    val fee = remember(parking?.feeInfo, parking?.isFree) { parking.toParkingFeeInfo() }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("요금 안내", style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray800)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ParkingInfoRow("초기무료", fee.initialFree)
            ParkingInfoRow("기본요금", fee.base)
            ParkingInfoRow("추가요금", fee.additional)
            ParkingInfoRow("할증기준시간", fee.surcharge)
        }
    }
}

@Composable
private fun ParkingInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = RoutiTheme.typography.caption1Medium,
            color = RoutiTheme.colors.gray800,
        )
        DashedInfoDivider(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f)
                .height(1.dp),
        )
        Text(
            text = value,
            style = RoutiTheme.typography.body3SemiBold,
            color = RoutiTheme.colors.gray800,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun DashedInfoDivider(modifier: Modifier = Modifier) {
    val color = RoutiTheme.colors.gray400.copy(alpha = 0.35f)
    Box(
        modifier = modifier.drawBehind {
            val segment = 6.dp.toPx()
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(segment, segment), 0f),
            )
        },
    )
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
                Text(roadAddress, style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray800)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("지번", style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray600)
                Text(jibunAddress, style = RoutiTheme.typography.body3Medium, color = RoutiTheme.colors.gray800)
            }
        }
    }
}

@Composable
private fun TagRow(difficulty: Difficulty, tags: Set<PracticeTag>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DifficultyTag(difficulty)
        tags.take(2).forEach { tag ->
            PracticeTagChip(tag.label)
        }
    }
}

@Composable
private fun DifficultyTag(difficulty: Difficulty) {
    val bgColor = when (difficulty) {
        Difficulty.LV1 -> Color(0xFFCDF2F6)
        Difficulty.LV2 -> Color(0xFFD0F7DF)
        Difficulty.LV3 -> Color(0xFFFFF6A4)
        Difficulty.LV4 -> Color(0xFFFFE6C0)
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
                    modifier = Modifier.width(54.dp),
                )
                Text(
                    text = point.name.stripCityPrefix(),
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

private data class ParkingFeeInfo(
    val initialFree: String,
    val base: String,
    val additional: String,
    val surcharge: String = "해당항목없음",
)

private fun ParkingDetail?.parkingTypeDisplay(): String = when {
    this == null -> "공영주차장"
    isFree -> "무료 주차장"
    parkingType?.isNotBlank() == true -> "공영주차장"
    else -> "공영주차장"
}

private fun ParkingDetail?.operatingSummary(): String {
    val weekday = this?.operatingHours?.weekday.orEmpty()
    if (weekday.isBlank()) return "영업시간 정보 없음"
    val normalized = weekday.replace(" ", "")
    if (normalized.startsWith("00:00") && (normalized.endsWith("23:59") || normalized.endsWith("24:00"))) {
        return "24시간 영업"
    }
    return "${normalized.substringBefore("-")}에 영업 시작"
}

private fun String?.toDisplayHours(): String {
    val value = this?.trim().orEmpty()
    if (value.isBlank()) return "해당항목없음"
    return value.replace("-", " - ")
}

private fun ParkingDetail?.toParkingFeeInfo(): ParkingFeeInfo {
    if (this == null) {
        return ParkingFeeInfo(
            initialFree = "해당항목없음",
            base = "해당항목없음",
            additional = "해당항목없음",
        )
    }
    if (isFree) {
        return ParkingFeeInfo(
            initialFree = "무료",
            base = "무료",
            additional = "해당항목없음",
        )
    }

    val baseMinutes = feeInfo.extractFeeNumber("baseMinutes")
    val baseFee = feeInfo.extractFeeNumber("baseFee")
    val addMinutes = feeInfo.extractFeeNumber("addUnitMinutes")
    val addFee = feeInfo.extractFeeNumber("addUnitFee")

    return ParkingFeeInfo(
        initialFree = note?.takeIf { it.contains("무료") } ?: "해당항목없음",
        base = formatFee(baseMinutes, baseFee),
        additional = formatFee(addMinutes, addFee),
    )
}

private fun String?.extractFeeNumber(key: String): Int? {
    val value = this ?: return null
    val escapedKey = Regex.escape(key)
    val match = Regex("""["']$escapedKey["']\s*:\s*(\d+)""").find(value) ?: return null
    return match.groupValues[1].toIntOrNull()
}

private fun formatFee(minutes: Int?, fee: Int?): String {
    if (minutes == null || fee == null) return "해당항목없음"
    return "${minutes}분 ･ ${"%,d".format(fee)}원"
}

private fun Context.hasLoadedMapBefore(): Boolean =
    getSharedPreferences(HOME_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_HAS_LOADED_MAP, false)

private fun Context.markMapLoaded() {
    getSharedPreferences(HOME_PREFS, Context.MODE_PRIVATE)
        .edit {
            putBoolean(KEY_HAS_LOADED_MAP, true)
        }
}

// ── 주소 단축 헬퍼 ────────────────────────────────────────────────────────────

private val CITY_PREFIXES = listOf(
    "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시",
    "대전광역시", "울산광역시", "세종특별자치시", "강원특별자치도", "경기도",
    "충청남도", "충청북도", "전라남도", "전라북도", "경상남도", "경상북도",
    "제주특별자치도", "서울", "부산", "대구", "인천", "광주", "대전", "울산",
    "세종", "강원", "경기", "제주",
)

/** 시/도 레벨 접두어만 제거 (구·군·동은 유지). VerticalStepList용. */
private fun String.stripCityPrefix(): String {
    val s = trim()
    CITY_PREFIXES.forEach { city -> if (s.startsWith("$city ")) return s.removePrefix("$city ").trim() }
    return s
}

/** 시/도 + 구/군 접두어까지 제거. 공백 구분 주소용. */
private fun String.stripCityAndDistrict(): String =
    stripCityPrefix().replace(Regex("^[가-힣0-9]+[구군]\\s+"), "")

/** 도로명 주소를 짧게 표시. "마포나루길 467 스타벅스" 형태. */
private fun String.shortenRoadAddress(): String {
    if (isBlank()) return this
    return if (contains(',')) shortenCommaRoad() else stripCityAndDistrict()
}

private fun String.shortenCommaRoad(): String {
    val parts = split(',').map { it.trim() }.filter {
        it.isNotEmpty() && !it.matches(Regex("\\d{5}")) && it != "대한민국"
    }
    // 도로명: 로/길/대로 로 끝나는 토큰
    val roadIdx = parts.indexOfFirst { p ->
        p.endsWith("로") || p.endsWith("길") || p.endsWith("대로") ||
                Regex("로\\d|길\\d").containsMatchIn(p)
    }
    if (roadIdx < 0) return parts.firstOrNull() ?: this

    val road = parts[roadIdx]
    // 번지: 도로명 바로 앞 토큰이 숫자/지하 면 사용
    val number = parts.getOrNull(roadIdx - 1)
        ?.takeIf { it.matches(Regex("\\d+(-\\d+)?|지하\\s*\\d+")) }
    // 건물명: 첫 토큰이 숫자·행정구역 접미사가 아닌 경우
    val building = parts.firstOrNull()?.takeIf { first ->
        parts.indexOf(first) != roadIdx &&
                !first.matches(Regex("\\d.*")) &&
                !first.endsWith("동") && !first.endsWith("구") && !first.endsWith("시") &&
                !first.endsWith("읍") && !first.endsWith("면") &&
                !first.endsWith("로") && !first.endsWith("길") && !first.endsWith("대로")
    }

    return buildString {
        append(road)
        if (number != null) append(" $number")
        if (building != null) append(" $building")
    }
}

/** 지번 주소를 짧게 표시. "망원동 205-4" 형태. */
private fun String.shortenJibunAddress(): String {
    if (isBlank()) return this

    // 공백 구분 형식: "xxx동 번지" 패턴 탐색
    if (!contains(',')) {
        val match = Regex("([가-힣]+(?:동|리))\\s+(\\d+(?:-\\d+)?)").find(this)
        if (match != null) return "${match.groupValues[1]} ${match.groupValues[2]}"
        return stripCityAndDistrict()
    }

    // 쉼표 형식: 동/리 토큰 + 번지 토큰
    val parts = split(',').map { it.trim() }.filter {
        it.isNotEmpty() && !it.matches(Regex("\\d{5}")) && it != "대한민국"
    }
    val dong = parts.firstOrNull { it.endsWith("동") || it.endsWith("리") }
    val num = parts.firstOrNull { it.matches(Regex("\\d+(-\\d+)?")) }

    return when {
        dong != null && num != null -> "$dong $num"
        dong != null -> dong
        else -> shortenCommaRoad() // 동이 없으면 도로명으로 폴백
    }
}

@Composable
private fun BottomSheetPreviewWrapper(content: @Composable () -> Unit) {
    RoutiTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray)
                .padding(top = 40.dp),
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

@Preview(showBackground = true)
@Composable
fun DistanceFilterBarPreview() {
    RoutiTheme {
        Column(
            modifier = Modifier
                .background(Color.LightGray)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DistanceFilterBar(selectedKm = null, onSelect = {})
            DistanceFilterBar(selectedKm = 3, onSelect = {})
            DistanceFilterBar(selectedKm = 5, onSelect = {})
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
fun MapLoadingScreenPreview() {
    RoutiTheme {
        MapLoadingScreen()
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
fun MapNetworkErrorScreenPreview() {
    RoutiTheme {
        MapNetworkErrorScreen(onRetry = {})
    }
}
