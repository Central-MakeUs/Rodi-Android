package com.dororong.rodi.feature.home.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
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
import android.graphics.Color as AndroidColor

private const val ROUTE_LINE_COLOR = "#5640FF"  // primary600
private const val ROUTE_LINE_STROKE_COLOR = "#2600B1" // primary800 (내곽선)
private const val ROUTE_LINE_WIDTH = 10f
private const val FIT_PADDING_PX = 140

/** 지도에서 코스 관련 레이어(마커·경로선)를 모두 지운다. */
fun KakaoMap.clearCourse() {
    detailLabelLayer()?.removeAll()
    routeLineManager?.layer?.removeAll()
}

fun KakaoMap.renderPlaceCourseMarkers(context: Context, place: PlaceDetail) {
    clearCourse()
    place.course?.waypoints.orEmpty().sortedBy { it.sequence }.forEach { waypoint ->
        val icon = when (waypoint.type) {
            PlaceWaypointType.START -> R.drawable.ic_pin_start
            PlaceWaypointType.VIA -> R.drawable.ic_pin_waypoint
            PlaceWaypointType.DESTINATION -> R.drawable.ic_pin_arrival
        }
        addMarkerAt(
            context = context,
            position = LatLng.from(waypoint.point.lat, waypoint.point.lng),
            iconRes = icon,
            index = waypoint.sequence,
        )
    }
}

fun KakaoMap.renderPlaceCourse(
    context: Context,
    place: PlaceDetail,
    routePoints: List<LatLng>,
    snappedPoints: List<LatLng> = emptyList(),
) {
    clearCourse()
    place.course?.waypoints.orEmpty().sortedBy { it.sequence }.forEachIndexed { index, waypoint ->
        val icon = when (waypoint.type) {
            PlaceWaypointType.START -> R.drawable.ic_pin_start
            PlaceWaypointType.VIA -> R.drawable.ic_pin_waypoint
            PlaceWaypointType.DESTINATION -> R.drawable.ic_pin_arrival
        }
        addMarkerAt(
            context = context,
            position = snappedPoints.getOrNull(index)
                ?: LatLng.from(waypoint.point.lat, waypoint.point.lng),
            iconRes = icon,
            index = waypoint.sequence,
        )
    }
    if (routePoints.size >= 2) drawRouteLine(routePoints)
}

/** [renderCourse]가 그린 경로에 카메라를 맞춘다. */
fun KakaoMap.fitCourseToScreen(routePoints: List<LatLng>) {
    if (routePoints.size >= 2) fitTo(routePoints)
}

private fun KakaoMap.addMarkerAt(context: Context, position: LatLng, iconRes: Int, index: Int) {
    val manager = labelManager ?: return
    val layer = detailLabelLayer() ?: return
    val bitmap = context.vectorToBitmap(iconRes, sizeDp = 34)
    val style = LabelStyle.from(bitmap)
    val styles = manager.addLabelStyles(LabelStyles.from(style))
    val options = LabelOptions.from(position)
        .setStyles(styles)
        .setTag(index)
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

private fun KakaoMap.drawRouteLine(points: List<LatLng>) {
    if (points.size < 2) return
    val manager = routeLineManager ?: return
    val style = RouteLineStyle.from(
        ROUTE_LINE_WIDTH,
        ROUTE_LINE_COLOR.toColorInt(),
        2f,
        ROUTE_LINE_STROKE_COLOR.toColorInt(),
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

/** 단일 지점(주차장 등)을 지정한 줌 레벨로 확대하며 중앙 정렬한다. */
fun KakaoMap.focusOn(position: LatLng, zoomLevel: Int) {
    moveCamera(CameraUpdateFactory.newCenterPosition(position, zoomLevel), CameraAnimation.from(400))
}

private const val CHIP_BG_COLOR = 0xFF7062FF.toInt()
private const val CHIP_TEXT_SIZE_SP = 12f
private const val CHIP_PADDING_H_DP = 10f
private const val CHIP_PADDING_V_DP = 4f

internal fun createCourseChipBitmap(text: String, density: Float): Bitmap {
    val paddingH = (CHIP_PADDING_H_DP * density)
    val paddingV = (CHIP_PADDING_V_DP * density)
    val textSizePx = CHIP_TEXT_SIZE_SP * density

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = textSizePx
        typeface = Typeface.DEFAULT
        color = AndroidColor.WHITE
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
        color = CHIP_BG_COLOR
        style = Paint.Style.FILL
    }
    val radius = height / 2f
    canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), radius, radius, bgPaint)

    val textX = paddingH
    val textY = paddingV - fm.ascent
    canvas.drawText(text, textX, textY, textPaint)

    return bitmap
}
