package com.dororong.rodi.feature.home.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceWaypoint
import com.dororong.rodi.feature.home.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val ROUTE_LINE_WIDTH = 13f
private const val FIT_PADDING_PX = 140
private const val ENDPOINT_OVERLAP_THRESHOLD_METERS = 2.0
private const val OVERLAPPED_START_ANCHOR_X = 0.25f
private const val OVERLAPPED_DESTINATION_ANCHOR_X = 0.75f
private const val CENTER_ANCHOR = 0.5f
private const val PIN_ANCHOR_Y = 1f
private const val EARTH_RADIUS_METERS = 6_371_000.0

data class RouteLineColors(
    @get:ColorInt val lineColor: Int,
    @get:ColorInt val strokeColor: Int,
)

/** 지도에서 코스 관련 레이어(마커·경로선)를 모두 지운다. */
fun KakaoMap.clearCourse() {
    detailLabelLayer()?.removeAll()
    routeLineManager?.layer?.removeAll()
}

fun KakaoMap.renderPlaceCourseMarkers(context: Context, place: PlaceDetail) {
    clearCourse()
    val waypoints = place.course?.waypoints.orEmpty().sortedBy { it.sequence }
    val overlapAnchors = waypoints.overlapAnchors()
    waypoints.forEachIndexed { index, waypoint ->
        addMarkerAt(
            context = context,
            position = LatLng.from(waypoint.point.lat, waypoint.point.lng),
            iconRes = waypointIconRes(index, waypoints.lastIndex),
            index = waypoint.sequence,
            anchorX = overlapAnchors[index],
        )
    }
}

fun KakaoMap.renderPlaceCourse(
    context: Context,
    place: PlaceDetail,
    routePoints: List<LatLng>,
    snappedPoints: List<LatLng> = emptyList(),
    routeLineColors: RouteLineColors,
) {
    clearCourse()
    val waypoints = place.course?.waypoints.orEmpty().sortedBy { it.sequence }
    val overlapAnchors = waypoints.overlapAnchors()
    waypoints.forEachIndexed { index, waypoint ->
        addMarkerAt(
            context = context,
            position = snappedPoints.getOrNull(index)
                ?: LatLng.from(waypoint.point.lat, waypoint.point.lng),
            iconRes = waypointIconRes(index, waypoints.lastIndex),
            index = waypoint.sequence,
            anchorX = overlapAnchors[index],
        )
    }
    if (routePoints.size >= 2) drawRouteLine(routePoints, routeLineColors)
}

/**
 * 좌표가 서로 임계값 이내인 waypoint끼리 묶어, 묶음 안에서만 앵커를 좌우로 고르게 벌린다.
 * 출발·도착만 겹치는 흔한 경우뿐 아니라 경유지가 셋 이상 같은 지점에 몰리는 경우도 다룬다
 * (첫/마지막 인덱스만 보던 이전 버전은 중간 경유지가 겹치면 오프셋을 못 줬다).
 * 반환값은 정렬된 waypoints 리스트 기준 index -> anchorX. 겹치지 않는 waypoint는 키가 없다
 * (= addMarkerAt에서 SDK 기본 앵커를 그대로 쓴다).
 */
private fun List<PlaceWaypoint>.overlapAnchors(): Map<Int, Float> {
    if (size < 2) return emptyMap()
    val anchors = mutableMapOf<Int, Float>()
    val grouped = BooleanArray(size)
    for (i in indices) {
        if (grouped[i]) continue
        val group = mutableListOf(i)
        for (j in i + 1 until size) {
            if (!grouped[j] && this[i].point.distanceMetersTo(this[j].point) <= ENDPOINT_OVERLAP_THRESHOLD_METERS) {
                group += j
            }
        }
        if (group.size < 2) continue
        group.forEach { grouped[it] = true }
        group.forEachIndexed { rank, waypointIndex -> anchors[waypointIndex] = overlapAnchorX(rank, group.size) }
    }
    return anchors
}

private fun overlapAnchorX(rank: Int, groupSize: Int): Float {
    if (groupSize < 2) return CENTER_ANCHOR
    val span = OVERLAPPED_DESTINATION_ANCHOR_X - OVERLAPPED_START_ANCHOR_X
    return OVERLAPPED_START_ANCHOR_X + span * rank / (groupSize - 1)
}

private fun GeoPoint.distanceMetersTo(other: GeoPoint): Double {
    val latitudeDelta = Math.toRadians(other.lat - lat)
    val longitudeDelta = Math.toRadians(other.lng - lng)
    val startLatitude = Math.toRadians(lat)
    val endLatitude = Math.toRadians(other.lat)
    val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(startLatitude) * cos(endLatitude) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    return 2 * EARTH_RADIUS_METERS * atan2(sqrt(haversine), sqrt(1 - haversine))
}

/**
 * 서버 `waypoint.type`을 그대로 신뢰하지 않고 정렬된 순서(첫 번째=출발, 마지막=도착)로
 * 아이콘을 정한다. 순환 코스처럼 출발·도착이 같은 지점인 경우 서버가 두 지점의 type을
 * 요청마다 다르게 줄 가능성이 있어, sequence 순서만으로 결정해야 같은 코스를 다시 열어도
 * 항상 같은 그림이 나온다.
 */
private fun waypointIconRes(index: Int, lastIndex: Int): Int = when {
    index == 0 -> R.drawable.ic_pin_start
    index == lastIndex -> R.drawable.ic_pin_arrival
    else -> R.drawable.ic_pin_waypoint
}

/**
 * [renderCourse]가 그린 경로에 카메라를 맞춘다. topPaddingPx는 지도 위에 떠 있는 검색창(상태
 * 표시줄 포함) 높이다 — 빠지면 세로로 긴 경로의 출발지·도착지 마커가 검색창 뒤로 가려진다.
 */
internal fun KakaoMap.applyMapContentPadding(topPaddingPx: Int, bottomPaddingPx: Int) {
    setPadding(0, topPaddingPx, 0, bottomPaddingPx)
}

fun KakaoMap.fitCourseToScreen(routePoints: List<LatLng>, topPaddingPx: Int, bottomPaddingPx: Int) {
    applyMapContentPadding(topPaddingPx, bottomPaddingPx)
    if (routePoints.size >= 2) fitTo(routePoints)
}

/**
 * [index]는 waypoint.sequence다. rank로도 그대로 쓴다 — 순환 코스처럼 출발·도착 지점이
 * 거의 겹치면 Kakao 지도가 겹친 라벨 중 어느 걸 위에 그릴지 보장하지 않아서, 열 때마다
 * 다른 핀이 보이는 것처럼 깜빡이는 문제가 있었다. sequence가 큰(나중) 지점을 항상 위에
 * 그리게 고정하면 같은 코스는 언제 열어도 같은 그림이 나온다.
 */
private fun KakaoMap.addMarkerAt(
    context: Context,
    position: LatLng,
    iconRes: Int,
    index: Int,
    anchorX: Float?,
) {
    val manager = labelManager ?: return
    val layer = detailLabelLayer() ?: return
    val bitmap = context.vectorToBitmap(iconRes, sizeDp = 34)
    val style = LabelStyle.from(bitmap).apply {
        anchorX?.let { setAnchorPoint(it, PIN_ANCHOR_Y) }
    }
    val styles = manager.addLabelStyles(LabelStyles.from(style))
    val options = LabelOptions.from(position)
        .setStyles(styles)
        .setTag(index)
        .setRank(index.toLong())
    layer.addLabel(options)
}

private fun Context.vectorToBitmap(@DrawableRes iconRes: Int, sizeDp: Int): Bitmap {
    val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
    val drawable = ContextCompat.getDrawable(this, iconRes) ?: return createBitmap(1, 1)
    val bm = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bm)
    drawable.setBounds(0, 0, sizePx, sizePx)
    drawable.draw(canvas)
    return bm
}

private fun KakaoMap.drawRouteLine(points: List<LatLng>, colors: RouteLineColors) {
    if (points.size < 2) return
    val manager = routeLineManager ?: return
    val style = RouteLineStyle.from(
        ROUTE_LINE_WIDTH,
        colors.lineColor,
        2f,
        colors.strokeColor,
    )
    val stylesSet = RouteLineStylesSet.from(RouteLineStyles.from(style))
    val segment = RouteLineSegment.from(points).setStyles(stylesSet.getStyles(0))
    val options = RouteLineOptions.from(segment).setStylesSet(stylesSet)
    manager.layer?.addRouteLine(options)
}

private fun KakaoMap.fitTo(points: List<LatLng>) {
    val pts = points.toTypedArray()
    if (pts.size >= 2) {
        moveCamera(CameraUpdateFactory.fitMapPoints(pts, FIT_PADDING_PX), CameraAnimation.from(400))
    }
}

/** 단일 지점(주차장 등)을 시트가 제외된 지도 가시 영역의 중앙에 맞춘다. */
fun KakaoMap.focusOn(position: LatLng, zoomLevel: Int, bottomPaddingPx: Int) {
    applyMapContentPadding(topPaddingPx = 0, bottomPaddingPx = bottomPaddingPx)
    moveCamera(CameraUpdateFactory.newCenterPosition(position, zoomLevel), CameraAnimation.from(400))
}

private const val CHIP_PADDING_H_DP = 10f
private const val CHIP_PADDING_V_DP = 4f

internal data class MapBitmapTextStyle(
    @param:ColorInt val color: Int,
    val textSizePx: Float,
    val typeface: Typeface,
)

internal data class MapBitmapStyle(
    @param:ColorInt val courseChipBackgroundColor: Int,
    val courseChipText: MapBitmapTextStyle,
    @param:ColorInt val clusterBackgroundColor: Int,
    val clusterText: MapBitmapTextStyle,
    @param:ColorInt val clusterShadowColor: Int,
)

internal fun createCourseChipBitmap(
    text: String,
    density: Float,
    style: MapBitmapStyle,
): Bitmap {
    val paddingH = (CHIP_PADDING_H_DP * density)
    val paddingV = (CHIP_PADDING_V_DP * density)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = style.courseChipText.textSizePx
        typeface = style.courseChipText.typeface
        color = style.courseChipText.color
        textAlign = Paint.Align.LEFT
    }

    val textWidth = textPaint.measureText(text)
    val fm = textPaint.fontMetrics
    val textHeight = fm.descent - fm.ascent

    val width = (textWidth + paddingH * 2).toInt().coerceAtLeast(1)
    val height = (textHeight + paddingV * 2).toInt().coerceAtLeast(1)

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.courseChipBackgroundColor
        this.style = Paint.Style.FILL
    }
    val radius = height / 2f
    canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), radius, radius, bgPaint)

    val textX = paddingH
    val textY = paddingV - fm.ascent
    canvas.drawText(text, textX, textY, textPaint)

    return bitmap
}
