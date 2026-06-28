package com.cmc.routi.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
import androidx.core.graphics.createBitmap

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
    courses.forEach { course ->
        val bitmap = createChipBitmap(course.courseNickname, density)
        addChipAt(
            context = context,
            position = LatLng.from(course.startWaypoint.lat, course.startWaypoint.lng),
            bitmap = bitmap,
            tag = course.id,
        )
    }
}

private fun KakaoMap.addChipAt(context: Context, position: LatLng, bitmap: Bitmap, tag: String) {
    val manager = labelManager ?: return
    val layer = manager.layer ?: return
    val style = LabelStyle.from(bitmap)
    val styles = manager.addLabelStyles(LabelStyles.from(style))
    val options = LabelOptions.from(position).setStyles(styles).setTag(tag)
    layer.addLabel(options)
}

private fun createChipBitmap(text: String, density: Float): Bitmap {
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
