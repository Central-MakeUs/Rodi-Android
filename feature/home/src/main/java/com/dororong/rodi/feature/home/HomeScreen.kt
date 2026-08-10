package com.dororong.rodi.feature.home

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.ui.components.RodiBottomNavigation
import com.dororong.rodi.core.ui.components.RodiBottomNavigationDestination
import com.dororong.rodi.core.ui.components.AccountRecoveryDialog
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarDuration
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.components.LoginRequiredDialog
import com.dororong.rodi.feature.home.components.HomeSearchBar
import com.dororong.rodi.feature.home.components.MapListButton
import com.dororong.rodi.feature.home.components.MapLoadingScreen
import com.dororong.rodi.feature.home.components.MapNetworkErrorScreen
import com.dororong.rodi.feature.home.components.MapResearchButton
import com.dororong.rodi.feature.home.components.MyLocationButton
import com.dororong.rodi.feature.home.components.NaviPickerMode
import com.dororong.rodi.feature.home.components.NaviPickerSheet
import com.dororong.rodi.feature.home.detail.CourseDetailSheet
import com.dororong.rodi.feature.home.detail.CourseReviewViewModel
import com.dororong.rodi.feature.home.detail.components.LevelReviewSection
import com.dororong.rodi.feature.home.detail.levelreviews.LevelReviewsOverlay
import com.dororong.rodi.feature.home.review.ReviewWriteScreen
import com.dororong.rodi.feature.home.review.PracticePromptDialog
import com.dororong.rodi.feature.home.review.notvisited.NotVisitedReasonScreen
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
import com.dororong.rodi.core.ui.permission.hasLocationPermission
import com.dororong.rodi.feature.home.location.rememberDeviceHeading
import com.dororong.rodi.feature.home.map.BrowseLabelTag
import com.dororong.rodi.feature.home.network.isNetworkAvailable
import com.dororong.rodi.feature.home.network.networkAvailabilityFlow
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
import com.dororong.rodi.feature.home.map.markMapLoaded
import com.dororong.rodi.feature.home.map.rememberMapViewWithLifecycle
import com.dororong.rodi.feature.home.map.renderClusters
import com.dororong.rodi.feature.home.map.renderCurrentLocationMarker
import com.dororong.rodi.feature.home.map.renderIndividualMarkers
import com.dororong.rodi.feature.home.map.renderPlaceCourse
import com.dororong.rodi.feature.home.map.renderPlaceCourseMarkers
import com.dororong.rodi.feature.home.map.renderSelectedParkingMarker
import com.dororong.rodi.feature.home.map.selectParkingMarker
import com.dororong.rodi.feature.home.map.viewportOrNull
import com.dororong.rodi.feature.home.map.viewportAboveBottomInsetOrNull
import com.dororong.rodi.feature.home.map.visibleViewportOrNull
import com.dororong.rodi.feature.home.map.boundsOrNull
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
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
private val PARTIAL_LIST_HEADER_HEIGHT = 48.dp
private val FULL_LIST_HEADER_HEIGHT = 64.dp
private val FULL_LIST_CONTENT_TOP_PADDING = 20.dp
private const val LIST_TITLE_CENTERING_START = 0.5f
private const val MIN_ZOOM = 6
private const val MAP_RETRY_DEBOUNCE_MILLIS = 1_500L
private const val MAP_NETWORK_SNACKBAR_ID = "map-network"
private val LIST_HEADER_DRAG_THRESHOLD = 12.dp
private val PARKING_DETAIL_SHEET_MAX_HEIGHT = 400.dp
private val FILTER_HEADER_ICON_TOUCH_SIZE = 48.dp

typealias KakaoLoginRequest = (
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit,
) -> Unit

private data class ReviewWriteTarget(
    val placeId: Long,
    val placeName: String,
    val review: Review?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMyPageClick: () -> Unit,
    onSearchClick: (GeoPoint) -> Unit,
    onGuestSignUp: () -> Unit,
    onRequestKakaoLogin: KakaoLoginRequest,
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { RodiSnackbarHostState() }
    val density = LocalDensity.current

    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapViewSize by remember { mutableStateOf(IntSize.Zero) }
    var mapZoomLevel by remember { mutableIntStateOf(DEFAULT_ZOOM) }
    var currentViewport by remember { mutableStateOf<MapViewport?>(null) }
    var pendingMapSearch by remember { mutableStateOf<PendingMapSearch?>(null) }
    var activeClusterMemberIds by remember { mutableStateOf<Set<Long>?>(null) }
    var mapSearchGeneration by remember { mutableStateOf(0L) }
    var isInitialLocationCameraMovePending by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var permissionGranted by remember { mutableStateOf(context.hasLocationPermission()) }
    var initialLocationState by remember { mutableStateOf(InitialLocationState.Pending) }
    var mapRetryKey by remember { mutableIntStateOf(0) }
    var lastMapRetryAtMillis by remember { mutableLongStateOf(0L) }
    var showMapNetworkSnackbar by remember { mutableStateOf(false) }
    var hasMapLoadedThisEntry by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(context.isNetworkAvailable()) }
    var mapScreenState by remember {
        mutableStateOf(
            when {
                !isOnline -> MapScreenState.NetworkError
                hasLoadedMapInSession || context.hasLoadedMapBefore() -> MapScreenState.Ready
                else -> MapScreenState.Loading
            },
        )
    }
    var isAtCurrentLocation by remember { mutableStateOf(false) }
    var hasUserMovedMap by remember { mutableStateOf(false) }
    var hasUserChosenMapViewport by remember { mutableStateOf(false) }
    var hasCenteredInitialLocation by remember { mutableStateOf(false) }
    var naviPlaceId by remember { mutableStateOf<Long?>(null) }
    var installNaviPlaceId by remember { mutableStateOf<Long?>(null) }
    var courseDetailSheetHeightPx by remember { mutableIntStateOf(0) }
    var parkingSheetLayout by remember { mutableStateOf(ParkingSheetLayoutState()) }
    var bottomNavigationHeightPx by remember { mutableIntStateOf(0) }
    var reviewToReport by remember { mutableStateOf<Review?>(null) }
    var reviewToBlock by remember { mutableStateOf<Review?>(null) }
    var reviewToDelete by remember { mutableStateOf<Review?>(null) }
    var reviewToWrite by remember { mutableStateOf<ReviewWriteTarget?>(null) }
    var isNotVisitedReasonVisible by remember { mutableStateOf(false) }
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        permissionGranted = result.values.any { it }
        if (!permissionGranted) initialLocationState = InitialLocationState.Unavailable
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = context.hasLocationPermission()
                vm.onIntent(HomeIntent.OnAppResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
    val currentIsEmptySheet by rememberUpdatedState(isEmptySheet)
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
        confirmValueChange = { targetValue ->
            targetValue != SheetValue.Expanded || !currentIsEmptySheet
        },
    )
    val scaffoldState = rememberBottomSheetScaffoldState(sheetState)
    var scaffoldSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(state.surfaceState, isEmptySheet) {
        when (state.surfaceState) {
            HomeSurfaceState.Navigation, HomeSurfaceState.Detail -> sheetState.hide()
            HomeSurfaceState.PartialList -> sheetState.partialExpand()
            HomeSurfaceState.FullList -> if (isEmptySheet) sheetState.partialExpand() else sheetState.expand()
        }
    }
    LaunchedEffect(sheetState, state.showEmpty, state.showInitialError) {
        snapshotFlow { sheetState.currentValue }.drop(1).collect { value ->
            when (value) {
                SheetValue.Hidden -> if (state.surfaceState != HomeSurfaceState.Detail) {
                    vm.onIntent(HomeIntent.OnListCollapse)
                }

                SheetValue.PartiallyExpanded -> if (state.surfaceState == HomeSurfaceState.FullList) {
                    vm.onIntent(HomeIntent.OnListCollapse)
                }

                SheetValue.Expanded -> if (state.showEmpty || state.showInitialError) {
                    sheetState.partialExpand()
                } else {
                    vm.onIntent(HomeIntent.OnListExpand)
                }
            }
        }
    }

    val sheetOffsetPx by remember {
        derivedStateOf { runCatching { sheetState.requireOffset() }.getOrDefault(scaffoldSize.height.toFloat()) }
    }
    val visibleSheetHeightPx = BottomSheetViewportPolicy.bottomPaddingPx(
        mapHeightPx = scaffoldSize.height,
        sheetTopPx = sheetOffsetPx,
    )
    val visibleSheetHeight = with(density) {
        visibleSheetHeightPx.toDp()
    }
    val sheetExpansionProgress = with(density) {
        val partialSheetOffsetPx = (scaffoldSize.height - 380.dp.toPx()).coerceAtLeast(0f)
        if (partialSheetOffsetPx == 0f) {
            1f
        } else {
            ((partialSheetOffsetPx - sheetOffsetPx) / partialSheetOffsetPx).coerceIn(0f, 1f)
        }
    }
    val navigationInsetPx = WindowInsets.navigationBars.getBottom(density)
    val navigationInset = with(density) { navigationInsetPx.toDp() }
    val selectedDetailPlaceId = state.selectedPlace?.id
    val bottomControlOffset = with(density) {
        if (state.surfaceState == HomeSurfaceState.Detail && state.selectedPlace?.type == PlaceType.PARKING) {
            val parkingHeightPx = parkingSheetLayout
                .takeIf { it.placeId == selectedDetailPlaceId }
                ?.currentHeightPx
                ?: 0
            maxOf(68.dp, parkingHeightPx.toDp() + 12.dp - navigationInset)
        } else if (visibleSheetHeight > 0.dp) {
            maxOf(68.dp, visibleSheetHeight + 12.dp - navigationInset)
        } else {
            68.dp
        }
    }
    val listViewportHeight = (
        visibleSheetHeight - lerp(PARTIAL_LIST_HEADER_HEIGHT, FULL_LIST_HEADER_HEIGHT, sheetExpansionProgress)
        ).coerceAtLeast(0.dp)
    val mapContentBottomPaddingPx = when {
        state.surfaceState == HomeSurfaceState.PartialList ||
            state.surfaceState == HomeSurfaceState.FullList -> visibleSheetHeightPx
        state.surfaceState == HomeSurfaceState.Navigation -> bottomNavigationHeightPx
        state.surfaceState != HomeSurfaceState.Detail -> 0
        state.selectedPlace?.type == PlaceType.COURSE -> courseDetailSheetHeightPx
        state.selectedPlace?.type == PlaceType.PARKING -> parkingSheetLayout
            .takeIf { it.placeId == selectedDetailPlaceId }
            ?.initialMapPaddingPx
            ?: 0
        else -> 0
    }
    val clusterFitPaddingPx = with(density) { CLUSTER_FIT_PADDING_DP.dp.roundToPx() }
    val mapBrandOffset = maxOf(0.dp, 68.dp + navigationInset - 4.dp)
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

    BackHandler(enabled = state.isFilterSheetVisible || state.surfaceState != HomeSurfaceState.Navigation) {
        if (state.isFilterSheetVisible) {
            if (!state.isFilterSaving) vm.onIntent(HomeIntent.OnFilterDismiss)
        } else {
            when (state.surfaceState) {
                HomeSurfaceState.Detail -> dismissDetail()
                else -> vm.onIntent(HomeIntent.OnListCollapse)
            }
        }
    }

    CollectEffect(vm.effect) { effect ->
        when (effect) {
            is HomeEffect.LaunchKakaoMap -> KakaoMapLauncher.launch(context, effect.place)
            is HomeEffect.LaunchKakaoNavi -> KakaoNaviLauncher.launch(context, effect.place)
            is HomeEffect.ShowNaviPicker -> naviPlaceId = effect.place.id
            is HomeEffect.ShowInstallNaviPicker -> installNaviPlaceId = effect.place.id
            is HomeEffect.OpenNaviInstallPage -> when (effect.app) {
                NaviApp.KAKAOMAP -> KakaoMapLauncher.openInstallPage(context)
                NaviApp.KAKAONAVI -> KakaoNaviLauncher.openInstallPage(context)
            }

            is HomeEffect.ShowSnackbar -> snackbarHostState.show(RodiSnackbarData(message = effect.message))
            is HomeEffect.NavigateSearch -> onSearchClick(effect.origin)
            HomeEffect.NavigateMyPage -> onMyPageClick()
            HomeEffect.NavigateGuestSignUp -> onGuestSignUp()
        }
    }

    fun retryMap() {
        if (!isOnline) {
            if (!hasMapLoadedThisEntry) mapScreenState = MapScreenState.NetworkError
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

    LaunchedEffect(isOnline) {
        when {
            isOnline ->
                if (mapScreenState == MapScreenState.NetworkError || showMapNetworkSnackbar) retryMap()
            hasMapLoadedThisEntry -> showMapNetworkSnackbar = true
            else -> {
                showMapNetworkSnackbar = false
                mapScreenState = MapScreenState.NetworkError
            }
        }
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
        currentViewport = viewport
        vm.onIntent(HomeIntent.OnViewportSettled(viewport.toQuery(currentLocation)))
    }

    LaunchedEffect(kakaoMap, currentLocation, hasUserMovedMap, hasUserChosenMapViewport) {
        val map = kakaoMap ?: return@LaunchedEffect
        val location = currentLocation ?: return@LaunchedEffect
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
        state.searchedQuery,
        activeClusterMemberIds,
    ) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (state.surfaceState == HomeSurfaceState.Detail) return@LaunchedEffect
        map.clearCourse()
        val searchedViewport = state.searchedQuery?.let { MapViewport(it.northEast, it.southWest) }
        if (state.coordinates.isEmpty() || searchedViewport == null) {
            map.clearBrowseLabels()
            return@LaunchedEffect
        }
        val clusterScopedCoordinates = activeClusterMemberIds?.let { memberIds ->
            state.coordinates.filter { it.id in memberIds }
        } ?: state.coordinates
        val visibleCoordinates = clusterScopedCoordinates.filter { searchedViewport.contains(it.point) }
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
                    )
                    if (mapContentBottomPaddingPx > 0) {
                        map.fitCourseToScreen(routePoints, mapContentBottomPaddingPx)
                    }
                }
            }
        }
    }

    LaunchedEffect(kakaoMap, mapBrandOffset, mapScaleBarOffset, mapContentBottomPaddingPx) {
        val map = kakaoMap ?: return@LaunchedEffect
        map.setPadding(0, 0, 0, mapContentBottomPaddingPx)
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
        snackbarHost = { RodiSnackbarHost(snackbarHostState) },
        content = { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                BottomSheetScaffold(
                    modifier = Modifier.onSizeChanged { scaffoldSize = it },
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 380.dp,
                    sheetShape = RoundedCornerShape(
                        topStart = 20.dp * (1f - sheetExpansionProgress),
                        topEnd = 20.dp * (1f - sheetExpansionProgress),
                    ),
                    sheetContainerColor = RodiTheme.colors.white,
                    sheetShadowElevation = 8.dp,
                    sheetSwipeEnabled = false,
                    sheetDragHandle = null,
                    sheetContent = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                        ) {
                            if (!state.showEmpty && !state.showInitialError) {
                                ListSheetHeader(
                                    expansionProgress = sheetExpansionProgress,
                                    onExpand = { vm.onIntent(HomeIntent.OnListExpand) },
                                    onBack = { vm.onIntent(HomeIntent.OnListCollapse) },
                                    onFilterClick = { vm.onIntent(HomeIntent.OnFilterOpen) },
                                )
                            }
                            when {
                                state.listState == HomeListState.Loading -> PlaceListLoadingContent(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(listViewportHeight),
                                )

                                state.showEmpty || state.showInitialError -> PlaceEmptyContent(
                                    isInitialError = state.showInitialError,
                                    onHandleDragDown = { scope.launch { sheetState.hide() } },
                                )
                                else -> key(state.placeListGeneration) {
                                    PlaceListContent(
                                        places = state.places,
                                        onPlaceClick = {
                                            vm.onIntent(HomeIntent.OnPlaceClick(it, HomeDetailOrigin.List))
                                        },
                                        onLoadNextPage = { vm.onIntent(HomeIntent.OnLoadNextPage) },
                                        isNextPageLoading = state.isNextPageLoading,
                                        topContentPadding = lerp(
                                            0.dp,
                                            FULL_LIST_CONTENT_TOP_PADDING,
                                            sheetExpansionProgress,
                                        ),
                                        modifier = Modifier.height(listViewportHeight),
                                    )
                                }
                            }
                        }
                    },
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
                                                if (hasMapLoadedThisEntry) {
                                                    showMapNetworkSnackbar = true
                                                } else {
                                                    showMapNetworkSnackbar = false
                                                    mapScreenState = MapScreenState.NetworkError
                                                }
                                            }
                                        },
                                        object : KakaoMapReadyCallback() {
                                            override fun onMapReady(map: KakaoMap) {
                                                kakaoMap = map
                                                map.setPadding(0, 0, 0, 0)
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
                                                        currentViewport = viewport
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

                                            override fun getPosition(): LatLng = currentLocation ?: SEOUL
                                            override fun getZoomLevel(): Int = DEFAULT_ZOOM
                                        },
                                    )
                                    mapView
                                },
                            )
                        }

                        HomeSearchBar(
                            onClick = {
                                vm.onIntent(
                                    HomeIntent.OnSearchClick(
                                        currentViewport?.toQuery(currentLocation)?.origin,
                                    ),
                                )
                            },
                            searchKeyword = state.searchKeyword,
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
                                            bottomInsetPx = visibleSheetHeightPx,
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
                                .padding(bottom = bottomControlOffset),
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
                                .padding(end = 12.dp, bottom = bottomControlOffset),
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
                                        onReportReviewClick = { reviewToReport = it },
                                        onBlockMemberClick = { reviewToBlock = it },
                                        scrollState = sheetScrollState,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .then(
                                    if (selectedPlace?.type == PlaceType.PARKING) {
                                        Modifier
                                            .heightIn(max = PARKING_DETAIL_SHEET_MAX_HEIGHT)
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
                                    onHandleDragDown = dragDismissDetail,
                                )

                            selectedPlace?.type == PlaceType.PARKING -> ParkingDetailContent(
                                place = selectedPlace,
                                isBookmarkUpdating = state.isBookmarkUpdating,
                                onDismiss = dismissDetail,
                                onHandleDragDown = dragDismissDetail,
                                onBookmarkClick = { vm.onIntent(HomeIntent.OnBookmarkClick) },
                                onNavigate = {
                                    vm.onIntent(
                                        HomeIntent.OnNavigateClick(
                                            kakaoMapInstalled = context.isPackageInstalled("net.daum.android.map"),
                                            kakaoNaviInstalled = context.isPackageInstalled("com.locnall.KimGiSa"),
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
                    MapScreenState.NetworkError -> MapNetworkErrorScreen(onRetry = ::retryMap)
                    MapScreenState.Ready -> Unit
                }
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
                    ),
                )
            },
            onEditReviewClick = { reviewToWrite = ReviewWriteTarget(levelReviewsPlace.id, levelReviewsPlace.name, it) },
            onDeleteReviewClick = { reviewToDelete = it },
            onReportReviewClick = { reviewToReport = it },
            onBlockMemberClick = { reviewToBlock = it },
        )
    }
    reviewToReport?.let { review ->
        ReviewReportScreen(
            reviewId = review.reviewId,
            onClose = { reviewToReport = null },
            modifier = Modifier.fillMaxSize(),
        )
    }
    reviewToWrite?.let { target ->
        ReviewWriteScreen(
            placeId = target.placeId,
            placeName = target.placeName,
            editing = target.review,
            onClose = { reviewToWrite = null },
            onCompleted = {
                reviewToWrite = null
                reviewVm.refresh()
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
    state.practicePrompt?.let { session ->
        PracticePromptDialog(
            session = session,
            onVisited = {
                vm.onIntent(HomeIntent.OnPracticePromptVisited)
                reviewToWrite = ReviewWriteTarget(session.placeId, session.placeName, null)
            },
            onNotVisited = {
                vm.onIntent(HomeIntent.OnPracticePromptNotVisited)
                isNotVisitedReasonVisible = true
            },
            onDismiss = { vm.onIntent(HomeIntent.OnPracticePromptDismiss) },
        )
    }
    if (isNotVisitedReasonVisible) {
        NotVisitedReasonScreen(onClose = { isNotVisitedReasonVisible = false })
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
                vm.onIntent(HomeIntent.OnNaviAppSelected(app, always))
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

@Composable
private fun ListSheetHeader(
    expansionProgress: Float,
    onExpand: () -> Unit,
    onBack: () -> Unit,
    onFilterClick: () -> Unit,
) {
    val density = LocalDensity.current
    var titleWidthPx by remember { mutableIntStateOf(0) }
    val dragThresholdPx = with(density) { LIST_HEADER_DRAG_THRESHOLD.toPx() }
    val titleCenteringProgress = (
            (expansionProgress - LIST_TITLE_CENTERING_START) / (1f - LIST_TITLE_CENTERING_START)
            ).coerceIn(0f, 1f)
    val titleTopPadding = lerp(24.dp, 40.dp, expansionProgress)
    val handleAlpha = (1f - expansionProgress / LIST_TITLE_CENTERING_START).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(lerp(PARTIAL_LIST_HEADER_HEIGHT, FULL_LIST_HEADER_HEIGHT, expansionProgress))
            .pointerInput(expansionProgress, dragThresholdPx) {
                var totalDrag = 0f
                var actionTriggered = false
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        actionTriggered = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (actionTriggered) return@detectVerticalDragGestures
                        totalDrag += dragAmount
                        when {
                            totalDrag <= -dragThresholdPx && expansionProgress < 1f -> {
                                actionTriggered = true
                                change.consume()
                                onExpand()
                            }

                            totalDrag >= dragThresholdPx -> {
                                actionTriggered = true
                                change.consume()
                                onBack()
                            }
                        }
                    },
                )
            },
    ) {
        val titleWidth = with(density) { titleWidthPx.toDp() }
        val centeredTitleStart = (maxWidth - titleWidth) / 2
        val titleTranslation = with(density) { (centeredTitleStart - 16.dp).toPx() }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(width = 60.dp, height = 4.dp)
                .graphicsLayer { alpha = handleAlpha }
                .background(RodiTheme.colors.handleBar, RoundedCornerShape(2.dp)),
        )
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            contentDescription = "뒤로가기",
            tint = RodiTheme.colors.black,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 40.dp)
                .size(24.dp)
                .clip(CircleShape)
                .graphicsLayer { alpha = titleCenteringProgress }
                .clickable(enabled = titleCenteringProgress == 1f, onClick = onBack),
        )
        FilterHeaderIconButton(
            painter = painterResource(R.drawable.ic_filter_top),
            iconSize = 24.dp,
            visualSize = 24.dp,
            visualBottomInset = 0.dp,
            backgroundColor = null,
            onClick = onFilterClick,
            enabled = titleCenteringProgress == 1f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp, top = 16.dp)
                .graphicsLayer { alpha = titleCenteringProgress }
        )
        FilterHeaderIconButton(
            painter = painterResource(R.drawable.ic_filter),
            iconSize = 16.dp,
            visualSize = 23.dp,
            visualBottomInset = 1.dp,
            backgroundColor = RodiTheme.colors.gray100,
            onClick = onFilterClick,
            enabled = titleCenteringProgress == 0f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp)
                .graphicsLayer { alpha = 1f - titleCenteringProgress }
        )
        Text(
            text = "추천 목록",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
            onTextLayout = { titleWidthPx = it.size.width },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = titleTopPadding)
                .graphicsLayer { translationX = titleTranslation * titleCenteringProgress },
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
                expansionProgress = 0f,
                onExpand = {},
                onBack = {},
                onFilterClick = {},
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
                expansionProgress = 1f,
                onExpand = {},
                onBack = {},
                onFilterClick = {},
            )
        }
    }
}
