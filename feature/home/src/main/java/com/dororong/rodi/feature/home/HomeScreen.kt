package com.dororong.rodi.feature.home

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.dororong.rodi.core.data.navi.NaviApp
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.ui.terms.TermsDocument
import com.dororong.rodi.core.ui.terms.TermsWebView
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.location.awaitCurrentLocation
import com.dororong.rodi.feature.home.location.hasLocationPermission
import com.dororong.rodi.feature.home.map.fitCourseToScreen
import com.dororong.rodi.feature.home.map.focusOn
import com.dororong.rodi.feature.home.map.rememberMapViewWithLifecycle
import com.dororong.rodi.feature.home.map.renderCourse
import com.dororong.rodi.feature.home.map.renderCourseChips
import com.dororong.rodi.feature.home.map.renderCourseMarkers
import com.dororong.rodi.feature.home.navi.KakaoMapLauncher
import com.dororong.rodi.feature.home.navi.KakaoNaviLauncher
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

private const val PARKING_FOCUS_ZOOM = 15

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var naviCourse by remember { mutableStateOf<Course?>(null) }
    var installNaviCourse by remember { mutableStateOf<Course?>(null) }
    var isAtCurrentLocation by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var selectedTermsDocument by remember { mutableStateOf<TermsDocument?>(null) }
    var initialCameraMap by remember { mutableStateOf<KakaoMap?>(null) }
    var hasMovedToCurrentLocation by remember { mutableStateOf(false) }
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
        vm.onIntent(HomeIntent.OnLocationUpdate(loc.latitude, loc.longitude))
    }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val peekHeight = maxOf(380.dp, screenHeightDp.dp * 0.468f)
    val sheetPeekHeight = if (state.selectedCourse == null) peekHeight else 1.dp
    val density = LocalDensity.current
    val peekHeightPx = with(density) { peekHeight.roundToPx() }
    val logoMarginPx = with(density) { 8.dp.toPx() }
    val handleHeightDp = 24.dp
    val handleHeightPx = with(density) { handleHeightDp.toPx() }

    var scaffoldHeightPx by remember { mutableIntStateOf(0) }

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
        if (state.selectedCourseId != null) vm.onIntent(HomeIntent.OnDismissDetail)
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

    // 시트가 실제로 화면에서 차지하는 높이. 지도 카메라 정렬이 이 값을 기준으로 삼아야
    // 코스/주차장 상세가 펼쳐졌을 때 보이는 지도 영역(상단) 중앙에 맞춰진다.
    val detailSheetHeightPx = if (scaffoldHeightPx > 0 && sheetOffsetPx != Float.MAX_VALUE) {
        (scaffoldHeightPx - sheetOffsetPx).toInt().coerceIn(0, scaffoldHeightPx)
    } else {
        peekHeightPx
    }
    val mapBottomPaddingPx = if (state.selectedCourse == null) peekHeightPx else detailSheetHeightPx

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
            vm.onIntent(HomeIntent.OnCourseClick(courseId))
            isAtCurrentLocation = false
            true
        }
    }

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is HomeEffect.LaunchKakaoMap -> KakaoMapLauncher.launch(context, effect.course)
                is HomeEffect.LaunchKakaoNavi -> KakaoNaviLauncher.launch(context, effect.course)
                is HomeEffect.ShowNaviPicker -> naviCourse = effect.course
                is HomeEffect.ShowInstallNaviPicker -> installNaviCourse = effect.course
                is HomeEffect.OpenNaviInstallPage -> when (effect.app) {
                    NaviApp.KAKAOMAP -> KakaoMapLauncher.openInstallPage(context)
                    NaviApp.KAKAONAVI -> KakaoNaviLauncher.openInstallPage(context)
                }

            }
        }
    }

    LaunchedEffect(kakaoMap, mapBottomPaddingPx) {
        val map = kakaoMap ?: return@LaunchedEffect
        map.setPadding(0, 0, 0, mapBottomPaddingPx)
        map.logo?.setPosition(
            MapGravity.BOTTOM or MapGravity.LEFT,
            logoMarginPx,
            logoMarginPx,
        )
    }

    LaunchedEffect(kakaoMap, currentLocation) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (initialCameraMap !== map) {
            initialCameraMap = map
            hasMovedToCurrentLocation = currentLocation != null
            map.moveCamera(CameraUpdateFactory.newCenterPosition(currentLocation ?: SEOUL, DEFAULT_ZOOM))
            return@LaunchedEffect
        }
        if (!hasMovedToCurrentLocation && currentLocation != null) {
            hasMovedToCurrentLocation = true
            map.moveCamera(CameraUpdateFactory.newCenterPosition(currentLocation, DEFAULT_ZOOM))
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

    // 코스 선택 여부 + 필터된 코스 목록에 따라 지도 마커/경로선을 그린다 (카메라 정렬은 별도).
    // 길안내 API 응답 전(route == null)에는 직선 미리보기 없이 마커만 그려서, 실제 경로로
    // 바뀔 때 지도가 두 번 움직이는 것처럼 보이지 않게 한다.
    LaunchedEffect(kakaoMap, state.selectedCourseId, state.selectedRoute, state.filteredCourses) {
        val map = kakaoMap ?: return@LaunchedEffect
        val course = state.selectedCourse
        if (course == null) {
            map.renderCourseChips(context, state.filteredCourses)
        } else if (course.isParking) {
            map.renderCourseChips(context, listOf(course))
        } else {
            val route = state.selectedRoute
            if (route == null) {
                map.renderCourseMarkers(context, course)
            } else {
                map.renderCourse(context, course, route.points, route.snappedPoints)
            }
        }
    }

    val sheetSettled by remember {
        derivedStateOf {
            state.selectedCourseId == null ||
                    scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
        }
    }

    // 카메라 정렬. 시트 확장 애니메이션이 끝나고(sheetSettled) + 실제 경로가 준비된 뒤에만
    // 한 번에 정렬한다. 직선 미리보기 단계에서는 절대 카메라를 움직이지 않는다.
    LaunchedEffect(kakaoMap, state.selectedCourseId, state.selectedRoute, sheetSettled) {
        val map = kakaoMap ?: return@LaunchedEffect
        val course = state.selectedCourse
        val paddingPx = if (course == null || scaffoldHeightPx == 0 || sheetOffsetPx == Float.MAX_VALUE) {
            peekHeightPx
        } else {
            (scaffoldHeightPx - sheetOffsetPx).toInt().coerceIn(0, scaffoldHeightPx)
        }
        map.setPadding(0, 0, 0, paddingPx)
        if (course == null || !sheetSettled) return@LaunchedEffect
        if (course.isParking) {
            map.focusOn(LatLng.from(course.startWaypoint.lat, course.startWaypoint.lng), PARKING_FOCUS_ZOOM)
        } else {
            val route = state.selectedRoute ?: return@LaunchedEffect
            map.fitCourseToScreen(route.points)
        }
    }

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
            sheetPeekHeight = sheetPeekHeight,
            sheetContainerColor = RodiTheme.colors.white,
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
                                // 코스/주차장 상세가 열려 있을 때는 올리기·내리기 모두 막는다.
                                enabled = state.selectedCourse == null,
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
                                .background(RodiTheme.colors.handleBar),
                        )
                    }
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
                                        vm.onIntent(HomeIntent.OnCourseClick(id))
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
                            vm.onIntent(HomeIntent.OnDismissDetail)
                            coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
                        }
                        val navigate: () -> Unit = {
                            val kakaoMapInstalled = runCatching {
                                context.packageManager.getPackageInfo("net.daum.android.map", 0); true
                            }.getOrDefault(false)
                            val kakaoNaviInstalled = runCatching {
                                context.packageManager.getPackageInfo("com.locnall.KimGiSa", 0); true
                            }.getOrDefault(false)
                            vm.onIntent(
                                HomeIntent.OnNavigateClick(
                                    course = selectedCourse,
                                    kakaoMapInstalled = kakaoMapInstalled,
                                    kakaoNaviInstalled = kakaoNaviInstalled,
                                ),
                            )
                        }
                        StableMeasuredDetailSheet(
                            itemKey = selectedCourse.id,
                            maxHeight = boxHeightDp,
                        ) { detailModifier ->
                            if (selectedCourse.isParking) {
                                ParkingDetailContent(
                                    course = selectedCourse,
                                    onDismiss = dismissDetail,
                                    onNavigate = navigate,
                                    modifier = detailModifier,
                                )
                            } else {
                                CourseDetailContent(
                                    course = selectedCourse,
                                    route = state.selectedRoute,
                                    isRouting = state.isRouting,
                                    onDismiss = dismissDetail,
                                    onNavigate = navigate,
                                    modifier = detailModifier,
                                )
                            }
                        }
                    }
                }
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                key(mapRetryKey) {
                    val mapView = rememberMapViewWithLifecycle()

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
                        onSelect = { vm.onIntent(HomeIntent.OnDistanceFilterChange(it)) },
                    )
                }

                // 설정/현위치 버튼 — 시트 우상단 위 12dp에 부유
                if (sheetOffsetPx != Float.MAX_VALUE) {
                    val buttonTopDp = with(density) { sheetOffsetPx.toDp() } - 40.dp - 12.dp
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp)
                            .absoluteOffset(y = buttonTopDp - 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SettingsButton(onClick = { showSettings = true })
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

    if (selectedTermsDocument != null) {
        BackHandler { selectedTermsDocument = null }
    } else if (showSettings) {
        BackHandler { showSettings = false }
    }

    val termsDocument = selectedTermsDocument
    if (termsDocument != null) {
        TermsWebView(
            url = termsDocument.url,
            modifier = Modifier.fillMaxSize(),
        )
    } else if (showSettings) {
        SettingsTermsScreen(
            onBack = { showSettings = false },
            onTermsClick = { selectedTermsDocument = it },
        )
    }

    naviCourse?.let { course ->
        NaviPickerSheet(
            onDismiss = { naviCourse = null },
            onSelect = { app, always ->
                vm.onIntent(HomeIntent.OnNaviAppSelected(app, course, always))
                naviCourse = null
            },
        )
    }

    installNaviCourse?.let {
        NaviPickerSheet(
            mode = NaviPickerMode.INSTALL,
            onDismiss = { installNaviCourse = null },
            onSelect = { app, _ ->
                vm.onIntent(HomeIntent.OnInstallNaviAppSelected(app))
                installNaviCourse = null
            },
        )
    }
}
