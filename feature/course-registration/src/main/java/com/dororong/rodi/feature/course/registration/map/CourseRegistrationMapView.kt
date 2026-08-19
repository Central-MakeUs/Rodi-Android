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
import androidx.compose.runtime.key
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
    val currentCenter by rememberUpdatedState(center)
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

    // center는 사용자가 지도를 드래그/핀치할 때마다(onCameraMoveEnd) 바뀌므로 key에 넣지 않는다 —
    // 넣으면 매번 이 effect가 재실행돼 카메라를 level 14로 되돌려서 확대/축소가 즉시 원복돼 버린다.
    // 실제 "프로그래매틱 재중심" 트리거는 centerGeneration 증가뿐이다.
    LaunchedEffect(map, centerGeneration) {
        val target = currentCenter ?: return@LaunchedEffect
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
        // AndroidView의 factory는 최초 컴포지션에서 한 번만 실행된다. retryToken이 바뀌어
        // remember(retryToken)로 새 MapView를 만들어도 key로 감싸지 않으면 옛 뷰가 화면에
        // 그대로 남고 start()도 다시 불리지 않아 "다시 시도"가 동작하지 않는다.
        key(retryToken) {
            AndroidView(
                factory = {
                    mapView.start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {
                                map = null
                            }
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
                LabelStyle.from(context.registrationPinBitmap(waypoint.type))
                    .setApplyDpScale(true)
                    // 기본 앵커는 비트맵 중앙(0.5, 0.5)이라 마커가 목표 좌표보다 아래로 쏠려
                    // 보였다. 리티클(FixedCenterPin)이 핀 끝(하단 중앙)을 좌표에 맞추는 것과
                    // 동일하게 하단 중앙(0.5, 1.0)으로 앵커를 맞춘다.
                    .setAnchorPoint(0.5f, 1f),
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
    // Compose 리티클(34dp)과 실기기 스크린샷을 픽셀 단위로 비교해 보정한 값이다.
    // setApplyDpScale(true)를 켜도 이 SDK의 실제 배율은 (dp 그대로 만든 비트맵 기준)
    // Compose dp 배율의 절반 정도였다 — 34를 그대로 쓰면 마커가 리티클의 절반 크기로 찍혔다.
    val size = 68
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    ContextCompat.getDrawable(this, resource)?.also { drawable ->
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
    }
    return bitmap
}
