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
import com.dororong.rodi.core.domain.model.course.Course
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

/**
 * 길안내 API 응답 대기 중(직선 좌표조차 아직 확정 전) 출발/경유/도착 마커를 그린다.
 * 도로 경로가 준비되기 전에 직선 미리보기를 그리면 실제 경로로 다시 그려질 때 지도가
 * 두 번 움직이는 것처럼 보이므로, 경로선/카메라 정렬은 [renderCourse]·[fitCourseToScreen]에서
 * 실제 경로가 확보된 뒤에만 수행한다.
 */
fun KakaoMap.renderCourseMarkers(context: Context, course: Course) {
    clearCourse()
    val points = course.allPoints
    points.forEachIndexed { i, p ->
        val isStart = i == 0
        val isEnd = i == points.lastIndex
        val icon = when {
            isStart -> R.drawable.ic_pin_start
            isEnd -> R.drawable.ic_pin_arrival
            else -> R.drawable.ic_pin_waypoint
        }
        addMarkerAt(context, LatLng.from(p.lat, p.lng), icon, i)
    }
}

/**
 * 코스의 출발/경유/도착 마커 + 도로 경로선을 그린다. 실제 경로가 확보된 뒤에만 호출한다.
 * 카메라 정렬은 시트 애니메이션이 끝난 실제 패딩 값을 알아야 하므로 [fitCourseToScreen]으로 분리한다.
 *
 * @param routePoints Directions API 로 받은 도로 경로 좌표 (실제 경로 또는 API 레벨 직선 폴백).
 * @param snappedPoints 각 지점의 도로 스냅 좌표. 있으면 마커를 그 위치에 표시.
 */
fun KakaoMap.renderCourse(
    context: Context,
    course: Course,
    routePoints: List<LatLng>,
    snappedPoints: List<LatLng> = emptyList(),
) {
    clearCourse()
    val points = course.allPoints
    points.forEachIndexed { i, p ->
        val isStart = i == 0
        val isEnd = i == points.lastIndex
        val icon = when {
            isStart -> R.drawable.ic_pin_start
            isEnd -> R.drawable.ic_pin_arrival
            else -> R.drawable.ic_pin_waypoint
        }
        val pos = snappedPoints.getOrNull(i) ?: LatLng.from(p.lat, p.lng)
        addMarkerAt(context, pos, icon, i)
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

// primary500 = #6C5CFF
private const val CHIP_BG_COLOR = 0xFF6C5CFF.toInt()
private const val CHIP_TEXT_SIZE_SP = 12f
private const val CHIP_PADDING_H_DP = 10f
private const val CHIP_PADDING_V_DP = 4f

/**
 * 필터링된 코스 목록의 출발지에 장소 칩(이름 라벨)을 지도에 표시한다.
 * 이전 레이블은 모두 제거된 뒤 다시 그린다.
 * 칩 탭 이벤트는 [KakaoMap.setOnLabelClickListener]로 처리하고,
 * 각 라벨의 tag 에 course.id 를 저장한다.
 */
fun KakaoMap.renderCourseChips(context: Context, courses: List<Course>) {
    clearCourse()
    val density = context.resources.displayMetrics.density
    val parkingBitmap by lazy { context.vectorToBitmap(R.drawable.ic_pin_parking, sizeDp = 34) }
    courses.forEach { course ->
        val bitmap = if (course.isParking) {
            parkingBitmap
        } else {
            createCourseChipBitmap(course.courseNickname, density)
        }
        addChipAt(
            context = context,
            position = LatLng.from(course.startWaypoint.lat, course.startWaypoint.lng),
            bitmap = bitmap,
            tag = course.id,
        )
    }
}

private fun KakaoMap.addChipAt(context: Context, position: LatLng, bitmap: Bitmap, tag: Int) {
    val manager = labelManager ?: return
    val layer = detailLabelLayer() ?: return
    val style = LabelStyle.from(bitmap)
    val styles = manager.addLabelStyles(LabelStyles.from(style))
    val options = LabelOptions.from(position).setStyles(styles).setTag(tag)
    layer.addLabel(options)
}

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
