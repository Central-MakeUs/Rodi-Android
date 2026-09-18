package com.dororong.rodi.feature.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.ui.components.RodiBottomNavigation
import com.dororong.rodi.core.ui.components.RodiBottomNavigationDestination
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarDuration
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.components.MapListButton
import com.dororong.rodi.feature.home.components.MyLocationButton
import com.dororong.rodi.feature.home.detail.CourseReviewViewModel
import com.dororong.rodi.feature.home.detail.reviewactions.ReviewActionsEffect
import com.dororong.rodi.feature.home.detail.reviewactions.ReviewActionsViewModel
import com.dororong.rodi.feature.home.location.awaitCurrentLocation
import com.dororong.rodi.feature.home.location.currentLocationUpdates
import androidx.core.app.ActivityCompat
import com.dororong.rodi.core.ui.permission.findActivity
import com.dororong.rodi.core.ui.permission.hasLocationPermission
import com.dororong.rodi.core.ui.permission.openAppSettings
import com.dororong.rodi.core.ui.map.lastMapCameraOrNull
import com.dororong.rodi.core.ui.map.saveLastMapCamera
import com.dororong.rodi.feature.home.location.rememberDeviceHeading
import com.dororong.rodi.feature.home.map.BrowseLabelTag
import com.dororong.rodi.feature.home.map.centerPoint
import com.dororong.rodi.core.ui.network.networkAvailabilityFlow
import com.dororong.rodi.feature.home.map.ClusterPolicy
import com.dororong.rodi.feature.home.map.DEFAULT_ZOOM
import com.dororong.rodi.feature.home.map.CameraSettleAction
import com.dororong.rodi.feature.home.map.InitialViewportSearchPolicy
import com.dororong.rodi.feature.home.map.InitialLocationState
import com.dororong.rodi.feature.home.map.MapClusterer
import com.dororong.rodi.feature.home.map.MapBitmapStyle
import com.dororong.rodi.feature.home.map.MapBitmapTextStyle
import com.dororong.rodi.feature.home.map.MapSearchMoveReason
import com.dororong.rodi.feature.home.map.rememberHomeMapState
import com.dororong.rodi.feature.home.map.rememberMapLoadStatus
import com.dororong.rodi.feature.home.map.MapViewport
import com.dororong.rodi.feature.home.map.ProjectedMapItem
import com.dororong.rodi.feature.home.map.SEOUL
import com.dororong.rodi.feature.home.map.clearBrowseLabels
import com.dororong.rodi.feature.home.map.clearCourse
import com.dororong.rodi.feature.home.map.clearCurrentLocationMarker
import com.dororong.rodi.feature.home.map.deselectParkingMarker
import com.dororong.rodi.feature.home.map.fitCourseToScreen
import com.dororong.rodi.feature.home.map.focusOn
import com.dororong.rodi.feature.home.map.hasLoadedMapInSession
import com.dororong.rodi.feature.home.map.initialMapCenter
import com.dororong.rodi.feature.home.map.markMapLoaded
import com.dororong.rodi.feature.home.map.markerViewportOrNull
import com.dororong.rodi.feature.home.map.rememberMapViewWithLifecycle
import com.dororong.rodi.feature.home.map.renderClusters
import com.dororong.rodi.feature.home.map.renderCurrentLocationMarker
import com.dororong.rodi.feature.home.map.renderIndividualMarkers
import com.dororong.rodi.feature.home.map.renderPlaceCourse
import com.dororong.rodi.feature.home.map.renderPlaceCourseMarkers
import com.dororong.rodi.feature.home.map.RouteLineColors
import com.dororong.rodi.feature.home.map.renderSelectedParkingMarker
import com.dororong.rodi.feature.home.map.selectParkingMarker
import com.dororong.rodi.feature.home.map.viewportOrNull
import com.dororong.rodi.feature.home.map.viewportAboveBottomInsetOrNull
import com.dororong.rodi.feature.home.map.visibleViewportOrNull
import com.dororong.rodi.feature.home.map.boundsOrNull
import com.dororong.rodi.feature.home.map.applyMapContentPadding
import com.dororong.rodi.feature.home.navi.KakaoMapLauncher
import com.dororong.rodi.feature.home.navi.KakaoNaviLauncher
import com.dororong.rodi.feature.home.search.RegionOfficeLocation
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapGravity
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.dororong.rodi.core.ui.R as CoreUiR
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private const val CLUSTER_DISTANCE_DP = 56
private const val CLUSTER_FIT_PADDING_DP = 64
private const val SURFACE_ANIMATION_MILLIS = 300
private val LIST_SHEET_PEEK_HEIGHT = 380.dp
private val BOTTOM_CONTROL_MIN_OFFSET = 68.dp
private val BOTTOM_CONTROL_SHEET_GAP = 12.dp
private const val MIN_ZOOM = 6
private const val MAP_NETWORK_SNACKBAR_ID = "map-network"
// HomeSearchBar가 지도 위에 statusBarsPadding() + vertical 5dp로 떠 있는 만큼. 경로 핏 계산에
// 이 높이를 반영하지 않으면 세로로 긴 코스의 출발지·도착지 마커가 검색창 뒤에 가려진다.
private val MAP_SEARCH_BAR_TOP_INSET = 5.dp + 46.dp

typealias KakaoLoginRequest = (
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit,
) -> Unit
typealias DrivingStartRequest = (PlaceDetail) -> Result<String>

@Composable
fun HomeScreen(
    onMyPageClick: () -> Unit,
    onCourseRegistrationClick: () -> Unit = {},
    onSearchClick: (GeoPoint) -> Unit,
    onGuestSignUp: () -> Unit,
    onRequestKakaoLogin: KakaoLoginRequest,
    onStartDriving: DrivingStartRequest,
    onStopDriving: () -> Unit = {},
    onPracticeSkipReasonClick: (Long) -> Unit = {},
    bottomNavigation: @Composable () -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val reviewVm: CourseReviewViewModel = hiltViewModel()
    val reviewActionsVm: ReviewActionsViewModel = hiltViewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by vm.uiState.collectAsStateWithLifecycle()
    val reviewState by reviewVm.uiState.collectAsStateWithLifecycle()
    val reviewActionsState by reviewActionsVm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { RodiSnackbarHostState() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lastSavedCamera = remember { context.lastMapCameraOrNull() }

    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    val mapState = rememberHomeMapState()
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var permissionGranted by remember { mutableStateOf(context.hasLocationPermission()) }
    var initialLocationState by remember { mutableStateOf(InitialLocationState.Pending) }
    val mapLoad = rememberMapLoadStatus(context)
    val overlay = rememberHomeOverlayState()
    var pendingDrivingEffect by remember { mutableStateOf<HomeEffect.LaunchNavi?>(null) }
    var courseDetailSheetHeightPx by remember { mutableIntStateOf(0) }
    var parkingSheetLayout by remember { mutableStateOf(ParkingSheetLayoutState()) }
    var bottomNavigationHeightPx by remember { mutableIntStateOf(0) }
    var restoredViewportMap by remember { mutableStateOf<KakaoMap?>(null) }
    val deviceHeading = rememberDeviceHeading()
    val clusterDistancePx = with(density) { CLUSTER_DISTANCE_DP.dp.roundToPx() }
    val colors = RodiTheme.colors
    val typography = RodiTheme.typography
    val courseChipTypeface = remember(context) {
        requireNotNull(ResourcesCompat.getFont(context, CoreUiR.font.pretendard_regular))
    }
    val clusterTypeface = remember(context) {
        requireNotNull(ResourcesCompat.getFont(context, CoreUiR.font.pretendard_medium))
    }
    val mapBitmapStyle = with(density) {
        MapBitmapStyle(
            courseChipBackgroundColor = colors.primary500.toArgb(),
            courseChipText = MapBitmapTextStyle(
                color = colors.white.toArgb(),
                textSizePx = typography.caption1Regular.fontSize.toPx(),
                typeface = courseChipTypeface,
            ),
            clusterBackgroundColor = colors.primary500.toArgb(),
            clusterText = MapBitmapTextStyle(
                color = colors.white.toArgb(),
                textSizePx = typography.body3Medium.fontSize.toPx(),
                typeface = clusterTypeface,
            ),
            clusterShadowColor = colors.black.copy(alpha = 0.3f).toArgb(),
        )
    }
    val currentLocationMarkerColor = RodiTheme.colors.primary600.toArgb()

    fun launchNaviApp(effect: HomeEffect.LaunchNavi) {
        when (effect.app) {
            NaviApp.KAKAOMAP -> KakaoMapLauncher.launch(
                context = context,
                place = effect.place,
                origin = currentLocation?.let { GeoPoint(lat = it.latitude, lng = it.longitude) },
            )
            NaviApp.KAKAONAVI -> KakaoNaviLauncher.launch(context, effect.place)
        }
    }

    suspend fun launchDriving(effect: HomeEffect.LaunchNavi) {
        if (effect.startDriving) {
            val startResult = onStartDriving(effect.place)
            val startError = startResult.exceptionOrNull()
            if (startError != null) {
                snackbarHostState.show(
                    RodiSnackbarData(
                        message = startError.message
                            ?: "운전 상태 추적을 시작하지 못했어요. 다시 시도해 주세요.",
                    ),
                )
                return
            }
        }
        launchNaviApp(effect)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        permissionGranted = result.values.any { it }
        if (!permissionGranted) {
            initialLocationState = InitialLocationState.Unavailable
            // 영구 거부 상태면 launch가 창도 못 띄우고 바로 거부로 끝난다. 그때는 설정으로 보낸다.
            val canAskAgain = context.findActivity()?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            } ?: false
            if (!canAskAgain) context.openAppSettings()
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        vm.onIntent(HomeIntent.OnNotificationPermissionResult(granted))
    }
    val drivingPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionGranted = context.hasLocationPermission()
        val pending = pendingDrivingEffect
        pendingDrivingEffect = null
        if (pending != null) {
            scope.launch {
                val missingPermissions = context.missingDrivingPermissions()
                if (missingPermissions.isEmpty()) {
                    launchDriving(pending)
                } else {
                    // 권한을 못 받으면 추적 없이 경로만 띄운다("경로만 보기"와 같은 결과).
                    // 필요성은 이미 팝업으로 안내했으니 토스트까지 겹쳐 띄우지 않는다.
                    launchNaviApp(pending)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    permissionGranted = context.hasLocationPermission()
                    mapState.resetEntryFlags()
                    // 진행 중이던 연습 세션이 있으면 "이어서 측정할까요?" 다이얼로그를 다시 띄운다.
                    vm.onIntent(HomeIntent.OnAppResumed)
                    // 설정에서 차단을 풀거나 내 활동에서 후기를 고치고 돌아올 수 있다.
                    // 열려 있는 장소가 없으면 refresh는 아무 것도 하지 않는다.
                    reviewVm.refresh()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    mapState.currentViewport?.centerPoint()?.let { center ->
                        context.saveLastMapCamera(center, mapState.zoomLevel)
                    }
                    Unit
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        mapState.resetEntryFlags()
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }
    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) {
            currentLocation = null
            initialLocationState = InitialLocationState.Unavailable
            kakaoMap?.clearCurrentLocationMarker()
            return@LaunchedEffect
        }
        initialLocationState = InitialLocationState.Pending
        currentLocation = context.awaitCurrentLocation()
        initialLocationState = if (currentLocation == null) {
            InitialLocationState.Unavailable
        } else {
            InitialLocationState.Ready
        }
        context.currentLocationUpdates().collect {
            currentLocation = it
            initialLocationState = InitialLocationState.Ready
        }
    }

    val isEmptySheet = state.showEmpty || state.showInitialError
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val listSheetState = remember { AnchoredDraggableState(ListSheetValue.Hidden) }
    val listSheetDrag = Modifier.anchoredDraggable(
        state = listSheetState,
        orientation = Orientation.Vertical,
        flingBehavior = AnchoredDraggableDefaults.flingBehavior(listSheetState),
    )
    val peekHeightPx = with(density) { LIST_SHEET_PEEK_HEIGHT.toPx() }

    LaunchedEffect(listSheetState) {
        snapshotFlow { listSheetState.settledValue }
            .drop(1)
            .collect { vm.onIntent(HomeIntent.OnListSheetSettled(it.toSurfaceState())) }
    }
    // 컨테이너 크기는 onSizeChanged가 잡지만 목록이 비는 건 크기 변화가 아니라서 여기서 앵커를 다시 만든다.
    // updateAnchors는 같은 앵커면 아무것도 하지 않아 onSizeChanged와 겹쳐 불려도 안전하다.
    LaunchedEffect(isEmptySheet, containerSize.height) {
        if (containerSize.height <= 0) return@LaunchedEffect
        listSheetState.updateAnchors(
            listSheetAnchors(
                containerHeightPx = containerSize.height,
                peekHeightPx = peekHeightPx,
                allowFull = !isEmptySheet,
            ),
        )
    }
    LaunchedEffect(state.surfaceState, isEmptySheet, containerSize.height) {
        if (containerSize.height <= 0) return@LaunchedEffect
        val target = state.surfaceState.toListSheetValue(allowFull = !isEmptySheet)
        if (listSheetState.settledValue != target) listSheetState.animateTo(target)
    }

    // 아래 람다들은 반드시 layout/draw 스코프 안에서만 호출한다. 컴포지션에서 읽으면 드래그 한 프레임마다
    // HomeScreen 전체가(지도 AndroidView 포함) recompose 된다.
    val listSheetOffsetPx: () -> Float = {
        listSheetState.offset.takeIf { !it.isNaN() } ?: containerSize.height.toFloat()
    }
    val visibleSheetHeightPx: () -> Float = {
        BottomSheetViewportPolicy.bottomPaddingPx(
            mapHeightPx = containerSize.height,
            sheetTopPx = listSheetOffsetPx(),
        ).toFloat()
    }
    val listSheetProgress: () -> Float = {
        ListSheetAnchorPolicy.expansionProgress(
            offsetPx = listSheetOffsetPx(),
            partialOffsetPx = (containerSize.height - peekHeightPx).coerceAtLeast(0f),
        )
    }
    val listHeaderHeightPx: Density.() -> Float = {
        lerp(PARTIAL_LIST_HEADER_HEIGHT, FULL_LIST_HEADER_HEIGHT, listSheetProgress()).toPx()
    }
    val listViewportHeightPx: Density.() -> Float = {
        (visibleSheetHeightPx() - listHeaderHeightPx()).coerceAtLeast(0f)
    }

    val navigationInsetPx = WindowInsets.navigationBars.getBottom(density)
    val navigationInset = with(density) { navigationInsetPx.toDp() }
    val selectedDetailPlaceId = state.selectedPlace?.id
    val bottomControlOffsetPx: Density.() -> Float = {
        val minimum = BOTTOM_CONTROL_MIN_OFFSET.toPx()
        val sheetHeightPx =
            if (state.surfaceState == HomeSurfaceState.Detail && state.selectedPlace?.type == PlaceType.PARKING) {
                parkingSheetLayout
                    .takeIf { it.placeId == selectedDetailPlaceId }
                    ?.currentHeightPx
                    ?.toFloat()
                    ?: 0f
            } else {
                visibleSheetHeightPx()
            }
        if (sheetHeightPx > 0f) {
            maxOf(minimum, sheetHeightPx + BOTTOM_CONTROL_SHEET_GAP.toPx() - navigationInsetPx)
        } else {
            minimum
        }
    }

    // 지도 패딩은 드래그 중에는 건드리지 않는다. setPadding/마커 재렌더는 네이티브 호출이라
    // 프레임마다 부르면 그대로 드랍으로 이어진다. 정착한 앵커 기준 값만 쓴다.
    val settledSheetInsetPx = when (listSheetState.settledValue) {
        ListSheetValue.Hidden -> 0
        ListSheetValue.Partial -> peekHeightPx.roundToInt().coerceAtMost(containerSize.height)
        ListSheetValue.Full -> containerSize.height
    }
    val mapContentBottomPaddingPx = when {
        state.surfaceState == HomeSurfaceState.PartialList ||
            state.surfaceState == HomeSurfaceState.FullList -> settledSheetInsetPx
        state.surfaceState == HomeSurfaceState.Navigation -> bottomNavigationHeightPx
        state.surfaceState != HomeSurfaceState.Detail -> 0
        state.selectedPlace?.type == PlaceType.COURSE -> courseDetailSheetHeightPx
        state.selectedPlace?.type == PlaceType.PARKING -> parkingSheetLayout
            .takeIf { it.placeId == selectedDetailPlaceId }
            ?.initialMapPaddingPx
            ?: 0
        else -> 0
    }
    val mapContentTopPaddingPx = WindowInsets.statusBars.getTop(density) +
        with(density) { MAP_SEARCH_BAR_TOP_INSET.roundToPx() }
    val clusterFitPaddingPx = with(density) { CLUSTER_FIT_PADDING_DP.dp.roundToPx() }
    val mapBrandOffset = maxOf(0.dp, BOTTOM_CONTROL_MIN_OFFSET + navigationInset - 4.dp)
    val mapScaleBarOffset = (mapBrandOffset - 2.dp).coerceAtLeast(0.dp)

    LaunchedEffect(
        state.surfaceState,
        selectedDetailPlaceId,
        state.selectedPlace?.type,
        state.isDetailLoading,
    ) {
        val activeParkingPlaceId = if (
            state.surfaceState == HomeSurfaceState.Detail &&
            state.selectedPlace?.type == PlaceType.PARKING &&
            !state.isDetailLoading
        ) {
            selectedDetailPlaceId
        } else {
            null
        }
        parkingSheetLayout = parkingSheetLayout.forPlace(activeParkingPlaceId)
    }

    val deselectSelectedParkingMarker: () -> Unit = {
        val selectedParkingId = state.selectedPlace
            ?.takeIf { it.type == PlaceType.PARKING }
            ?.id
            ?: state.selectedPlaceId?.takeIf { selectedPlaceId ->
                state.coordinates.firstOrNull { it.id == selectedPlaceId }?.type == PlaceType.PARKING
            }
        selectedParkingId?.let { kakaoMap?.deselectParkingMarker(context, it) }
    }
    val dismissDetail: () -> Unit = {
        deselectSelectedParkingMarker()
        vm.onIntent(HomeIntent.OnDismissDetail)
    }
    val requestNavigate: () -> Unit = {
        vm.onIntent(
            HomeIntent.OnNavigateClick(
                kakaoMapInstalled = KakaoMapLauncher.isInstalled(context),
                kakaoNaviInstalled = KakaoNaviLauncher.isInstalled(context),
                notificationPermissionGranted = context.hasNotificationPermission(),
            ),
        )
    }
    val dragDismissDetail: () -> Unit = {
        deselectSelectedParkingMarker()
        vm.onIntent(HomeIntent.OnDragDismissDetail)
    }
    val dismissLogin: () -> Unit = {
        val pendingPlaceId = (state.pendingAction as? PendingHomeAction.OpenDetail)?.placeId
        val isPendingParking = state.coordinates.firstOrNull { it.id == pendingPlaceId }?.type == PlaceType.PARKING
        if (pendingPlaceId != null && isPendingParking) {
            kakaoMap?.deselectParkingMarker(context, pendingPlaceId)
        }
        vm.onIntent(HomeIntent.OnDismissLogin)
    }

    var pendingRegionMove by remember { mutableStateOf<RegionOfficeLocation?>(null) }
    CollectEffect(vm.effect) { effect ->
        when (effect) {
            is HomeEffect.LaunchNavi -> {
                if (!effect.startDriving) {
                    launchDriving(effect)
                } else {
                    val missingPermissions = context.missingDrivingPermissions()
                    if (missingPermissions.isEmpty()) {
                        launchDriving(effect)
                    } else {
                        pendingDrivingEffect = effect
                        drivingPermissionLauncher.launch(missingPermissions)
                    }
                }
            }
            is HomeEffect.ShowNaviPicker -> overlay.naviPlaceId = effect.place.id
            is HomeEffect.MoveToRegion -> pendingRegionMove = effect.region
            HomeEffect.RefreshReviews -> reviewVm.refresh()
            is HomeEffect.ShowInstallNaviPicker -> overlay.installNaviPlaceId = effect.place.id
            is HomeEffect.OpenPracticeReview -> {
                overlay.reviewToWrite = ReviewWriteTarget(effect.placeId, effect.placeName, null)
            }
            is HomeEffect.OpenPracticeSkipReason -> onPracticeSkipReasonClick(effect.practiceId)
            is HomeEffect.OpenNaviInstallPage -> when (effect.app) {
                NaviApp.KAKAOMAP -> KakaoMapLauncher.openInstallPage(context)
                NaviApp.KAKAONAVI -> KakaoNaviLauncher.openInstallPage(context)
            }

            is HomeEffect.ShowSnackbar -> snackbarHostState.show(RodiSnackbarData(message = effect.message))
            is HomeEffect.NavigateSearch -> onSearchClick(effect.origin)
            HomeEffect.NavigateMyPage -> onMyPageClick()
            HomeEffect.NavigateCourseRegistration -> onCourseRegistrationClick()
            HomeEffect.NavigateGuestSignUp -> onGuestSignUp()
            HomeEffect.StopDrivingTracking -> onStopDriving()
        }
    }
    CollectEffect(vm.permissionEffect) { effect ->
        when (effect) {
            HomePermissionEffect.RequestNotificationPermission -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    vm.onIntent(HomeIntent.OnNotificationPermissionResult(granted = true))
                }
            }
        }
    }

    fun retryMap() {
        if (mapLoad.retry()) kakaoMap = null
    }

    val networkErrorSnackbarIcon = painterResource(CoreUiR.drawable.ic_alert_circle)
    LaunchedEffect(mapLoad.showNetworkSnackbar) {
        if (mapLoad.showNetworkSnackbar) {
            snackbarHostState.showImmediately(
                RodiSnackbarData(
                    id = MAP_NETWORK_SNACKBAR_ID,
                    message = "네트워크 연결이 원활하지 않아요.\n다시 시도해볼까요?",
                    icon = networkErrorSnackbarIcon,
                    duration = RodiSnackbarDuration.Indefinite,
                    actionLabel = "새로고침",
                    onAction = ::retryMap,
                ),
            )
        } else {
            snackbarHostState.dismiss(MAP_NETWORK_SNACKBAR_ID)
        }
    }

    // 상세 진입 때 카메라가 그 장소로 옮겨가므로, 상세를 닫으면 옮겨간 위치 기준으로 목록·마커를 다시 받는다.
    // 사용자가 손으로 지도를 끄는 경우는 여기 해당하지 않아 "재검색" 버튼 UX가 유지된다.
    var wasDetailSurface by remember { mutableStateOf(false) }
    LaunchedEffect(state.surfaceState, mapState.currentViewport) {
        val isDetail = state.surfaceState == HomeSurfaceState.Detail
        if (wasDetailSurface && !isDetail) {
            mapState.currentViewport?.let { viewport ->
                vm.onIntent(HomeIntent.OnProgrammaticSearch(viewport.toQuery(currentLocation)))
            }
        }
        wasDetailSurface = isDetail
    }

    LaunchedEffect(Unit) {
        networkAvailabilityFlow(context).collect { mapLoad.isOnline = it }
    }

    // 연결이 돌아오면 이 이펙트가 재시작되며 오프라인 유예 delay가 취소된다(MapLoadStatus.awaitOfflineGrace).
    LaunchedEffect(mapLoad.isOnline) {
        if (mapLoad.isOnline) {
            if (mapLoad.shouldRetryOnReconnect) retryMap()
        } else {
            mapLoad.awaitOfflineGrace()
        }
    }

    LaunchedEffect(kakaoMap, mapState.mapViewSize) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (mapState.mapViewSize.width <= 0 || mapState.mapViewSize.height <= 0 || restoredViewportMap === map) {
            return@LaunchedEffect
        }
        restoredViewportMap = map
        val viewport = mapState.currentViewport ?: return@LaunchedEffect
        mapState.hasCenteredInitialLocation = true
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(
                    (viewport.northEast.lat + viewport.southWest.lat) / 2,
                    (viewport.northEast.lng + viewport.southWest.lng) / 2,
                ),
                mapState.zoomLevel,
            ),
            CameraAnimation.from(0),
        )
    }

    LaunchedEffect(kakaoMap, mapState.mapViewSize, currentLocation, initialLocationState) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (!InitialViewportSearchPolicy.canDispatch(
                locationState = initialLocationState,
                hasCurrentLocation = currentLocation != null,
                hasCenteredInitialLocation = mapState.hasCenteredInitialLocation,
                isInitialLocationCameraMovePending = mapState.isInitialLocationCameraMovePending,
            )
        ) {
            return@LaunchedEffect
        }
        val viewport = map.viewportOrNull(mapState.mapViewSize) ?: return@LaunchedEffect
        mapState.currentViewport = viewport
        vm.onIntent(HomeIntent.OnViewportSettled(viewport.toQuery(currentLocation)))
    }

    LaunchedEffect(kakaoMap, permissionGranted) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (!permissionGranted) return@LaunchedEffect
        // 상세 화면(Detail)에서는 선택한 장소를 보여주는 별도 카메라 포커스 이펙트가 있다 —
        // 여기서 현위치로 재센터링하면 그 포커스를 덮어써 버리므로 건너뛴다.
        if (state.surfaceState == HomeSurfaceState.Detail) return@LaunchedEffect
        val location = snapshotFlow { currentLocation }.filterNotNull().first()
        if (!mapState.hasCenteredInitialLocation && !mapState.hasUserMovedMap && !mapState.hasUserChosenMapViewport) {
            mapState.hasCenteredInitialLocation = true
            mapState.isInitialLocationCameraMovePending = true
            mapState.moveWithSearch(
                target = GeoPoint(location.latitude, location.longitude),
                targetZoom = DEFAULT_ZOOM,
                reason = MapSearchMoveReason.INITIAL_LOCATION,
            ) {
                map.moveCamera(
                    CameraUpdateFactory.newCenterPosition(location, DEFAULT_ZOOM),
                    CameraAnimation.from(300),
                )
            }
        }
    }

    // 지역 선택은 검색 화면에서 돌아오는 중에 도착해 지도가 아직 다시 만들어지지 않았을 수 있다.
    // 지도가 준비될 때까지 보관했다가 한 번만 소비해, 이후 재진입에서는 이동이 반복되지 않게 한다.
    LaunchedEffect(kakaoMap, pendingRegionMove) {
        val map = kakaoMap ?: return@LaunchedEffect
        val region = pendingRegionMove ?: return@LaunchedEffect
        pendingRegionMove = null
        mapState.hasUserChosenMapViewport = true
        mapState.isAtCurrentLocation = false
        mapState.moveWithSearch(
            target = region.point,
            targetZoom = region.zoomLevel,
            reason = MapSearchMoveReason.REGION,
        ) {
            map.moveCamera(
                CameraUpdateFactory.newCenterPosition(
                    LatLng.from(region.point.lat, region.point.lng),
                    region.zoomLevel,
                ),
                CameraAnimation.from(300),
            )
        }
    }

    LaunchedEffect(kakaoMap, permissionGranted, currentLocation, deviceHeading.value, currentLocationMarkerColor) {
        val map = kakaoMap ?: return@LaunchedEffect
        val location = currentLocation
        if (!permissionGranted || location == null) {
            map.clearCurrentLocationMarker()
        } else {
            map.renderCurrentLocationMarker(context, location, deviceHeading.value, currentLocationMarkerColor)
        }
    }

    LaunchedEffect(
        kakaoMap,
        state.coordinates,
        state.surfaceState,
        mapState.zoomLevel,
        mapState.mapViewSize,
        mapContentBottomPaddingPx,
        mapBitmapStyle,
        mapState.currentViewport,
        state.searchedQuery,
        mapState.activeClusterMemberIds,
    ) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (state.surfaceState == HomeSurfaceState.Detail) return@LaunchedEffect
        map.clearCourse()
        val markerViewport = markerViewportOrNull(mapState.currentViewport, state.searchedQuery)
        if (state.coordinates.isEmpty() || markerViewport == null) {
            map.clearBrowseLabels()
            return@LaunchedEffect
        }
        val clusterScopedCoordinates = mapState.activeClusterMemberIds?.let { memberIds ->
            state.coordinates.filter { it.id in memberIds }
        } ?: state.coordinates
        val visibleCoordinates = clusterScopedCoordinates.filter { markerViewport.contains(it.point) }
        when (val policy = ClusterPolicy.forZoom(mapState.zoomLevel)) {
            null -> {
                map.renderIndividualMarkers(context, visibleCoordinates, mapBitmapStyle)
            }

            else -> {
                val clusters = MapClusterer.clusterByScreenDistance(
                    items = visibleCoordinates.mapNotNull { place ->
                        val point = map.toScreenPoint(LatLng.from(place.point.lat, place.point.lng))
                            ?: return@mapNotNull null
                        ProjectedMapItem(place.id, place.point, point.x, point.y)
                    },
                    viewport = map.visibleViewportOrNull(mapState.mapViewSize)?.screen ?: return@LaunchedEffect,
                    minimumDistancePx = clusterDistancePx,
                    targetZoom = policy.targetZoom,
                )
                map.renderClusters(
                    context = context,
                    clusters = clusters,
                    placesById = visibleCoordinates.associateBy { it.id },
                    style = mapBitmapStyle,
                )
            }
        }
    }

    LaunchedEffect(
        kakaoMap,
        state.surfaceState,
        selectedDetailPlaceId,
        state.selectedRoute,
        mapContentBottomPaddingPx,
        colors,
    ) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (state.surfaceState != HomeSurfaceState.Detail || mapContentBottomPaddingPx <= 0) {
            return@LaunchedEffect
        }
        val place = state.selectedPlace ?: return@LaunchedEffect
        when (place.type) {
            PlaceType.PARKING -> {
                val coordinate = state.coordinates.firstOrNull { it.id == place.id }
                if (coordinate != null) {
                    if (!map.selectParkingMarker(context, coordinate.id)) {
                        map.renderSelectedParkingMarker(context, coordinate)
                    }
                    map.focusOn(
                        position = LatLng.from(coordinate.point.lat, coordinate.point.lng),
                        zoomLevel = 15,
                        bottomPaddingPx = mapContentBottomPaddingPx,
                    )
                }
            }

            PlaceType.COURSE -> {
                map.clearBrowseLabels()
                val route = state.selectedRoute
                if (route == null) {
                    map.renderPlaceCourseMarkers(context, place)
                } else {
                    val routePoints = route.points.map { LatLng.from(it.lat, it.lng) }
                    map.renderPlaceCourse(
                        context = context,
                        place = place,
                        routePoints = routePoints,
                        snappedPoints = route.snappedPoints.map { LatLng.from(it.lat, it.lng) },
                        routeLineColors = RouteLineColors(
                            lineColor = colors.primary600.toArgb(),
                            strokeColor = colors.primary800.toArgb(),
                        ),
                    )
                    if (mapContentBottomPaddingPx > 0) {
                        map.fitCourseToScreen(routePoints, mapContentTopPaddingPx, mapContentBottomPaddingPx)
                    }
                }
            }
        }
    }

    LaunchedEffect(
        kakaoMap,
        mapContentTopPaddingPx,
        mapBrandOffset,
        mapScaleBarOffset,
        mapContentBottomPaddingPx,
    ) {
        val map = kakaoMap ?: return@LaunchedEffect
        map.applyMapContentPadding(mapContentTopPaddingPx, mapContentBottomPaddingPx)
        val mapPaddingCompensationPx = -mapContentBottomPaddingPx.toFloat()
        val brandBottomPx = with(density) { mapBrandOffset.toPx() } + mapPaddingCompensationPx
        val scaleBarBottomPx = with(density) { mapScaleBarOffset.toPx() } + mapPaddingCompensationPx
        map.logo?.setPosition(
            MapGravity.BOTTOM or MapGravity.LEFT,
            with(density) { 16.dp.toPx() },
            brandBottomPx,
        )
        map.scaleBar?.apply {
            setAutoHide(false)
            setPosition(
                MapGravity.BOTTOM or MapGravity.RIGHT,
                with(density) { 60.dp.toPx() },
                scaleBarBottomPx,
            )
            show()
        }
    }

    HomeContent(
        state = state,
        reviewState = reviewState,
        overlay = overlay,
        mapScreenState = mapLoad.screenState,
        isAtCurrentLocation = mapState.isAtCurrentLocation,
        snackbarHostState = snackbarHostState,
        sheet = HomeContentSheetLayout(
            isContainerMeasured = containerSize.height > 0,
            listSheetDrag = listSheetDrag,
            listSheetOffsetPx = listSheetOffsetPx,
            listSheetProgress = listSheetProgress,
            listHeaderHeightPx = listHeaderHeightPx,
            listViewportHeightPx = listViewportHeightPx,
            bottomControlOffsetPx = bottomControlOffsetPx,
        ),
        actions = HomeContentActions(
            onSearchClick = {
                vm.onIntent(
                    HomeIntent.OnSearchClick(
                        mapState.currentViewport?.toQuery(currentLocation)?.origin,
                    ),
                )
            },
            onResearchClick = {
                val viewport = when (state.surfaceState) {
                    HomeSurfaceState.PartialList,
                    HomeSurfaceState.FullList,
                    -> kakaoMap?.viewportAboveBottomInsetOrNull(
                        size = mapState.mapViewSize,
                        bottomInsetPx = visibleSheetHeightPx().roundToInt(),
                    )

                    else -> mapState.currentViewport
                }
                if (viewport != null) {
                    mapState.hasUserChosenMapViewport = true
                    vm.onIntent(HomeIntent.OnResearch(viewport.toQuery(currentLocation)))
                }
            },
            onMyLocationClick = {
                val location = currentLocation
                if (location == null) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                } else {
                    mapState.moveWithSearch(
                        target = GeoPoint(location.latitude, location.longitude),
                        targetZoom = DEFAULT_ZOOM,
                        reason = MapSearchMoveReason.CURRENT_LOCATION,
                    ) {
                        kakaoMap?.apply {
                            moveCamera(
                                CameraUpdateFactory.newCenterPosition(location, DEFAULT_ZOOM),
                                CameraAnimation.from(250),
                            )
                        }
                    }
                    mapState.isAtCurrentLocation = true
                }
            },
            onRetryPlaces = {
                val query = state.searchedQuery
                    ?: mapState.currentViewport?.toQuery(currentLocation)
                query?.let { vm.onIntent(HomeIntent.OnProgrammaticSearch(it)) }
            },
            onRetryMap = ::retryMap,
            onDismissDetail = dismissDetail,
            onDragDismissDetail = dragDismissDetail,
            onNavigate = requestNavigate,
            onLoadReviews = reviewVm::load,
            onSelectReviewLevel = reviewVm::selectLevel,
            onContainerSizeChanged = { containerSize = it },
            onBottomNavigationHeightChanged = { bottomNavigationHeightPx = it },
            onCourseDetailSheetHeightChanged = { courseDetailSheetHeightPx = it },
            onParkingSheetMeasured = { placeId, heightPx ->
                parkingSheetLayout = parkingSheetLayout
                    .forPlace(placeId)
                    .onMeasured(placeId, heightPx)
            },
        ),
        onIntent = vm::onIntent,
        mapView = {
            key(mapLoad.retryKey) {
                val mapView = rememberMapViewWithLifecycle()
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { mapState.mapViewSize = it },
                    factory = {
                        mapView.start(
                            object : MapLifeCycleCallback() {
                                override fun onMapDestroy() = Unit
                                override fun onMapError(error: Exception?) {
                                    kakaoMap = null
                                    mapLoad.onMapError()
                                }
                            },
                            object : KakaoMapReadyCallback() {
                                override fun onMapReady(map: KakaoMap) {
                                    kakaoMap = map
                                    map.setCameraMinLevel(MIN_ZOOM)
                                    map.setGestureEnable(GestureType.Rotate, false)
                                    map.setGestureEnable(GestureType.RotateZoom, false)
                                    map.setGestureEnable(GestureType.Tilt, false)
                                    map.setOnCameraMoveStartListener { _, gesture ->
                                        if (gesture != GestureType.Unknown) {
                                            mapState.onUserGesture()
                                            vm.onIntent(HomeIntent.OnMapGesture)
                                        }
                                    }
                                    map.setOnCameraMoveEndListener { movedMap, _, _ ->
                                        val viewport = movedMap.viewportOrNull(mapState.mapViewSize)
                                        val action = mapState.onCameraMoveEnd(
                                            viewport = viewport,
                                            zoomLevel = movedMap.zoomLevel,
                                            locationState = initialLocationState,
                                            hasCurrentLocation = currentLocation != null,
                                        )
                                        if (viewport != null) {
                                            when (action) {
                                                CameraSettleAction.ProgrammaticSearch -> vm.onIntent(
                                                    HomeIntent.OnProgrammaticSearch(viewport.toQuery(currentLocation)),
                                                )
                                                CameraSettleAction.ViewportSettled -> vm.onIntent(
                                                    HomeIntent.OnViewportSettled(viewport.toQuery(currentLocation)),
                                                )
                                                CameraSettleAction.None -> Unit
                                            }
                                        }
                                        if (mapLoad.onMapRendered()) {
                                            hasLoadedMapInSession = true
                                            context.markMapLoaded()
                                        }
                                    }
                                    map.setOnLabelClickListener { _, _, label ->
                                        when (val tag = label.tag) {
                                            is BrowseLabelTag.Cluster -> {
                                                mapState.hasUserChosenMapViewport = true
                                                val memberPoints = tag.memberPoints.distinct()
                                                val memberBounds = memberPoints.boundsOrNull()
                                                val target = memberBounds?.let {
                                                    GeoPoint(
                                                        lat = (it.northEast.lat + it.southWest.lat) / 2.0,
                                                        lng = (it.northEast.lng + it.southWest.lng) / 2.0,
                                                    )
                                                } ?: tag.point
                                                val canFitBounds = memberPoints.size >= 2 && memberBounds != null &&
                                                    (memberBounds.northEast != memberBounds.southWest)
                                                val cameraUpdate = if (canFitBounds) {
                                                    CameraUpdateFactory.fitMapPoints(
                                                        memberPoints.map { LatLng.from(it.lat, it.lng) }.toTypedArray(),
                                                        clusterFitPaddingPx,
                                                    )
                                                } else {
                                                    CameraUpdateFactory.newCenterPosition(
                                                        LatLng.from(target.lat, target.lng),
                                                        tag.targetZoom,
                                                    )
                                                }
                                                mapState.moveWithSearch(
                                                    target = target,
                                                    targetZoom = tag.targetZoom.takeUnless { canFitBounds },
                                                    reason = MapSearchMoveReason.CLUSTER,
                                                    requiredBounds = memberBounds.takeIf { canFitBounds },
                                                    clusterMemberIds = tag.memberIds,
                                                ) {
                                                    map.moveCamera(cameraUpdate, CameraAnimation.from(350))
                                                }
                                            }

                                            is BrowseLabelTag.Place -> {
                                                if (state.coordinates.firstOrNull { it.id == tag.id }?.type == PlaceType.PARKING) {
                                                    map.selectParkingMarker(context, tag.id)
                                                }
                                                vm.onIntent(
                                                    HomeIntent.OnPlaceClick(
                                                        tag.id,
                                                        HomeDetailOrigin.Map,
                                                    ),
                                                )
                                            }
                                        }
                                        true
                                    }
                                }

                                // 복귀 직후에는 위치 스트림보다 MapView가 먼저 시작될 수 있으므로
                                // 저장된 화면을 첫 프레임 위치로 사용해 현재 위치로 튀는 이동을 막는다.
                                override fun getPosition(): LatLng {
                                    val center = initialMapCenter(
                                        savedViewport = mapState.currentViewport,
                                        currentLocation = currentLocation?.let {
                                            GeoPoint(it.latitude, it.longitude)
                                        },
                                        lastSavedCenter = lastSavedCamera?.center,
                                        fallback = GeoPoint(SEOUL.latitude, SEOUL.longitude),
                                    )
                                    return LatLng.from(center.lat, center.lng)
                                }

                                override fun getZoomLevel(): Int =
                                    if (mapState.currentViewport == null) {
                                        lastSavedCamera?.zoomLevel ?: mapState.zoomLevel
                                    } else {
                                        mapState.zoomLevel
                                    }
                            },
                        )
                        mapView
                    },
                )
            }
        },
        bottomNavigation = bottomNavigation,
    )

    HomeOverlayHost(
        state = state,
        reviewState = reviewState,
        isBlocking = reviewActionsState.isBlocking,
        isDeleting = reviewActionsState.isDeleting,
        overlay = overlay,
        onIntent = vm::onIntent,
        reviewActions = HomeReviewOverlayActions(
            onSelectLevel = reviewVm::selectLevelAndLoadReviews,
            onLoadInitialReviews = reviewVm::loadInitialReviews,
            onLoadNextReviews = reviewVm::loadNextPage,
            onReviewReported = reviewVm::excludeReportedReview,
            onReviewSubmitted = { result ->
                reviewVm.onReviewSubmitted(result)
                reviewVm.refresh()
            },
            onBlockMember = reviewActionsVm::blockMember,
            onDeleteReview = reviewActionsVm::deleteReview,
        ),
        onNavigate = requestNavigate,
        onDismissLogin = dismissLogin,
        onKakaoLoginClick = {
            onRequestKakaoLogin(
                { token -> vm.onIntent(HomeIntent.OnKakaoLoginCredential(token)) },
                { message ->
                    if (message.contains("취소")) dismissLogin()
                    else vm.onIntent(HomeIntent.OnKakaoLoginFailed(message))
                },
            )
        },
        notificationPermissionGranted = context::hasNotificationPermission,
    )
    CollectEffect(reviewActionsVm.effect) { effect ->
        when (effect) {
            is ReviewActionsEffect.Blocked -> {
                reviewVm.excludeMemberReviews(effect.memberId)
                overlay.reviewToBlock = null
                snackbarHostState.show(RodiSnackbarData(message = "사용자를 차단했습니다."))
            }
            is ReviewActionsEffect.BlockFailed -> {
                overlay.reviewToBlock = null
                snackbarHostState.show(RodiSnackbarData(message = effect.message))
            }
            is ReviewActionsEffect.Deleted -> {
                reviewVm.removeReview(effect.reviewId)
                overlay.reviewToDelete = null
                snackbarHostState.show(RodiSnackbarData(message = "후기를 삭제했습니다."))
            }
            is ReviewActionsEffect.DeleteFailed -> {
                snackbarHostState.show(RodiSnackbarData(message = effect.message))
            }
        }
    }
    LaunchedEffect(overlay.ownReviewActionToastMessage) {
        overlay.ownReviewActionToastMessage?.let { message ->
            snackbarHostState.show(RodiSnackbarData(message = message))
            overlay.ownReviewActionToastMessage = null
        }
    }
    // 후기 조회가 실패하면 화면은 "후기 없음"과 구분되지 않는다. 실패를 삼키지 않고 드러낸다.
    LaunchedEffect(reviewState.errorMessage) {
        reviewState.errorMessage?.let { message ->
            snackbarHostState.show(RodiSnackbarData(message = message))
        }
    }
}

private fun MapViewport.toQuery(currentLocation: LatLng?): PlaceViewportQuery {
    val center = GeoPoint(
        lat = (northEast.lat + southWest.lat) / 2.0,
        lng = (northEast.lng + southWest.lng) / 2.0,
    )
    return PlaceViewportQuery(
        southWest = southWest,
        northEast = northEast,
        origin = currentLocation?.let { GeoPoint(it.latitude, it.longitude) } ?: center,
    )
}

/**
 * "물어본 적 있는지"(DataStore 플래그)와 "지금 허용돼 있는지"는 다르다. 한 번 거부한 뒤에도
 * 플래그만 보고 다음 요청을 그냥 통과시키면, 실제로는 여전히 거부 상태인데 추적이 시작된다.
 * 매 요청마다 실제 OS 권한 상태를 다시 확인해야 한다.
 */
private fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

private fun Context.missingDrivingPermissions(): Array<String> = buildList {
    if (!hasLocationPermission()) {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            this@missingDrivingPermissions,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

private fun Array<String>.deniedDrivingPermissionMessage(): String =
    if (contains(Manifest.permission.POST_NOTIFICATIONS)) {
        "알림 권한을 허용해야 운전 상태를 안전하게 표시할 수 있어요."
    } else {
        "위치 권한을 허용해야 운전 상태를 추적할 수 있어요."
    }

@Preview(name = "Home chrome - 375x812", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun HomeChromePreview() {
    RodiTheme {
        Box(Modifier
            .fillMaxSize()
            .background(RodiTheme.colors.gray100)) {
            RodiBottomNavigation(
                selectedDestination = RodiBottomNavigationDestination.Home,
                onHomeClick = {},
                onMyClick = {},
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            MapListButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 78.dp),
            )
            MyLocationButton(
                isActive = false,
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 78.dp),
            )
        }
    }
}

@Preview(
    name = "Home chrome - small large font",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    fontScale = 1.3f,
)
@Composable
private fun HomeChromeSmallPreview() {
    HomeChromePreview()
}

