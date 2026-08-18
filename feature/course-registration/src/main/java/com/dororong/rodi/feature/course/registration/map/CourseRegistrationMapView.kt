package com.dororong.rodi.feature.course.registration.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.KakaoMapReadyCallback

private const val REGISTRATION_LABEL_LAYER_ID = "rodi-course-registration-layer"
private const val DEFAULT_LAT = 37.5665
private const val DEFAULT_LNG = 126.9780

/** Kakao MapView는 자체 생명주기를 가지므로 Compose가 제거될 때 finish를 호출한다. */
@Composable
fun CourseRegistrationMapView(
    modifier: Modifier = Modifier,
    retryToken: Int,
    center: GeoPoint?,
    centerGeneration: Long,
    waypoints: List<RegistrationWaypoint>,
    route: RouteResult?,
    editingWaypointIndex: Int?,
    temporaryPin: GeoPoint?,
    onReady: (Boolean) -> Unit,
    onCameraCenterChanged: (GeoPoint) -> Unit = {},
    onMapTapped: (GeoPoint) -> Unit = {},
    onWaypointTapped: (Int) -> Unit = {},
    topPaddingPx: Int = 0,
    bottomPaddingPx: Int = 0,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentCenterChanged by rememberUpdatedState(onCameraCenterChanged)
    val currentMapTapped by rememberUpdatedState(onMapTapped)
    val currentWaypointTapped by rememberUpdatedState(onWaypointTapped)
    val routeColor = RodiTheme.colors.primary600.toArgb()
    val routeStrokeColor = RodiTheme.colors.primary800.toArgb()
    var map by remember { mutableStateOf<KakaoMap?>(null) }
    val mapView = remember(retryToken) { MapView(context) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    LaunchedEffect(map, topPaddingPx, bottomPaddingPx) {
        map?.setPadding(0, topPaddingPx, 0, bottomPaddingPx)
    }

    LaunchedEffect(map, centerGeneration, center) {
        val target = center ?: return@LaunchedEffect
        map?.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(target.lat, target.lng), 14),
            CameraAnimation.from(350),
        )
    }

    LaunchedEffect(map, waypoints, route, editingWaypointIndex) {
        val currentMap = map ?: return@LaunchedEffect
        currentMap.renderRegistrationContent(
            context = context,
            waypoints = waypoints,
            // 핀 수정 중인 위치는 중앙 리티클로만 보여주고, 지도 위 확정 마커는 감춘다.
            excludedIndex = editingWaypointIndex,
            route = route,
            routeColor = routeColor,
            routeStrokeColor = routeStrokeColor,
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                mapView.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit
                        override fun onMapError(error: Exception?) {
                            map = null
                            onReady(false)
                        }
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(readyMap: KakaoMap) {
                            map = readyMap
                            readyMap.setCameraMinLevel(7)
                            readyMap.setGestureEnable(com.kakao.vectormap.GestureType.Rotate, false)
                            readyMap.setGestureEnable(com.kakao.vectormap.GestureType.Tilt, false)
                            readyMap.setOnCameraMoveEndListener { _, cameraPosition, gesture ->
                                if (gesture != com.kakao.vectormap.GestureType.Unknown) {
                                    val point = cameraPosition.position
                                    currentCenterChanged(GeoPoint(point.latitude, point.longitude))
                                }
                            }
                            readyMap.setOnMapClickListener { _, point, _, _ ->
                                currentMapTapped(GeoPoint(point.latitude, point.longitude))
                            }
                            readyMap.setOnLabelClickListener { _, _, label ->
                                (label.tag as? Int)?.let(currentWaypointTapped)
                                true
                            }
                            val initialCenter = center ?: GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
                            readyMap.moveCamera(
                                CameraUpdateFactory.newCenterPosition(
                                    LatLng.from(initialCenter.lat, initialCenter.lng),
                                    14,
                                ),
                            )
                            currentCenterChanged(initialCenter)
                            onReady(true)
                        }
                    },
                )
                mapView
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun KakaoMap.registrationLabelLayer(): LabelLayer? = labelManager?.let { manager ->
    manager.getLayer(REGISTRATION_LABEL_LAYER_ID) ?: manager.addLayer(
        LabelLayerOptions.from(REGISTRATION_LABEL_LAYER_ID)
            .setZOrder(5_200)
            .setClickable(true),
    )
}

private fun KakaoMap.renderRegistrationContent(
    context: Context,
    waypoints: List<RegistrationWaypoint>,
    excludedIndex: Int?,
    route: RouteResult?,
    routeColor: Int,
    routeStrokeColor: Int,
) {
    val layer = registrationLabelLayer() ?: return
    layer.removeAll()
    routeLineManager?.layer?.removeAll()
    val manager = labelManager ?: return
    waypoints.forEachIndexed { index, waypoint ->
        if (index == excludedIndex) return@forEachIndexed
        val point = GeoPoint(waypoint.lat, waypoint.lng)
        val styles = manager.addLabelStyles(
            LabelStyles.from(
                LabelStyle.from(context.registrationPinBitmap(waypoint.type)).setApplyDpScale(true),
            ),
        )
        layer.addLabel(
            LabelOptions.from(LatLng.from(point.lat, point.lng))
                .setStyles(styles)
                .setClickable(true)
                .setTag(index)
                .setRank(index.toLong()),
        )
    }
    val points = route?.points.orEmpty()
    if (points.size < 2) return
    val routeManager = routeLineManager ?: return
    val style = RouteLineStyle.from(10f, routeColor, 2f, routeStrokeColor)
    val stylesSet = RouteLineStylesSet.from(RouteLineStyles.from(style))
    routeManager.layer?.addRouteLine(
        RouteLineOptions.from(
            RouteLineSegment.from(points.map { LatLng.from(it.lat, it.lng) })
                .setStyles(stylesSet.getStyles(0)),
        ).setStylesSet(stylesSet),
    )
}

private fun Context.registrationPinBitmap(type: RegistrationWaypointType): Bitmap {
    val resource = when (type) {
        RegistrationWaypointType.START -> R.drawable.ic_registration_pin_start
        RegistrationWaypointType.VIA -> R.drawable.ic_registration_pin_via
        RegistrationWaypointType.DESTINATION -> R.drawable.ic_registration_pin_destination
    }
    // setApplyDpScale(true)로 SDK가 자체적으로 밀도 변환을 하므로 비트맵은 dp 값 그대로 생성한다.
    val size = 34
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    ContextCompat.getDrawable(this, resource)?.also { drawable ->
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
    }
    return bitmap
}
