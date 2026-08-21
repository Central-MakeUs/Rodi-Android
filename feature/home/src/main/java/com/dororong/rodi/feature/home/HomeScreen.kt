package com.dororong.rodi.feature.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.ui.components.RodiBottomNavigation
import com.dororong.rodi.core.ui.components.RodiBottomNavigationDestination
import com.dororong.rodi.core.ui.components.AccountRecoveryDialog
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.components.dialog.LevelUpDialog
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarDuration
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.components.LoginRequiredDialog
import com.dororong.rodi.feature.home.components.HomeSearchBar
import com.dororong.rodi.feature.home.components.MapListButton
import com.dororong.rodi.core.ui.components.map.MapLoadingScreen
import com.dororong.rodi.core.ui.components.map.MapNetworkErrorScreen
import com.dororong.rodi.feature.home.components.MapResearchButton
import com.dororong.rodi.feature.home.components.MyLocationButton
import com.dororong.rodi.feature.home.components.NaviPickerMode
import com.dororong.rodi.feature.home.components.NaviPickerSheet
import com.dororong.rodi.feature.home.detail.CourseDetailSheet
import com.dororong.rodi.feature.home.detail.CourseReviewViewModel
import com.dororong.rodi.feature.home.detail.components.LevelReviewSection
import com.dororong.rodi.feature.home.detail.levelreviews.LevelReviewsOverlay
import com.dororong.rodi.feature.home.review.ReviewWriteScreen
import com.dororong.rodi.feature.home.review.NotificationPermissionDialog
import com.dororong.rodi.feature.home.review.PracticeContinueDialog
import com.dororong.rodi.feature.home.review.PracticePromptDialog
import com.dororong.rodi.feature.home.detail.reviewactions.BlockMemberDialog
import com.dororong.rodi.feature.home.detail.reviewactions.ReviewActionsViewModel
import com.dororong.rodi.feature.home.detail.reviewactions.ReviewReportScreen
import com.dororong.rodi.feature.home.detail.components.ParkingDetailContent
import com.dororong.rodi.feature.home.detail.components.PlaceDetailLoading
import com.dororong.rodi.feature.home.filter.FilterBottomSheet
import com.dororong.rodi.feature.home.list.components.PlaceEmptyContent
import com.dororong.rodi.feature.home.list.components.PlaceListContent
import com.dororong.rodi.feature.home.location.awaitCurrentLocation
import com.dororong.rodi.feature.home.location.currentLocationUpdates
import androidx.core.app.ActivityCompat
import com.dororong.rodi.core.ui.permission.findActivity
import com.dororong.rodi.core.ui.permission.hasLocationPermission
import com.dororong.rodi.core.ui.permission.openAppSettings
import com.dororong.rodi.feature.home.location.rememberDeviceHeading
import com.dororong.rodi.feature.home.map.BrowseLabelTag
import com.dororong.rodi.core.ui.network.isNetworkAvailable
import com.dororong.rodi.core.ui.network.networkAvailabilityFlow
import com.dororong.rodi.feature.home.map.ClusterPolicy
import com.dororong.rodi.feature.home.map.DEFAULT_ZOOM
import com.dororong.rodi.feature.home.map.InitialViewportSearchPolicy
import com.dororong.rodi.feature.home.map.InitialLocationState
import com.dororong.rodi.feature.home.map.MapClusterer
import com.dororong.rodi.feature.home.map.MapBitmapStyle
import com.dororong.rodi.feature.home.map.MapBitmapTextStyle
import com.dororong.rodi.feature.home.map.MapSearchMoveReason
import com.dororong.rodi.feature.home.map.MapScreenState
import com.dororong.rodi.feature.home.map.MapViewport
import com.dororong.rodi.feature.home.map.PendingMapSearch
import com.dororong.rodi.feature.home.map.PendingMapSearchMatcher
import com.dororong.rodi.feature.home.map.ProjectedMapItem
import com.dororong.rodi.feature.home.map.SEOUL
import com.dororong.rodi.feature.home.map.clearBrowseLabels
import com.dororong.rodi.feature.home.map.clearCourse
import com.dororong.rodi.feature.home.map.clearCurrentLocationMarker
import com.dororong.rodi.feature.home.map.deselectParkingMarker
import com.dororong.rodi.feature.home.map.fitCourseToScreen
import com.dororong.rodi.feature.home.map.focusOn
import com.dororong.rodi.feature.home.map.hasLoadedMapBefore
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
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapGravity
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.dororong.rodi.core.ui.R as CoreUiR

private const val CLUSTER_DISTANCE_DP = 56
private const val CLUSTER_FIT_PADDING_DP = 64
private const val SURFACE_ANIMATION_MILLIS = 300
private const val RESEARCH_BUTTON_FADE_IN_MILLIS = 150
private const val RESEARCH_BUTTON_FADE_OUT_MILLIS = 100
private const val LIST_BUTTON_FADE_OUT_MILLIS = 100
private const val LIST_BUTTON_FADE_IN_DELAY_MILLIS = 100
private const val LIST_BUTTON_FADE_IN_MILLIS = 180
private val LIST_BUTTON_VISUAL_OFFSET = 7.dp
private val LIST_SHEET_PEEK_HEIGHT = 380.dp
private val LIST_SHEET_CORNER_RADIUS = 20.dp
private val LIST_SHEET_SHADOW_ELEVATION = 8.dp
private val PARTIAL_LIST_HEADER_HEIGHT = 48.dp
private val FULL_LIST_HEADER_HEIGHT = 64.dp
private val FULL_LIST_CONTENT_TOP_PADDING = 20.dp
private val PARTIAL_LIST_TITLE_TOP_PADDING = 24.dp
private val FULL_LIST_TITLE_TOP_PADDING = 40.dp
private val LIST_HEADER_HORIZONTAL_PADDING = 16.dp
private val BOTTOM_CONTROL_MIN_OFFSET = 68.dp
private val BOTTOM_CONTROL_SHEET_GAP = 12.dp
private const val LIST_TITLE_CENTERING_START = 0.5f
private const val MIN_ZOOM = 6
private const val MAP_RETRY_DEBOUNCE_MILLIS = 1_500L

/** 오프라인이 이만큼 이어지면 지도를 덮고 안내 화면을 띄운다. */
internal const val MAP_NETWORK_ERROR_GRACE_MILLIS = 3_000L
private const val MAP_NETWORK_SNACKBAR_ID = "map-network"
// 주차장 상세는 내용 길이와 무관하게 코스 상세와 같은 높이로 고정한다.
private val PARKING_DETAIL_SHEET_HEIGHT = 400.dp
// HomeSearchBar가 지도 위에 statusBarsPadding() + vertical 5dp로 떠 있는 만큼. 경로 핏 계산에
// 이 높이를 반영하지 않으면 세로로 긴 코스의 출발지·도착지 마커가 검색창 뒤에 가려진다.
private val MAP_SEARCH_BAR_TOP_INSET = 5.dp + 46.dp
private val FILTER_HEADER_ICON_TOUCH_SIZE = 48.dp

typealias KakaoLoginRequest = (
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit,
) -> Unit
typealias DrivingStartRequest = (PlaceDetail) -> Result<String>

private data class ReviewWriteTarget(
    val placeId: Long,
    val placeName: String,
    val review: Review?,
)

private val mapViewportSaver: Saver<MapViewport?, Any> = mapSaver(
    save = { viewport ->
        viewport?.let {
            mapOf(
                "northEastLat" to it.northEast.lat,
                "northEastLng" to it.northEast.lng,
                "southWestLat" to it.southWest.lat,
                "southWestLng" to it.southWest.lng,
            )
        } ?: emptyMap()
    },
    restore = { saved ->
        val northEastLat = saved["northEastLat"] as? Double ?: return@mapSaver null
        val northEastLng = saved["northEastLng"] as? Double ?: return@mapSaver null
        val southWestLat = saved["southWestLat"] as? Double ?: return@mapSaver null
        val southWestLng = saved["southWestLng"] as? Double ?: return@mapSaver null
        MapViewport(
            northEast = GeoPoint(northEastLat, northEastLng),
            southWest = GeoPoint(southWestLat, southWestLng),
        )
    },
)

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
    val state by vm.state.collectAsStateWithLifecycle()
    val reviewState by reviewVm.state.collectAsStateWithLifecycle()
    val reviewActionsState by reviewActionsVm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { RodiSnackbarHostState() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapViewSize by remember { mutableStateOf(IntSize.Zero) }
    var mapZoomLevel by rememberSaveable { mutableIntStateOf(DEFAULT_ZOOM) }
    var currentViewport by rememberSaveable(stateSaver = mapViewportSaver) {
        mutableStateOf<MapViewport?>(null)
    }
    var pendingMapSearch by remember { mutableStateOf<PendingMapSearch?>(null) }
    var activeClusterMemberIds by remember { mutableStateOf<Set<Long>?>(null) }
    var mapSearchGeneration by remember { mutableStateOf(0L) }
    var isInitialLocationCameraMovePending by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var permissionGranted by remember { mutableStateOf(context.hasLocationPermission()) }
    var initialLocationState by remember { mutableStateOf(InitialLocationState.Pending) }
    var mapRetryKey by remember { mutableIntStateOf(0) }
    var lastMapRetryAtMillis by remember { mutableLongStateOf(0L) }
    var hasMapLoadedThisEntry by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(context.isNetworkAvailable()) }
    var showMapNetworkSnackbar by remember { mutableStateOf(!isOnline) }
    // 최초 진입이 오프라인이어도 여기서 곧장 NetworkError로 시작하지 않는다 — 그러면 아래
    // LaunchedEffect(isOnline)의 3초 유예를 건너뛰게 된다. 유예는 그 이펙트가 책임진다.
    var mapScreenState by remember {
        mutableStateOf(
            when {
                hasLoadedMapInSession || context.hasLoadedMapBefore() -> MapScreenState.Ready
                else -> MapScreenState.Loading
            },
        )
    }
    var isAtCurrentLocation by remember { mutableStateOf(false) }
    var hasUserMovedMap by rememberSaveable { mutableStateOf(false) }
    var hasUserChosenMapViewport by rememberSaveable { mutableStateOf(false) }
    var hasCenteredInitialLocation by rememberSaveable { mutableStateOf(false) }
    var naviPlaceId by remember { mutableStateOf<Long?>(null) }
    var installNaviPlaceId by remember { mutableStateOf<Long?>(null) }
    var pendingDrivingEffect by remember { mutableStateOf<HomeEffect?>(null) }
    var courseDetailSheetHeightPx by remember { mutableIntStateOf(0) }
    var parkingSheetLayout by remember { mutableStateOf(ParkingSheetLayoutState()) }
    var bottomNavigationHeightPx by remember { mutableIntStateOf(0) }
    var reviewToReport by remember { mutableStateOf<Review?>(null) }
    var reviewToBlock by remember { mutableStateOf<Review?>(null) }
    var reviewToDelete by remember { mutableStateOf<Review?>(null) }
    var reviewToWrite by remember { mutableStateOf<ReviewWriteTarget?>(null) }
    var ownReviewActionToastMessage by remember { mutableStateOf<String?>(null) }
    var restoredViewportMap by remember { mutableStateOf<KakaoMap?>(null) }
    fun handleReportReviewClick(review: Review) {
        if (review.isMine) {
            ownReviewActionToastMessage = "내가 쓴 후기는 신고할 수 없습니다"
        } else {
            reviewToReport = review
        }
    }

    fun handleBlockMemberClick(review: Review) {
        if (review.isMine) {
            ownReviewActionToastMessage = "내가 쓴 후기는 차단할 수 없습니다"
        } else {
            reviewToBlock = review
        }
    }

    fun updateCurrentViewport(viewport: MapViewport?) {
        currentViewport = viewport
    }
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

    suspend fun launchDriving(effect: HomeEffect) {
        val place = when (effect) {
            is HomeEffect.LaunchKakaoMap -> effect.place
            is HomeEffect.LaunchKakaoNavi -> effect.place
            else -> return
        }
        val shouldStartDriving = when (effect) {
            is HomeEffect.LaunchKakaoMap -> effect.startDriving
            is HomeEffect.LaunchKakaoNavi -> effect.startDriving
            else -> false
        }
        if (shouldStartDriving) {
            val startResult = onStartDriving(place)
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
        when (effect) {
            is HomeEffect.LaunchKakaoMap -> KakaoMapLauncher.launch(context, place)
            is HomeEffect.LaunchKakaoNavi -> KakaoNaviLauncher.launch(context, place)
            else -> Unit
        }
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
                    when (pending) {
                        is HomeEffect.LaunchKakaoMap -> KakaoMapLauncher.launch(context, pending.place)
                        is HomeEffect.LaunchKakaoNavi -> KakaoNaviLauncher.launch(context, pending.place)
                        else -> Unit
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = context.hasLocationPermission()
                hasCenteredInitialLocation = false
                hasUserMovedMap = false
                hasUserChosenMapViewport = false
                // 설정에서 차단을 풀거나 내 활동에서 후기를 고치고 돌아올 수 있다.
                // 열려 있는 장소가 없으면 refresh는 아무 것도 하지 않는다.
                reviewVm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        hasCenteredInitialLocation = false
        hasUserMovedMap = false
        hasUserChosenMapViewport = false
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

    val shouldShowResearch = state.surfaceState != HomeSurfaceState.Detail && state.isMapSearchDirty
    val showSearchBackButton = state.searchKeyword != null || state.detailOrigin == HomeDetailOrigin.List

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

    val handleSystemBack: () -> Unit = {
        if (state.isFilterSheetVisible) {
            if (!state.isFilterSaving) vm.onIntent(HomeIntent.OnFilterDismiss)
        } else {
            when (state.surfaceState) {
                HomeSurfaceState.Detail -> dismissDetail()
                else -> vm.onIntent(HomeIntent.OnListCollapse)
            }
        }
    }
    BackHandler(enabled = state.isFilterSheetVisible || state.surfaceState != HomeSurfaceState.Navigation) {
        handleSystemBack()
    }

    CollectEffect(vm.effect) { effect ->
        when (effect) {
            is HomeEffect.LaunchKakaoMap,
            is HomeEffect.LaunchKakaoNavi,
            -> {
                val shouldStartDriving = when (effect) {
                    is HomeEffect.LaunchKakaoMap -> effect.startDriving
                    is HomeEffect.LaunchKakaoNavi -> effect.startDriving
                    else -> false
                }
                if (!shouldStartDriving) {
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
            is HomeEffect.ShowNaviPicker -> naviPlaceId = effect.place.id
            is HomeEffect.ShowInstallNaviPicker -> installNaviPlaceId = effect.place.id
            is HomeEffect.OpenPracticeReview -> {
                reviewToWrite = ReviewWriteTarget(effect.placeId, effect.placeName, null)
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
        if (!isOnline) {
            mapScreenState = MapScreenState.NetworkError
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastMapRetryAtMillis < MAP_RETRY_DEBOUNCE_MILLIS) return
        lastMapRetryAtMillis = now
        if (mapScreenState != MapScreenState.Ready) mapScreenState = MapScreenState.Loading
        kakaoMap = null
        mapRetryKey += 1
    }

    val networkErrorSnackbarIcon = painterResource(CoreUiR.drawable.ic_alert_circle)
    LaunchedEffect(showMapNetworkSnackbar) {
        if (showMapNetworkSnackbar) {
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
    LaunchedEffect(state.surfaceState, currentViewport) {
        val isDetail = state.surfaceState == HomeSurfaceState.Detail
        if (wasDetailSurface && !isDetail) {
            currentViewport?.let { viewport ->
                vm.onIntent(HomeIntent.OnProgrammaticSearch(viewport.toQuery(currentLocation)))
            }
        }
        wasDetailSurface = isDetail
    }

    LaunchedEffect(Unit) {
        networkAvailabilityFlow(context).collect { isOnline = it }
    }

    // 끊기자마자 지도를 덮으면 잠깐 끊겼다 붙는 구간에서 화면이 번쩍인다. 토스트는 바로,
    // 안내 화면은 유예 시간을 넘겨 계속 끊겨 있을 때만 덮는다(iOS와 동일).
    // isOnline이 다시 true가 되면 이 이펙트가 재시작되며 delay가 취소돼 원래 화면으로 돌아온다.
    LaunchedEffect(isOnline) {
        if (isOnline) {
            if (mapScreenState == MapScreenState.NetworkError || showMapNetworkSnackbar) retryMap()
        } else {
            showMapNetworkSnackbar = true
            delay(MAP_NETWORK_ERROR_GRACE_MILLIS)
            mapScreenState = MapScreenState.NetworkError
        }
    }

    LaunchedEffect(kakaoMap, mapViewSize) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (mapViewSize.width <= 0 || mapViewSize.height <= 0 || restoredViewportMap === map) {
            return@LaunchedEffect
        }
        restoredViewportMap = map
        val viewport = currentViewport ?: return@LaunchedEffect
        hasCenteredInitialLocation = true
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(
                    (viewport.northEast.lat + viewport.southWest.lat) / 2,
                    (viewport.northEast.lng + viewport.southWest.lng) / 2,
                ),
                mapZoomLevel,
            ),
            CameraAnimation.from(0),
        )
    }

    LaunchedEffect(kakaoMap, mapViewSize, currentLocation, initialLocationState) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (!InitialViewportSearchPolicy.canDispatch(
                locationState = initialLocationState,
                hasCurrentLocation = currentLocation != null,
                hasCenteredInitialLocation = hasCenteredInitialLocation,
                isInitialLocationCameraMovePending = isInitialLocationCameraMovePending,
            )
        ) {
            return@LaunchedEffect
        }
        val viewport = map.viewportOrNull(mapViewSize) ?: return@LaunchedEffect
        updateCurrentViewport(viewport)
        vm.onIntent(HomeIntent.OnViewportSettled(viewport.toQuery(currentLocation)))
    }

    LaunchedEffect(kakaoMap, permissionGranted) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (!permissionGranted) return@LaunchedEffect
        // 상세 화면(Detail)에서는 선택한 장소를 보여주는 별도 카메라 포커스 이펙트가 있다 —
        // 여기서 현위치로 재센터링하면 그 포커스를 덮어써 버리므로 건너뛴다.
        if (state.surfaceState == HomeSurfaceState.Detail) return@LaunchedEffect
        val location = snapshotFlow { currentLocation }.filterNotNull().first()
        if (!hasCenteredInitialLocation && !hasUserMovedMap && !hasUserChosenMapViewport) {
            activeClusterMemberIds = null
            hasCenteredInitialLocation = true
            isInitialLocationCameraMovePending = true
            mapSearchGeneration += 1
            pendingMapSearch = PendingMapSearch(
                generation = mapSearchGeneration,
                target = GeoPoint(location.latitude, location.longitude),
                targetZoom = DEFAULT_ZOOM,
                reason = MapSearchMoveReason.INITIAL_LOCATION,
            )
            map.moveCamera(
                CameraUpdateFactory.newCenterPosition(location, DEFAULT_ZOOM),
                CameraAnimation.from(300),
            )
        }
    }

    var consumedRegionSearchGeneration by remember { mutableStateOf(0L) }
    LaunchedEffect(kakaoMap, state.regionSearchGeneration) {
        val map = kakaoMap ?: return@LaunchedEffect
        val region = state.regionSearch ?: return@LaunchedEffect
        if (state.regionSearchGeneration == 0L) return@LaunchedEffect
        if (state.regionSearchGeneration == consumedRegionSearchGeneration) return@LaunchedEffect
        consumedRegionSearchGeneration = state.regionSearchGeneration
        activeClusterMemberIds = null
        hasUserChosenMapViewport = true
        isAtCurrentLocation = false
        mapSearchGeneration += 1
        pendingMapSearch = PendingMapSearch(
            generation = mapSearchGeneration,
            target = region.point,
            targetZoom = region.zoomLevel,
            reason = MapSearchMoveReason.REGION,
        )
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(region.point.lat, region.point.lng),
                region.zoomLevel,
            ),
            CameraAnimation.from(300),
        )
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
        mapZoomLevel,
        mapViewSize,
        mapContentBottomPaddingPx,
        mapBitmapStyle,
        currentViewport,
        state.searchedQuery,
        activeClusterMemberIds,
    ) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (state.surfaceState == HomeSurfaceState.Detail) return@LaunchedEffect
        map.clearCourse()
        val markerViewport = markerViewportOrNull(currentViewport, state.searchedQuery)
        if (state.coordinates.isEmpty() || markerViewport == null) {
            map.clearBrowseLabels()
            return@LaunchedEffect
        }
        val clusterScopedCoordinates = activeClusterMemberIds?.let { memberIds ->
            state.coordinates.filter { it.id in memberIds }
        } ?: state.coordinates
        val visibleCoordinates = clusterScopedCoordinates.filter { markerViewport.contains(it.point) }
        when (val policy = ClusterPolicy.forZoom(mapZoomLevel)) {
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
                    viewport = map.visibleViewportOrNull(mapViewSize)?.screen ?: return@LaunchedEffect,
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .onSizeChanged { containerSize = it },
            ) {
                Box(Modifier.fillMaxSize()) {
                        key(mapRetryKey) {
                            val mapView = rememberMapViewWithLifecycle()
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { mapViewSize = it },
                                factory = {
                                    mapView.start(
                                        object : MapLifeCycleCallback() {
                                            override fun onMapDestroy() = Unit
                                            override fun onMapError(error: Exception?) {
                                                kakaoMap = null
                                                // SDK 초기화·렌더링 실패도 이 콜백을 타므로, 온라인
                                                // 상태에서까지 "네트워크 연결이 원활하지 않아요"로
                                                // 안내하면 원인과 다른 메시지가 뜬다.
                                                if (isOnline) {
                                                    showMapNetworkSnackbar = false
                                                    mapScreenState = MapScreenState.Error
                                                } else {
                                                    // 오프라인 안내 화면 전환은 3초 유예를 갖고 있는
                                                    // LaunchedEffect(isOnline)에 맡긴다.
                                                    showMapNetworkSnackbar = true
                                                }
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
                                                        isAtCurrentLocation = false
                                                        hasUserMovedMap = true
                                                        hasUserChosenMapViewport = true
                                                        pendingMapSearch = null
                                                        activeClusterMemberIds = null
                                                        isInitialLocationCameraMovePending = false
                                                        vm.onIntent(HomeIntent.OnMapGesture)
                                                    }
                                                }
                                                map.setOnCameraMoveEndListener { movedMap, _, _ ->
                                                    mapZoomLevel = movedMap.zoomLevel
                                                    movedMap.viewportOrNull(mapViewSize)?.let { viewport ->
                                                        updateCurrentViewport(viewport)
                                                        val pending = pendingMapSearch
                                                        if (
                                                            pending != null &&
                                                            PendingMapSearchMatcher.matches(
                                                                pending = pending,
                                                                viewport = viewport,
                                                                zoomLevel = movedMap.zoomLevel,
                                                            )
                                                        ) {
                                                            pendingMapSearch = null
                                                            isInitialLocationCameraMovePending = false
                                                            vm.onIntent(
                                                                HomeIntent.OnProgrammaticSearch(
                                                                    viewport.toQuery(currentLocation),
                                                                ),
                                                            )
                                                        } else if (
                                                            pending != null &&
                                                            pending.generation != mapSearchGeneration
                                                        ) {
                                                            pendingMapSearch = null
                                                        } else if (
                                                            pending == null &&
                                                            InitialViewportSearchPolicy.canDispatch(
                                                                locationState = initialLocationState,
                                                                hasCurrentLocation = currentLocation != null,
                                                                hasCenteredInitialLocation = hasCenteredInitialLocation,
                                                                isInitialLocationCameraMovePending = isInitialLocationCameraMovePending,
                                                            )
                                                        ) {
                                                            vm.onIntent(
                                                                HomeIntent.OnViewportSettled(
                                                                    viewport.toQuery(currentLocation),
                                                                ),
                                                            )
                                                        }
                                                    }
                                                    if (isOnline) {
                                                        hasLoadedMapInSession = true
                                                        hasMapLoadedThisEntry = true
                                                        context.markMapLoaded()
                                                        mapScreenState = MapScreenState.Ready
                                                        showMapNetworkSnackbar = false
                                                    }
                                                }
                                                map.setOnLabelClickListener { _, _, label ->
                                                    when (val tag = label.tag) {
                                                        is BrowseLabelTag.Cluster -> {
                                                            hasUserChosenMapViewport = true
                                                            mapSearchGeneration += 1
                                                            val memberPoints = tag.memberPoints.distinct()
                                                            activeClusterMemberIds = tag.memberIds
                                                            val memberBounds = memberPoints.boundsOrNull()
                                                            val target = memberBounds?.let {
                                                                GeoPoint(
                                                                    lat = (it.northEast.lat + it.southWest.lat) / 2.0,
                                                                    lng = (it.northEast.lng + it.southWest.lng) / 2.0,
                                                                )
                                                            } ?: tag.point
                                                            val canFitBounds = memberPoints.size >= 2 && memberBounds != null &&
                                                                (memberBounds.northEast != memberBounds.southWest)
                                                            pendingMapSearch = PendingMapSearch(
                                                                generation = mapSearchGeneration,
                                                                target = target,
                                                                targetZoom = tag.targetZoom.takeUnless { canFitBounds },
                                                                reason = MapSearchMoveReason.CLUSTER,
                                                                requiredBounds = memberBounds.takeIf { canFitBounds },
                                                            )
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
                                                            map.moveCamera(cameraUpdate, CameraAnimation.from(350))
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
                                                    savedViewport = currentViewport,
                                                    currentLocation = currentLocation?.let {
                                                        GeoPoint(it.latitude, it.longitude)
                                                    },
                                                    fallback = GeoPoint(SEOUL.latitude, SEOUL.longitude),
                                                )
                                                return LatLng.from(center.lat, center.lng)
                                            }

                                            override fun getZoomLevel(): Int = mapZoomLevel
                                        },
                                    )
                                    mapView
                                },
                            )
                        }

                        HomeSearchBar(
                            onClick = {
                                if (showSearchBackButton) {
                                    handleSystemBack()
                                } else {
                                    vm.onIntent(
                                        HomeIntent.OnSearchClick(
                                            currentViewport?.toQuery(currentLocation)?.origin,
                                        ),
                                    )
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
                            MapResearchButton(
                                onClick = {
                                    val viewport = when (state.surfaceState) {
                                        HomeSurfaceState.PartialList,
                                        HomeSurfaceState.FullList,
                                        -> kakaoMap?.viewportAboveBottomInsetOrNull(
                                            size = mapViewSize,
                                            bottomInsetPx = visibleSheetHeightPx().roundToInt(),
                                        )

                                        else -> currentViewport
                                    } ?: return@MapResearchButton
                                    hasUserChosenMapViewport = true
                                    vm.onIntent(HomeIntent.OnResearch(viewport.toQuery(currentLocation)))
                                },
                            )
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
                                onClick = { vm.onIntent(HomeIntent.OnListOpen) },
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
                                onClick = {
                                    val location = currentLocation
                                    if (location == null) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            ),
                                        )
                                    } else {
                                        activeClusterMemberIds = null
                                        mapSearchGeneration += 1
                                        pendingMapSearch = PendingMapSearch(
                                            generation = mapSearchGeneration,
                                            target = GeoPoint(location.latitude, location.longitude),
                                            targetZoom = DEFAULT_ZOOM,
                                            reason = MapSearchMoveReason.CURRENT_LOCATION,
                                        )
                                        kakaoMap?.apply {
                                            moveCamera(
                                                CameraUpdateFactory.newCenterPosition(location, DEFAULT_ZOOM),
                                                CameraAnimation.from(250),
                                            )
                                        }
                                        isAtCurrentLocation = true
                                    }
                                },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .onSizeChanged { bottomNavigationHeightPx = it.height },
                        ) {
                            bottomNavigation()
                        }
                }

                // 앵커가 잡히기 전에는 offset이 NaN이라 시트가 펼쳐진 위치에 그려진다. 첫 측정 전까지 미룬다.
                if (containerSize.height > 0) {
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
                                    onBack = { vm.onIntent(HomeIntent.OnListCollapse) },
                                    onFilterClick = { vm.onIntent(HomeIntent.OnFilterOpen) },
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
                                    onRetry = {
                                        val query = state.searchedQuery
                                            ?: currentViewport?.toQuery(currentLocation)
                                        query?.let { vm.onIntent(HomeIntent.OnProgrammaticSearch(it)) }
                                    },
                                    dragHandleModifier = listSheetDrag,
                                )

                                else -> key(state.placeListGeneration) {
                                    PlaceListContent(
                                        places = state.places,
                                        onPlaceClick = {
                                            vm.onIntent(HomeIntent.OnPlaceClick(it, HomeDetailOrigin.List))
                                        },
                                        onLoadNextPage = { vm.onIntent(HomeIntent.OnLoadNextPage) },
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
                            onDismiss = dismissDetail,
                            onBookmarkClick = { vm.onIntent(HomeIntent.OnBookmarkClick) },
                            onNavigate = {
                                vm.onIntent(
                                    HomeIntent.OnNavigateClick(
                                        kakaoMapInstalled = context.isPackageInstalled("net.daum.android.map"),
                                        kakaoNaviInstalled = context.isPackageInstalled("com.locnall.KimGiSa"),
                                        notificationPermissionGranted = context.hasNotificationPermission(),
                                    ),
                                )
                            },
                            onSheetHeightChanged = { height -> courseDetailSheetHeightPx = height },
                            reviewContent = { sheetScrollState ->
                                LaunchedEffect(selectedPlace.id) { reviewVm.load(selectedPlace.id) }
                                if (!reviewState.isGuest) {
                                    LevelReviewSection(
                                        totalCount = reviewState.totalCount,
                                        recommendCount = reviewState.recommendCount,
                                        selectedLevel = reviewState.selectedLevel,
                                        difficultyCounts = reviewState.difficultyCounts,
                                        review = reviewState.latestReviews.firstOrNull(),
                                        onSelectLevel = reviewVm::selectLevel,
                                        onAllClick = { vm.onIntent(HomeIntent.OnLevelReviewsOpen) },
                                        onWriteReviewClick = { reviewToWrite = ReviewWriteTarget(selectedPlace.id, selectedPlace.name, null) },
                                        onEditReviewClick = { reviewToWrite = ReviewWriteTarget(selectedPlace.id, selectedPlace.name, it) },
                                        onDeleteReviewClick = { reviewToDelete = it },
                                        onReportReviewClick = ::handleReportReviewClick,
                                        onBlockMemberClick = ::handleBlockMemberClick,
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
                                .collect { if (it == DetailSheetValue.Dismissed) dragDismissDetail() }
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
                                                    parkingSheetLayout = parkingSheetLayout
                                                        .forPlace(placeId)
                                                        .onMeasured(placeId, size.height)
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
                                    onDismiss = dismissDetail,
                                    dragHandleModifier = detailSheetDrag,
                                    onBookmarkClick = { vm.onIntent(HomeIntent.OnBookmarkClick) },
                                    onNavigate = {
                                        vm.onIntent(
                                            HomeIntent.OnNavigateClick(
                                                kakaoMapInstalled = context.isPackageInstalled("net.daum.android.map"),
                                                kakaoNaviInstalled = context.isPackageInstalled("com.locnall.KimGiSa"),
                                                notificationPermissionGranted = context.hasNotificationPermission(),
                                            ),
                                        )
                                    },
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
                        onRetry = ::retryMap,
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

    if (state.pendingAction != null && !state.hasPendingRestore) {
        LoginRequiredDialog(
            isLoggingIn = state.isLoginInProgress,
            onDismiss = dismissLogin,
            onKakaoLoginClick = {
                onRequestKakaoLogin(
                    { token -> vm.onIntent(HomeIntent.OnKakaoLoginCredential(token)) },
                    { message ->
                        if (message.contains("취소")) dismissLogin()
                        else vm.onIntent(HomeIntent.OnKakaoLoginFailed(message))
                    },
                )
            },
        )
    }

    val levelReviewsPlace = state.selectedPlace
    if (state.isLevelReviewsVisible && levelReviewsPlace?.type == PlaceType.COURSE) {
        LevelReviewsOverlay(
            recommendCount = reviewState.recommendCount,
            selectedLevel = reviewState.selectedLevel,
            difficultyCounts = reviewState.difficultyCounts,
            reviews = reviewState.reviews,
            isBookmarked = levelReviewsPlace.isBookmarked,
            isBookmarkUpdating = state.isBookmarkUpdating,
            onClose = { vm.onIntent(HomeIntent.OnLevelReviewsClose) },
            onSelectLevel = reviewVm::selectLevelAndLoadReviews,
            onLoadInitial = reviewVm::loadInitialReviews,
            onLoadNext = reviewVm::loadNextPage,
            onBookmarkClick = { vm.onIntent(HomeIntent.OnBookmarkClick) },
            onNavigate = {
                vm.onIntent(
                    HomeIntent.OnNavigateClick(
                        kakaoMapInstalled = context.isPackageInstalled("net.daum.android.map"),
                        kakaoNaviInstalled = context.isPackageInstalled("com.locnall.KimGiSa"),
                        notificationPermissionGranted = context.hasNotificationPermission(),
                    ),
                )
            },
            onEditReviewClick = { reviewToWrite = ReviewWriteTarget(levelReviewsPlace.id, levelReviewsPlace.name, it) },
            onDeleteReviewClick = { reviewToDelete = it },
            onReportReviewClick = ::handleReportReviewClick,
            onBlockMemberClick = ::handleBlockMemberClick,
        )
    }
    reviewToReport?.let { review ->
        ReviewReportScreen(
            reviewId = review.reviewId,
            onClose = { reviewToReport = null },
            modifier = Modifier.fillMaxSize(),
            onReported = reviewVm::excludeReportedReview,
        )
    }
    reviewToWrite?.let { target ->
        ReviewWriteScreen(
            placeId = target.placeId,
            placeName = target.placeName,
            editingReviewId = target.review?.reviewId,
            onClose = { reviewToWrite = null },
            onCompleted = { result ->
                reviewToWrite = null
                reviewVm.onReviewSubmitted(result)
                reviewVm.refresh()
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
    state.practicePrompt?.let { session ->
        PracticePromptDialog(
            practice = session,
            onVisited = {
                vm.onIntent(HomeIntent.OnPracticePromptVisited)
            },
            onNotVisited = {
                vm.onIntent(HomeIntent.OnPracticePromptNotVisited)
            },
            onDismiss = { vm.onIntent(HomeIntent.OnPracticePromptDismiss) },
        )
    }
    state.activePracticeSession
        ?.takeIf { state.isPracticeContinueDialogVisible }
        ?.let { session ->
            PracticeContinueDialog(
                placeName = session.placeName,
                onContinue = { vm.onIntent(HomeIntent.OnPracticeContinueMeasurement) },
                onStop = { vm.onIntent(HomeIntent.OnPracticeStopMeasurement) },
                onDismiss = { vm.onIntent(HomeIntent.OnPracticeContinueMeasurement) },
            )
        }
    if (state.isNotificationPermissionRationaleVisible) {
        NotificationPermissionDialog(
            onAllow = { vm.onIntent(HomeIntent.OnNotificationPermissionAllow) },
            onRouteOnly = { vm.onIntent(HomeIntent.OnNotificationPermissionRouteOnly) },
        )
    }
    state.levelUp?.let { level ->
        LevelUpDialog(
            level = level,
            onConfirm = { vm.onIntent(HomeIntent.OnLevelUpDismiss) },
            onDismissRequest = { vm.onIntent(HomeIntent.OnLevelUpDismiss) },
        )
    }
    reviewToBlock?.let { review ->
        BlockMemberDialog(
            isBlocking = reviewActionsState.isBlocking,
            onConfirm = { reviewActionsVm.blockMember(review.memberId) },
            onDismiss = { reviewToBlock = null },
        )
    }
    reviewToDelete?.let { review ->
        RodiAlertDialog(
            title = "후기를 삭제할까요?",
            description = "삭제한 후기는 되돌릴 수 없어요.",
            confirmText = if (reviewActionsState.isDeleting) "삭제 중" else "삭제",
            dismissText = "취소",
            enabled = !reviewActionsState.isDeleting,
            dismissible = !reviewActionsState.isDeleting,
            onConfirm = { reviewActionsVm.deleteReview(review.reviewId) },
            onDismiss = { reviewToDelete = null },
            onDismissRequest = { if (!reviewActionsState.isDeleting) reviewToDelete = null },
        )
    }
    LaunchedEffect(reviewActionsState.blockedMemberId, reviewActionsState.blockErrorMessage) {
        when {
            reviewActionsState.blockedMemberId != null -> {
                reviewVm.excludeMemberReviews(reviewActionsState.blockedMemberId ?: return@LaunchedEffect)
                reviewToBlock = null
                snackbarHostState.show(RodiSnackbarData(message = "사용자를 차단했습니다."))
                reviewActionsVm.consumeBlockResult()
            }

            reviewActionsState.blockErrorMessage != null -> {
                reviewToBlock = null
                snackbarHostState.show(
                    RodiSnackbarData(
                        message = reviewActionsState.blockErrorMessage ?: "사용자를 차단할 수 없습니다.",
                    ),
                )
                reviewActionsVm.consumeBlockResult()
            }
        }
    }
    LaunchedEffect(reviewActionsState.deletedReviewId, reviewActionsState.deleteErrorMessage) {
        when {
            reviewActionsState.deletedReviewId != null -> {
                reviewVm.removeReview(reviewActionsState.deletedReviewId ?: return@LaunchedEffect)
                reviewToDelete = null
                snackbarHostState.show(RodiSnackbarData(message = "후기를 삭제했습니다."))
                reviewActionsVm.consumeDeleteResult()
            }
            reviewActionsState.deleteErrorMessage != null -> {
                snackbarHostState.show(RodiSnackbarData(message = reviewActionsState.deleteErrorMessage ?: "후기를 삭제할 수 없습니다."))
                reviewActionsVm.consumeDeleteResult()
            }
        }
    }
    LaunchedEffect(ownReviewActionToastMessage) {
        ownReviewActionToastMessage?.let { message ->
            snackbarHostState.show(RodiSnackbarData(message = message))
            ownReviewActionToastMessage = null
        }
    }
    // 후기 조회가 실패하면 화면은 "후기 없음"과 구분되지 않는다. 실패를 삼키지 않고 드러낸다.
    LaunchedEffect(reviewState.errorMessage) {
        reviewState.errorMessage?.let { message ->
            snackbarHostState.show(RodiSnackbarData(message = message))
        }
    }
    LaunchedEffect(state.reviewRefreshGeneration) {
        if (state.reviewRefreshGeneration > 0) reviewVm.refresh()
    }
    if (state.hasPendingRestore) {
        AccountRecoveryDialog(
            isRestoring = state.isRestoreInProgress,
            onConfirm = { vm.onIntent(HomeIntent.OnRestoreAccount) },
            onDismiss = { vm.onIntent(HomeIntent.OnDismissRestore) },
        )
    }

    if (state.isFilterSheetVisible) {
        FilterBottomSheet(
            activeCategory = state.activeFilterCategory,
            selectedPracticeTypes = state.selectedFilterPracticeTypes,
            onCategorySelect = { vm.onIntent(HomeIntent.OnFilterCategorySelect(it)) },
            onPracticeOptionToggle = { vm.onIntent(HomeIntent.OnFilterPracticeOptionToggle(it)) },
            onReset = { vm.onIntent(HomeIntent.OnFilterReset) },
            onApply = { vm.onIntent(HomeIntent.OnFilterApply) },
            onDismiss = { vm.onIntent(HomeIntent.OnFilterDismiss) },
            isSaving = state.isFilterSaving,
        )
    }

    naviPlaceId?.let {
        NaviPickerSheet(
            onDismiss = { naviPlaceId = null },
            onSelect = { app, always ->
                vm.onIntent(
                    HomeIntent.OnNaviAppSelected(
                        app = app,
                        always = always,
                        notificationPermissionGranted = context.hasNotificationPermission(),
                    ),
                )
                naviPlaceId = null
            },
        )
    }
    installNaviPlaceId?.let {
        NaviPickerSheet(
            mode = NaviPickerMode.INSTALL,
            onDismiss = { installNaviPlaceId = null },
            onSelect = { app, _ ->
                vm.onIntent(HomeIntent.OnInstallNaviAppSelected(app))
                installNaviPlaceId = null
            },
        )
    }
}

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

private fun Context.isPackageInstalled(packageName: String): Boolean = runCatching {
    packageManager.getPackageInfo(packageName, 0)
}.isSuccess

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
