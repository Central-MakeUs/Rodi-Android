package com.cmc.routi.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.cmc.routi.R
import com.cmc.routi.model.Course
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import androidx.core.graphics.toColorInt

private const val ROUTE_LINE_COLOR = "#5640FF"  // primary600
private const val ROUTE_LINE_STROKE_COLOR = "#2600B1" // primary800 (내곽선)
private const val ROUTE_LINE_WIDTH = 10f
private const val FIT_PADDING_PX = 140

/** 지도에서 코스 관련 레이어(마커·경로선)를 모두 지운다. */
fun KakaoMap.clearCourse() {
    labelManager?.layer?.removeAll()
    routeLineManager?.layer?.removeAll()
}

/**
 * 코스의 출발/경유/목적 마커 + 경로선을 그리고 카메라를 경로에 맞춘다.
 *
 * @param routePoints Directions API 로 받은 도로 경로 좌표. null/빈값이면 지점 직선으로 폴백.
 * @param snappedPoints 각 지점의 도로 스냅 좌표. 있으면 마커를 그 위치에 표시.
 */
fun KakaoMap.renderCourse(
    context: Context,
    course: Course,
    routePoints: List<LatLng>?,
    snappedPoints: List<LatLng> = emptyList(),
) {
    clearCourse()
    val points = course.allPoints
    points.forEachIndexed { i, p ->
        val isStart = i == 0
        val isEnd = i == points.lastIndex
        if (!isStart && !isEnd) return@forEachIndexed
        val icon = if (isStart) R.drawable.ic_pin_start else R.drawable.ic_pin_arrival
        val pos = snappedPoints.getOrNull(i) ?: LatLng.from(p.lat, p.lng)
        addMarkerAt(context, pos, icon, i)
    }
    val line = routePoints?.takeIf { it.size >= 2 }
        ?: points.map { LatLng.from(it.lat, it.lng) }
    if (line.size >= 2) drawRouteLine(line)
    fitTo(line)
}

private fun KakaoMap.addMarkerAt(context: Context, position: LatLng, iconRes: Int, index: Int) {
    val manager = labelManager ?: return
    val layer = manager.layer ?: return
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
    val drawable = ContextCompat.getDrawable(this, iconRes)
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val bm = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
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
