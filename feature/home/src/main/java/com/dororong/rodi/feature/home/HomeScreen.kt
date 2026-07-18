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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.ui.components.RodiBottomNavigation
import com.dororong.rodi.core.ui.components.RodiBottomNavigationDestination
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.components.LoginRequiredDialog
import com.dororong.rodi.feature.home.components.MapListButton
import com.dororong.rodi.feature.home.components.MapLoadingScreen
import com.dororong.rodi.feature.home.components.MapNetworkErrorScreen
import com.dororong.rodi.feature.home.components.MapResearchButton
import com.dororong.rodi.feature.home.components.MyLocationButton
import com.dororong.rodi.feature.home.components.NaviPickerMode
import com.dororong.rodi.feature.home.components.NaviPickerSheet
import com.dororong.rodi.feature.home.detail.components.CourseDetailContent
import com.dororong.rodi.feature.home.detail.components.ParkingDetailContent
import com.dororong.rodi.feature.home.detail.components.PlaceDetailLoading
import com.dororong.rodi.feature.home.list.components.PlaceEmptyContent
import com.dororong.rodi.feature.home.list.components.PlaceListContent
import com.dororong.rodi.feature.home.location.currentLocationUpdates
import com.dororong.rodi.feature.home.location.hasLocationPermission
import com.dororong.rodi.feature.home.location.rememberDeviceHeading
import com.dororong.rodi.feature.home.map.BrowseLabelTag
import com.dororong.rodi.feature.home.map.ClusterPolicy
import com.dororong.rodi.feature.home.map.DEFAULT_ZOOM
import com.dororong.rodi.feature.home.map.MapClusterer
import com.dororong.rodi.feature.home.map.MapCoursePoint
import com.dororong.rodi.feature.home.map.MapScreenState
import com.dororong.rodi.feature.home.map.MapViewport
import com.dororong.rodi.feature.home.map.NationalGrid
import com.dororong.rodi.feature.home.map.ProjectedMapItem
import com.dororong.rodi.feature.home.map.SEOUL
import com.dororong.rodi.feature.home.map.ViewportSearchThreshold
import com.dororong.rodi.feature.home.map.animateParkingMarkerSelection
import com.dororong.rodi.feature.home.map.animateParkingMarkerDeselection
import com.dororong.rodi.feature.home.map.clearBrowseLabels
import com.dororong.rodi.feature.home.map.clearCourse
import com.dororong.rodi.feature.home.map.fitCourseToScreen
import com.dororong.rodi.feature.home.map.clearCurrentLocationMarker
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
import com.dororong.rodi.feature.home.map.focusOn
import com.dororong.rodi.feature.home.map.viewportOrNull
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
import kotlinx.coroutines.launch

private const val CLUSTER_DISTANCE_DP = 56
private const val SURFACE_ANIMATION_MILLIS = 300

typealias KakaoLoginRequest = (
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit,
) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMyPageClick: () -> Unit,
    onRequestKakaoLogin: KakaoLoginRequest,
    vm: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current

    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapViewSize by remember { mutableStateOf(IntSize.Zero) }
    var mapZoomLevel by remember { mutableIntStateOf(DEFAULT_ZOOM) }
    var currentViewport by remember { mutableStateOf<MapViewport?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var permissionGranted by remember { mutableStateOf(context.hasLocationPermission()) }
    var mapRetryKey by remember { mutableIntStateOf(0) }
    var mapScreenState by remember {
        mutableStateOf(
            if (hasLoadedMapInSession || context.hasLoadedMapBefore()) MapScreenState.Ready
            else MapScreenState.Loading,
        )
    }
    var isAtCurrentLocation by remember { mutableStateOf(false) }
    var hasUserMovedMap by remember { mutableStateOf(false) }
    var hasCenteredInitialLocation by remember { mutableStateOf(false) }
    var naviPlaceId by remember { mutableStateOf<Long?>(null) }
    var installNaviPlaceId by remember { mutableStateOf<Long?>(null) }
    val deviceHeading = rememberDeviceHeading()
    val clusterDistancePx = with(density) { CLUSTER_DISTANCE_DP.dp.roundToPx() }
    val clusterBackground = RodiTheme.colors.primary500.toArgb()
    val clusterText = RodiTheme.colors.white.toArgb()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> permissionGranted = result.values.any { it } }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = context.hasLocationPermission()
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
            kakaoMap?.clearCurrentLocationMarker()
            return@LaunchedEffect
        }
        context.currentLocationUpdates().collect { currentLocation = it }
    }

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(sheetState)
    var scaffoldSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(state.surfaceState) {
        when (state.surfaceState) {
            HomeSurfaceState.Navigation, HomeSurfaceState.Detail -> sheetState.hide()
            HomeSurfaceState.PartialList -> sheetState.partialExpand()
            HomeSurfaceState.FullList -> sheetState.expand()
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
    val navigationInsetPx = WindowInsets.navigationBars.getBottom(density)
    val navigationInset = with(density) { navigationInsetPx.toDp() }
    val bottomControlOffset = with(density) {
        val visibleSheetHeight = (scaffoldSize.height - sheetOffsetPx)
            .coerceAtLeast(0f)
            .toDp()
        if (visibleSheetHeight > 0.dp) {
            maxOf(70.dp, visibleSheetHeight + 12.dp - navigationInset)
        } else {
            70.dp
        }
    }
    val mapBrandOffset = maxOf(0.dp, bottomControlOffset + navigationInset - 8.dp)

    val shouldShowResearch = state.surfaceState != HomeSurfaceState.Detail && state.searchedQuery?.let { searched ->
        currentViewport?.let { current ->
            ViewportSearchThreshold.isExceeded(
                MapViewport(searched.northEast, searched.southWest),
                current,
            )
        }
    } == true

    val dismissDetail: () -> Unit = {
        val place = state.selectedPlace
        val shouldReverseParking = place?.type == PlaceType.PARKING
        val dismissed = if (shouldReverseParking) {
            kakaoMap?.animateParkingMarkerDeselection(context, place.id) {
                vm.onIntent(HomeIntent.OnDismissDetail)
            } == true
        } else {
            false
        }
        if (!dismissed) vm.onIntent(HomeIntent.OnDismissDetail)
    }
    val dismissLogin: () -> Unit = {
        val pendingPlaceId = (state.pendingAction as? PendingHomeAction.OpenDetail)?.placeId
        val isPendingParking = state.coordinates.firstOrNull { it.id == pendingPlaceId }?.type == PlaceType.PARKING
        val reversed = if (pendingPlaceId != null && isPendingParking) {
            kakaoMap?.animateParkingMarkerDeselection(context, pendingPlaceId) {
                vm.onIntent(HomeIntent.OnDismissLogin)
            } == true
        } else {
            false
        }
        if (!reversed) vm.onIntent(HomeIntent.OnDismissLogin)
    }

    BackHandler(enabled = state.surfaceState != HomeSurfaceState.Navigation) {
        when (state.surfaceState) {
            HomeSurfaceState.Detail -> dismissDetail()
            else -> vm.onIntent(HomeIntent.OnListCollapse)
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
            is HomeEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            HomeEffect.NavigateMyPage -> onMyPageClick()
        }
    }

    LaunchedEffect(kakaoMap, mapViewSize, currentLocation) {
        val map = kakaoMap ?: return@LaunchedEffect
        val viewport = map.viewportOrNull(mapViewSize) ?: return@LaunchedEffect
        currentViewport = viewport
        vm.onIntent(HomeIntent.OnViewportSettled(viewport.toQuery(currentLocation)))
    }

    LaunchedEffect(kakaoMap, currentLocation, hasUserMovedMap) {
        val map = kakaoMap ?: return@LaunchedEffect
        val location = currentLocation ?: return@LaunchedEffect
        if (!hasCenteredInitialLocation && !hasUserMovedMap) {
            hasCenteredInitialLocation = true
            map.moveCamera(
                CameraUpdateFactory.newCenterPosition(location, DEFAULT_ZOOM),
                CameraAnimation.from(300),
            )
        }
    }

    LaunchedEffect(kakaoMap, permissionGranted, currentLocation, deviceHeading.value) {
        val map = kakaoMap ?: return@LaunchedEffect
        val location = currentLocation
        if (!permissionGranted || location == null) {
            map.clearCurrentLocationMarker()
        } else {
            map.renderCurrentLocationMarker(context, location, deviceHeading.value)
        }
    }

    LaunchedEffect(
        kakaoMap,
        state.coordinates,
        state.surfaceState,
        mapZoomLevel,
        mapViewSize,
        clusterBackground,
        clusterText,
    ) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (state.surfaceState == HomeSurfaceState.Detail) return@LaunchedEffect
        map.clearCourse()
        if (state.coordinates.isEmpty()) return@LaunchedEffect
        when (val policy = ClusterPolicy.forZoom(mapZoomLevel)) {
            null -> map.renderIndividualMarkers(context, state.coordinates)
            else -> {
                val clusters = if (policy.grid != null) {
                    MapClusterer.clusterInFixedGeoGrid(
                        items = state.coordinates.map { MapCoursePoint(it.id, it.point) },
                        northEast = NationalGrid.northEast,
                        southWest = NationalGrid.southWest,
                        policy = policy,
                    )
                } else {
                    MapClusterer.clusterByScreenDistance(
                        items = state.coordinates.mapNotNull { place ->
                            val point = map.toScreenPoint(LatLng.from(place.point.lat, place.point.lng))
                                ?: return@mapNotNull null
                            ProjectedMapItem(place.id, place.point, point.x, point.y)
                        },
                        viewportWidth = mapViewSize.width,
                        viewportHeight = mapViewSize.height,
                        minimumDistancePx = clusterDistancePx,
                        targetZoom = policy.targetZoom,
                    )
                }
                map.renderClusters(
                    context = context,
                    clusters = clusters,
                    placesById = state.coordinates.associateBy { it.id },
                    backgroundColor = clusterBackground,
                    textColor = clusterText,
                )
            }
        }
    }

    LaunchedEffect(kakaoMap, state.selectedPlace, state.selectedRoute) {
        val map = kakaoMap ?: return@LaunchedEffect
        val place = state.selectedPlace ?: return@LaunchedEffect
        when (place.type) {
            PlaceType.PARKING -> {
                val coordinate = state.coordinates.firstOrNull { it.id == place.id }
                if (coordinate != null) {
                    if (!map.animateParkingMarkerSelection(context, coordinate.id)) {
                        map.renderSelectedParkingMarker(context, coordinate)
                    }
                    map.focusOn(LatLng.from(coordinate.point.lat, coordinate.point.lng), 15)
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
                    map.fitCourseToScreen(routePoints)
                }
            }
        }
    }

    LaunchedEffect(kakaoMap, mapBrandOffset) {
        val map = kakaoMap ?: return@LaunchedEffect
        val bottomPx = with(density) { mapBrandOffset.toPx() }
        map.logo?.setPosition(MapGravity.BOTTOM or MapGravity.LEFT, with(density) { 8.dp.toPx() }, bottomPx)
        map.scaleBar?.apply {
            setAutoHide(false)
            setPosition(
                MapGravity.BOTTOM or MapGravity.LEFT,
                with(density) { 80.dp.toPx() },
                (bottomPx - with(density) { 8.dp.toPx() }).coerceAtLeast(0f),
            )
            show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    sheetContainerColor = RodiTheme.colors.white,
                    sheetShadowElevation = 8.dp,
                    sheetSwipeEnabled = state.surfaceState != HomeSurfaceState.Detail,
                    sheetDragHandle = null,
                    sheetContent = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .size(width = 60.dp, height = 4.dp)
                                        .background(RodiTheme.colors.handleBar, RoundedCornerShape(2.dp)),
                                )
                            }
                            if (!state.showEmpty && !state.showInitialError && state.surfaceState != HomeSurfaceState.FullList) {
                                Text(
                                    text = "추천 목록",
                                    style = RodiTheme.typography.headline1,
                                    color = RodiTheme.colors.black,
                                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
                                )
                            }
                            when {
                                state.listState == HomeListState.Loading -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(color = RodiTheme.colors.primary600)
                                }
                                state.showEmpty || state.showInitialError -> PlaceEmptyContent(state.showInitialError)
                                else -> PlaceListContent(
                                    places = state.places,
                                    onPlaceClick = {
                                        vm.onIntent(HomeIntent.OnPlaceClick(it, HomeDetailOrigin.List))
                                    },
                                    onLoadNextPage = { vm.onIntent(HomeIntent.OnLoadNextPage) },
                                    isNextPageLoading = state.isNextPageLoading,
                                    showTopBar = state.surfaceState == HomeSurfaceState.FullList,
                                )
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
                                                mapScreenState = MapScreenState.NetworkError
                                            }
                                        },
                                        object : KakaoMapReadyCallback() {
                                            override fun onMapReady(map: KakaoMap) {
                                                kakaoMap = map
                                                map.setPadding(0, 0, 0, 0)
                                                map.setOnCameraMoveStartListener { _, gesture ->
                                                    if (gesture != GestureType.Unknown) {
                                                        isAtCurrentLocation = false
                                                        hasUserMovedMap = true
                                                    }
                                                }
                                                map.setOnCameraMoveEndListener { movedMap, _, _ ->
                                                    mapZoomLevel = movedMap.zoomLevel
                                                    movedMap.viewportOrNull(mapViewSize)?.let { viewport ->
                                                        currentViewport = viewport
                                                        vm.onIntent(HomeIntent.OnViewportSettled(viewport.toQuery(currentLocation)))
                                                    }
                                                    hasLoadedMapInSession = true
                                                    context.markMapLoaded()
                                                    mapScreenState = MapScreenState.Ready
                                                }
                                                map.setOnLabelClickListener { _, _, label ->
                                                    when (val tag = label.tag) {
                                                        is BrowseLabelTag.Cluster -> map.moveCamera(
                                                            CameraUpdateFactory.newCenterPosition(
                                                                LatLng.from(tag.point.lat, tag.point.lng),
                                                                tag.targetZoom,
                                                            ),
                                                            CameraAnimation.from(350),
                                                        )
                                                        is BrowseLabelTag.Place -> {
                                                            if (state.coordinates.firstOrNull { it.id == tag.id }?.type == PlaceType.PARKING) {
                                                                map.animateParkingMarkerSelection(context, tag.id)
                                                            }
                                                            vm.onIntent(HomeIntent.OnPlaceClick(tag.id, HomeDetailOrigin.Map))
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

                        AnimatedVisibility(
                            visible = shouldShowResearch,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 63.dp),
                        ) {
                            MapResearchButton(onClick = {
                                val viewport = currentViewport ?: return@MapResearchButton
                                vm.onIntent(HomeIntent.OnResearch(viewport.toQuery(currentLocation)))
                            })
                        }

                        AnimatedVisibility(
                            visible = state.surfaceState == HomeSurfaceState.Navigation,
                            enter = fadeIn(tween(SURFACE_ANIMATION_MILLIS)),
                            exit = fadeOut(tween(SURFACE_ANIMATION_MILLIS)),
                            modifier = Modifier.align(Alignment.BottomCenter),
                        ) {
                            RodiBottomNavigation(
                                selectedDestination = RodiBottomNavigationDestination.Home,
                                onHomeClick = { vm.onIntent(HomeIntent.OnListOpen) },
                                onMyClick = { vm.onIntent(HomeIntent.OnMyClick) },
                            )
                        }

                        AnimatedVisibility(
                            visible = state.surfaceState == HomeSurfaceState.Navigation,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = bottomControlOffset),
                        ) {
                            MapListButton(onClick = { vm.onIntent(HomeIntent.OnListOpen) })
                        }

                        AnimatedVisibility(
                            visible = state.surfaceState != HomeSurfaceState.FullList &&
                                state.surfaceState != HomeSurfaceState.Detail,
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
                                        kakaoMap?.apply {
                                            setPadding(0, 0, 0, 0)
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
                    }
                }

                if (state.surfaceState == HomeSurfaceState.Detail) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .then(
                                if (state.selectedPlace?.type == PlaceType.PARKING) Modifier.height(400.dp)
                                else Modifier,
                            ),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = RodiTheme.colors.white,
                        shadowElevation = 8.dp,
                    ) {
                        val selectedPlace = state.selectedPlace
                        when {
                            state.isDetailLoading -> PlaceDetailLoading()
                            selectedPlace?.type == PlaceType.COURSE -> CourseDetailContent(
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
                            )
                            selectedPlace?.type == PlaceType.PARKING -> ParkingDetailContent(
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
                            )
                        }
                    }
                }

                when (mapScreenState) {
                    MapScreenState.Loading -> MapLoadingScreen()
                    MapScreenState.NetworkError -> MapNetworkErrorScreen(onRetry = {
                        mapScreenState = MapScreenState.Loading
                        kakaoMap = null
                        mapRetryKey += 1
                    })
                    MapScreenState.Ready -> Unit
                }
            }
        },
    )

    if (state.pendingAction != null) {
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
        Box(Modifier.fillMaxSize().background(RodiTheme.colors.gray100)) {
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

@Preview(name = "Home chrome - small large font", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun HomeChromeSmallPreview() {
    HomeChromePreview()
}
